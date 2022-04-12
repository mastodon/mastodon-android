package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;

public class SetDomainBlocked extends MastodonAPIRequest<Object>{
	public SetDomainBlocked(String domain, boolean blocked){
		super(blocked ? HttpMethod.POST : HttpMethod.DELETE, "/domain_blocks", Object.class);
		setRequestBody(new Request(domain));
	}

	private static class Request{
		public String domain;

		public Request(String domain){
			this.domain=domain;
		}
	}
}
