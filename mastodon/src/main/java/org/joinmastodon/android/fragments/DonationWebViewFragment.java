package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceRequest;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.DismissDonationCampaignBannerEvent;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;

import java.util.Objects;

import me.grishka.appkit.Nav;

public class DonationWebViewFragment extends WebViewFragment{
	public static final String SUCCESS_URL="https://sponsor.joinmastodon.org/donate/success";
	public static final String FAILURE_URL="https://sponsor.joinmastodon.org/donate/failure";
	public static final String CANCEL_URL="https://sponsor.joinmastodon.org/donate/cancel";

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(BuildConfig.DEBUG){
			setHasOptionsMenu(true);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		webView.loadUrl(Objects.requireNonNull(getArguments().getString("url")));
	}

	@Override
	protected boolean shouldOverrideUrlLoading(WebResourceRequest req){
		String url=req.getUrl().buildUpon().clearQuery().fragment(null).build().toString();
		if(url.equalsIgnoreCase(SUCCESS_URL)){
			onSuccess();
			return true;
		}else if(url.equalsIgnoreCase(FAILURE_URL)){
			onFailure();
			return true;
		}else if(url.equalsIgnoreCase(CANCEL_URL)){
			onCancel();
			return true;
		}
		return false;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		super.onCreateOptionsMenu(menu, inflater);
		if(BuildConfig.DEBUG){
			menu.add(0, 0, 0, "Simulate success");
			menu.add(0, 1, 0, "Simulate failure");
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getItemId()==0)
			onSuccess();
		else if(item.getItemId()==1)
			onFailure();
		return super.onOptionsItemSelected(item);
	}

	private void onFailure(){
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.error)
				.setMessage(R.string.donation_server_error)
				.setPositiveButton(R.string.ok, null)
				.setOnDismissListener(dlg->Nav.finish(this))
				.show();
	}

	private void onSuccess(){
		String campaignID=getArguments().getString("campaignID");
		AccountSessionManager.getInstance().markDonationCampaignAsDismissed(campaignID);
		E.post(new DismissDonationCampaignBannerEvent(campaignID));
		getActivity().setResult(Activity.RESULT_OK, new Intent().putExtra("postText", getArguments().getString("successPostText")));
		getActivity().finish();
	}

	private void onCancel(){
		getActivity().finish();
	}
}
