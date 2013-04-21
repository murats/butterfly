package com.mekya.streamserver.servers;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import com.mekya.streamserver.IStreamListener;

/**
 * This server accepts stream data from clients
 * @author aomermerkaya
 *
 */
public class StreamTcpServer extends Thread{

	private ArrayList<IStreamListener> streamListeners = new ArrayList<IStreamListener>();

	private ServerSocket serverSocket;

	private int port;
	
	public StreamTcpServer(int port) {
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void register(IStreamListener streamListener) {
		streamListeners.add(streamListener);
	}



	public void removeListener(IStreamListener streamListener) {
		streamListeners.remove(streamListener);
	}

	private void feedStreamListeners(byte[] data, int len) {
		int size = streamListeners.size();
		for (int i = 0; i < size ; i++) {
			streamListeners.get(i).dataReceived(data, len);
		}
	}



	@Override
	public void run() {
		try {
			Socket socket;
			while (true) {
				socket = serverSocket.accept();
				new SessionHandler(socket).start();
			}
		}catch (SocketException e) {
			//e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void stopServer() {
		try {
			serverSocket.close();
			this.join();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public class SessionHandler extends Thread {

		private final Socket socket;


		public SessionHandler(Socket socket) {
			this.socket = socket;
		}


		@Override
		public void run() {
			try {
				InputStream istream = socket.getInputStream();
				byte[] data = new byte[10240];
				int len;
				while ((len = istream.read(data, 0, data.length)) > 0) {
					System.out.println("received data in stream server");
					feedStreamListeners(data, len);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}


}
