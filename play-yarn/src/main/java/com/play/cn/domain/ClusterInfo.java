package com.play.cn.domain;

public class ClusterInfo {

	private String id;
	private String startedOn;
	private String state;
	private String haState;
	private String rmStateStoreName;
	private String resourceManagerVersion;
	private String resourceManagerVersionBuiltOn;
	private String hadoopVersion;
	private String hadoopBuildVersion;
	private String haZooKeeperConnectionState;

	public ClusterInfo() {
	}

	public ClusterInfo(String id, String startedOn, String state, String haState, String rmStateStoreName, String resourceManagerVersion, String resourceManagerVersionBuiltOn, String hadoopVersion, String hadoopBuildVersion, String haZooKeeperConnectionState) {
		this.id = id;
		this.startedOn = startedOn;
		this.state = state;
		this.haState = haState;
		this.rmStateStoreName = rmStateStoreName;
		this.resourceManagerVersion = resourceManagerVersion;
		this.resourceManagerVersionBuiltOn = resourceManagerVersionBuiltOn;
		this.hadoopVersion = hadoopVersion;
		this.hadoopBuildVersion = hadoopBuildVersion;
		this.haZooKeeperConnectionState = haZooKeeperConnectionState;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(String startedOn) {
		this.startedOn = startedOn;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getHaState() {
		return haState;
	}

	public void setHaState(String haState) {
		this.haState = haState;
	}

	public String getRmStateStoreName() {
		return rmStateStoreName;
	}

	public void setRmStateStoreName(String rmStateStoreName) {
		this.rmStateStoreName = rmStateStoreName;
	}

	public String getResourceManagerVersion() {
		return resourceManagerVersion;
	}

	public void setResourceManagerVersion(String resourceManagerVersion) {
		this.resourceManagerVersion = resourceManagerVersion;
	}

	public String getResourceManagerVersionBuiltOn() {
		return resourceManagerVersionBuiltOn;
	}

	public void setResourceManagerVersionBuiltOn(String resourceManagerVersionBuiltOn) {
		this.resourceManagerVersionBuiltOn = resourceManagerVersionBuiltOn;
	}

	public String getHadoopVersion() {
		return hadoopVersion;
	}

	public void setHadoopVersion(String hadoopVersion) {
		this.hadoopVersion = hadoopVersion;
	}

	public String getHadoopBuildVersion() {
		return hadoopBuildVersion;
	}

	public void setHadoopBuildVersion(String hadoopBuildVersion) {
		this.hadoopBuildVersion = hadoopBuildVersion;
	}

	public String getHaZooKeeperConnectionState() {
		return haZooKeeperConnectionState;
	}

	public void setHaZooKeeperConnectionState(String haZooKeeperConnectionState) {
		this.haZooKeeperConnectionState = haZooKeeperConnectionState;
	}
}
