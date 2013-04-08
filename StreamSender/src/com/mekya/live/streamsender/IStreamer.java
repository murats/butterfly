package com.mekya.live.streamsender;

public interface IStreamer {

	public void startAudio(String address, int port);
	public void startVideo(String address, int port);

	public void startStreaming(String address, int port);
	
	public void stopStreaming();
}
