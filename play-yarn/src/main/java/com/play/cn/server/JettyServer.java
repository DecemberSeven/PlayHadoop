package com.play.cn.server;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import com.play.cn.utils.FileUtils;
import com.play.cn.utils.LoggerUtils;
import com.play.cn.utils.PathUtils;
import com.play.cn.utils.StringUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.webapp.WebAppContext;
public class JettyServer {

	private String webAppDir;
	private int port;
	private String context;
	private boolean running = false;
	private Server server;
	private WebAppContext webApp;

	public JettyServer(String webAppDir, int port, String context) {
		if (webAppDir == null) {
			throw new IllegalStateException("Invalid webAppDir of web server: " + webAppDir);
		}
		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("Invalid port of web server: " + port);
		}
		if (StringUtils.isBlank(context)) {
			throw new IllegalStateException("Invalid context of web server: " + context);
		}

		this.webAppDir = webAppDir;
		this.port = port;
		this.context = context;
	}

	public void start() {
		if (!running) {
			try {
				running = true;
				doStart();
			} catch (Exception e) {
				System.err.println(e.getMessage());
				LoggerUtils.error(this.getClass(), e.getMessage());
			}
		}
	}

	public void stop() {
		if (running) {
			try {server.stop();} catch (Exception e) {
				LoggerUtils.error(this.getClass(), e.getMessage());
			}
			running = false;
		}
	}

	private void doStart() throws IOException {
		if (!available(port)) {
			throw new IllegalStateException("port: " + port + " already in use!");
		}

		deleteSessionData();

		server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);
		webApp = new WebAppContext();
		webApp.setThrowUnavailableOnStartupException(true);	// 在启动过程中允许抛出异常终止启动并退出 JVM
		webApp.setContextPath(context);
		webApp.setResourceBase(webAppDir);	// webApp.setWar(webAppDir);
		webApp.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
		webApp.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");	// webApp.setInitParams(Collections.singletonMap("org.mortbay.jetty.servlet.Default.useFileMappedBuffer", "false"));
		persistSession(webApp);

		server.setHandler(webApp);
		try {
			System.out.println("Starting web server on port: " + port);
			server.start();
			System.out.println("Starting Complete. )");
			server.join();
		} catch (Exception e) {
			LoggerUtils.error(this.getClass(), e.getMessage());
			System.exit(100);
		}
		return;
	}

	private void deleteSessionData() {
		try {
			FileUtils.delete(new File(getStoreDir()));
		}
		catch (Exception e) {
			LoggerUtils.error(this.getClass(), e.getMessage());
		}
	}

	private String getStoreDir() {
		String storeDir = PathUtils.getWebRootPath() + "/../../session_data" + context;
		if ("\\".equals(File.separator)) {
			storeDir = storeDir.replaceAll("/", "\\\\");
		}
		return storeDir;
	}

	private void persistSession(WebAppContext webApp) throws IOException {
		String storeDir = getStoreDir();

		SessionManager sm = webApp.getSessionHandler().getSessionManager();
		if (sm instanceof HashSessionManager) {
			((HashSessionManager)sm).setStoreDirectory(new File(storeDir));
			return ;
		}

		HashSessionManager hsm = new HashSessionManager();
		hsm.setStoreDirectory(new File(storeDir));
		SessionHandler sh = new SessionHandler();
		sh.setSessionManager(hsm);
		webApp.setSessionHandler(sh);
	}

	private static boolean available(int port) {
		if (port <= 0) {
			throw new IllegalArgumentException("Invalid start port: " + port);
		}

		ServerSocket ss = null;
		DatagramSocket ds = null;
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			ds = new DatagramSocket(port);
			ds.setReuseAddress(true);
			return true;
		} catch (IOException e) {
			LoggerUtils.error(JettyServer.class, e.getMessage());
		} finally {
			if (ds != null) {
				ds.close();
			}

			if (ss != null) {
				try {
					ss.close();
				} catch (IOException e) {
					// should not be thrown, just detect port available.
					LoggerUtils.error(JettyServer.class, e.getMessage());
				}
			}
		}
		return false;
	}
}
