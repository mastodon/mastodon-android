package org.joinmastodon.android.api.requests.accounts;

import android.net.Uri;

import org.joinmastodon.android.api.AvatarResizedImageRequestBody;
import org.joinmastodon.android.api.ContentUriRequestBody;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.ResizedImageRequestBody;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class UpdateAccountCredentials extends MastodonAPIRequest<Account>{
	private String displayName, bio;
	private Uri avatar, cover;
	private File avatarFile, coverFile;
	private List<AccountField> fields;

	public UpdateAccountCredentials(String displayName, String bio, Uri avatar, Uri cover, List<AccountField> fields){
		super(HttpMethod.PATCH, "/accounts/update_credentials", Account.class);
		this.displayName=displayName;
		this.bio=bio;
		this.avatar=avatar;
		this.cover=cover;
		this.fields=fields;
	}

	public UpdateAccountCredentials(String displayName, String bio, File avatar, File cover, List<AccountField> fields){
		super(HttpMethod.PATCH, "/accounts/update_credentials", Account.class);
		this.displayName=displayName;
		this.bio=bio;
		this.avatarFile=avatar;
		this.coverFile=cover;
		this.fields=fields;
	}

	@Override
	public RequestBody getRequestBody() throws IOException{
		MultipartBody.Builder bldr=new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("display_name", displayName)
				.addFormDataPart("note", bio);

		if(avatar!=null){
			bldr.addFormDataPart("avatar", UiUtils.getFileName(avatar), new AvatarResizedImageRequestBody(avatar, null));
		}else if(avatarFile!=null){
			bldr.addFormDataPart("avatar", avatarFile.getName(), new AvatarResizedImageRequestBody(Uri.fromFile(avatarFile), null));
		}
		if(cover!=null){
			bldr.addFormDataPart("header", UiUtils.getFileName(cover), new ResizedImageRequestBody(cover, 1500*500, null));
		}else if(coverFile!=null){
			bldr.addFormDataPart("header", coverFile.getName(), new ResizedImageRequestBody(Uri.fromFile(coverFile), 1500*500, null));
		}
		if(fields.isEmpty()){
			bldr.addFormDataPart("fields_attributes[0][name]", "").addFormDataPart("fields_attributes[0][value]", "");
		}else{
			int i=0;
			for(AccountField field:fields){
				bldr.addFormDataPart("fields_attributes["+i+"][name]", field.name).addFormDataPart("fields_attributes["+i+"][value]", field.value);
				i++;
			}
		}

		return bldr.build();
	}
}
