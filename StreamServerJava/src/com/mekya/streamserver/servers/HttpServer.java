package com.mekya.streamserver.servers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.mekya.streamserver.IStreamListener;
import com.mekya.streamserver.servers.NanoHTTPD.Response.Status;

public class HttpServer extends NanoHTTPD {

	private IStreamPostman streamer;
	public HttpServer(int port) {
		super(port);
	}

	@Override
	public Response serve(IStreamListener streamListener,String uri, Method method,
			Map<String, String> header, Map<String, String> parms,
			Map<String, String> files) {
		// TODO Auto-generated method stub

		System.out.println(uri);
		Response res = null;
		if (uri.equals("/live")) {
			res = new NanoHTTPD.Response(Status.OK, NanoHTTPD.MIME_PLAINTEXT, true);
			getStreamer().register(streamListener);
		}
		return res;
	}



	public void setStreamer(IStreamPostman messenger) {
		this.streamer = messenger;
	}

	public IStreamPostman getStreamer() {
		return streamer;
	}

}
