package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

public enum ExpandMedia {
    @SerializedName("default")
    DEFAULT,
    @SerializedName("show_all")
    SHOW_ALL,
    @SerializedName("hide_all")
    HIDE_ALL;
}
