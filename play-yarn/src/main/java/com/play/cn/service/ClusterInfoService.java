package com.play.cn.service;

import com.play.cn.domain.ClusterInfo;
import com.play.cn.utils.HttpClientUtils;
import net.sf.json.JSONObject;

public class ClusterInfoService {

	/**
	 * 获取集群基础信息
	 * @param ip
	 * @param port
	 * @return
	 */
	public ClusterInfo getClusterInfo(String ip, String port) {
		String url = "http://" + ip + ":" + port + "/ws/v1/cluster/info";
		String resultStr = HttpClientUtils.get(url, 5000);

		resultStr = "{\n" +
				"  \"clusterInfo\":\n" +
				"  {\n" +
				"    \"id\":1324053971963,\n" +
				"    \"startedOn\":1324053971963,\n" +
				"    \"state\":\"STARTED\",\n" +
				"    \"haState\":\"ACTIVE\",\n" +
				"    \"rmStateStoreName\":\"org.apache.hadoop.yarn.server.resourcemanager.recovery.NullRMStateStore\",\n" +
				"    \"resourceManagerVersion\":\"3.0.0-SNAPSHOT\",\n" +
				"    \"resourceManagerBuildVersion\":\"3.0.0-SNAPSHOT from unknown by user1 source checksum 11111111111111111111111111111111\",\n" +
				"    \"resourceManagerVersionBuiltOn\":\"2016-01-01T01:00Z\",\n" +
				"    \"hadoopVersion\":\"3.0.0-SNAPSHOT\",\n" +
				"    \"hadoopBuildVersion\":\"3.0.0-SNAPSHOT from unknown by user1 source checksum 11111111111111111111111111111111\",\n" +
				"    \"hadoopVersionBuiltOn\":\"2016-01-01T01:00Z\",\n" +
				"    \"haZooKeeperConnectionState\": \"ResourceManager HA is not enabled.\"  }\n" +
				"}";

		JSONObject resultJson = JSONObject.fromObject(resultStr);
		JSONObject clusterInfoJson = resultJson.getJSONObject("clusterInfo");
		ClusterInfo  clusterInfo = new ClusterInfo(clusterInfoJson.getString("id"), clusterInfoJson.getString("startedOn"), clusterInfoJson.getString("state"),
				clusterInfoJson.getString("haState"), clusterInfoJson.getString("rmStateStoreName"),
				clusterInfoJson.getString("resourceManagerVersion"), clusterInfoJson.getString("resourceManagerVersionBuiltOn"),
				clusterInfoJson.getString("hadoopVersion"), clusterInfoJson.getString("hadoopBuildVersion"),
				clusterInfoJson.getString("haZooKeeperConnectionState"));
		return clusterInfo;
	}
}
