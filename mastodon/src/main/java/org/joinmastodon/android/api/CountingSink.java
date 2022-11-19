package org.joinmastodon.android.api;

import android.os.SystemClock;

import org.joinmastodon.android.ui.utils.UiUtils;

import java.io.IOException;

import okio.Buffer;
import okio.ForwardingSink;
import okio.Sink;

class CountingSink extends ForwardingSink{
	private long bytesWritten;
	private long lastCallbackTime;
	private final long length;
	private final ProgressListener progressListener;

	public CountingSink(long length, ProgressListener progressListener, Sink delegate){
		super(delegate);
		this.length=length;
		this.progressListener=progressListener;
	}

	@Override
	public void write(Buffer source, long byteCount) throws IOException{
		super.write(source, byteCount);
		bytesWritten+=byteCount;
		if(SystemClock.uptimeMillis()-lastCallbackTime>=100L || bytesWritten==length){
			lastCallbackTime=SystemClock.uptimeMillis();
			UiUtils.runOnUiThread(()->progressListener.onProgress(bytesWritten, length));
		}
	}
}
