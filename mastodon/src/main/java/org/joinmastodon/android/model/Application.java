package org.joinmastodon.android.model;

import androidx.annotation.NonNull;

import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

@Parcel
public class Application extends BaseModel {
    @RequiredField
    public String name;
    public String website;
    public String vapidKey;
    public String clientId;
    public String clientSecret;

    @NonNull
    @Override
    public String toString() {
        return "Application{" +
                "name='" + name + '\'' +
                ", website='" + website + '\'' +
                ", vapidKey='" + vapidKey + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                '}';
    }
}
