package org.joinmastodon.android.events;

public class DismissDonationCampaignBannerEvent{
	public final String campaignID;

	public DismissDonationCampaignBannerEvent(String campaignID){
		this.campaignID=campaignID;
	}
}
