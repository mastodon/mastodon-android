package org.joinmastodon.android.fragments;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ReplacementSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.onboarding.InstanceCatalogSignupFragment;
import org.joinmastodon.android.fragments.onboarding.InstanceChooserLoginFragment;
import org.joinmastodon.android.ui.views.SizeListenerFrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import me.grishka.appkit.Nav;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.utils.V;

public class SplashFragment extends AppKitFragment{

	private SizeListenerFrameLayout contentView;
	private View artContainer, blueFill, greenFill;
	private ViewPager2 pager;
	private ViewGroup pagerDots;
	private View artClouds, artPlaneElephant, artRightHill, artLeftHill, artCenterHill;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		contentView=(SizeListenerFrameLayout) inflater.inflate(R.layout.fragment_splash, container, false);
		contentView.findViewById(R.id.btn_get_started).setOnClickListener(this::onButtonClick);
		contentView.findViewById(R.id.btn_log_in).setOnClickListener(this::onButtonClick);
		artClouds=contentView.findViewById(R.id.art_clouds);
		artPlaneElephant=contentView.findViewById(R.id.art_plane_elephant);
		artRightHill=contentView.findViewById(R.id.art_right_hill);
		artLeftHill=contentView.findViewById(R.id.art_left_hill);
		artCenterHill=contentView.findViewById(R.id.art_center_hill);
		pager=contentView.findViewById(R.id.pager);
		pagerDots=contentView.findViewById(R.id.pager_dots);
		pager.setAdapter(new PagerAdapter());
		pager.setOffscreenPageLimit(3);
		pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){
				for(int i=0;i<pagerDots.getChildCount();i++){
					float alpha;
					if(i==position){
						alpha=0.3f+0.7f*(1f-positionOffset);
					}else if(i==position+1){
						alpha=0.3f+0.7f*positionOffset;
					}else{
						alpha=0.3f;
					}
					pagerDots.getChildAt(i).setAlpha(alpha);
				}

				float parallaxProgress=(position+positionOffset)/2f;
				artClouds.setTranslationX(V.dp(-27)*(position>=1 ? 1f : positionOffset));
				artPlaneElephant.setTranslationX(V.dp(101.55f)*parallaxProgress);
				artLeftHill.setTranslationX(V.dp(-88)*parallaxProgress);
				artLeftHill.setTranslationY(V.dp(24)*parallaxProgress);
				artRightHill.setTranslationX(V.dp(-88)*parallaxProgress);
				artRightHill.setTranslationY(V.dp(-24)*parallaxProgress);
				artCenterHill.setTranslationX(V.dp(-40)*parallaxProgress);
			}
		});

		artContainer=contentView.findViewById(R.id.art_container);
		blueFill=contentView.findViewById(R.id.blue_fill);
		greenFill=contentView.findViewById(R.id.green_fill);

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

	private class PagerAdapter extends RecyclerView.Adapter<PagerViewHolder>{

		@NonNull
		@Override
		public PagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new PagerViewHolder(viewType);
		}

		@Override
		public void onBindViewHolder(@NonNull PagerViewHolder holder, int position){}

		@Override
		public int getItemCount(){
			return 3;
		}

		@Override
		public int getItemViewType(int position){
			return position;
		}
	}

	private class PagerViewHolder extends RecyclerView.ViewHolder{
		public PagerViewHolder(int page){
			super(new LinearLayout(getActivity()));
			LinearLayout ll=(LinearLayout) itemView;
			ll.setOrientation(LinearLayout.VERTICAL);
			int pad=V.dp(16);
			ll.setPadding(pad, pad, pad, pad);
			ll.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

			TextView title=new TextView(getActivity());
			title.setTextAppearance(R.style.m3_headline_medium);
			title.setText(switch(page){
				case 0 -> getString(R.string.welcome_page1_title);
				case 1 -> getString(R.string.welcome_page2_title);
				case 2 -> getString(R.string.welcome_page3_title);
				default -> throw new IllegalStateException("Unexpected value: "+page);
			});
			title.setTextColor(0xFF17063B);
			LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(page==0 ? 46 : 36));
			lp.bottomMargin=V.dp(page==0 ? 4 : 14);
			ll.addView(title, lp);

			TextView text=new TextView(getActivity());
			text.setTextAppearance(R.style.m3_body_medium);
			text.setText(switch(page){
				case 0 -> R.string.welcome_page1_text;
				case 1 -> R.string.welcome_page2_text;
				case 2 -> R.string.welcome_page3_text;
				default -> throw new IllegalStateException("Unexpected value: "+page);
			});
			text.setTextColor(0xFF17063B);
			ll.addView(text, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}
	}
}
