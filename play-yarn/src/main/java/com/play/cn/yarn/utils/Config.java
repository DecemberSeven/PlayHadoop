package com.play.cn.yarn.utils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;

/**
 * 配置文件工具类
 */
public final class Config {
    private static final Logger LOG = LoggerFactory.getLogger(Config.class);
    private static Configuration conf;

    private Config() {

    }


    /**
     * @return conf
     */
    public static Configuration getConf() {
        if (conf == null) {
          conf = loadConf();
        }
        return conf;
    }

    /**
     * 加载配置文件
     * @return
     */
    private static Configuration loadConf() {
        Configuration conf = new YarnConfiguration();
        // 先加载当前项目中的默认配置文件
        conf.addResource(Constants.CONF_FILE_DEFAULT);
        // 再加载当前项目中的新增配置文件
        conf.addResource(Constants.CONF_FILE);
        // 获取配置文件路径
        String confDir = System.getenv(Constants.TEST_CONF_DIR);
        if (confDir != null) { // 如果confDir 不为空，则继续依次加载外部配置文件（默认、新增）
            conf.addResource(new Path(confDir + Constants.CONF_FILE_DEFAULT));
            conf.addResource(new Path(confDir + Constants.CONF_FILE));
        }
        return conf;
    }

    /**
     * @param path
     * @param confMap
     * @throws IOException
     */
    public static void writeConfig(String path, Map<String, String> confMap) throws Exception {
        Configuration targetConf = new Configuration();
        for (Map.Entry<String, String> entry : confMap.entrySet()) {
            targetConf.set(entry.getKey(), entry.getValue());
        }

        Path confPath = new Path(path);
        LOG.info("Generate {} ...." + confPath.toString());

        DataOutputStream out = null;
        try {
            FileContext lfs = FileContext.getLocalFSFileContext();
            out = lfs.create(confPath, EnumSet.of(org.apache.hadoop.fs.CreateFlag.CREATE, org.apache.hadoop.fs.CreateFlag.OVERWRITE));
            targetConf.writeXml(out);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Generate {} failed, ", confPath.toString(), e);
            throw e;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * @param key
     * @return value
     */
    public static String getEnv(String key) {
        String env = getEnv(key, null);
        if (env == null) {
            throw new IllegalArgumentException("Can't get [ " + key + " ] from env. please check it!");
        }
        return env;
    }

    /**
     * @param key
     * @param def 默认值
     * @return string
     */
    public static String getEnv(String key, String def) {
        String env = System.getenv(key);
        if (env == null) {
            env = def;
        }
        return env;
    }

    /**
     * 获取hdfs临时文件目录
     * @param conf
     * @param appId
     * @return string
     * @throws IOException
     */
    public static String getHDFSTmpDir(Configuration conf, String appId) throws IOException {
        return getHDFSDirPath(conf, appId, Constants.CONF_CORE_HDFS_TMP_DIR, Constants.CONF_CORE_CORE_HDFS_TMP_DIR_DEFAULT);
    }

    /**
     * 获取历史log日志文件夹
     * @param conf
     * @param appId
     * @return log dir
     * @throws IOException
     */
    public static String getHDFSHistoryLogDir(Configuration conf, String appId) throws IOException {
        return getHDFSDirPath(conf, appId, Constants.CONF_CORE_HDFS_TMP_DIR, Constants.CONF_CORE_CORE_HDFS_TMP_DIR_DEFAULT);
    }

    /**
     * 获取hdfs文件夹路径
     * @param conf
     * @param appId
     * @param key
     * @param defvalue
     * @return
     * @throws IOException
     */
    private static String getHDFSDirPath(Configuration conf, String appId, String key, String defvalue) throws IOException {
        String dir = conf.get(key, defvalue);
        Path path = new Path(dir + Path.SEPARATOR + appId);
        FsPermission dirPerm = new FsPermission(FsAction.ALL, FsAction.ALL, FsAction.ALL);
        FileSystem fs = FileSystem.get(path.toUri(), conf);
        if (fs.exists(path)) {
        if (!fs.isDirectory(path)) {
            throw new IOException("The tmp directory does not exist. path: " + path.toUri());
        }
        } else {
        if (!fs.mkdirs(path, dirPerm)) {
            throw new IOException("Create directory failed. path: " + path.toUri());
        }
            fs.setPermission(path, dirPerm);
        }
        String result = fs.getUri().toString() + path.toString();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Conf: " + key + "=" + result);
        }
        return result;
    }
}
