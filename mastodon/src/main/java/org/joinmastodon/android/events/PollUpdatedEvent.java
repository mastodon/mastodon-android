package org.joinmastodon.android.events;

import org.joinmastodon.android.model.Poll;

public class PollUpdatedEvent {
    public String accountID;
    public Poll poll;

    public PollUpdatedEvent(String accountID, Poll poll) {
        this.accountID = accountID;
        this.poll = poll;
    }
}
