/**
 * 
 */
package com.mekya.streamserver.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.mekya.streamserver.IStreamListener;
import com.mekya.streamserver.servers.StreamTcpServer;

/**
 * @author aomermerkaya
 *
 */
public class StreamServerTest {

	private static final int PORT = 52147;
	private StreamTcpServer streamServer;

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
		streamServer = new StreamTcpServer(PORT);
		streamServer.start();
	}
	
	@After
	public void after(){
		streamServer.stopServer();
		//streamServer = null;
	}
	
	/**
	 * Test if StreamServer delivers messages to all registered members
	 */
	@Test
	public void testRun() {
		
		ArrayList<StreamListener> listeners = new ArrayList<StreamServerTest.StreamListener>();
		StreamListener listener;
		for (int i = 0; i < 50; i++) {
			listener = new StreamListener();
			streamServer.register(listener);
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

		@Override
		public int dataReceived(byte[] data, int length) {
			this.dataReceived = true;
			this.data = data;
			this.setLength(length);
			return length;
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
