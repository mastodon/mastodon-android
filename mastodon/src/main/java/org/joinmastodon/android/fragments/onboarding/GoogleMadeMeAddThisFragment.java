package org.joinmastodon.android.fragments.onboarding;

import android.app.Activity;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.ElevationOnScrollListener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.parceler.Parcels;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;
import me.grishka.appkit.views.UsableRecyclerView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GoogleMadeMeAddThisFragment extends ToolbarFragment{
	private UsableRecyclerView list;
	private MergeRecyclerAdapter adapter;
	private Button btn;
	private View buttonBar;
	private Instance instance;
	private ArrayList<Item> items=new ArrayList<>();
	private Call currentRequest;
	private ItemsAdapter itemsAdapter;
	private ElevationOnScrollListener onScrollListener;

	private static final int SIGNUP_REQUEST=722;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setTitle(R.string.privacy_policy_title);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setNavigationBarColor(UiUtils.getThemeColor(activity, R.attr.colorWindowBackground));
		instance=Parcels.unwrap(getArguments().getParcelable("instance"));

		items.add(new Item("Mastodon for Android Privacy Policy", getString(R.string.privacy_policy_explanation), "joinmastodon.org", "https://joinmastodon.org/android/privacy", "https://joinmastodon.org/favicon-32x32.png"));
		loadServerPrivacyPolicy();
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
		}
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_onboarding_rules, container, false);

		list=view.findViewById(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		View headerView=inflater.inflate(R.layout.item_list_header_simple, list, false);
		TextView text=headerView.findViewById(R.id.text);
		text.setText(getString(R.string.privacy_policy_subtitle, instance.uri));

		adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(new SingleViewRecyclerAdapter(headerView));
		adapter.addAdapter(itemsAdapter=new ItemsAdapter());
		list.setAdapter(adapter);

		btn=view.findViewById(R.id.btn_next);
		btn.setOnClickListener(v->onButtonClick());
		buttonBar=view.findViewById(R.id.button_bar);

		Button backBtn=view.findViewById(R.id.btn_back);
		backBtn.setText(getString(R.string.server_policy_disagree, instance.uri));
		backBtn.setOnClickListener(v->{
			setResult(false, null);
			Nav.finish(this);
		});

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addOnScrollListener(onScrollListener=new ElevationOnScrollListener((FragmentRootLinearLayout) view, buttonBar, getToolbar()));
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		if(onScrollListener!=null){
			onScrollListener.setViews(buttonBar, getToolbar());
		}
	}

	protected void onButtonClick(){
		Bundle args=new Bundle();
		args.putParcelable("instance", Parcels.wrap(instance));
		Nav.goForResult(getActivity(), SignupFragment.class, args, SIGNUP_REQUEST, this);
	}

	@Override
	public void onFragmentResult(int reqCode, boolean success, Bundle result){
		super.onFragmentResult(reqCode, success, result);
		if(reqCode==SIGNUP_REQUEST && !success){
			setResult(false, null);
			Nav.finish(this);
		}
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

	private void loadServerPrivacyPolicy(){
		Request req=new Request.Builder()
				.url("https://"+instance.uri+"/terms")
				.addHeader("Accept-Language", Locale.getDefault().toLanguageTag())
				.build();
		currentRequest=MastodonAPIController.getHttpClient().newCall(req);
		currentRequest.enqueue(new Callback(){
			@Override
			public void onFailure(@NonNull Call call, @NonNull IOException e){
				currentRequest=null;
			}

			@Override
			public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException{
				currentRequest=null;
				try(ResponseBody body=response.body()){
					if(!response.isSuccessful())
						return;
					Document doc=Jsoup.parse(Objects.requireNonNull(body).byteStream(), Objects.requireNonNull(body.contentType()).charset(StandardCharsets.UTF_8).name(), req.url().toString());
					final Item item=new Item(doc.title(), null, instance.uri, req.url().toString(), "https://"+instance.uri+"/favicon.ico");
					Activity activity=getActivity();
					if(activity!=null){
						activity.runOnUiThread(()->{
							items.add(item);
							itemsAdapter.notifyItemInserted(items.size()-1);
						});
					}
				}
			}
		});
	}

	private class ItemsAdapter extends RecyclerView.Adapter<ItemViewHolder>{

		@NonNull
		@Override
		public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new ItemViewHolder();
		}

		@Override
		public void onBindViewHolder(@NonNull ItemViewHolder holder, int position){
			holder.bind(items.get(position));
		}

		@Override
		public int getItemCount(){
			return items.size();
		}
	}

	private class ItemViewHolder extends BindableViewHolder<Item> implements UsableRecyclerView.Clickable{
		private final TextView title;
		private final TextView subtitle;

		public ItemViewHolder(){
			super(getActivity(), R.layout.item_privacy_policy_link, list);
			title=findViewById(R.id.title);
			subtitle=findViewById(R.id.subtitle);
		}

		@Override
		public void onBind(Item item){
			title.setText(item.title);
			if(TextUtils.isEmpty(item.subtitle)){
				subtitle.setVisibility(View.GONE);
			}else{
				subtitle.setVisibility(View.VISIBLE);
				subtitle.setText(item.subtitle);
			}
		}

		@Override
		public void onClick(){
			UiUtils.launchWebBrowser(getActivity(), item.url);
		}
	}

	private static class Item{
		public String title, subtitle, domain, url, faviconUrl;

		public Item(String title, String subtitle, String domain, String url, String faviconUrl){
			this.title=title;
			this.subtitle=subtitle;
			this.domain=domain;
			this.url=url;
			this.faviconUrl=faviconUrl;
		}
	}
}
