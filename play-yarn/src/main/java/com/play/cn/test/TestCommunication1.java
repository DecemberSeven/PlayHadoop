package com.play.cn.test;


import com.play.cn.domain.ContainerInfo;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 测试程序
 */
public class TestCommunication1 {

    public ConcurrentLinkedQueue<ContainerInfo> getRunningQueue() {
        return runningQueue;
    }

    // 创建线程池
    private ExecutorService exeService = Executors.newCachedThreadPool();

    // 保存正在运行的container
    private ConcurrentLinkedQueue<ContainerInfo> runningQueue = new ConcurrentLinkedQueue<ContainerInfo>();

    public void start() throws InterruptedException {
        init();
        run();
        monitor();
    }

    private void init() {
        String nodeId = "nodeId";
        int totalMemoryNeededMB = 100;
        int totalVCoresNeeded = 10;
        String state = "state";
        String diagnostics = "diagnostics";
        String containerLogsLink = "containerLogsLink";
        String user = "user";
        String id = "id";
        int exitCode = 0;
        String executionType = "executionType";
        for (int i = 0; i < 10; i++) {
            runningQueue.add(new ContainerInfo(nodeId + i, totalMemoryNeededMB + i, totalVCoresNeeded + i,
                    state + i, diagnostics + i, containerLogsLink + i, user + i, id + i,
                    exitCode + i, executionType + i));
        }
    }


    private void run() {
        for (int i = 0; i < runningQueue.size(); i++) {
            exeService.submit(new RunningTask(runningQueue));
        }
    }

    private void monitor() throws InterruptedException {
        boolean flag = true;
		while (flag) {
            Thread.sleep((int)(Math.random()*5) * 1000);
            int size = runningQueue.size();
            if(size > 0)
                System.out.println("队列剩余数量。。。。。" + size);
            else {
                System.out.println("执行完毕");
                flag = false;
            }
        }
    }


    private class RunningTask implements Runnable {
        private ConcurrentLinkedQueue<ContainerInfo> runningQueue = null;
        RunningTask(ConcurrentLinkedQueue runningQueue) {
            this.runningQueue = runningQueue;
        }

        @Override
        public void run() {
            try {
				Thread.sleep((int)(Math.random()*30)*1000);
                synchronized (TestCommunication1.class) {
                    ContainerInfo containerInfo = runningQueue.remove();
                    System.out.println(containerInfo.getNodeId() + "--被移除");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
