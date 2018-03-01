package com.play.cn.utils;

import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * description：HttpClientUtils
 */
public class HttpClientUtils {

    /**
     * post请求
     * @param url         url地址
     * @param jsonParam   json参数
     * @return
     */
    public static String httpPost(String url, JSONObject jsonParam, Integer timeOut){
        HttpPost httpPost = getPost(url, jsonParam, timeOut);
        try {
            return executePost(httpPost, null, null);
        } catch (IOException e) {
            LoggerUtils.error(HttpClientUtils.class, e.getMessage(), e);
            return "";
        }
    }


    /**
     * 使用代理ip请求
     * @param url
     * @param ip
     * @param port
     * @param jsonParam
     * @return
     */
    public static String httpPost(String url, String ip, String port, JSONObject jsonParam, Integer timeOut) throws IOException {
        HttpPost httpPost = getPost(url, jsonParam, timeOut);
        return executePost(httpPost, ip, port);
    }

    /**
     * 使用代理访问
     * @param url
     * @param ip
     * @param port
     * @return
     */
    public static String get(String url, String ip, String port, Integer timeOut) throws IOException {
        HttpGet httpGet = getHttpGet(url);
        httpGet.setConfig(setConfig(timeOut));
        return executeGet(httpGet, ip, port);
    }

    /**
     * 使用本机ip进行访问
     * @param url
     * @return
     */
    public static String get(String url, Integer timeOut) {
        HttpGet httpGet = getHttpGet(url);
        httpGet.setConfig(setConfig(timeOut));
        try {
            return executeGet(httpGet, null, null);
        } catch (IOException e) {
            LoggerUtils.error(HttpClientUtils.class, e.getMessage(), e);
            return "";
        }
    }


    /**
     * 获取httpGet
     * @param url
     * @return
     */
    private static HttpGet getHttpGet(String url) {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "Accept text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        httpGet.setHeader("Accept-Charset", "utf-8");
        httpGet.setHeader("Accept-Encoding", "gzip, deflate");
        httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        httpGet.setHeader("refer", url);
        httpGet.setHeader("Upgrade-Insecure-Requests", "1");
        httpGet.setHeader("Proxy-Connection", "keep-alive");
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.167 Safari/537.36");
        return httpGet;
    }

    /**
     * 执行get请求
     * @param httpRequestBase
     * @param ip
     * @param port
     * @return
     */
    private static String executeGet(HttpRequestBase httpRequestBase, String ip, String port) throws IOException {
        // 创建HttpClientBuilder
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
		CloseableHttpClient closeableHttpClient;
        if (ip != null && port != null) {
            // 依次是代理地址，代理端口号，协议类型
            HttpHost proxy = new HttpHost(ip, Integer.parseInt(port));
            // 创建HttpClient, 并设置代理
            httpClientBuilder.setProxy(proxy);

		}
        closeableHttpClient = httpClientBuilder.build();
        String html = null;
        try {
            HttpResponse response = closeableHttpClient.execute(httpRequestBase);
            // 请求发送成功，并得到响应
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                Header header = entity.getContentEncoding();
                if (header != null) {
                    HeaderElement[] codecs = header.getElements();
                    for (int i = 0; i < codecs.length; i++) {
                        if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                            response.setEntity(new GzipDecompressingEntity(entity));
                        }
                    }
                }
                html = EntityUtils.toString(response.getEntity()).trim();
            }
        } finally {
            try {
                closeableHttpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return html;
    }


    /**
     * 设置请求配置
     * @param timeOut
     * @return
     */
    private static RequestConfig setConfig(Integer timeOut) {
        if (timeOut == null || timeOut == 0)
            timeOut = 10000;
        return RequestConfig.custom().setConnectTimeout(timeOut).setSocketTimeout(timeOut).build();
    }

    /**
     * 获取默认httpPost
     * @param url
     * @param jsonParam
     * @param timeOut
     * @return
     */
    private static HttpPost getPost(String url, JSONObject jsonParam, Integer timeOut) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(setConfig(timeOut));

        if (null != jsonParam) {
            List<NameValuePair> params = new ArrayList();
            Iterator keys = jsonParam.keys();
            while (keys.hasNext()) {
                String str = (String) keys.next();
                params.add(new BasicNameValuePair(str, jsonParam.getString(str)));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(params,Consts.UTF_8));
        }
        return httpPost;
    }


    /**
     * 执行post
     * @param httpPost
     * @param ip
     * @param port
     * @return
     */
    private static String executePost(HttpPost httpPost, String ip, String port) throws IOException {
        // 创建HttpClientBuilder
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        CloseableHttpClient closeableHttpClient;
        if (ip != null && port != null) {
            // 依次是代理地址，代理端口号，协议类型
            HttpHost proxy = new HttpHost(ip, Integer.parseInt(port));
            // 创建HttpClient, 并设置代理
            httpClientBuilder.setProxy(proxy);
        }
        closeableHttpClient = httpClientBuilder.build();

        String resultStr = "";
        HttpResponse httpResponse = closeableHttpClient.execute(httpPost);
        if (httpResponse.getStatusLine().getStatusCode() == 200) {
            try {
                resultStr = EntityUtils.toString(httpResponse.getEntity());
            } catch (Exception e) {
                LoggerUtils.error(HttpClientUtils.class,"post请求提交失败", e);
            }
        }
        return resultStr;
    }
}

