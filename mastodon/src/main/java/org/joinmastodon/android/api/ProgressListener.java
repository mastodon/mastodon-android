package org.joinmastodon.android.api;

public interface ProgressListener{
	void onProgress(long transferred, long total);
}
