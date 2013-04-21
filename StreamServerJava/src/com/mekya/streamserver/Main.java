package com.mekya.streamserver;

import java.io.IOException;

import com.mekya.streamserver.servers.HttpServer;
import com.mekya.streamserver.servers.StreamTcpServer;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		StreamTcpServer messenger = new StreamTcpServer(53007);
		
		HttpServer server = new HttpServer(24007);
		server.setStreamer(messenger);
		
		try {
			messenger.start();
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
