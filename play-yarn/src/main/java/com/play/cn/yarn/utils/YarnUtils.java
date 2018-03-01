package com.play.cn.yarn.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.Apps;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

import java.io.IOException;
import java.util.Map;

/**
 * YarnUtils.
 */
public class YarnUtils {

    private static final Log LOG = LogFactory.getLog(YarnUtils.class);

    /**
     * 根据配置文件创建资源对象
     * @param yarnConf
     * @param appId
     * @param resource
     * @throws IOException
    */
    public static LocalResource createLocalResourceOfFile(Configuration yarnConf, String appId, String resource) throws IOException {
        String dir = Config.getHDFSTmpDir(yarnConf, appId);
        LocalResource localResource = Records.newRecord(LocalResource.class);
        Path resourcePath = new Path(dir.toString(), resource);
        LOG.info("resource path " + resourcePath);
        FileStatus jarStat = FileSystem.get(resourcePath.toUri(), yarnConf).getFileStatus(resourcePath);
        localResource.setResource(ConverterUtils.getYarnUrlFromPath(resourcePath));
        localResource.setSize(jarStat.getLen());
        localResource.setTimestamp(jarStat.getModificationTime());
        localResource.setType(LocalResourceType.FILE);
        localResource.setVisibility(LocalResourceVisibility.APPLICATION);
        return localResource;
    }

    /**
     * 将命令行数组按照制定分隔符整理成字符串
     * @param list
     * @param separator 分隔符
     * @return
    */
    public static String buildCommand(String[] list, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.length; i++) {
            builder.append(list[i]);
            if (i < list.length - 1) {
                builder.append(separator);
            }
        }
        return builder.toString();
    }

    /**
     * 将本地文件拷贝到hdfs
     * @param conf 配置文件
     * @param fs
     * @param appId
     * @param srcPath 源文件
     * @param dstFileName 目的文件名称
     * @return Path
     * @throws IOException
    */
    public static Path copyLocalFileToDfs(Configuration conf, FileSystem fs, String appId, Path srcPath, String dstFileName) throws IOException {
        String dir = Config.getHDFSTmpDir(conf, appId);
        Path dstPath = new Path(dir, dstFileName);
        LOG.info("Copying " + srcPath + " to " + dstPath);
        fs.copyFromLocalFile(srcPath, dstPath);
        return dstPath;
    }

    /**
     * @param conf conf
     * @param fs fs
     * @param appId applicationId
     * @param dstFileName destfilename
     * @return path on hdfs
     * @throws IOException if failed
    */
    public static Path writeConfToDfs(Configuration conf, FileSystem fs, String appId, String dstFileName) throws IOException {
        String dir = Config.getHDFSTmpDir(conf, appId);
        Path dstPath = new Path(dir, dstFileName);
        LOG.info("Write configuration to " + dstPath);
        FSDataOutputStream outputStream = null;
        try {
            outputStream = fs.create(dstPath);
            conf.writeXml(outputStream);
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
        return dstPath;
    }

    /**
     * @param option option
     * @param value value
     * @return string
    */
    public static String mkOption(String option, Object value) {
        return "--" + option + " " + value;
    }


    /**
     * 设置环境变量
    * @param env env
    * @param conf conf
    */
    public static void setJavaEnv(Map<String, String> env, Configuration conf) {
        env.put(ApplicationConstants.Environment.LD_LIBRARY_PATH.name(), ApplicationConstants.Environment.LD_LIBRARY_PATH.$() + ":`pwd`");
        String classpath = ApplicationConstants.Environment.CLASSPATH.name();
        for (String path : conf.getStrings(YarnConfiguration.YARN_APPLICATION_CLASSPATH, YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH)) {
            Apps.addToEnvironment(env, classpath, path.trim(), ApplicationConstants.CLASS_PATH_SEPARATOR);
        }
        Apps.addToEnvironment(env, classpath, ApplicationConstants.Environment.PWD.$() + Path.SEPARATOR, ApplicationConstants.CLASS_PATH_SEPARATOR);
        Apps.addToEnvironment(env, classpath, ApplicationConstants.Environment.PWD.$() + Path.SEPARATOR + Constants.CONF_FILE, ApplicationConstants.CLASS_PATH_SEPARATOR);
        Apps.addToEnvironment(env, classpath, ApplicationConstants.Environment.PWD.$() + Path.SEPARATOR + "*", ApplicationConstants.CLASS_PATH_SEPARATOR);
        Apps.addToEnvironment(env, classpath, ApplicationConstants.Environment.HADOOP_YARN_HOME.$() + "/share/hadoop/hdfs/*", ApplicationConstants.CLASS_PATH_SEPARATOR);
        Apps.addToEnvironment(env, classpath, ApplicationConstants.Environment.HADOOP_YARN_HOME.$() + "/share/hadoop/hdfs/lib/*", ApplicationConstants.CLASS_PATH_SEPARATOR);
        Apps.addToEnvironment(env, classpath, ApplicationConstants.Environment.HADOOP_YARN_HOME.$() + "/etc/hadoop", ApplicationConstants.CLASS_PATH_SEPARATOR);
        Apps.addToEnvironment(env, classpath, "$(" + ApplicationConstants.Environment.HADOOP_YARN_HOME.$() + "/bin/hdfs classpath --glob)");
        Apps.addToEnvironment(env, ApplicationConstants.Environment.LD_LIBRARY_PATH.name(), ApplicationConstants.Environment.HADOOP_YARN_HOME.$() + "/lib/native", ApplicationConstants.CLASS_PATH_SEPARATOR);
        Apps.addToEnvironment(env, ApplicationConstants.Environment.LD_LIBRARY_PATH.name(), ApplicationConstants.Environment.HADOOP_HDFS_HOME.$() + "/lib/native", ApplicationConstants.CLASS_PATH_SEPARATOR);
        Apps.addToEnvironment(env, ApplicationConstants.Environment.LD_LIBRARY_PATH.name(), ApplicationConstants.Environment.HADOOP_CONF_DIR.$() + "/../../lib/native", ApplicationConstants.CLASS_PATH_SEPARATOR);
    }
}
