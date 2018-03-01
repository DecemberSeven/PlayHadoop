package com.play.cn.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class FileUtils {
	/**
	 * 删除文件
	 * @param file
	 */
	public static void delete(File file) {
		if (file != null && file.exists()) {
			if (file.isFile()) {
				file.delete();
			}
			else if (file.isDirectory()) {
				File files[] = file.listFiles();
				if (files != null) {
					for (int i=0; i<files.length; i++) {
						delete(files[i]);
					}
				}
				file.delete();
			}
		}
	}

	/**
	 * 获取文件扩展名
	 * @param fileFullName
	 * @return
	 */
	public static String getFileExtension(String fileFullName) {
		if (StringUtils.isBlank(fileFullName)) {
			throw new RuntimeException("fileFullName is empty");
		}
		return  getFileExtension(new File(fileFullName));
	}

	public static String getFileExtension(File file) {
		if (null == file) {
			throw new NullPointerException();
		}
		String fileName = file.getName();
		int dotIdx = fileName.lastIndexOf('.');
		return (dotIdx == -1) ? "" : fileName.substring(dotIdx + 1);
	}

	public static File file(String path) {
		if (StringUtils.isBlank(path)) {
			throw new NullPointerException("File path is blank!");
		} else {
			return new File(getAbsolutePath(path));
		}
	}
	
	public static String getAbsolutePath(String path) {
		return getAbsolutePath(path, (Class)null);
	}

	public static String getAbsolutePath(String path, Class<?> baseClass) {
		return "";
	}

	public static File touch(String fullFilePath) throws Exception {
		return fullFilePath == null ? null : touch(file(fullFilePath));
	}

	public static File touch(File file) throws Exception {
		if (null == file) {
			return null;
		} else {
			if (!file.exists()) {
				mkParentDirs(file);

				try {
					file.createNewFile();
				} catch (Exception var2) {
					throw new Exception(var2);
				}
			}

			return file;
		}
	}

	public static File mkParentDirs(File file) {
		File parentFile = file.getParentFile();
		if (null != parentFile && !parentFile.exists()) {
			parentFile.mkdirs();
		}

		return parentFile;
	}
}
