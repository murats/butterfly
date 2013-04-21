package com.mekya.streamserver.servers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.mekya.streamserver.IStreamListener;
import com.mekya.streamserver.servers.NanoHTTPD.Response.Status;

public class HttpServer extends NanoHTTPD {

	LiveInputStream istream = new LiveInputStream();
	private StreamTcpServer streamer;
	public HttpServer(int port) {
		super(port);
	}

	@Override
	public Response serve(String uri, Method method,
			Map<String, String> header, Map<String, String> parms,
			Map<String, String> files) {
		// TODO Auto-generated method stub
		
		System.out.println(uri);
		
		
		Response res = new NanoHTTPD.Response(Status.OK, NanoHTTPD.MIME_PLAINTEXT, true);
		getStreamer().register(res);
		
		return res;
	}
	

	
	public class LiveInputStream extends InputStream {

		private byte[] buffer = new byte[256];
		private boolean updated = false;
		private int length = buffer.length;
		
		
		public void setBuffer(byte[] data, int dataLength) {
			updated = true;
			int length = dataLength;
			if (dataLength > buffer.length) {
				length = buffer.length;
			}
			System.arraycopy(data, 0, buffer, 0, length);
		}
		
		@Override @Deprecated
		public int read() throws IOException {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override @Deprecated
		public int read(byte[] b, int off, int len) throws IOException {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public int read(byte[] b) throws IOException {
			System.out.println("HttpServer.LiveInputStream.read()");
			int length = b.length;
			while (true) {
				try {
					Thread.sleep(1000000);
					if (updated == true) {
						if (b.length > this.length) {
							length = this.length;
						}
						System.arraycopy(buffer, 0, b, 0, length);
						break;
					}
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			return length;
		}
	
		
//		@Override
//		public int read(byte[] b, int off, int len) throws IOException {
//			System.out.println("HttpServer.LiveInputStream.read()");
//			while (true) {
//				try {
//					Thread.sleep(10000000);
//					if (updated == true) {
//						System.arraycopy(buffer, 0, b, off, this.length);
//						break;
//					}
//					
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//			
//			return 
//		}
		
	}

	public void setStreamer(StreamTcpServer messenger) {
		this.streamer = messenger;
	}
	
	public StreamTcpServer getStreamer() {
		return streamer;
	}

}
