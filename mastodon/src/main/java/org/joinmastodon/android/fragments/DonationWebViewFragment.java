package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.DismissDonationCampaignBannerEvent;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;

import java.util.Objects;

import me.grishka.appkit.Nav;

public class DonationWebViewFragment extends WebViewFragment{
	public static final String SUCCESS_URL="https://sponsor.joinmastodon.org/donation/success";
	public static final String FAILURE_URL="https://sponsor.joinmastodon.org/donation/failure";

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		webView.loadUrl(Objects.requireNonNull(getArguments().getString("url")));
	}

	@Override
	protected boolean shouldOverrideUrlLoading(WebResourceRequest req){
		String url=req.getUrl().buildUpon().clearQuery().fragment(null).build().toString();
		if(url.equalsIgnoreCase(SUCCESS_URL)){
			new M3AlertDialogBuilder(getActivity())
					.setTitle("Success")
					.setMessage("Some sort of UI that would tell the user that their payment was successful")
					.setPositiveButton(R.string.ok, null)
					.setOnDismissListener(dlg->Nav.finish(this))
					.show();
			String campaignID=getArguments().getString("campaignID");
			AccountSessionManager.getInstance().markDonationCampaignAsDismissed(campaignID);
			E.post(new DismissDonationCampaignBannerEvent(campaignID));
			return true;
		}else if(url.equalsIgnoreCase(FAILURE_URL)){
			new M3AlertDialogBuilder(getActivity())
					.setTitle("Failure")
					.setMessage("Some sort of UI that would tell the user that their payment didn't go through")
					.setPositiveButton(R.string.ok, null)
					.setOnDismissListener(dlg->Nav.finish(this))
					.show();
			return true;
		}
		return false;
	}
}
