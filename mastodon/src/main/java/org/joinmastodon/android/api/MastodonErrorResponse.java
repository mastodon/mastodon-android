package org.joinmastodon.android.api;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.R;

import me.grishka.appkit.api.ErrorResponse;

public class MastodonErrorResponse extends ErrorResponse{
	public final String error;
	public final int httpStatus;
	public final Throwable underlyingException;

	public MastodonErrorResponse(String error, int httpStatus, Throwable exception){
		this.error=error;
		this.httpStatus=httpStatus;
		this.underlyingException=exception;
	}

	@Override
	public void bindErrorView(View view){
		TextView text=view.findViewById(R.id.error_text);
		text.setText(error);
	}

	@Override
	public void showToast(Context context){
		if(context==null)
			return;
		Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
	}
}
