package org.joinmastodon.android.api;

import java.io.IOException;

import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

abstract class CountingRequestBody extends RequestBody {
    protected long length;
    protected ProgressListener progressListener;

    CountingRequestBody(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    @Override
    public long contentLength() throws IOException {
        return length;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        if (progressListener != null) {
            try (Source source = openSource()) {
                BufferedSink wrappedSink = Okio.buffer(new CountingSink(length, progressListener, sink));
                wrappedSink.writeAll(source);
                wrappedSink.flush();
            }
        } else {
            try (Source source = openSource()) {
                sink.writeAll(source);
            }
        }
    }

    protected abstract Source openSource() throws IOException;
}
