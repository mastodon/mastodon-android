package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

@Parcel
public class AccountWarning extends BaseModel{
	@RequiredField
	public String id;
	@RequiredField
	public Action action=Action.NONE;
	public String text;

	public enum Action{
		@SerializedName("none")
		NONE,
		@SerializedName("disable")
		DISABLE,
		@SerializedName("mark_statuses_as_sensitive")
		MARK_STATUSES_AS_SENSITIVE,
		@SerializedName("delete_statuses")
		DELETE_STATUSES,
		@SerializedName("sensitive")
		SENSITIVE,
		@SerializedName("silence")
		SILENCE,
		@SerializedName("suspend")
		SUSPEND
	}
}
