package com.play.cn.yarn.application;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.play.cn.yarn.utils.ArgsParser;
import com.play.cn.yarn.utils.Config;
import com.play.cn.yarn.utils.Constants;
import com.play.cn.yarn.utils.YarnUtils;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync;
import org.apache.hadoop.yarn.client.api.async.impl.NMClientAsyncImpl;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.security.AMRMTokenIdentifier;
import org.apache.hadoop.yarn.util.ConverterUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ApplicationMaster {
    private static final Log LOG = LogFactory.getLog(ApplicationMaster.class);

    // 运行的application中的内容
    private AppContext appContext;
    private String hostname;

    private String userName = "";
    private String jobUserName = "";

    // 认证内容（如果服集群开启了相关认证）
    private ByteBuffer mAllTokens = null;
    private Credentials credentials = null;

    // ApplicationAttemptId
    private ApplicationAttemptId appAttemptId = null;

    // rmClientAsync与RM交互
    private AMRMClientAsync rmClientAsync = null;

    // noClientAsync与NM交互
    private NMClientAsyncImpl nmClientAsync = null;

    // 总的container的数量
    private AtomicInteger numTotalContainers = new AtomicInteger(10);

    // 已完成的container的数量
    private AtomicInteger numCompletedConatiners = new AtomicInteger(0);

    // 创建线程池
    private ExecutorService exeService = Executors.newCachedThreadPool();

    // 保存正在运行的container
    private Map<ContainerId, Container> runningContainers = new ConcurrentHashMap<ContainerId, Container>();

    // 配置
    private Configuration conf = null;

    /**
     * 真正处理数据的是由App Mstr 由nmClientAsync.startContainerAsync(container, ctx)提交的 Container application,
     * 然后这这个应用并不需要特殊编写，任何程序通过提交相应的运行信息都可以在这些Node中的某个Container 中执行，
     * 所以这个程序可以是一个复杂的MapReduce  Task 或者 是一个简单的脚本
     */
//    public static void main(String[] args) throws Exception {
//
//    }

    public ApplicationMaster() throws IOException {
        this.userName = UserGroupInformation.getCurrentUser().getShortUserName();
        this.jobUserName = System.getenv(ApplicationConstants.Environment.USER.name());
        this.credentials = UserGroupInformation.getCurrentUser().getCredentials();
        this.hostname = System.getenv(ApplicationConstants.Environment.NM_HOST.name());
    }


    /**
     * 执行
     * @param args
     * @throws Exception
     */
    public void run(String[] args) throws Exception {
        String containerIdStr = System.getenv(ApplicationConstants.Environment.CONTAINER_ID.name());
        LOG.info("containerIdStr........is ........." + containerIdStr);

        ContainerId containerId = ConverterUtils.toContainerId(containerIdStr);
        appAttemptId = containerId.getApplicationAttemptId();

        // 判断是否开启了权限验证
        if (UserGroupInformation.isSecurityEnabled()) {
            credentials = UserGroupInformation.getCurrentUser().getCredentials();
            DataOutputBuffer credentialsBuffer = new DataOutputBuffer();
            credentials.writeTokenStorageToStream(credentialsBuffer);
            // Now remove the AM -> RM token so that containers cannot access it.
            Iterator<Token<?>> iter = credentials.getAllTokens().iterator();
            while (iter.hasNext()) {
                Token<?> token = iter.next();
                if (token.getKind().equals(AMRMTokenIdentifier.KIND_NAME)) {
                    iter.remove();
                }
            }
            mAllTokens = ByteBuffer.wrap(credentialsBuffer.getData(), 0, credentialsBuffer.getLength());
        }

        // 获取配置文件（包括输入参数）
        conf = getConf(args);

        // 1. 创建 rmClientAsync，负责与Resource Manager 交互
        rmClientAsync = AMRMClientAsync.createAMRMClientAsync(1000, new RMCallbackHandler(conf));
        rmClientAsync.init(conf);
        rmClientAsync.start();

        // 2. 创建 nmClientAsync，这个对象负责与Node Manager 交互
        nmClientAsync = new NMClientAsyncImpl(new NMCallbackHandler());
        nmClientAsync.init(conf);
        nmClientAsync.start();

        // 3. 负责与 Node Manager 交互，注册
        RegisterApplicationMasterResponse response = rmClientAsync.registerApplicationMaster(
        																	NetUtils.getHostname(), -1, "");
        // 4. 申请 containers
        for (int i = 0; i < numTotalContainers.get(); i++) {
        	// 设置需要申请的container相关信息
            ContainerRequest containerAsk = new ContainerRequest(Resource.newInstance(appContext.getWorkerMemory(),
														appContext.getWorkerCores()), null, null, Priority.newInstance(0));
			// 向RM申请 Container
            rmClientAsync.addContainerRequest(containerAsk);
            Thread.sleep(100);
        }
    }

    /**
     * 等待Container 执行完毕，清理退出
     * @throws YarnException
     * @throws IOException
     */
	public void waitComplete() throws YarnException, IOException {
        while(numTotalContainers.get() != numCompletedConatiners.get()){
            try{
                Thread.sleep(1000);
                LOG.info("waitComplete" + ", numTotalContainers=" + numTotalContainers.get() + ", numCompletedConatiners=" + numCompletedConatiners.get());
            } catch (InterruptedException ex){}
        }
        exeService.shutdown();
        nmClientAsync.stop();
        // 解除注册
        rmClientAsync.unregisterApplicationMaster(FinalApplicationStatus.SUCCEEDED, "dummy Message", null);
        rmClientAsync.stop();
    }


    /**
     * 获取配置文件
     * @param args
     * @return
     * @throws ParseException
     */
    private Configuration getConf(String[] args) throws ParseException {
        Configuration conf = Config.getConf();
        if (!parseArgs(args)) {
            System.exit(1);
        }
        return conf;
    }


    /**
     * 解析参数
     * @param args args
	 * @return
     * @throws ParseException if failed
     */
    private boolean parseArgs(String[] args) throws ParseException {
        Options options = makeOptions();
        if (args.length <= 0) {
            ArgsParser.printUsage(options, "ApplicationMaster 相关参数选项");
            return false;
        }

        Preconditions.checkArgument(args.length > 0, "客户端没有提交任何参数");
        ArgsParser cliParser = new ArgsParser(options, args);
        if (cliParser.hasOption("help")) {
            ArgsParser.printUsage(options, "ApplicationMaster 相关参数选项");
            return false;
        }

        LOG.info("\nAppMaster submit args: \n " + cliParser.toString());
        appContext = new AppContext();
        appContext.setCmd(cliParser.getOption(Constants.OPT_CMD));
        appContext.setInput(cliParser.getOption(Constants.OPT_INPUT));
        appContext.setOutput(cliParser.getOption(Constants.OPT_OUTPUT));
        appContext.setWorkerNum(Integer.parseInt(cliParser.getOptionValue(Constants.OPT_WORKER_NUM, Constants.OPT_DEFAULT_WORKER_NUM)));
        appContext.setWorkerCores(Integer.parseInt(cliParser.getOptionValue(Constants.OPT_WORKER_CORES, Constants.OPT_DEFAULT_WORKER_CORES)));
        appContext.setWorkerMemory(Integer.parseInt(cliParser.getOptionValue(Constants.OPT_WORKER_MEMORY, Constants.OPT_DEFAULT_WORKER_MEMROY)));
        appContext.setResourcePath(cliParser.getOption(Constants.OPT_RESOURCE_PATH));
        LOG.info(appContext.toString());
        validate(appContext);
        return true;
    }

    /**
     * 验证参数
     * @param appContext
     */
    private void validate(AppContext appContext) {
        Preconditions.checkArgument(appContext.getWorkerNum() > 0, "workerNum 不合法：" + appContext.getWorkerNum());
        Preconditions.checkArgument(appContext.getWorkerCores() > 0, "mWorkerCores 不合法：" + appContext.getWorkerCores());
        Preconditions.checkArgument(appContext.getWorkerMemory() > 0, "mWorkerMEM 不合法：" + appContext.getWorkerMemory());
        Preconditions.checkArgument(appContext.getCmd() != null, "cmd 不合法：" + appContext.getCmd());
        Preconditions.checkArgument(appContext.getInput() != null, "inputPath 不合法：" + appContext.getInput());
        Preconditions.checkArgument(appContext.getOutput() != null, "outputPath 不合法：" + appContext.getOutput());
        Preconditions.checkArgument(appContext.getResourcePath() != null, "resource_path 不合法：" + appContext.getResourcePath());
    }

    /**
     * 创建option
     * @return
     */
    private Options makeOptions() {
        Options options = new Options();
        options.addOption(Constants.OPT_CMD, true, " cmd");
        options.addOption(Constants.OPT_INPUT, true, "input file path");
        options.addOption(Constants.OPT_OUTPUT, true, "output file path");
        options.addOption(Constants.OPT_WORKER_NUM, true, "worker number, Default 1");
        options.addOption(Constants.OPT_WORKER_MEMORY, true, "worker memory, Default 1024M");
        options.addOption(Constants.OPT_WORKER_CORES, true, "worker cores. Default 1");
        options.addOption(Constants.OPT_RESOURCE_PATH, true, "resource path. Default false");
        options.addOption("help", false, "Print usage");
        return options;
    }


    private class LaunchContainerTask implements Runnable {
        Container container;
        Configuration conf;
        int index;
        public LaunchContainerTask(Container container, Configuration conf, int index) {
            this.container = container;
            this.conf = conf;
            this.index = index;
        }
        public void run() {
            String[] commands = getCommands();
            LOG.info("commands info is ......****....." + YarnUtils.buildCommand(commands, " "));
            ContainerLaunchContext ctx = ContainerLaunchContext.newInstance(makeLocalResources(), getEnv(),
                    Lists.newArrayList(YarnUtils.buildCommand(commands, " ")), null, null, null);
            nmClientAsync.startContainerAsync(container, ctx);
        }

        /**
         * 获取命令参数
         * @return
         */
        private String[] getCommands() {
            String opts =  conf.get(Constants.CONF_CORE_WORKER_JAVA_OPTS, Constants.CONF_CORE_WORKER_JAVA_OPTS_DEFAULT);
            String[] commands = new String[] {
                    ApplicationConstants.Environment.JAVA_HOME.$$() + "/bin/java", opts,
                    YarnUtils.mkOption(Constants.OPT_CMD, appContext.getCmd()),
                    YarnUtils.mkOption(Constants.OPT_INPUT,appContext.getInput()),
                    YarnUtils.mkOption(Constants.OPT_OUTPUT, appContext.getOutput()),
                    YarnUtils.mkOption(Constants.OPT_NODE_PORT, 0),
                    YarnUtils.mkOption(Constants.OPT_NODE_CORES, appContext.getWorkerCores()),
                    YarnUtils.mkOption(Constants.OPT_NODE_MEMORY, appContext.getWorkerMemory()),
                    YarnUtils.mkOption(Constants.OPT_CLUSTER_ADDR, hostname),
                    YarnUtils.mkOption(Constants.OPT_CLUSTER_PORT, (10000 + ((int) (Math.random() * (5000)) + 1))),
                    YarnUtils.mkOption(Constants.OPT_TASK_ROLE, Constants.NODE_ROLE_WORKER),
                    YarnUtils.mkOption(Constants.OPT_TASK_INDEX, index),
                    new String("1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/" + ApplicationConstants.STDOUT),
                    new String("2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/" + ApplicationConstants.STDERR)
//                    ";java -cp HadoopTest.jar com.test.yarnDemo.TestCommunication  ",
//                    new String("1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/" + ApplicationConstants.STDOUT),
//                    new String("2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/" + ApplicationConstants.STDERR)
            };
            return commands;
        }


        /**
         * 获取环境信息
         * @return
         */
        private Map<String, String> getEnv() {
            Map<String, String> env = new HashMap();
            YarnUtils.setJavaEnv(env, conf);
            return env;
        }


    }

    /**
     * makeLocalResources.
     * @return local resource
     * @throws IOException if read file failed
     */
    public Map<String, LocalResource> makeLocalResources() {
        Set<String> resourceName = new HashSet();
        resourceName.add(Constants.TEST_ON_YARN_HOME_JAR_NAME);
        resourceName.add(Constants.CONF_FILE);
        try {
            Map<String, LocalResource> localResources = new HashMap<String, LocalResource>();
            for (String res : resourceName) {
                LOG.info("Local resource: " + res);
                localResources.put(res, YarnUtils.createLocalResourceOfFile(conf, appAttemptId.getApplicationId().toString(), res));
            }
            return localResources;
        } catch (Exception e) {
            LOG.error(e);
            return null;
        }
    }

    /**
     * 其功能是处理由Resource Manager收到的消息,
     */
    private class RMCallbackHandler implements AMRMClientAsync.CallbackHandler {
        private Configuration conf;
        public RMCallbackHandler(Configuration conf) {
            this.conf = conf;
        }

        // 当有Container完成时方法被调用，完成的Container会以参数的方式传入。可以在这个方法中实现Container完成后的操作
        public void onContainersCompleted(List<ContainerStatus> statuses) {
            for (ContainerStatus status : statuses) {
                LOG.info("Container Completed: " + status.getContainerId().toString() + " exitStatus="+ status.getExitStatus());
                if (status.getExitStatus() != 0) {
                    // restart
                }
                ContainerId id = status.getContainerId();
                // 将执行完成的container从集合中移除
                runningContainers.remove(id);
                numCompletedConatiners.addAndGet(1);
            }
        }
        // 申请Container成功之后被调用，申请成功的Container会以参数方式传入。在这个方法中我们要实现启动Worker操作
        public void onContainersAllocated(List<Container> containers) {
            for (int i = 0; i < containers.size(); i++) {
                Container c = containers.get(i);
                LOG.info("Container Allocated" + ", id=" + c.getId() + ", containerNode=" + c.getNodeId());
                exeService.submit(new LaunchContainerTask(c, conf, i));
                runningContainers.put(c.getId(), c);
            }
        }

        //  如果AM接收到了关闭请求，则此方法被调用。我们可以在这里实现AM关闭时的清理操作。
        public void onShutdownRequest() {
            LOG.info("onShutdownRequest .........");
        }

        // 如果运行过程中发现有些节点的状态改变了，此方法会被调用。在这里可以实现Node状态改变之后的处理逻辑
        public void onNodesUpdated(List<NodeReport> updatedNodes) {
            LOG.info("onNodesUpdated ........." + updatedNodes.toString());
        }

        // 获取APP进度
        public float getProgress() {
            LOG.info("getProgress >>>>>>>");
            float progress = 0;
            return progress;
        }

        public void onError(Throwable e) {
            LOG.info("onError .........");
            rmClientAsync.stop();
        }

    }

    private class NMCallbackHandler implements NMClientAsync.CallbackHandler {
        public void onContainerStarted(ContainerId containerId, Map<String, ByteBuffer> allServiceResponse) {
            LOG.info("Container onContainerStarted " + containerId.toString() + ">>>>>>>>>>>>>>>>>");
        }

        public void onContainerStatusReceived(ContainerId containerId, ContainerStatus containerStatus) {
            LOG.info("Container onContainerStatusReceived " + containerId.toString() + ">>>>>>>>>>>>>>>>>");
        }

        public void onContainerStopped(ContainerId containerId) {
            LOG.info("Container onContainerStopped " + containerId.toString() + ">>>>>>>>>>>>>>>>>");
        }

        public void onStartContainerError(ContainerId containerId, Throwable t) {
            LOG.info("Container onStartContainerError " + containerId.toString() + ">>>>>>>>>>>>>>>>>");
        }

        public void onGetContainerStatusError(ContainerId containerId, Throwable t) {
            LOG.info("Container onGetContainerStatusError " + containerId.toString() + ">>>>>>>>>>>>>>>>>");
        }

        public void onStopContainerError(ContainerId containerId, Throwable t) {
            LOG.info("Container onStopContainerError " + containerId.toString() + ">>>>>>>>>>>>>>>>>");
        }
    }


	public AppContext getAppContext() {
		return appContext;
	}

	public String getHostname() {
		return hostname;
	}

	public String getUserName() {
		return userName;
	}

	public String getJobUserName() {
		return jobUserName;
	}

	public ByteBuffer getmAllTokens() {
		return mAllTokens;
	}

	public Credentials getCredentials() {
		return credentials;
	}

	public ApplicationAttemptId getAppAttemptId() {
		return appAttemptId;
	}

	public AMRMClientAsync getRmClientAsync() {
		return rmClientAsync;
	}

	public NMClientAsyncImpl getNmClientAsync() {
		return nmClientAsync;
	}

	public AtomicInteger getNumTotalContainers() {
		return numTotalContainers;
	}

	public AtomicInteger getNumCompletedConatiners() {
		return numCompletedConatiners;
	}

	public ExecutorService getExeService() {
		return exeService;
	}

	public Map<ContainerId, Container> getRunningContainers() {
		return runningContainers;
	}

	public Configuration getConf() {
		return conf;
	}
}
