package com.play.cn.domain;

/**
 * container信息
 */
public class ContainerInfo {

	// "nodeId" : "host.domain.com:8041",
	private String nodeId;

	// "totalMemoryNeededMB" : 2048,
	private Integer totalMemoryNeededMB;

	// "totalVCoresNeeded" : 1,
	private Integer totalVCoresNeeded;

	// "state" : "RUNNING",
	private String state;

	// "diagnostics" : "",
	private String diagnostics;

	// "containerLogsLink" : "http://host.domain.com:8042/node/containerlogs/container_1326121700862_0007_01_000001/user1",
	private String containerLogsLink;

	// "user" : "user1",
	private String user;

	// "id" : "container_1326121700862_0007_01_000001",
	private String id;

	// "exitCode" : -1000,
	private Integer exitCode;

	// "executionType": "GUARANTEED",
	private String executionType;

	public ContainerInfo(String nodeId, Integer totalMemoryNeededMB, Integer totalVCoresNeeded, String state, String diagnostics, String containerLogsLink, String user, String id, Integer exitCode, String executionType) {
		this.nodeId = nodeId;
		this.totalMemoryNeededMB = totalMemoryNeededMB;
		this.totalVCoresNeeded = totalVCoresNeeded;
		this.state = state;
		this.diagnostics = diagnostics;
		this.containerLogsLink = containerLogsLink;
		this.user = user;
		this.id = id;
		this.exitCode = exitCode;
		this.executionType = executionType;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public Integer getTotalMemoryNeededMB() {
		return totalMemoryNeededMB;
	}

	public void setTotalMemoryNeededMB(Integer totalMemoryNeededMB) {
		this.totalMemoryNeededMB = totalMemoryNeededMB;
	}

	public Integer getTotalVCoresNeeded() {
		return totalVCoresNeeded;
	}

	public void setTotalVCoresNeeded(Integer totalVCoresNeeded) {
		this.totalVCoresNeeded = totalVCoresNeeded;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getDiagnostics() {
		return diagnostics;
	}

	public void setDiagnostics(String diagnostics) {
		this.diagnostics = diagnostics;
	}

	public String getContainerLogsLink() {
		return containerLogsLink;
	}

	public void setContainerLogsLink(String containerLogsLink) {
		this.containerLogsLink = containerLogsLink;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getExitCode() {
		return exitCode;
	}

	public void setExitCode(Integer exitCode) {
		this.exitCode = exitCode;
	}

	public String getExecutionType() {
		return executionType;
	}

	public void setExecutionType(String executionType) {
		this.executionType = executionType;
	}
}
