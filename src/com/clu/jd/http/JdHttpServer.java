package com.clu.jd.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.clu.jd.JDMain;

public class JdHttpServer {

	/**
	 * WEB_ROOT是HTML和其它文件存放的目录. 这里的WEB_ROOT为工作目录下的webroot目录
	 */
	//	public static final String		WEB_ROOT			= System.getProperty("user.dir") + File.separator + "src";

	/**
	 * 生命周期：多久时间没有请求就关闭服务，单位：毫秒
	 */
	private static final long		LIFE_TIME			= 300 * 1000;

	public static final String		ENCODING			= "UTF-8";

	private static volatile boolean	isRunning			= false;

	/**
	 * 最后一次请求时间
	 */
	private static volatile long	LAST_REQUEST_TIME	= System.currentTimeMillis();

	private static int				PORT				= 9989;

	public static void main(String[] args) {
		if (args != null && args.length > 0) {
			String portString = args[0];
			try {
				PORT = Integer.parseInt(portString);
			} catch (Exception e) {
			}
		}
		JdHttpServer server = new JdHttpServer();
		//等待连接请求
		server.waitRequest();
	}

	private ServerSocket	serverSocket;

	private JdHttpServer() {
		this.startServer();
	}

	private void startServer() {
		try {
			//服务器套接字对象
			this.serverSocket = new ServerSocket(JdHttpServer.PORT, 50, InetAddress.getByName("127.0.0.1"));
			Logger.info("JdHttpServer start up, bind port: " + JdHttpServer.PORT);
			startAutoExitThread();
			Worker.startWorkers();
		} catch (IOException e) {
			Logger.error(e.getMessage(), e);
			this.stopServer();
		}
	}

	private void stopServer() {
		System.exit(1);
	}

	private void startAutoExitThread() {
		new Thread() {
			@Override
			public void run() {
				while (System.currentTimeMillis() - LAST_REQUEST_TIME < LIFE_TIME) {
					try {
						Thread.sleep(LIFE_TIME);
					} catch (Exception e) {
						// ignore
					}
				}
				Logger.info("Auto Exit!");
				Worker.syncStopWorker();
				System.exit(0);
			}
		}.start();
	}

	public void waitRequest() {
		while (isRunning) {
			try {
				Socket socket = serverSocket.accept();
				LAST_REQUEST_TIME = System.currentTimeMillis();
				Worker.process(socket);
			} catch (Exception e) {
				Logger.error(e.getMessage(), e);
			}
		}
	}

	static class Worker extends Thread {

		private static final BlockingQueue<Socket>	REQUEST_QUEUE	= new LinkedBlockingQueue<Socket>();

		private static final int					WORKER_COUNT	= 5;

		private static final List<Worker>			WORKERS			= new ArrayList<>();

		static void startWorkers() {
			isRunning = true;
			for (int i = 0; i < WORKER_COUNT; i++) {
				Worker worker = new Worker();
				WORKERS.add(worker);
				worker.start();
			}
		}

		/**
		 * 同步停止 
		 * @since 1.0.0
		 */
		static void syncStopWorker() {
			isRunning = false;
			for (Worker worker : WORKERS) {
				try {
					worker.interrupt();
					worker.join();
				} catch (InterruptedException e) {
					// ignore
				}
			}
			Logger.info("Server Stopped!");
		}

		static void process(Socket socket) {
			if (!isRunning) {
				throw new IllegalStateException("Worker not started yet!");
			}
			if (socket == null || socket.isClosed()) {
				throw new RuntimeException("socket is null or closed!");
			}

			try {
				REQUEST_QUEUE.put(socket);
			} catch (InterruptedException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		private Socket				socket;

		private static final String	URL_PREFIX	= "/?s=";

		@Override
		public void run() {
			this.setName("Worker-" + this.getName());
			Logger.info(this.getName() + " started!");
			while (isRunning) {
				try {
					this.socket = REQUEST_QUEUE.take();
					//long startTime = System.currentTimeMillis();
					InputStream input = socket.getInputStream();
					OutputStream output = socket.getOutputStream();

					// 创建Request对象并解析
					Request request = new Request(input);
					Logger.info("process request: " + request.getUri());
					if (request.getUri() != null && request.getUri().startsWith(URL_PREFIX)) {
						String filePath = request.getUri().substring(URL_PREFIX.length());
						try {
							filePath = URLDecoder.decode(filePath, JdHttpServer.ENCODING);
						} catch (Exception e) {
						}

						// 创建 Response 对象
						Response response = new Response(output);
						// response.sendStaticResponse(request.getUri());
						String source = JDMain.decompile(filePath, filePath);
						response.sendText(source);
					}
					// Logger.info("cost " + (System.currentTimeMillis() - startTime));
				} catch (Exception e) {
					if (isRunning) {
						Logger.error(e.getMessage(), e);
					}
				} finally {
					if (this.socket != null) {
						try {
							this.socket.close();
						} catch (IOException e) {
							// ignore
						}
					}
				}
			}
			Logger.info(this.getName() + " stopped!");
		}
	}
}