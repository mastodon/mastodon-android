package org.joinmastodon.android.fragments.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.requests.instance.GetInstanceExtendedDescription;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;
import org.joinmastodon.android.ui.viewholders.SimpleListItemViewHolder;
import org.joinmastodon.android.ui.views.FixedAspectRatioImageView;
import org.joinmastodon.android.utils.ViewImageLoaderHolderTarget;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class SettingsServerAboutFragment extends LoaderFragment{
	private String accountID;
	private Instance instance;

	private WebView webView;
	private LinearLayout scrollingLayout;
	public ScrollView scroller;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		instance=AccountSessionManager.getInstance().getInstanceInfo(AccountSessionManager.get(accountID).domain);
		loadData();
	}

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		webView=new WebView(getActivity());
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient(){
			@Override
			public void onPageFinished(WebView view, String url){
				dataLoaded();
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url){
				Uri uri=Uri.parse(url);
				if(uri.getScheme().equals("http") || uri.getScheme().equals("https")){
					UiUtils.launchWebBrowser(getActivity(), url);
				}else{
					Intent intent=new Intent(Intent.ACTION_VIEW, uri);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					try{
						startActivity(intent);
					}catch(ActivityNotFoundException x){
						Toast.makeText(getActivity(), R.string.no_app_to_handle_action, Toast.LENGTH_SHORT).show();
					}
				}
				return true;
			}
		});

		scrollingLayout=new LinearLayout(getActivity());
		scrollingLayout.setOrientation(LinearLayout.VERTICAL);
		scroller=new ScrollView(getActivity());
		scroller.setNestedScrollingEnabled(true);
		scroller.setClipToPadding(false);
		scroller.addView(scrollingLayout);

		FixedAspectRatioImageView banner=new FixedAspectRatioImageView(getActivity());
		banner.setAspectRatio(1.914893617f);
		banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
		banner.setOutlineProvider(OutlineProviders.bottomRoundedRect(16));
		banner.setClipToOutline(true);
		ViewImageLoader.loadWithoutAnimation(banner, getResources().getDrawable(R.drawable.image_placeholder, getActivity().getTheme()), new UrlImageLoaderRequest(instance.thumbnail));
		LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		blp.bottomMargin=V.dp(24);
		scrollingLayout.addView(banner, blp);

		boolean needDivider=false;
		if(instance.contactAccount!=null){
			needDivider=true;
			TextView heading=new TextView(getActivity());
			heading.setTextAppearance(R.style.m3_title_small);
			heading.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant));
			heading.setSingleLine();
			heading.setText(R.string.server_administrator);
			heading.setGravity(Gravity.CENTER_VERTICAL);
			LinearLayout.LayoutParams hlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(20));
			hlp.bottomMargin=V.dp(4);
			hlp.leftMargin=hlp.rightMargin=V.dp(16);
			scrollingLayout.addView(heading, hlp);

			AccountViewModel model=new AccountViewModel(instance.contactAccount, accountID);
			AccountViewHolder holder=new AccountViewHolder(this, scrollingLayout, null);
			holder.bind(model);
			holder.itemView.setBackground(UiUtils.getThemeDrawable(getActivity(), android.R.attr.selectableItemBackground));
			holder.itemView.setOnClickListener(v->holder.onClick());
			scrollingLayout.addView(holder.itemView);
			ViewImageLoader.load(new ViewImageLoaderHolderTarget(holder, 0), null, model.avaRequest, false);
			for(int i=0;i<model.emojiHelper.getImageCount();i++){
				ViewImageLoader.load(new ViewImageLoaderHolderTarget(holder, i+1), null, model.emojiHelper.getImageRequest(i), false);
			}
		}
		if(!TextUtils.isEmpty(instance.email)){
			needDivider=true;
			SimpleListItemViewHolder holder=new SimpleListItemViewHolder(getActivity(), scrollingLayout);
			ListItem<Void> item=new ListItem<>(R.string.send_email_to_server_admin, 0, R.drawable.ic_mail_24px, ()->{});
			holder.bind(item);
			holder.itemView.setBackground(UiUtils.getThemeDrawable(getActivity(), android.R.attr.selectableItemBackground));
			holder.itemView.setOnClickListener(v->openAdminEmail());
			scrollingLayout.addView(holder.itemView);
		}
		if(needDivider){
			View divider=new View(getActivity());
			divider.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
			LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(1));
			dlp.leftMargin=dlp.rightMargin=V.dp(16);
			scrollingLayout.addView(divider, dlp);
		}
		scrollingLayout.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		return scroller;
	}

	@Override
	protected void doLoadData(){
		new GetInstanceExtendedDescription()
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(GetInstanceExtendedDescription.Response result){
						MastodonAPIController.runInBackground(()->{
							Activity activity=getActivity();
							if(activity==null)
								return;
							String template;
							try(BufferedReader reader=new BufferedReader(new InputStreamReader(getActivity().getAssets().open("server_about_template.htm")))){
								StringBuilder sb=new StringBuilder();
								String line;
								while((line=reader.readLine())!=null){
									sb.append(line);
									sb.append('\n');
								}
								template=sb.toString();
							}catch(IOException x){
								throw new RuntimeException(x);
							}

							HashMap<String, String> templateParams=new HashMap<>();
							templateParams.put("content", result.content);
							templateParams.put("colorSurface", getThemeColorAsCss(R.attr.colorM3Surface, 1));
							templateParams.put("colorOnSurface", getThemeColorAsCss(R.attr.colorM3OnSurface, 1));
							templateParams.put("colorPrimary", getThemeColorAsCss(R.attr.colorM3Primary, 1));
							templateParams.put("colorPrimaryTransparent", getThemeColorAsCss(R.attr.colorM3Primary, 0.2f));
							for(Map.Entry<String, String> param:templateParams.entrySet()){
								template=template.replace("{{"+param.getKey()+"}}", param.getValue());
							}

							final String html=template;
							activity.runOnUiThread(()->{
								webView.loadDataWithBaseURL(null, html, "text/html; charset=utf-8", null, null);
							});
						});
					}
				})
				.exec(accountID);
	}

	@Override
	public void onRefresh(){}

	private void openAdminEmail(){
		Intent intent=new Intent(Intent.ACTION_VIEW, Uri.fromParts("mailto", instance.email, null));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try{
			startActivity(intent);
		}catch(ActivityNotFoundException x){
			Toast.makeText(getActivity(), R.string.no_app_to_handle_action, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
			scroller.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
			progress.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
			insets=insets.inset(0, 0, 0, insets.getSystemWindowInsetBottom());
		}else{
			scroller.setPadding(0, 0, 0, 0);
		}
		super.onApplyWindowInsets(insets);
	}

	private String getThemeColorAsCss(int attr, float alpha){
		int color=UiUtils.getThemeColor(getActivity(), attr);
		if(alpha==1f){
			return String.format(Locale.US, "#%06X", color & 0xFFFFFF);
		}else{
			int r=(color >> 16) & 0xFF;
			int g=(color >> 8) & 0xFF;
			int b=color & 0xFF;
			return "rgba("+r+","+g+","+b+","+alpha+")";
		}
	}
}
