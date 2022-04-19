package org.joinmastodon.android.fragments.report;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.fragments.MastodonToolbarFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.ReportReason;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class ReportDoneFragment extends MastodonToolbarFragment{
	private String accountID;
	private Account reportAccount;
	private Button btn;
	private View buttonBar;
	private ReportReason reason;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setNavigationBarColor(UiUtils.getThemeColor(activity, R.attr.colorWindowBackground));
		accountID=getArguments().getString("account");
		reportAccount=Parcels.unwrap(getArguments().getParcelable("reportAccount"));
		reason=ReportReason.valueOf(getArguments().getString("reason"));
		setTitle(getString(R.string.report_title, reportAccount.acct));
	}


	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_report_done, container, false);

		TextView title=view.findViewById(R.id.title);
		TextView subtitle=view.findViewById(R.id.subtitle);
		if(reason==ReportReason.PERSONAL){
			title.setText(R.string.report_personal_title);
			subtitle.setText(R.string.report_personal_subtitle);
		}else{
			title.setText(R.string.report_sent_title);
			subtitle.setText(getString(R.string.report_sent_subtitle, '@'+reportAccount.acct));
		}

		btn=view.findViewById(R.id.btn_next);
		btn.setOnClickListener(this::onButtonClick);
		buttonBar=view.findViewById(R.id.button_bar);
		btn.setText(R.string.done);

		if(reason!=ReportReason.PERSONAL){
			View doneOverlay=view.findViewById(R.id.reported_overlay);
			doneOverlay.setOutlineProvider(OutlineProviders.roundedRect(7));
			ImageView ava=view.findViewById(R.id.avatar);
			ava.setOutlineProvider(OutlineProviders.roundedRect(24));
			ava.setClipToOutline(true);
			ViewImageLoader.load(ava, null, new UrlImageLoaderRequest(reportAccount.avatar));
			doneOverlay.setScaleX(1.5f);
			doneOverlay.setScaleY(1.5f);
			doneOverlay.setAlpha(0f);
			doneOverlay.animate().scaleX(1f).scaleY(1f).alpha(1f).rotation(8.79f).setDuration(300).setStartDelay(300).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
		}else{
			view.findViewById(R.id.ava_reported).setVisibility(View.GONE);
		}

		TextView unfollowTitle=view.findViewById(R.id.unfollow_title);
		TextView muteTitle=view.findViewById(R.id.mute_title);
		TextView blockTitle=view.findViewById(R.id.block_title);

		unfollowTitle.setText(getString(R.string.unfollow_user, '@'+reportAccount.acct));
		muteTitle.setText(getString(R.string.mute_user, '@'+reportAccount.acct));
		blockTitle.setText(getString(R.string.block_user, '@'+reportAccount.acct));

		view.findViewById(R.id.unfollow_btn).setOnClickListener(v->onUnfollowClick());
		view.findViewById(R.id.mute_btn).setOnClickListener(v->onMuteClick());
		view.findViewById(R.id.block_btn).setOnClickListener(v->onBlockClick());

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		view.setBackgroundColor(UiUtils.getThemeColor(getActivity(), android.R.attr.colorBackground));
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=27){
			int inset=insets.getSystemWindowInsetBottom();
			buttonBar.setPadding(0, 0, 0, inset>0 ? Math.max(inset, V.dp(36)) : 0);
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0));
		}else{
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
		}
	}

	private void onButtonClick(View v){
		Nav.finish(this);
	}

	private void onUnfollowClick(){
		new SetAccountFollowed(reportAccount.id, false, false)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Relationship result){
						Nav.finish(ReportDoneFragment.this);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.loading, false)
				.exec(accountID);
	}

	private void onMuteClick(){
		UiUtils.confirmToggleMuteUser(getActivity(), accountID, reportAccount, false, rel->Nav.finish(this));
	}

	private void onBlockClick(){
		UiUtils.confirmToggleBlockUser(getActivity(), accountID, reportAccount, false, rel->Nav.finish(this));
	}
}
