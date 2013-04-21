/**
 * 
 */
package com.mekya.streamserver.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mekya.streamserver.IStreamListener;
import com.mekya.streamserver.SocketMessenger;

/**
 * @author aomermerkaya
 *
 */
public class SocketMessengerTest {

	private static final int PORT = 52147;
	private static final int PORT_INTERNAL = 45878;
	private SocketMessenger socketMessenger;

	/**
	 * Test method for {@link com.mekya.streamserver.servers.StreamTcpServer#register(com.mekya.streamserver.IStreamListener)}.
	 */
	@Test
	public void testRegister() {
	}

	/**
	 * Test method for {@link com.mekya.streamserver.servers.StreamTcpServer#removeListener(com.mekya.streamserver.IStreamListener)}.
	 */
	@Test
	public void testRemoveListener() {
	}

	/**
	 * Test method for {@link com.mekya.streamserver.servers.StreamTcpServer#stopServer()}.
	 */
	@Test
	public void testStopServer() {
	}
	
	@Before 
	public void before() {
		socketMessenger = new SocketMessenger(PORT, PORT_INTERNAL);
		socketMessenger.start();
	}
	
	@After
	public void after(){
		socketMessenger.stopServer();
		//streamServer = null;
	}
	
	/**
	 * Test if StreamServer delivers messages to all registered members
	 */
	@Test
	public void testRun() {
		
		ArrayList<StreamListener> listeners = new ArrayList<StreamListener>();
		StreamListener listener;
		for (int i = 0; i < 50; i++) {
			listener = new StreamListener();
			listener.connect(PORT_INTERNAL);
			listener.listen();
			listeners.add(listener);
		}
		
	
		ClientStreamer client = new ClientStreamer();
		client.connect();
		
		assertTrue(client.isConnected());
		
		for (int i = 0; i < 50; i++) {
			assertFalse(listeners.get(i).isDataReceived());
		}
		
		
		String data = "This is a test";
		//send data to stream server
		client.sendData(data.getBytes());
		
		//wait a little to let message delivered
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		for (int i = 0; i < 50; i++) {
			assertTrue(listeners.get(i).isDataReceived());
			Assert.assertEquals(data.getBytes().length, listeners.get(i).getLength());
			for (int j = 0; j < data.getBytes().length; j++) {
				Assert.assertEquals(data.getBytes()[j], listeners.get(i).getData()[j]);
			}
		}
		
		
		for (int i = 0; i < 50; i++) {
			assertFalse(listeners.get(i).isDataReceived());
		}
	}
	
	
	
	private class ClientStreamer {
		
		private Socket socket;
		private OutputStream outputStream;

		public void connect() {
			try {
				socket = new Socket("127.0.0.1", PORT);
				outputStream = socket.getOutputStream();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
		
		public void sendData(byte[] data) {
			try {
				outputStream.write(data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public boolean isConnected() {
			return socket.isConnected();
		}
	}
	
	private class StreamListener implements IStreamListener {

		private boolean dataReceived = false;
		private byte[] data;
		private int length;
		private Socket socket;

		@Override
		public int dataReceived(byte[] data, int length) {
			this.dataReceived = true;
			this.data = data;
			this.setLength(length);
			return length;
		}

		public void connect(int portInternal) {
			try {
				socket = new Socket("127.0.0.1", portInternal);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void listen(){
			Thread thread = new Thread() {
				@Override
				public void run() {
					byte[] data = new byte[10024];
					int len;
					try {
						while ((len = socket.getInputStream().read(data, 0, data.length))>0) {
							dataReceived(data, len);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			thread.start();
			
		}

		public boolean isDataReceived() {
			boolean received = dataReceived;
			dataReceived = false;
			return received;
		}

		/**
		 * @return the data
		 */
		public byte[] getData() {
			return data;
		}

		/**
		 * @param data the data to set
		 */
		public void setData(byte[] data) {
			this.data = data;
		}

		/**
		 * @return the length
		 */
		public int getLength() {
			return length;
		}

		/**
		 * @param length the length to set
		 */
		public void setLength(int length) {
			this.length = length;
		}
		
	}

}
