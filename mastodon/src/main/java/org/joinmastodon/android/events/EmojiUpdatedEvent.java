package org.joinmastodon.android.events;

public class EmojiUpdatedEvent{
	public String instanceDomain;

	public EmojiUpdatedEvent(String instanceDomain){
		this.instanceDomain=instanceDomain;
	}
}
