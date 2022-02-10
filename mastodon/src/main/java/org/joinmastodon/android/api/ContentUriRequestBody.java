package org.joinmastodon.android.api;

import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.OpenableColumns;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;
import okio.Source;

public class ContentUriRequestBody extends RequestBody{
	private final Uri uri;
	private final long length;
	private ProgressListener progressListener;

	public ContentUriRequestBody(Uri uri, ProgressListener progressListener){
		this.uri=uri;
		this.progressListener=progressListener;
		try(Cursor cursor=MastodonApp.context.getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)){
			cursor.moveToFirst();
			length=cursor.getInt(0);
		}
	}

	@Override
	public MediaType contentType(){
		return MediaType.get(MastodonApp.context.getContentResolver().getType(uri));
	}

	@Override
	public long contentLength() throws IOException{
		return length;
	}

	@Override
	public void writeTo(BufferedSink sink) throws IOException{
		if(progressListener!=null){
			try(Source source=Okio.source(MastodonApp.context.getContentResolver().openInputStream(uri))){
				BufferedSink wrappedSink=Okio.buffer(new CountingSink(sink));
				wrappedSink.writeAll(source);
				wrappedSink.flush();
			}
		}else{
			try(Source source=Okio.source(MastodonApp.context.getContentResolver().openInputStream(uri))){
				sink.writeAll(source);
			}
		}
	}

	private class CountingSink extends ForwardingSink{
		private long bytesWritten=0;
		private long lastCallbackTime;
		public CountingSink(Sink delegate){
			super(delegate);
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
}
