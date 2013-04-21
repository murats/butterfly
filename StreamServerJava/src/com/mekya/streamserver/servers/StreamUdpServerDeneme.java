package com.mekya.streamserver.servers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class StreamUdpServerDeneme {

	protected static final int PORT = 50021;
	private static StreamUdpServerDeneme udpServer;
	protected DatagramSocket datagramServerSocket;
	private Thread listenThread;

	public StreamUdpServerDeneme() {

	}


	public static StreamUdpServerDeneme getUdpServer() {
		if (udpServer == null) {
			udpServer = new StreamUdpServerDeneme();
		}
		return udpServer;
	}

	public void listen()
	{
		listenThread = new Thread(){


			private int totalLength = 0;
			public void run() {
				try {
					datagramServerSocket = new DatagramSocket(53007);

					byte[] data = new byte[10*1024];
					DatagramPacket datagramPacket;

					while (true) {
						datagramPacket = new DatagramPacket(data, data.length);
						datagramServerSocket.receive(datagramPacket);					
						totalLength += datagramPacket.getLength();
						
						System.out.println("data received from port 530007");

					}
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		};
		listenThread.start();

	}

	public void stop() {
		try {
			datagramServerSocket.close();
			listenThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


}
