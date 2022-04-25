package org.joinmastodon.android.ui.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;

import me.grishka.appkit.utils.CubicBezierInterpolator;

public class DiscoverInfoBannerHelper{
	private View banner;
	private final BannerType type;

	public DiscoverInfoBannerHelper(BannerType type){
		this.type=type;
	}

	private SharedPreferences getPrefs(){
		return MastodonApp.context.getSharedPreferences("onboarding", Context.MODE_PRIVATE);
	}

	public void maybeAddBanner(FrameLayout view){
		if(!getPrefs().getBoolean("bannerHidden_"+type, false)){
			((Activity)view.getContext()).getLayoutInflater().inflate(R.layout.discover_info_banner, view);
			banner=view.findViewById(R.id.discover_info_banner);
			view.findViewById(R.id.banner_dismiss).setOnClickListener(this::onDismissClick);
			TextView text=view.findViewById(R.id.banner_text);
			text.setText(switch(type){
				case TRENDING_POSTS -> R.string.trending_posts_info_banner;
				case TRENDING_HASHTAGS -> R.string.trending_hashtags_info_banner;
				case TRENDING_LINKS -> R.string.trending_links_info_banner;
				case LOCAL_TIMELINE -> R.string.local_timeline_info_banner;
			});
		}
	}

	private void onDismissClick(View v){
		if(banner==null)
			return;
		View _banner=banner;
		banner.animate()
				.alpha(0)
				.setDuration(200)
				.setInterpolator(CubicBezierInterpolator.DEFAULT)
				.withEndAction(()->((ViewGroup)_banner.getParent()).removeView(_banner))
				.start();
		getPrefs().edit().putBoolean("bannerHidden_"+type, true).apply();
		banner=null;
	}

	public enum BannerType{
		TRENDING_POSTS,
		TRENDING_HASHTAGS,
		TRENDING_LINKS,
		LOCAL_TIMELINE,
//		ACCOUNTS
	}
}
