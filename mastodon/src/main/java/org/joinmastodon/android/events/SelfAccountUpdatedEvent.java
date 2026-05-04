package org.joinmastodon.android.events;

import org.joinmastodon.android.model.Account;

public record SelfAccountUpdatedEvent(String accountID, Account account){
}
