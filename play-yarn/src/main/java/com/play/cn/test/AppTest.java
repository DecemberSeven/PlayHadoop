package com.play.cn.test;

import com.play.cn.server.bootPlugin.BootPluginStart;
import com.play.cn.test.TestCommunication;

/**
 * 启动类
 */
public class AppTest {

	private static TestCommunication1 testCommunication1;

	public static void main(String[] args) throws InterruptedException {
		// 启动web服务
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					BootPluginStart.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();

		// 等待10秒，让web先启动
		Thread.sleep(10000);

		// 执行测试程序
//		TestCommunication testCommunication = TestCommunication.getInstance();
//		testCommunication.start();
		testCommunication1 = new TestCommunication1();
		testCommunication1.start();
		System.exit(0);
	}

	public static TestCommunication1 getTestCommunication1() {
		return testCommunication1;
	}
}
