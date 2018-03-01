package com.play.cn.yarn.client;

import com.google.common.base.Preconditions;
import com.play.cn.App;
import com.play.cn.yarn.utils.ArgsParser;
import com.play.cn.yarn.utils.Config;
import com.play.cn.yarn.utils.Constants;
import com.play.cn.yarn.utils.YarnUtils;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.hadoop.util.ClassUtil;
import org.apache.hadoop.util.ShutdownHookManager;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.ClientRMProxy;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class Client {

    private static final Log LOG = LogFactory.getLog(Client.class);

    public static void main(String[] args) throws Exception {
        Client client = new Client();
        client.run(args);
    }

    // 停止任务的线程的优先级
    public static final int SHUTDOWN_HOOK_PRIORITY = 29;

    // yarn配置
    private Configuration yarnConfiguration;

    // yarn客户端
    private YarnClient yarnClient;

    // ApplicationMaster需将该任务运行环境（包含运行命令、环境变量、依赖的外部文件等）连同Container中的资源信息封装到ContainerLaunchContext对象中
    // 每个ContainerLaunchContext和对应的Container信息（被封装到了ContainerToken中）将再次被封装到StartContainerRequest中，也就是说，
    // ApplicationMaster最终发送给NodeManager的是StartContainerRequest，每个StartContainerRequest对应一个Container和任务
    private ContainerLaunchContext containerLaunchContext;

    // 提交只是往中央异步处理器加入RMAppEventType.START事件，异步处理，之后不等待处理结果，直接返回个简单的respone
    private ApplicationSubmissionContext appContext;

    // 应用名称
    private String appName;

    // 应用类型
    private String appType;

    private int amPriority;

    // 使用队列
    private String amQueue;

    // 应用对应的id
    private ApplicationId appId;

    // AM对应的内存大小（由输入和配置两方面决定）
    private int amMemoryMB;

    // AM 分配cpu个数
    private int amVCores;

    // work 数量
    private int numWorkers;

    // work 分配的cpu个数
    private int workerCores;

    // work 分配的内存
    private int workerMemoryMB;

    // 输入路径
    private String inputPath;

    // 输出路径
    private String outputPath;

    // 命令
    private String cmd;

    // appId 存放路径
    private String saveAppidPath;

    // 格式化模板
    private static final String YARN_NOT_ENOUGH_RESOURCES = "%s %s specified above max threshold of cluster, specified=%s, max=%s";

    // ApplicationMaster 的jar文件路径
    private String appMasterJar;


    /**
     * 启动入口
     * @param args
     * @throws Exception
     */
    public void run(String[] args) throws Exception {
        yarnConfiguration = Config.getConf();
        if (!parseArgs(args)) {
            System.exit(1);
        }
        this.submitApplication();
    }

    /**
     * 提交应用
     * @throws YarnException
     * @throws IOException
     * @throws InterruptedException
     */
    private void submitApplication() throws YarnException, IOException, InterruptedException {
        // 1. 创建 yarnClient，并进行初始化
        yarnClient = YarnClient.createYarnClient();
        yarnClient.init(yarnConfiguration);
        yarnClient.start();

        // 2. 创建 application
        YarnClientApplication app = yarnClient.createApplication();
        // 设置Applicaton 的 内存 和 cpu 需求 以及 优先级和 Queue 信息， YARN将根据这些信息来调度App Master
        GetNewApplicationResponse appResponse = app.getNewApplicationResponse();

        appId = appResponse.getApplicationId();
        // 3. 检查集群资源
        checkClusterResource(appResponse);

        // 4. 创建并配置ContainerLaunchContext
        containerLaunchContext = Records.newRecord(ContainerLaunchContext.class);
        setupAMContainerLaunchContext();

        // 5.  设置提交环境参数
        appContext = app.getApplicationSubmissionContext();
        setupApplicationSubmissionContext();


        // 6. 添加状态监听，当任务运行完毕，启动该线程停止任务
//        appId = appContext.getApplicationId();
        ShutdownHookManager.get().addShutdownHook(new ClientShutdownHook(appId, yarnClient), SHUTDOWN_HOOK_PRIORITY);

        LOG.info("提交应用id " + appId + " 到 ResourceManager");
        // 7. 提交
        yarnClient.submitApplication(appContext);
        monitorApplication();
    }

    /**
     * 解析参数
     * @param args
     * @return
     * @throws Exception
     */
    private boolean parseArgs(String[] args) throws Exception {
        Options option = makeOptions();
        if (args.length <= 0) {
            ArgsParser.printUsage(option, "客户端选项");
            return false;
        }

        option.addOption("help", false, "打印参数选项");
        Preconditions.checkArgument(args.length > 0, "没有输入任何参数");
        ArgsParser cliParser = new ArgsParser(option, args);
        LOG.info("提交的参数 :" + cliParser.toString());
        if (cliParser.hasOption("help")) {
            ArgsParser.printUsage(option, "客户端选项");
            return false;
        }
        appName = cliParser.getOptionValue(Constants.OPT_APPNAME, Constants.OPT_DEFAULT_APPNAME);
        appType = cliParser.getOptionValue(Constants.OPT_APPTYPE, Constants.OPT_DEFAULT_APPTYPE);
        cmd = cliParser.getOption(Constants.OPT_CMD);

        amPriority = Integer.parseInt(cliParser.getOptionValue(Constants.OPT_PRIORITY, Constants.OPT_DEFAULT_PRIORITY));
        amQueue = cliParser.getOptionValue(Constants.OPT_QUEUE, Constants.OPT_DEFAULT_QUEUE);

//        amQueue = Constants.OPT_DEFAULT_QUEUE;
//        if (cliParser.hasOption(Constants.OPT_QUEUE)) {
//
//        } else {
//            String tQueue = "root.jdtest";
//            if (tQueue != null) {
//                amQueue = tQueue;
//            }
//        }
        amMemoryMB = Integer.parseInt(cliParser.getOptionValue(Constants.OPT_AM_MEMORY, Constants.OPT_DEFAULT_AM_MEMORY));
        amVCores = Integer.parseInt(cliParser.getOptionValue(Constants.OPT_AM_CORES, Constants.OPT_DEFAULT_AM_CORES));
        inputPath = cliParser.getOption(Constants.OPT_INPUT);
        outputPath = cliParser.getOption(Constants.OPT_OUTPUT);
        numWorkers = Integer.parseInt(cliParser.getOptionValue(Constants.OPT_WORKER_NUM, Constants.OPT_DEFAULT_WORKER_NUM));
        workerCores = Integer.parseInt(cliParser.getOptionValue(Constants.OPT_WORKER_CORES, Constants.OPT_DEFAULT_WORKER_CORES));
        workerMemoryMB = Integer.parseInt(cliParser.getOptionValue(Constants.OPT_WORKER_MEMORY, Constants.OPT_DEFAULT_WORKER_MEMROY));
        saveAppidPath = cliParser.getOptionValue(Constants.OPT_SAVE_APPID, Constants.OPT_DEFAULT_SAVE_APPID);
        Preconditions.checkArgument(cmd != null, "未输入相关命令：" + cmd);
        Preconditions.checkArgument(inputPath != null, "未输入数据输入路径：" + inputPath);
        Preconditions.checkArgument(outputPath != null, "未输入数据输出路径：" + outputPath);
        Preconditions.checkArgument(amMemoryMB > 0, "AM内存大小配置不合法：" + amMemoryMB);
        Preconditions.checkArgument(amVCores > 0, "AM的Cores配置不合法：" + amVCores);
        // 检查输入路径、输出路径
        handleInputAndOutputPath(inputPath, outputPath);

        Object[] array = cliParser.getOptionValues(Constants.OPT_CONF);
        handleInputCmd(array);
        LOG.info("客户端参数: " + getParams());
        return true;
    }


    /**
     * 处理输入、输入路径
     * @param inputPath
     * @param outputPath
     * @throws ParseException
     */
    private void handleInputAndOutputPath(String inputPath, String outputPath) throws ParseException {
        try {
            Path outPath = new Path(outputPath);
            FileSystem fs = FileSystem.get(outPath.toUri(), yarnConfiguration);
            // 多个路径，用","分割
            String[] inputs = inputPath.split(",");
            for (int i = 0; i < inputs.length; i++) {
                Path inPath = new Path(inputs[i]);
                if (!fs.exists(inPath)) {
                    throw new ParseException("输入路径存在: " + inputPath);
                }
            }
            fs.mkdirs(outPath);
        } catch (Exception e) {
            LOG.error("输入路径不合法, " + e.getMessage());
            throw new ParseException("输入路径不合法, " + e.getMessage());
        }
    }

    /**
     * 处理输入命令参数
     * @param array
     */
    private void handleInputCmd(Object[] array) {
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                String[] keyval = ((String) array[i]).split("=", 2);
                if (keyval.length == 2) {
                    yarnConfiguration.set(keyval[0], keyval[1], "来源于命令行");
                    LOG.info("添加key:" + keyval[0] + ", value:" + keyval[1] + "到conf中");
                }
            }
        }
    }


    /**
     * 检查集群资源
     * @param appResponse
     */
    private void checkClusterResource(GetNewApplicationResponse appResponse) {
        Resource clusterMax = appResponse.getMaximumResourceCapability();
        int maxMem = clusterMax.getMemory();
        int maxVCores = clusterMax.getVirtualCores();

        if (amMemoryMB > maxMem) {
            throw new RuntimeException(String.format(YARN_NOT_ENOUGH_RESOURCES, "ApplicationMaster", "memory", amMemoryMB, maxMem));
        }

        if (amVCores > maxVCores) {
            throw new RuntimeException(String.format(YARN_NOT_ENOUGH_RESOURCES, "ApplicationMaster", "virtual cores", amVCores, maxVCores));
        }
    }

    /**
     * 设置ContainerLaunchContext.
     *
     * @throws IOException
     * @throws YarnException
     */
    private void setupAMContainerLaunchContext() throws IOException, YarnException {
        // 设置执行命令
        String amCommand = makeAppMasterCommand();
        LOG.info("ApplicationMaster command: " + amCommand);
        containerLaunchContext.setCommands(Collections.singletonList(amCommand));

        // Setup local resources
        copyLocalFileToDFS();

        // Setup CLASSPATH for ApplicationMaster
        Map<String, String> appMasterEnv = new HashMap<String, String>();
        setupAppMasterEnv(appMasterEnv);
        containerLaunchContext.setEnvironment(appMasterEnv);

        // 判断是否设置了权限
        if (UserGroupInformation.isSecurityEnabled()) {
            Credentials credentials = new Credentials();
            String tokenRenewer = yarnConfiguration.get(YarnConfiguration.RM_PRINCIPAL);
            if (tokenRenewer == null || tokenRenewer.length() == 0) {
                throw new IOException("Can't get Master Kerberos principal for the RM to use as renewer");
            }
            FileSystem fs = FileSystem.get(yarnConfiguration);
            // getting tokens for the default file-system.
            final Token<?>[] tokens = fs.addDelegationTokens(tokenRenewer, credentials);
            if (tokens != null) {
                for (Token<?> token : tokens) {
                    LOG.info("Got dt for " + fs.getUri() + "; " + token);
                }
            }
            // getting yarn resource manager token
            Configuration config = yarnClient.getConfig();
            Token<TokenIdentifier> token = ConverterUtils.convertFromYarn(
                    yarnClient.getRMDelegationToken(new org.apache.hadoop.io.Text(tokenRenewer)),
                    ClientRMProxy.getRMDelegationTokenService(config));
            LOG.info("Added RM delegation token: " + token);
            credentials.addToken(token.getService(), token);
            DataOutputBuffer dob = new DataOutputBuffer();
            credentials.writeTokenStorageToStream(dob);
            ByteBuffer buffer = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());
            containerLaunchContext.setTokens(buffer);
        }
    }

    /**
     * 设置执行命令
     * @return
     * @throws IOException
     */
    private String makeAppMasterCommand() throws IOException {
        String opts = yarnConfiguration.get(Constants.CONF_CORE_AM_JAVA_OPTS, Constants.CONF_CORE_AM_JAVA_OPTS_DEFAULT);
        String stdout = "1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout";
        String stderr = "2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr";
        String[] commands = new String[]{
                ApplicationConstants.Environment.JAVA_HOME.$$() + "/bin/java", opts,
                // Set class name
                App.class.getName(),
                // worker info
                YarnUtils.mkOption(Constants.OPT_WORKER_NUM, this.numWorkers),
                YarnUtils.mkOption(Constants.OPT_WORKER_CORES, this.workerCores),
                YarnUtils.mkOption(Constants.OPT_WORKER_MEMORY, this.workerMemoryMB),
                // app info
                YarnUtils.mkOption(Constants.OPT_CMD, this.cmd),
                YarnUtils.mkOption(Constants.OPT_INPUT, this.inputPath),
                YarnUtils.mkOption(Constants.OPT_OUTPUT, this.outputPath),

                YarnUtils.mkOption(Constants.OPT_RESOURCE_PATH, Config.getHDFSTmpDir(yarnConfiguration, this.appId.toString())), stdout, stderr};
        return YarnUtils.buildCommand(commands, " ");
    }

    /**
     * 拷贝文件到HDFS
     * @throws IOException
     */
    private void copyLocalFileToDFS() throws IOException {
        this.appMasterJar = ClassUtil.findContainingJar(App.class);
        File appMasterJarFile = new File(appMasterJar);
        String path = appMasterJarFile.getPath();
        LOG.info("Jar original path is ..." + path);
        Set<Path> localFiles = new HashSet();
        // 上传jar
        Path tfJar = new Path(path);
        localFiles.add(tfJar);
        Map<String, LocalResource> localResources = new HashMap<String, LocalResource>();
        FileSystem fs = FileSystem.get(yarnConfiguration);
        // upload file and set local resource
        for (Path resource : localFiles) {
            YarnUtils.copyLocalFileToDfs(yarnConfiguration, fs, appId.toString(), resource, resource.getName());
            localResources.put(resource.getName(), YarnUtils.createLocalResourceOfFile(yarnConfiguration, appId.toString(), resource.getName()));
        }
        // conf
        Path dstConfPath = YarnUtils.writeConfToDfs(yarnConfiguration, fs, appId.toString(), Constants.CONF_FILE);
        localResources.put(dstConfPath.getName(), YarnUtils.createLocalResourceOfFile(yarnConfiguration, appId.toString(), dstConfPath.getName()));
        containerLaunchContext.setLocalResources(localResources);
    }

    /**
     * 设置AppMaster环境变量
     * @param appMasterEnv
     * @throws IOException
     */
    private void setupAppMasterEnv(Map<String, String> appMasterEnv) throws IOException {
        YarnUtils.setJavaEnv(appMasterEnv, yarnConfiguration);
    }

    /**
     * 设置Application提交参数
     */
    private void setupApplicationSubmissionContext() {
        appContext.setApplicationName(appName);                                     // BootPluginStart name
        appContext.setResource(Resource.newInstance(amMemoryMB, amVCores));         // mem , vcore
        appContext.setQueue(amQueue);                                               // queue
        appContext.setAMContainerSpec(containerLaunchContext);                      // ContainerLaunchContext
        appContext.setPriority(Priority.newInstance(amPriority));                   // priority
        appContext.setApplicationType(appType);
    }

    /**
     * 写出appId
     * @param path
     * @param appId
     * @throws IOException
     */
    private void serializeAppId(String path, String appId) throws IOException {
        DataOutputStream out = null;
        try {
            FileContext lfs = FileContext.getLocalFSFileContext();
            out = lfs.create(new Path(path), EnumSet.of(org.apache.hadoop.fs.CreateFlag.CREATE,
                    org.apache.hadoop.fs.CreateFlag.OVERWRITE));
            out.writeUTF(appId);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Monitor Application Status, such running, finished, killed or failed.
     * 打印状态信息
     */
    private void monitorApplication() throws YarnException, IOException, InterruptedException {
        while (true) {
            Thread.sleep(5000);
            ApplicationReport report = yarnClient.getApplicationReport(appId);
            YarnApplicationState state = report.getYarnApplicationState();
            FinalApplicationStatus dsStatus = report.getFinalApplicationStatus();
            if (saveAppidPath != null && !StringUtils.isEmpty(saveAppidPath)) {
                try {
                    serializeAppId(saveAppidPath, appId.toString());
                    LOG.info("正在序列化" + saveAppidPath + "----" + appId.toString());
                } catch (IOException e) {
                    LOG.info("Serialize AppId to " + saveAppidPath + " failed,  error: " + e);
                }
            }
            switch (state) {
                case ACCEPTED:
                    LOG.info("\nApplication Details: "
                            + "\n ApplicationID: " + report.getApplicationId()
                            + "\n Tracking url is: " + report.getTrackingUrl()
                            + "\n Status: " + state
                            + "\n Queue: " + report.getQueue()
                            + "\n Host: " + report.getHost());
                    break;
                case RUNNING:
                    Thread.sleep(1000);
                    break;
                case FINISHED:
                    if (FinalApplicationStatus.SUCCEEDED == dsStatus) {
                        LOG.info("Application has completed successfully");
                    } else {
                        LOG.info("Application finished unsuccessfully. YarnState="
                                + state.toString() + ", DSFinalStatus="
                                + dsStatus.toString() + ", " + report.getDiagnostics());
                    }
                    return;
                case KILLED: // intended to fall through
                    LOG.info("Application has been killed by user");
                    return;
                case FAILED:
                    LOG.info("Application did not finish. YarnState=" + state.toString()
                            + ", DSFinalStatus=" + dsStatus.toString() + ", " + report.getDiagnostics());
                    return;
                default:
                    LOG.info("未知状态。。。。。。。" + state);
            }
        }
    }

    /**
     * 创建参数
     * @return
     */
    private Options makeOptions() {
        Options option = new Options();
        option.addOption(Constants.OPT_APPNAME, true, "应用名称.");
        option.addOption(Constants.OPT_CMD, true, " 命令");
        option.addOption(Constants.OPT_PRIORITY, true, "应用优先级，默认 0");
        option.addOption(Constants.OPT_QUEUE, false, "RM Queue 所使用的队列，默认是root.jdtest");
        option.addOption(Constants.OPT_AM_MEMORY, true, "运行ApplicationMaster所分配的内存，默认是 2048M");
        option.addOption(Constants.OPT_AM_CORES, true, "运行ApplicationMaster所分配的cpu个数，默认是 1");
        option.addOption(Constants.OPT_INPUT, true, "文件输入路径，多个文件用英文都好分割");
        option.addOption(Constants.OPT_OUTPUT, true, "文件输出路径");
        option.addOption(Constants.OPT_WORKER_NUM, true, "worker个数，默认是 1");
        option.addOption(Constants.OPT_WORKER_MEMORY, true, "worker运行内存大小，默认是1024M");
        option.addOption(Constants.OPT_WORKER_CORES, true, "worker运行分配的cpu个数，默认是 1");
        option.addOption(Constants.OPT_CONF, false, "其它配置参数");
        option.addOption(Constants.OPT_SAVE_APPID, false, "appid存放路径");
        return option;
    }

    /**
     * 获取各个参数字符串
     * @return
     */
    private String getParams() {
        return "Client{"
            + "appName='" + appName + '\''
            + ", amPriority=" + amPriority
            + ", amQueue='" + amQueue + '\''
            + ", amMemoryMB=" + amMemoryMB
            + ", amVCores=" + amVCores
            + ", numWorkers=" + numWorkers
            + ", workerCores=" + workerCores
            + ", workerMemoryMB=" + workerMemoryMB
            + ", inputPath='" + inputPath + '\''
            + ", outputPath='" + outputPath + '\''
            + '}';
    }

    /**
     * 关闭资源
     */
    static class ClientShutdownHook implements Runnable {
        private ApplicationId appId;
        private YarnClient client;
        ClientShutdownHook(ApplicationId appId, YarnClient client) {
            this.appId = appId;
            this.client = client;
        }
        public void run() {
            LOG.info("Client received a signal. send signal to ResourceManager kill " + appId);
            try {
                this.client.killApplication(appId);
                if (this.client != null) {
                    this.client.stop();
                }
            } catch (Exception e) {
                LOG.error("Kill application " + appId + " failed, cause " + e.getCause());
            }
        }
    }
}
