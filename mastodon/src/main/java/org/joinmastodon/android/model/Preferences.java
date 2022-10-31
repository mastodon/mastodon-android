package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * Preferred common behaviors to be shared across clients.
 */
public class Preferences extends BaseModel {
    /**
     * Default visibility for new posts
     */
    @SerializedName("posting:default:visibility")
    public StatusPrivacy postingDefaultVisibility;

    /**
     * Default sensitivity flag for new posts
     */
    @SerializedName("posting:default:sensitive")
    public boolean postingDefaultSensitive;

    /**
     * Default language for new posts
     */
    @SerializedName("posting:default:language")
    public String postingDefaultLanguage;

    /**
     * Whether media attachments should be automatically displayed or blurred/hidden.
     */
    @SerializedName("reading:expand:media")
    public ExpandMedia readingExpandMedia;

    /**
     * Whether CWs should be expanded by default.
     */
    @SerializedName("reading:expand:spoilers")
    public boolean readingExpandSpoilers;
}