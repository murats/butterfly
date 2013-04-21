package com.mekya.streamserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * This server accepts stream data from clients
 * @author aomermerkaya
 *
 */
public class SocketMessenger extends Thread {

	private ArrayList<Socket> socketListeners = new ArrayList<Socket>();

	private ServerSocket serverSocket;

	private int port;

	private ServerSocket serverSocketInternal;

	private Thread internalServer;
	
	public SocketMessenger(int port, int portInternal) {
		try {
			serverSocket = new ServerSocket(port);
			serverSocketInternal = new ServerSocket(portInternal);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	

	private void startInternalServer(){
		
		internalServer = new Thread(){
			@Override
			public void run() {
				try {
					while (true) {
						socketListeners.add(serverSocketInternal.accept());
						System.out
						.println("SocketMessenger.startInternalServer().run()");
					}
				}catch (SocketException e) {
					//e.printStackTrace();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		internalServer.start();
	}





	private void feedStreamListeners(byte[] data, int len) {
		int size = socketListeners.size();
		
		for (Socket socket : socketListeners) {
			if (socket.isClosed()) {
				socketListeners.remove(socket);
			}
			else {
				try {
					socket.getOutputStream().write(data, 0, len);
					System.out.println("SocketMessenger.feedStreamListeners()");
				} catch (IOException e) {
					socketListeners.remove(socket);
					e.printStackTrace();
				}
			}
		}

	}



	@Override
	public void run() {
		try {
			startInternalServer();
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
			serverSocketInternal.close();
			internalServer.join();
			this.join();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * SessionHandler gets input from one client and delivers it to registered listeners
	 * @author aomermerkaya
	 *
	 */
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
					feedStreamListeners(data, len);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}


}
