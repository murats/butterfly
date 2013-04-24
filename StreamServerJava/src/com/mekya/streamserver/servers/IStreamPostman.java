package com.mekya.streamserver.servers;

import com.mekya.streamserver.IStreamListener;

public interface IStreamPostman {

	public abstract void register(IStreamListener streamListener);

	public abstract void removeListener(IStreamListener streamListener);

}