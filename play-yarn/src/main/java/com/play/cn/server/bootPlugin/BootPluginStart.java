package com.play.cn.server.bootPlugin;

import cn.hutool.core.util.ZipUtil;
import com.play.cn.server.JettyServer;
import com.play.cn.utils.PathUtils;
import com.play.cn.utils.StringUtils;

/**
 * maven中使用boot-plugin 插件打jar，需要使用下面方法进行启动server
 */
public class BootPluginStart {

	public static void start() {
		String baseBath = String.valueOf(BootPluginStart.class.getProtectionDomain().getCodeSource().getLocation()).split("!")[0];
		System.out.println("basePath: " + baseBath);
		String classPath, webRootPath, jarPath;
		int port = 8080;
		String contextPath = "/";
		String prefix = "jar:file:/";
		if (StringUtils.notBlank(baseBath) && baseBath.contains(prefix)) {
			// 获取运行操作系统的运行方式  window和linux的细微区别
			System.out.println(System.getProperties().getProperty("os.name"));
			jarPath = "/" + baseBath.substring(prefix.length());
			classPath = jarPath.substring(0, jarPath.lastIndexOf("/")) + "/class-path";
			System.out.println("jarPath:" + jarPath);
			System.out.println("classPath:" + classPath);
			webRootPath = classPath + "/BOOT-INF/classes";
			ZipUtil.unzip(jarPath, classPath);
			// 这两步是核心指定 webapp目录和classpath目录
			PathUtils.setWebRootPath(webRootPath);
			PathUtils.setRootClassPath(classPath);
			// eclipse 启动是4个参数
			JettyServer jettyServer = new JettyServer(webRootPath, port, contextPath);
			jettyServer.start();
		} else {
			throw new RuntimeException("路径不对!");
		}
	}
}
