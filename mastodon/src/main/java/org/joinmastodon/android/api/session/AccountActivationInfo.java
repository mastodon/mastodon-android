package org.joinmastodon.android.api.session;

import com.google.gson.annotations.SerializedName;

public class AccountActivationInfo{
	@SerializedName(value="email", alternate="a")
	public String email;
	@SerializedName(value="last_email_confirmation_resend", alternate="b")
	public long lastEmailConfirmationResend;

	public AccountActivationInfo(String email, long lastEmailConfirmationResend){
		this.email=email;
		this.lastEmailConfirmationResend=lastEmailConfirmationResend;
	}
}
