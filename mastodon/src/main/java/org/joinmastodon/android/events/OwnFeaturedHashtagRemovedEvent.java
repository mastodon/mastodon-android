package org.joinmastodon.android.events;

import org.joinmastodon.android.model.Hashtag;

public record OwnFeaturedHashtagRemovedEvent(String accountID, Hashtag tag, int newTotal){
}
