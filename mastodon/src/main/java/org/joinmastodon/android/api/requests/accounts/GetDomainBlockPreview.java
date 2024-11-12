package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;

public class GetDomainBlockPreview extends MastodonAPIRequest<GetDomainBlockPreview.Response>{
	public GetDomainBlockPreview(String domain){
		super(HttpMethod.GET, "/domain_blocks/preview", Response.class);
		addQueryParameter("domain", domain);
	}

	public static class Response{
		public int followingCount;
		public int followersCount;
	}
}
