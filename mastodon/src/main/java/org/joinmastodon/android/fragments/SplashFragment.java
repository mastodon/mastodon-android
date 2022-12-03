package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.onboarding.InstanceCatalogSignupFragment;
import org.joinmastodon.android.fragments.onboarding.InstanceChooserLoginFragment;
import org.joinmastodon.android.ui.InterpolatingMotionEffect;
import org.joinmastodon.android.ui.views.SizeListenerFrameLayout;

import androidx.annotation.Nullable;
import me.grishka.appkit.Nav;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.utils.V;

public class SplashFragment extends AppKitFragment{

	private SizeListenerFrameLayout contentView;
	private View artContainer, blueFill, greenFill;
	private InterpolatingMotionEffect motionEffect;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		motionEffect=new InterpolatingMotionEffect(MastodonApp.context);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		contentView=(SizeListenerFrameLayout) inflater.inflate(R.layout.fragment_splash, container, false);
		contentView.findViewById(R.id.btn_get_started).setOnClickListener(this::onButtonClick);
		contentView.findViewById(R.id.btn_log_in).setOnClickListener(this::onButtonClick);

		artContainer=contentView.findViewById(R.id.art_container);
		blueFill=contentView.findViewById(R.id.blue_fill);
		greenFill=contentView.findViewById(R.id.green_fill);
		motionEffect.addViewEffect(new InterpolatingMotionEffect.ViewEffect(contentView.findViewById(R.id.art_clouds), V.dp(-5), V.dp(5), V.dp(-5), V.dp(5)));
		motionEffect.addViewEffect(new InterpolatingMotionEffect.ViewEffect(contentView.findViewById(R.id.art_right_hill), V.dp(-15), V.dp(25), V.dp(-10), V.dp(10)));
		motionEffect.addViewEffect(new InterpolatingMotionEffect.ViewEffect(contentView.findViewById(R.id.art_left_hill), V.dp(-25), V.dp(15), V.dp(-15), V.dp(15)));
		motionEffect.addViewEffect(new InterpolatingMotionEffect.ViewEffect(contentView.findViewById(R.id.art_center_hill), V.dp(-14), V.dp(14), V.dp(-5), V.dp(25)));
		motionEffect.addViewEffect(new InterpolatingMotionEffect.ViewEffect(contentView.findViewById(R.id.art_plane_elephant), V.dp(-20), V.dp(12), V.dp(-20), V.dp(12)));

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

	private void updateArtSize(int w, int h){
		float scale=w/(float)V.dp(412);
		artContainer.setScaleX(scale);
		artContainer.setScaleY(scale);
		blueFill.setScaleY(h/2f);
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
}
