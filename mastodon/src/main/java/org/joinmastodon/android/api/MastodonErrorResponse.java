package org.joinmastodon.android.api;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import org.joinmastodon.android.R;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import me.grishka.appkit.api.ErrorResponse;

public class MastodonErrorResponse extends ErrorResponse{
	public final String error;
	public final int httpStatus;
	public final Throwable underlyingException;
	public final int messageResource;

	public MastodonErrorResponse(String error, int httpStatus, Throwable exception){
		this.error=error;
		this.httpStatus=httpStatus;
		this.underlyingException=exception;

		if(exception instanceof UnknownHostException){
			this.messageResource=R.string.could_not_reach_server;
		}else if(exception instanceof SocketTimeoutException){
			this.messageResource=R.string.connection_timed_out;
		}else if(exception instanceof JsonSyntaxException || exception instanceof JsonIOException || httpStatus>=500){
			this.messageResource=R.string.server_error;
		}else if(httpStatus==404){
			this.messageResource=R.string.not_found;
		}else{
			this.messageResource=0;
		}
	}

	@Override
	public void bindErrorView(View view){
		TextView text=view.findViewById(R.id.error_text);
		String message;
		if(messageResource>0){
			message=view.getContext().getString(messageResource, error);
		}else{
			message=error;
		}
		text.setText(message);
	}

	@Override
	public void showToast(Context context){
		if(context==null)
			return;
		String message;
		if(messageResource>0){
			message=context.getString(messageResource, error);
		}else{
			message=error;
		}
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
}
