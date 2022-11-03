package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

@Parcel
public class ListTimeline extends BaseModel {
    @RequiredField
    public String id;
    @RequiredField
    public String title;
    @RequiredField
    public RepliesPolicy repliesPolicy;

    @Override
    public String toString() {
        return "List{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", repliesPolicy=" + repliesPolicy +
                '}';
    }

    public enum RepliesPolicy{
        @SerializedName("followed")
        FOLLOWED,
        @SerializedName("list")
        LIST,
        @SerializedName("none")
        NONE
    }
}
