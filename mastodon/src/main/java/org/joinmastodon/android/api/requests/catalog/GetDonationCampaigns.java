package org.joinmastodon.android.api.requests.catalog;

import android.net.Uri;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.donations.DonationCampaign;

public class GetDonationCampaigns extends MastodonAPIRequest<DonationCampaign>{
	private final String locale, seed;

	public GetDonationCampaigns(String locale, String seed){
		super(HttpMethod.GET, null, DonationCampaign.class);
		this.locale=locale;
		this.seed=seed;
		setCacheable();
	}

	@Override
	public Uri getURL(){
		Uri.Builder builder=new Uri.Builder()
				.scheme("https")
				.authority("api.joinmastodon.org")
				.path("/donations/campaigns")
				.appendQueryParameter("platform", "android")
				.appendQueryParameter("locale", locale)
				.appendQueryParameter("seed", seed);
		return builder.build();
	}
}
