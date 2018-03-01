package com.play.cn;

import com.play.cn.server.bootPlugin.BootPluginStart;
import com.play.cn.test.TestCommunication;
import com.play.cn.yarn.application.ApplicationMaster;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * 启动类
 */
public class App {
	private static ApplicationMaster am;

	public static void main(String[] args) throws Exception {
		am = new ApplicationMaster();
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

		am.run(args);
		am.waitComplete();
		System.exit(0);
	}

	public static ApplicationMaster getAm() {
		return am;
	}
}
