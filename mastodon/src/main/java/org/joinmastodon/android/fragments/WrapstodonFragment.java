package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.requests.annual_reports.GetAnnualReports;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AnnualReport;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.wrapstodon.AppsWrapScene;
import org.joinmastodon.android.ui.wrapstodon.ArchetypeWrapScene;
import org.joinmastodon.android.ui.wrapstodon.ByTheNumbersWrapScene;
import org.joinmastodon.android.ui.wrapstodon.FavoriteAccountsWrapScene;
import org.joinmastodon.android.ui.wrapstodon.FavoriteHashtagsWrapScene;
import org.joinmastodon.android.ui.wrapstodon.InteractedAccountsWrapScene;
import org.joinmastodon.android.ui.wrapstodon.SummaryWrapScene;
import org.joinmastodon.android.ui.wrapstodon.TimeSeriesWrapScene;
import org.joinmastodon.android.ui.wrapstodon.WelcomeWrapScene;
import org.joinmastodon.android.ui.wrapstodon.AnnualWrapScene;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.utils.V;

public class WrapstodonFragment extends AppKitFragment{
	private boolean statusBarHidden;
	private FrameLayout contentWrap;
	private ViewPager2 pager;
	private ProgressBar progress;
	private FrameLayout innerWrap;
	private boolean dataLoaded;
	private String accountID;

	private List<AnnualWrapScene> scenes=List.of();

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		if(BuildConfig.DEBUG && new File(getActivity().getFilesDir(), "mockAnnualReports.json").exists()){
			try(FileReader reader=new FileReader(new File(getActivity().getFilesDir(), "mockAnnualReports.json"))){
				GetAnnualReports.Response res=MastodonAPIController.gson.fromJson(reader, GetAnnualReports.Response.class);
				showReport(res);
			}catch(IOException x){
				throw new RuntimeException(x);
			}
		}else{
			new GetAnnualReports()
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(GetAnnualReports.Response result){
							showReport(result);
						}

						@Override
						public void onError(ErrorResponse error){
							Activity a=getActivity();
							if(a==null)
								return;
							error.showToast(a);
							Nav.finish(WrapstodonFragment.this);
						}
					})
					.exec(accountID);
		}
	}

	private void showReport(GetAnnualReports.Response result){
		if(result.annualReports.isEmpty()){
			Nav.finish(WrapstodonFragment.this);
			return;
		}
		dataLoaded=true;
		if(progress!=null){
			V.setVisibilityAnimated(progress, View.GONE);
			V.setVisibilityAnimated(innerWrap, View.VISIBLE);
		}
		AccountSession session=AccountSessionManager.get(accountID);

		String year=String.valueOf(result.annualReports.get(result.annualReports.size()-1).year);
		AnnualReport report=result.annualReports.get(result.annualReports.size()-1).data;
		Map<String, Account> accounts=result.accounts.stream().collect(Collectors.toMap(a->a.id, Function.identity(), (a1, a2)->a2));
		Map<String, Status> statuses=result.statuses.stream().collect(Collectors.toMap(s->s.id, Function.identity(), (s1, s2)->s2));

		scenes=List.of(
				new WelcomeWrapScene(),
				new ArchetypeWrapScene(session.self.username, session.domain, session.self.avatarStatic, report.archetype),
				new ByTheNumbersWrapScene(report.typeDistribution),
				new FavoriteAccountsWrapScene(accounts, report.mostRebloggedAccounts),
				new FavoriteHashtagsWrapScene(report.topHashtags),
				new InteractedAccountsWrapScene(session.self, accounts, report.commonlyInteractedWithAccounts),
				new AppsWrapScene(report.mostUsedApps),
				new TimeSeriesWrapScene(report.timeSeries),
				new SummaryWrapScene(session.self, accounts, statuses, report)
		);

		for(AnnualWrapScene scene:scenes){
			scene.setYear(year);
		}

		if(pager!=null)
			pager.getAdapter().notifyDataSetChanged();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		contentWrap=new ContentWrapFrameLayout(getActivity());
		contentWrap.setBackgroundColor(0xff000000); // TODO optimize overdraw

		progress=new ProgressBar(getActivity());
		progress.setIndeterminateTintList(ColorStateList.valueOf(0xffffffff));
		contentWrap.addView(progress, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

		innerWrap=new InnerWrapFrameLayout(getActivity());
		innerWrap.setBackgroundColor(0xFF17063B);
		innerWrap.setOutlineProvider(OutlineProviders.roundedRect(8));
		innerWrap.setClipToOutline(true);
		contentWrap.addView(innerWrap, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

		pager=new ViewPager2(getActivity());
		pager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
		pager.setAdapter(new ScenesAdapter());
		pager.setOffscreenPageLimit(1);
		innerWrap.addView(pager, new FrameLayout.LayoutParams(V.dp(360), V.dp(640), Gravity.CENTER));

		ContextThemeWrapper themeContext=new ContextThemeWrapper(getActivity(), R.style.Theme_Mastodon_Dark);
		LayoutInflater.from(themeContext).inflate(R.layout.wrap_top_bar, innerWrap);
		innerWrap.findViewById(R.id.btn_back).setOnClickListener(v->Nav.finish(this));
		Button shareButton=innerWrap.findViewById(R.id.btn_share);
		shareButton.setOnClickListener(v->shareCurrentScene());
		shareButton.setVisibility(View.INVISIBLE);

		if(dataLoaded)
			progress.setVisibility(View.GONE);
		else
			innerWrap.setVisibility(View.INVISIBLE);

		pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
			private boolean isFirst=true;

			@Override
			public void onPageSelected(int position){
				if(isFirst!=(position==0)){
					isFirst=position==0;
					V.setVisibilityAnimated(shareButton, isFirst ? View.INVISIBLE : View.VISIBLE);
				}
			}
		});

		return contentWrap;
	}

	@Override
	protected void onShown(){
		super.onShown();
		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	@Override
	protected void onHidden(){
		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		super.onHidden();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		super.onApplyWindowInsets(insets);
		contentWrap.setPadding(0, statusBarHidden ? 0 : insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
	}

	@Override
	public boolean wantsLightNavigationBar(){
		return false;
	}

	@Override
	public boolean wantsLightStatusBar(){
		return false;
	}

	private void shareCurrentScene(){
		Bitmap bmp=scenes.get(pager.getCurrentItem()).renderToBitmap();
		try(FileOutputStream out=new FileOutputStream(new File(getActivity().getCacheDir(), "wrapstodon.png"))){
			bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
			Intent intent=new Intent(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://"+getActivity().getPackageName()+".provider.wrapstodon_share/wrapstodon.png"));
			intent.setType("image/png");
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(Intent.createChooser(intent, getString(R.string.share_toot_title)));
		}catch(IOException x){
			Toast.makeText(getActivity(), R.string.error_saving_file, Toast.LENGTH_SHORT).show();
		}
	}

	private class ContentWrapFrameLayout extends FrameLayout{
		public ContentWrapFrameLayout(@NonNull Context context){
			super(context);
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh){
			super.onSizeChanged(w, h, oldw, oldh);
			post(this::updateStatusBarVisibility);
		}

		private void updateStatusBarVisibility(){
			float aspect=(float)getWidth()/(getHeight()-getPaddingBottom());
			// Hide the status bar if the screen is 9:16 or wider
			if(aspect>=0.5625f){
				setSystemUiVisibility(getSystemUiVisibility() | SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
				statusBarHidden=true;
			}else{
				setSystemUiVisibility(getSystemUiVisibility() & ~(SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_IMMERSIVE_STICKY));
				statusBarHidden=false;
			}
		}
	}

	private class InnerWrapFrameLayout extends FrameLayout{
		public InnerWrapFrameLayout(@NonNull Context context){
			super(context);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
			int w=MeasureSpec.getSize(widthMeasureSpec);
			int h=MeasureSpec.getSize(heightMeasureSpec);
			int mw, mh;
			if(w>h){
				mw=Math.round(h*0.5626f);
				mh=h;
			}else{
				float aspect=(float)w/h;
				if(aspect<0.5625f){ // 9:16 or taller
					mw=w;
					mh=Math.round(w/0.5625f);
				}else if(aspect<0.62){ // special case for 9:16 screens with a navigation bar
					mw=w;
					mh=h;
				}else{
					mw=Math.round(h*0.5626f);
					mh=h;
				}
			}
			float contentScaleFactor=(float)mw/V.dp(360);
			View child0=getChildAt(0);
			child0.getLayoutParams().height=Math.round(mh/contentScaleFactor);
			child0.setScaleX(contentScaleFactor);
			child0.setScaleY(contentScaleFactor);
			super.onMeasure(mw | MeasureSpec.EXACTLY, mh | MeasureSpec.EXACTLY);
		}
	}

	private class ScenesAdapter extends RecyclerView.Adapter<SceneViewHolder>{

		@NonNull
		@Override
		public SceneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new SceneViewHolder(scenes.get(viewType));
		}

		@Override
		public void onBindViewHolder(@NonNull SceneViewHolder holder, int position){

		}

		@Override
		public int getItemCount(){
			return scenes.size();
		}

		@Override
		public int getItemViewType(int position){
			return position;
		}
	}

	private class SceneViewHolder extends RecyclerView.ViewHolder{
		public SceneViewHolder(AnnualWrapScene scene){
			super(scene.createContentView(getActivity()));
			itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}
	}
}
