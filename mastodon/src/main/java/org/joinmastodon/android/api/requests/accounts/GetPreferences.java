package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Preferences;

public class GetPreferences extends MastodonAPIRequest<Preferences> {
    public GetPreferences(){
        super(HttpMethod.GET, "/preferences", Preferences.class);
    }
}
