package com.mekya.live.streamsender.servers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.mekya.live.streamsender.IStreamer;


public class RtspServer {

	protected ServerSocket listenSocket;
	private Thread serverThread;
	private IStreamer istreamer;
	private Integer video_port;
	private int sessionId;
	private Integer audio_port = 53008;
	protected String clientAddr;

	public RtspServer(IStreamer istreamer) {
		listenHttp();
		this.istreamer = istreamer;
	}

	public void stop(){
		try {
			listenSocket.close();
			serverThread.join();			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	private void listenHttp()
	{
		serverThread = new Thread(){
			@Override
			public void run() {
				try {
					listenSocket = new ServerSocket(6454);
					boolean done = false;
					while (!done) {
						Socket RTSPsocket = listenSocket.accept();
						processRequest(RTSPsocket);
						if (done == true) {
							break;
						}						
					}
					listenSocket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		};
		serverThread.start();
	}

	class RtspRequest {
		public String command;
		public int creq;
		public Integer client_port;
		public Integer client_port2;
		public int trackId;

	}

	private RtspRequest parseRequest(BufferedReader reader){

		String line = "";
		String request = "";
		int lineIndex = 0;
		RtspRequest rtspReq = new RtspRequest();
		try {
			while ((line = reader.readLine()).length() != 0) {

				System.out.println(line);
				
				if (lineIndex == 0) {
					int pos = line.indexOf(" ");
					rtspReq.command = line.substring(0, pos);
					if (line.contains("trackID=0")) {
						rtspReq.trackId = 0;
					}
					else if (line.contains("trackID=1")) {
						rtspReq.trackId = 1;
					}
				}
				else {
					int pos = line.indexOf(" ");
					String lineDescriptor = line.substring(0, pos);
					if (lineDescriptor.equals("CSeq:")) {
						rtspReq.creq = Integer.valueOf(line.substring(pos+1));
					}
					else if (lineDescriptor.equals("Transport:")) {
						pos = line.indexOf("client_port=");
						int pos2 = line.indexOf("-", pos);
						rtspReq.client_port = Integer.valueOf(line.substring(pos+"client_port=".length(), pos2));
						rtspReq.client_port2 = Integer.valueOf(line.substring(pos2));
					}
				}
				lineIndex++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		catch(NullPointerException e) {
			e.printStackTrace();
		}

		return rtspReq;

	}


	private void replyRequest(BufferedWriter writer, RtspRequest rtspReq){
		try {
			if (rtspReq.command.equals("OPTIONS")) {
				replyOptions(writer, rtspReq);
			}
			else if (rtspReq.command.equals("DESCRIBE")) {
				replyDescribe(writer, rtspReq);
			}
			else if (rtspReq.command.equals("SETUP")) {
				replySetup(writer, rtspReq);
			}
			else if (rtspReq.command.equals("PLAY")) {
				replyPlay(writer, rtspReq);
			}
			else if (rtspReq.command.equals("TEARDOWN")) {
				replyTearDown(writer, rtspReq);
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}

	}

	private void replyTearDown(BufferedWriter writer, RtspRequest rtspReq) throws IOException {
		writer.write("RTSP/1.0 200 OK" + "\r\n"
				+ "CSeq: " + rtspReq.creq + "\r\n"
				+ "\r\n");
		writer.flush();
		
		istreamer.stopStreaming();
		
	}

	private void replyPlay(BufferedWriter writer, RtspRequest rtspReq) throws IOException 
	{
		writer.write("RTSP/1.0 200 OK" + "\r\n"
				+ "CSeq: " + rtspReq.creq + "\r\n"
				+ "Session: "+ sessionId + "\r\n"
				+ "\r\n");
		writer.flush();
		
		//istreamer.startStreaming(clientAddr, video_port, audio_port);
		istreamer.startVideo(clientAddr, video_port);
		istreamer.startAudio(clientAddr, audio_port);
	}

	private void replySetup(BufferedWriter writer, RtspRequest rtspReq) throws IOException {
		
		if (rtspReq.trackId == 0) {
			video_port = rtspReq.client_port;
			sessionId = (int)(Math.random() * 10000000);
		}
		else if (rtspReq.trackId == 1){
			audio_port = rtspReq.client_port;
		}
		writer.write("RTSP/1.0 200 OK" + "\r\n"
				+ "Cseq: "+ rtspReq.creq + "\r\n"
				+ "Session: "+ sessionId + "\r\n"
				+ "Transport: RTP/AVP/UDP;unicast;client_port="+rtspReq.client_port+"-"+rtspReq.client_port2 + "\r\n"
				+ "\r\n"					
				);
		
		writer.flush();
		
	}

	private void replyDescribe(BufferedWriter writer, RtspRequest rtspReq) throws IOException {
		String str = "v=0" + "\r\n"
				+ "o=- 0 0 IN IP4 0.0.0.0" + "\r\n"
				+ "s=No Name" + "\r\n"
				+ "c=IN IP4 0.0.0.0" + "\r\n"
	//			+ "b=AS:500" + "\r\n"
			//	+ "t=0 0" + "\r\n"
	//			+ "a=control:*" + "\r\n"
				+ "a=tool:libavformat 54.59.106" + "\r\n"
				+ "m=video 0 RTP/AVP 96" + "\r\n"
	//			+ "b=AS:500" + "\r\n"
				+ "a=rtpmap:96 H264/90000" + "\r\n"
	//			+ "a=control:trackID=0" + "\r\n"
				+ "a=fmtp:96 packetization-mode=1" + "\r\n"
	//			+ "m=audio 0 RTP/AVP 97" + "\r\n"
				//		+ "b=AS:120" + "\r\n"
				//		+ "a=rtpmap:97 AMR/8000/1"
				//		+ "a=rtpmap:97 GSM/8000/1"
	//			+ "a=rtpmap:97 MPEG4-GENERIC/44100/1" + "\r\n"
	//			+ "a=control:trackID=1" + "\r\n"
				//		+ "a=fmtp:97 vbr=off" + "\r\n"
				//		+ "a=fmtp:97 octet-align=1"
	//			+ "a=fmtp:97 profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3;" + "\r\n" //  
				+ "\r\n";

		writer.write("RTSP/1.0 200 OK" + "\r\n"
				+ "Content-type: application/sdp" + "\r\n"
				+ "Cache-Control: must-revalidate" + "\r\n"
				+ "CSeq: "+ rtspReq.creq +"\r\n"
				+ "Content-length: " + str.length() + "\r\n"
				+ "\r\n");
		writer.write(str);
		writer.flush();

	}

	private void replyOptions(BufferedWriter writer, RtspRequest rtspReq) throws IOException 
	{
		writer.write("RTSP/1.0 200 OK" + "\r\n"
				+ "CSeq: "+ rtspReq.creq + "\r\n"
				+ "Public: OPTIONS, DESCRIBE, SETUP, TEARDOWN, PLAY"+ "\r\n"
				+ "\r\n"
				);
		writer.flush();

	}

	private void processRequest(final Socket socket){
		new Thread(){
			public void run() {
				try {

					InputStream istream = socket.getInputStream();
					clientAddr = socket.getInetAddress().getHostAddress();


					BufferedReader reader = new BufferedReader(new InputStreamReader(istream));
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

					while (true) {
						RtspRequest rtspReq; 
						rtspReq = parseRequest(reader);
						replyRequest(writer, rtspReq);
						if (rtspReq.command.equals("TEARDOWN")) {
							break;
						}
					}
				

					socket.close();

				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		}.start();
	}

}
