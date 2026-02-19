package org.joinmastodon.android.events;

import org.joinmastodon.android.model.Hashtag;

public record OwnFeaturedHashtagAddedEvent(String accountID, Hashtag tag){
}
