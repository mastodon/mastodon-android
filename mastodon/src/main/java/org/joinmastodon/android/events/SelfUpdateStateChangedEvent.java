package org.joinmastodon.android.events;

import org.joinmastodon.android.updater.GithubSelfUpdater;

public class SelfUpdateStateChangedEvent{
	public final GithubSelfUpdater.UpdateState state;

	public SelfUpdateStateChangedEvent(GithubSelfUpdater.UpdateState state){
		this.state=state;
	}
}
