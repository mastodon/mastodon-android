package org.joinmastodon.android.model.donations;

import org.joinmastodon.android.api.AllFieldsAreRequired;
import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.BaseModel;

import java.util.Map;

@AllFieldsAreRequired
public class DonationCampaign extends BaseModel{
	public String id;
	public String bannerMessage;
	public String bannerButtonText;
	public String donationMessage;
	public String donationButtonText;
	public Amounts amounts;
	public String defaultCurrency;
	public String donationUrl;
	public String donationSuccessPost;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		amounts.postprocess();
	}

	public static class Amounts extends BaseModel{
		public Map<String, long[]> oneTime;
		@RequiredField
		public Map<String, long[]> monthly;
		public Map<String, long[]> yearly;
	}
}
