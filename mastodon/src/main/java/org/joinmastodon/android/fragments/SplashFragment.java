package org.joinmastodon.android.fragments;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.ProgressBar;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.catalog.GetCatalogDefaultInstances;
import org.joinmastodon.android.api.requests.instance.GetInstance;
import org.joinmastodon.android.fragments.onboarding.InstanceCatalogSignupFragment;
import org.joinmastodon.android.fragments.onboarding.InstanceChooserLoginFragment;
import org.joinmastodon.android.fragments.onboarding.InstanceRulesFragment;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.catalog.CatalogDefaultInstance;
import org.joinmastodon.android.ui.InterpolatingMotionEffect;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.ProgressBarButton;
import org.joinmastodon.android.ui.views.SizeListenerFrameLayout;
import org.parceler.Parcels;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import androidx.annotation.Nullable;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;

public class SplashFragment extends AppKitFragment{

	private static final String DEFAULT_SERVER="mastodon.social";

	private SizeListenerFrameLayout contentView;
	private View artContainer, blueFill, greenFill;
	private InterpolatingMotionEffect motionEffect;
	private View artClouds, artPlaneElephant, artRightHill, artLeftHill, artCenterHill;
	private ProgressBarButton defaultServerButton;
	private ProgressBar defaultServerProgress;
	private String chosenDefaultServer=DEFAULT_SERVER;
	private boolean loadingDefaultServer;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		motionEffect=new InterpolatingMotionEffect(MastodonApp.context);
		loadAndChooseDefaultServer();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		contentView=(SizeListenerFrameLayout) inflater.inflate(R.layout.fragment_splash, container, false);
		contentView.findViewById(R.id.btn_get_started).setOnClickListener(this::onButtonClick);
		contentView.findViewById(R.id.btn_log_in).setOnClickListener(this::onButtonClick);
		defaultServerButton=contentView.findViewById(R.id.btn_join_default_server);
		defaultServerButton.setText(getString(R.string.join_default_server, chosenDefaultServer));
		defaultServerButton.setOnClickListener(this::onJoinDefaultServerClick);
		defaultServerProgress=contentView.findViewById(R.id.action_progress);
		if(loadingDefaultServer){
			defaultServerButton.setTextVisible(false);
			defaultServerProgress.setVisibility(View.VISIBLE);
		}
		contentView.findViewById(R.id.btn_learn_more).setOnClickListener(this::onLearnMoreClick);

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

		return contentView;
	}

	private void onButtonClick(View v){
		Bundle extras=new Bundle();
		boolean isSignup=v.getId()==R.id.btn_get_started;
		extras.putBoolean("signup", isSignup);
		Nav.go(getActivity(), isSignup ? InstanceCatalogSignupFragment.class : InstanceChooserLoginFragment.class, extras);
	}

	private void onJoinDefaultServerClick(View v){
		if(loadingDefaultServer)
			return;
		new GetInstance()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Instance result){
						if(getActivity()==null)
							return;
						if(!result.registrations){
							new M3AlertDialogBuilder(getActivity())
									.setTitle(R.string.error)
									.setMessage(R.string.instance_signup_closed)
									.setPositiveButton(R.string.ok, null)
									.show();
							return;
						}
						Bundle args=new Bundle();
						args.putParcelable("instance", Parcels.wrap(result));
						Nav.go(getActivity(), InstanceRulesFragment.class, args);
					}

					@Override
					public void onError(ErrorResponse error){
						if(getActivity()==null)
							return;
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.loading_instance, true)
				.execNoAuth(chosenDefaultServer);
	}

	private void onLearnMoreClick(View v){
		View sheetView=getActivity().getLayoutInflater().inflate(R.layout.intro_bottom_sheet, null);
		BottomSheet sheet=new BottomSheet(getActivity());
		sheet.setContentView(sheetView);
		sheet.setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Surface),
				UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());
		sheet.show();
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
		return true;
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

	private void loadAndChooseDefaultServer(){
		loadingDefaultServer=true;
		new GetCatalogDefaultInstances()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<CatalogDefaultInstance> result){
						if(result.isEmpty()){
							setChosenDefaultServer(DEFAULT_SERVER);
							return;
						}
						float sum=0f;
						for(CatalogDefaultInstance inst:result){
							sum+=inst.weight;
						}
						if(sum<=0)
							sum=1f;
						for(CatalogDefaultInstance inst:result){
							inst.weight/=sum;
						}
						float rand=ThreadLocalRandom.current().nextFloat();
						float prev=0f;
						for(CatalogDefaultInstance inst:result){
							if(rand>=prev && rand<prev+inst.weight){
								setChosenDefaultServer(inst.domain);
								return;
							}
							prev+=inst.weight;
						}
						// Just in case something didn't add up
						setChosenDefaultServer(result.get(result.size()-1).domain);
					}

					@Override
					public void onError(ErrorResponse error){
						setChosenDefaultServer(DEFAULT_SERVER);
					}
				})
				.execNoAuth("");
	}

	private void setChosenDefaultServer(String domain){
		chosenDefaultServer=domain;
		loadingDefaultServer=false;
		if(defaultServerButton!=null && getActivity()!=null){
			defaultServerButton.setTextVisible(true);
			defaultServerProgress.setVisibility(View.GONE);
			defaultServerButton.setText(getString(R.string.join_default_server, chosenDefaultServer));
		}
	}
}
