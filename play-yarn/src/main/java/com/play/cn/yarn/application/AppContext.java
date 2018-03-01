package com.play.cn.yarn.application;

import java.io.Serializable;

/**
 * 运行的Application的信息
 */
public class AppContext implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * worker数
     */
    private int workerNum;

    /**
     * worker核心数
     */
    private int workerCores;

    /**
     * worker内存
     */
    private int workerMemory;

    /**
     * 命令
     */
    private String cmd;

    /**
     * 输入路径
     */
    private String input;

    /**
     * 输出路径
     */
    private String output;

    /**
     * 资源文件路径
     */
    private String resourcePath;

    public AppContext() {
        cmd = "";
        input = "";
        output = "";
        resourcePath = "";
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public int getWorkerNum() {
        return workerNum;
    }

    public void setWorkerNum(int workerNum) {
        this.workerNum = workerNum;
    }

    public int getWorkerCores() {
        return workerCores;
    }

    public void setWorkerCores(int workerCores) {
        this.workerCores = workerCores;
    }

    public int getWorkerMemory() {
        return workerMemory;
    }

    public void setWorkerMemory(int workerMemory) {
        this.workerMemory = workerMemory;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    /**
     * toString.
     * @return object string
     */
    @Override
    public String toString() {
        return "AppContext{"
            + ", workerNum=" + workerNum
            + ", workerCores=" + workerCores
            + ", workerMemory=" + workerMemory
            + ", cmd='" + cmd + '\''
            + ", input='" + input + '\''
            + ", output='" + output + '\''
            + ", resourcePath='" + resourcePath + '\''
            + '}';
    }
}
