package org.joinmastodon.android.fragments;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.requests.accounts.CheckInviteLink;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fork.ForkConfig;
import org.joinmastodon.android.fragments.onboarding.InstanceRulesFragment;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.ui.InterpolatingMotionEffect;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.views.ProgressBarButton;
import org.joinmastodon.android.ui.views.SizeListenerFrameLayout;
import org.parceler.Parcels;

import java.util.Objects;

import androidx.annotation.Nullable;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.utils.V;

public class SplashFragment extends AppKitFragment{

	// masto.nyc fork: this app is locked to a single server; there is no server picker.
	private static final String DEFAULT_SERVER=ForkConfig.INSTANCE_DOMAIN;

	private SizeListenerFrameLayout contentView;
	private View artContainer, blueFill, greenFill;
	private InterpolatingMotionEffect motionEffect;
	private View artClouds, artPlaneElephant, artRightHill, artLeftHill, artCenterHill;
	private ProgressBarButton defaultServerButton;
	private final String chosenDefaultServer=DEFAULT_SERVER;
	private boolean checkedInviteLink;
	private Uri currentInviteLink;
	private ProgressDialog instanceLoadingProgress;
	private String inviteCode;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		motionEffect=new InterpolatingMotionEffect(MastodonApp.context);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		contentView=(SizeListenerFrameLayout) inflater.inflate(R.layout.fragment_splash, container, false);
		contentView.findViewById(R.id.btn_log_in).setOnClickListener(this::onLogInClick);
		defaultServerButton=contentView.findViewById(R.id.btn_join_default_server);
		defaultServerButton.setText(getString(R.string.join_default_server, chosenDefaultServer));
		defaultServerButton.setOnClickListener(this::onJoinDefaultServerClick);

		artClouds=contentView.findViewById(R.id.art_clouds);
		artPlaneElephant=contentView.findViewById(R.id.art_plane_elephant);
		artRightHill=contentView.findViewById(R.id.art_right_hill);
		artLeftHill=contentView.findViewById(R.id.art_left_hill);
		artCenterHill=contentView.findViewById(R.id.art_center_hill);

		artContainer=contentView.findViewById(R.id.art_container);
		blueFill=contentView.findViewById(R.id.blue_fill);
		greenFill=contentView.findViewById(R.id.green_fill);
		motionEffect.addViewEffect(new InterpolatingMotionEffect.ViewEffect(artClouds, V.dp(-5), V.dp(5), V.dp(-5), V.dp(5)));
		motionEffect.addViewEffect(new InterpolatingMotionEffect.ViewEffect(artRightHill, V.dp(-15), V.dp(25), V.dp(-10), V.dp(10)));
		motionEffect.addViewEffect(new InterpolatingMotionEffect.ViewEffect(artLeftHill, V.dp(-25), V.dp(15), V.dp(-15), V.dp(15)));
		motionEffect.addViewEffect(new InterpolatingMotionEffect.ViewEffect(artCenterHill, V.dp(-14), V.dp(14), V.dp(-5), V.dp(25)));
		motionEffect.addViewEffect(new InterpolatingMotionEffect.ViewEffect(artPlaneElephant, V.dp(-20), V.dp(12), V.dp(-20), V.dp(12)));
		artContainer.setOnTouchListener(motionEffect);

		contentView.setSizeListener(new SizeListenerFrameLayout.OnSizeChangedListener(){
			@Override
			public void onSizeChanged(int w, int h, int oldw, int oldh){
				contentView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
						contentView.getViewTreeObserver().removeOnPreDrawListener(this);
						updateArtSize(w, h);
						return true;
					}
				});
			}
		});
		if(currentInviteLink!=null)
			defaultServerButton.setText(getString(R.string.join_server_x_with_invite, currentInviteLink.getHost()));
		else if(!checkedInviteLink)
			maybePickUpInviteLink();

		return contentView;
	}

	// masto.nyc fork: logging in skips the server chooser and goes straight to OAuth on our server.
	private void onLogInClick(View v){
		if(instanceLoadingProgress!=null)
			return;
		instanceLoadingProgress=new ProgressDialog(getActivity());
		instanceLoadingProgress.setCancelable(false);
		instanceLoadingProgress.setMessage(getString(R.string.loading_instance));
		instanceLoadingProgress.show();
		AccountSessionManager.loadInstanceInfo(ForkConfig.INSTANCE_DOMAIN, new Callback<>(){
			@Override
			public void onSuccess(Instance result){
				if(getActivity()==null)
					return;
				if(instanceLoadingProgress!=null)
					instanceLoadingProgress.dismiss();
				instanceLoadingProgress=null;
				AccountSessionManager.getInstance().authenticate(getActivity(), result);
			}

			@Override
			public void onError(ErrorResponse error){
				if(getActivity()==null)
					return;
				if(instanceLoadingProgress!=null)
					instanceLoadingProgress.dismiss();
				instanceLoadingProgress=null;
				error.showToast(getActivity());
			}
		});
	}

	private void onJoinDefaultServerClick(View v){
		instanceLoadingProgress=new ProgressDialog(getActivity());
		instanceLoadingProgress.setCancelable(false);
		instanceLoadingProgress.setMessage(getString(R.string.loading_instance));
		instanceLoadingProgress.show();
		if(currentInviteLink!=null){
			new CheckInviteLink(currentInviteLink.getPath())
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(CheckInviteLink.Response result){
							inviteCode=result.inviteCode;
							proceedWithServerDomain(currentInviteLink.getHost());
						}

						@Override
						public void onError(ErrorResponse error){
							if(getActivity()==null)
								return;
							if(instanceLoadingProgress!=null)
								instanceLoadingProgress.dismiss();
							instanceLoadingProgress=null;
							if(error instanceof MastodonErrorResponse mer){
								switch(mer.httpStatus){
									case 401 -> new M3AlertDialogBuilder(getActivity())
											.setTitle(R.string.expired_invite_link)
											.setMessage(getString(R.string.expired_clipboard_invite_link_alert, currentInviteLink.getHost(), chosenDefaultServer))
											.setPositiveButton(R.string.ok, null)
											.show();
									case 404 -> new M3AlertDialogBuilder(getActivity())
											.setTitle(R.string.invalid_invite_link)
											.setMessage(getString(R.string.invalid_clipboard_invite_link_alert, currentInviteLink.getHost(), chosenDefaultServer))
											.setPositiveButton(R.string.ok, null)
											.show();
									default -> error.showToast(getActivity());
								}
							}
						}
					})
					.execNoAuth(currentInviteLink.getHost());
			return;
		}
		proceedWithServerDomain(chosenDefaultServer);
	}

	private void proceedWithServerDomain(String domain){
		AccountSessionManager.loadInstanceInfo(domain, new Callback<>(){
					@Override
					public void onSuccess(Instance result){
						if(getActivity()==null)
							return;
						if(instanceLoadingProgress!=null)
							instanceLoadingProgress.dismiss();
						instanceLoadingProgress=null;
						if(!result.areRegistrationsOpen() && TextUtils.isEmpty(inviteCode)){
							new M3AlertDialogBuilder(getActivity())
									.setTitle(R.string.error)
									.setMessage(R.string.instance_signup_closed)
									.setPositiveButton(R.string.ok, null)
									.show();
							return;
						}
						Bundle args=new Bundle();
						args.putParcelable("instance", Parcels.wrap(result));
						if(inviteCode!=null)
							args.putString("inviteCode", inviteCode);
						Nav.go(getActivity(), InstanceRulesFragment.class, args);
					}

					@Override
					public void onError(ErrorResponse error){
						if(getActivity()==null)
							return;
						if(instanceLoadingProgress!=null)
							instanceLoadingProgress.dismiss();
						instanceLoadingProgress=null;
						error.showToast(getActivity());
					}
				});
	}

	private void updateArtSize(int w, int h){
		float scale=w/(float)V.dp(360);
		artContainer.setScaleX(scale);
		artContainer.setScaleY(scale);
		blueFill.setScaleY(artContainer.getBottom()-V.dp(90));
		greenFill.setScaleY(h-artContainer.getBottom()+V.dp(90));
	}


	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		super.onApplyWindowInsets(insets);
		int bottomInset=insets.getSystemWindowInsetBottom();
		if(bottomInset>0 && bottomInset<V.dp(36)){
			contentView.setPadding(contentView.getPaddingLeft(), contentView.getPaddingTop(), contentView.getPaddingRight(), V.dp(36));
		}
		((ViewGroup.MarginLayoutParams)blueFill.getLayoutParams()).topMargin=-contentView.getPaddingTop();
		((ViewGroup.MarginLayoutParams)greenFill.getLayoutParams()).bottomMargin=-contentView.getPaddingBottom();
	}

	@Override
	public boolean wantsLightStatusBar(){
		return true;
	}

	@Override
	public boolean wantsLightNavigationBar(){
		return false;
	}

	@Override
	protected void onShown(){
		super.onShown();
		motionEffect.activate();
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		motionEffect.deactivate();
	}

	// masto.nyc fork: replaces upstream's loadAndChooseDefaultServer(), which asked
	// api.joinmastodon.org to pick a random server. Our server is fixed, so all that's left is
	// honoring an invite link on the clipboard — and only if it's an invite to our own server.
	private void maybePickUpInviteLink(){
		checkedInviteLink=true;
		ClipData clipData=getActivity().getSystemService(ClipboardManager.class).getPrimaryClip();
		if(clipData==null || clipData.getItemCount()==0)
			return;
		String clipText=clipData.getItemAt(0).coerceToText(getActivity()).toString();
		if(!HtmlParser.isValidInviteUrl(clipText))
			return;
		Uri inviteLink=Uri.parse(clipText);
		String host=HtmlParser.normalizeDomain(Objects.requireNonNull(inviteLink.getHost()));
		if(!ForkConfig.isOurInstance(host))
			return;
		currentInviteLink=inviteLink;
		defaultServerButton.setText(getString(R.string.join_server_x_with_invite, host));
	}
}
