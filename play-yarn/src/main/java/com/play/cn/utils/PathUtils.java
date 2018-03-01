package com.play.cn.utils;

import java.io.File;


public class PathUtils {
	private static String webRootPath;
	private static String rootClassPath;

	/**
	 *  获取相对路径，例如   ..\path\abc.txt
	 * @param clazz
	 * @return
	 */
	public static String getPath(Class clazz) {
		String path = clazz.getResource("").getPath();
		return new File(path).getAbsolutePath();
	}

	/**
	 *  获取相对路径，例如   ..\path\abc.txt
	 * @param object
	 * @return
	 */
	public static String getPath(Object object) {
		String path = object.getClass().getResource("").getPath();
		return new File(path).getAbsolutePath();
	}

	// 注意：命令行返回的是命令行所在的当前路径
	public static String getRootClassPath() {
		if (rootClassPath == null) {
			try {
				String path = getClassLoader().getResource("").toURI().getPath();
				rootClassPath = new File(path).getAbsolutePath();
			} catch (Exception e) {
				String path = getClassLoader().getResource("").getPath();
				rootClassPath = new File(path).getAbsolutePath();
			}
		}
		return rootClassPath;
	}

	/**
	 * 优先使用 current thread 所使用的 ClassLoader 去获取路径
	 * 否则在某些情况下会获取到 tomcat 的 ClassLoader，那么路径值将是 TOMCAT_HOME/lib
	 */
	private static ClassLoader getClassLoader() {
		ClassLoader ret = Thread.currentThread().getContextClassLoader();
		return ret != null ? ret : PathUtils.class.getClassLoader();
	}

	public static void setRootClassPath(String rootClassPath) {
		PathUtils.rootClassPath = rootClassPath;
	}

	public static String getWebRootPath() {
		if (webRootPath == null) {
			webRootPath = detectWebRootPath();
		}
		return webRootPath;
	}

	public static void setWebRootPath(String webRootPath) {
		if (webRootPath == null) {
			return ;
		}
		if (webRootPath.endsWith(File.separator)) {
			webRootPath = webRootPath.substring(0, webRootPath.length() - 1);
		}
		PathUtils.webRootPath = webRootPath;
	}

	// 注意：命令行返回的是命令行所在路径的上层的上层路径
	private static String detectWebRootPath() {
		try {
			String path = PathUtils.class.getResource("/").toURI().getPath();
			return new File(path).getParentFile().getParentFile().getCanonicalPath();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
