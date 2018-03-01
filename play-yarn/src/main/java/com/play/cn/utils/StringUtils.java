package com.play.cn.utils;

public class StringUtils {

	/**
	 * 字符串为 null 或者内部字符全部为 ' ' '\t' '\n' '\r' 这四类字符时返回 true
	 */
	public static boolean isBlank(String str) {
		if (str == null) {
			return true;
		}
		int len = str.length();
		if (len == 0) {
			return true;
		}
		for (int i = 0; i < len; i++) {
			switch (str.charAt(i)) {
				case ' ':
				case '\t':
				case '\n':
				case '\r':
					break;
				default:
					return false;
			}
		}
		return true;
	}

	public static boolean notBlank(String str) {
		return !isBlank(str);
	}
}
