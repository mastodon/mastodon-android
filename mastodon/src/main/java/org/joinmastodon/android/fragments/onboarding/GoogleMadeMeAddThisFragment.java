package org.joinmastodon.android.fragments.onboarding;

import android.app.Activity;
import android.graphics.Paint;
import android.graphics.Rect;
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
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
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

		items.add(new Item("Mastodon for Android Privacy Policy", "joinmastodon.org", "https://joinmastodon.org/android/privacy", "https://joinmastodon.org/favicon-32x32.png"));
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
		text.setText(R.string.privacy_policy_subtitle);

		adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(new SingleViewRecyclerAdapter(headerView));
		adapter.addAdapter(itemsAdapter=new ItemsAdapter());
		list.setAdapter(adapter);
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorM3SurfaceVariant, 1, 56, 0, DividerItemDecoration.NOT_FIRST));

		btn=view.findViewById(R.id.btn_next);
		btn.setOnClickListener(v->onButtonClick());
		buttonBar=view.findViewById(R.id.button_bar);

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		setStatusBarColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Background));
		view.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Background));
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		getToolbar().setBackground(null);
		getToolbar().setElevation(0);
	}

	protected void onButtonClick(){
		Bundle args=new Bundle();
		args.putParcelable("instance", Parcels.wrap(instance));
		Nav.go(getActivity(), SignupFragment.class, args);
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
					final Item item=new Item(doc.title(), instance.uri, req.url().toString(), "https://"+instance.uri+"/favicon.ico");
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

		public ItemViewHolder(){
			super(getActivity(), R.layout.item_privacy_policy_link, list);
			title=findViewById(R.id.title);
			title.setPaintFlags(title.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		}

		@Override
		public void onBind(Item item){
			title.setText(item.title);
		}

		@Override
		public void onClick(){
			UiUtils.launchWebBrowser(getActivity(), item.url);
		}
	}

	private static class Item{
		public String title, domain, url, faviconUrl;

		public Item(String title, String domain, String url, String faviconUrl){
			this.title=title;
			this.domain=domain;
			this.url=url;
			this.faviconUrl=faviconUrl;
		}
	}
}
