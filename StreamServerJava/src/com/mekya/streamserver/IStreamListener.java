package com.mekya.streamserver;

public interface IStreamListener {

	public int dataReceived(byte[] data, int length);
}
