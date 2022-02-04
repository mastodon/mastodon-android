package org.joinmastodon.android.api.requests.statuses;

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.api.ContentUriRequestBody;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.ProgressListener;
import org.joinmastodon.android.model.Attachment;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class UploadAttachment extends MastodonAPIRequest<Attachment>{
	private Uri uri;
	private ProgressListener progressListener;

	public UploadAttachment(Uri uri){
		super(HttpMethod.POST, "/media", Attachment.class);
		this.uri=uri;
	}

	public UploadAttachment setProgressListener(ProgressListener progressListener){
		this.progressListener=progressListener;
		return this;
	}

	@Override
	public RequestBody getRequestBody(){
		String fileName;
		try(Cursor cursor=MastodonApp.context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)){
			cursor.moveToFirst();
			fileName=cursor.getString(0);
		}
		if(fileName==null)
			fileName=uri.getLastPathSegment();
		return new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("file", fileName, new ContentUriRequestBody(uri, progressListener))
				.build();
	}
}
