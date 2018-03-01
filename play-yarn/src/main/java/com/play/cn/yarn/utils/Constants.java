package com.play.cn.yarn.utils;

/**
 * Constants.
 */
public final class Constants {
    private Constants() { }
    // 参数 key 和 默认值
    public static final String OPT_APPNAME = "appname";
	public static final String OPT_DEFAULT_APPNAME = "Play_Yarn_Default_Name";

    public static final String OPT_APPTYPE = "apptype";
	public static final String OPT_DEFAULT_APPTYPE = "Play_Yarn_Default_Type";

    public static final String OPT_PRIORITY = "priority";
    public static final String OPT_DEFAULT_PRIORITY = "0";

    public static final String OPT_QUEUE = "queue";
    public static final String OPT_DEFAULT_QUEUE = "root.jdtest";

    public static final String OPT_AM_CORES = "am_cores";
    public static final String OPT_DEFAULT_AM_CORES = "1";

    public static final String OPT_AM_MEMORY = "am_memory";
    public static final String OPT_DEFAULT_AM_MEMORY = "2048";

    public static final String OPT_WORKER_NUM = "worker_num";
    public static final String OPT_DEFAULT_WORKER_NUM = "1";

    public static final String OPT_WORKER_MEMORY = "worker_memory";
    public static final String OPT_DEFAULT_WORKER_MEMROY = "1024";

    public static final String OPT_WORKER_CORES = "worker_cores";
    public static final String OPT_DEFAULT_WORKER_CORES = "1";

	public static final String OPT_SAVE_APPID = "save_appid";
//	public static final String OPT_DEFAULT_SAVE_APPID = "/tempData/";
	public static final String OPT_DEFAULT_SAVE_APPID = "/user/jd_test/tempTestData";

    public static final String OPT_INPUT = "input";
    public static final String OPT_OUTPUT = "output";
    public static final String OPT_CMD = "cmd";
    public static final String OPT_CONF = "conf";
    public static final String OPT_RESOURCE_PATH = "resource_path";
    public static final String OPT_TASK_ROLE = "task_role";
    public static final String OPT_TASK_INDEX = "task_index";
    public static final String OPT_NODE_PORT = "node_port";
    public static final String OPT_NODE_MEMORY = "node_memory";
    public static final String OPT_NODE_CORES = "node_cores";
    public static final String OPT_CLUSTER_ADDR = "cluster_addr";
    public static final String OPT_CLUSTER_PORT = "cluster_port";

    // conf name
//    public static final String CONF_FILE = "yarn-site-local.xml";
//    public static final String CONF_FILE_DEFAULT = "yarn-site-local.xml";

    public static final String CONF_FILE = "yarn-site-35.xml";
    public static final String CONF_FILE_DEFAULT = "yarn-site-35.xml";
    // 从环境变量中获取该key对应的内容
    public static final String TEST_CONF_DIR = "TEST_CONF_DIR";
    public static final String NODE_ROLE_WORKER = "worker";
    public static final String CONF_CORE_HDFS_TMP_DIR = "core.hdfs.tmp.dir";
//    public static final String CONF_CORE_CORE_HDFS_TMP_DIR_DEFAULT = "/tempData/defaultTempDir/";
    public static final String CONF_CORE_CORE_HDFS_TMP_DIR_DEFAULT = "/user/jd_test/defaultTempJarDir";
    public static final String CONF_CORE_AM_JAVA_OPTS = "core.am.java.opts";
    public static final String CONF_CORE_AM_JAVA_OPTS_DEFAULT = "-Xmx2048M";
    public static final String CONF_CORE_WORKER_JAVA_OPTS = "play-yarn.core.worker.java.opts";
    public static final String CONF_CORE_WORKER_JAVA_OPTS_DEFAULT = "-Xmx2048M";
    public static final String TEST_ON_YARN_HOME_JAR_NAME = "PlayHadoop.jar";
}
