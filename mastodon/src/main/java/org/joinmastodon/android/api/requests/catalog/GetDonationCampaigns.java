package org.joinmastodon.android.api.requests.catalog;

import android.net.Uri;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.donations.DonationCampaign;

public class GetDonationCampaigns extends MastodonAPIRequest<DonationCampaign>{
	private final String locale, seed;
	private boolean staging;

	public GetDonationCampaigns(String locale, String seed){
		super(HttpMethod.GET, null, DonationCampaign.class);
		this.locale=locale;
		this.seed=seed;
		setCacheable();
	}

	public void setStaging(boolean staging){
		this.staging=staging;
	}

	@Override
	public Uri getURL(){
		Uri.Builder builder=new Uri.Builder()
				.scheme("https")
				.authority("api.joinmastodon.org")
				.path("/v1/donations/campaigns/active")
				.appendQueryParameter("platform", "android")
				.appendQueryParameter("locale", locale)
				.appendQueryParameter("seed", seed);
		if(staging)
			builder.appendQueryParameter("environment", "staging");
		return builder.build();
	}
}
