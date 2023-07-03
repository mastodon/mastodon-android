package org.joinmastodon.android.model;

import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

/**
 * Represents a custom emoji.
 */
@Parcel
public class Emoji extends BaseModel {
    /**
     * The name of the custom emoji.
     */
    @RequiredField
    public String shortcode;
    /**
     * A link to the custom emoji.
     */
    @RequiredField
    public String url;
    /**
     * A link to a static copy of the custom emoji.
     */
    @RequiredField
    public String staticUrl;
    /**
     * Whether this Emoji should be visible in the picker or unlisted.
     */
    @RequiredField
    public boolean visibleInPicker;
    /**
     * Used for sorting custom emoji in the picker.
     */
    public String category;

    @Override
    public String toString() {
        return "Emoji{" +
                "shortcode='" + shortcode + '\'' +
                ", url='" + url + '\'' +
                ", staticUrl='" + staticUrl + '\'' +
                ", visibleInPicker=" + visibleInPicker +
                ", category='" + category + '\'' +
                '}';
    }
}
