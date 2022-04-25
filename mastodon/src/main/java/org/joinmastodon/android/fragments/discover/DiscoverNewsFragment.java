package org.joinmastodon.android.fragments.discover;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.trends.GetTrendingLinks;
import org.joinmastodon.android.fragments.ScrollableToTop;
import org.joinmastodon.android.model.Card;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.drawables.BlurhashCrossfadeDrawable;
import org.joinmastodon.android.ui.utils.DiscoverInfoBannerHelper;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class DiscoverNewsFragment extends BaseRecyclerFragment<Card> implements ScrollableToTop{
	private String accountID;
	private List<ImageLoaderRequest> imageRequests=Collections.emptyList();
	private DiscoverInfoBannerHelper bannerHelper=new DiscoverInfoBannerHelper(DiscoverInfoBannerHelper.BannerType.TRENDING_LINKS);

	public DiscoverNewsFragment(){
		super(10);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetTrendingLinks()
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Card> result){
						imageRequests=result.stream()
								.map(card->TextUtils.isEmpty(card.image) ? null : new UrlImageLoaderRequest(card.image, V.dp(150), V.dp(150)))
								.collect(Collectors.toList());
						onDataLoaded(result, false);
					}
				})
				.exec(accountID);
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		return new LinksAdapter();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorPollVoted, 1, 0, 0));
		bannerHelper.maybeAddBanner(contentWrap);
	}

	@Override
	public void scrollToTop(){
		smoothScrollRecyclerViewToTop(list);
	}

	private class LinksAdapter extends UsableRecyclerView.Adapter<LinkViewHolder> implements ImageLoaderRecyclerAdapter{
		public LinksAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public LinkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new LinkViewHolder();
		}

		@Override
		public int getItemCount(){
			return data.size();
		}

		@Override
		public void onBindViewHolder(LinkViewHolder holder, int position){
			holder.bind(data.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getImageCountForItem(int position){
			return imageRequests.get(position)==null ? 0 : 1;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return imageRequests.get(position);
		}
	}

	private class LinkViewHolder extends BindableViewHolder<Card> implements UsableRecyclerView.Clickable, ImageLoaderViewHolder{
		private final TextView name, title, subtitle;
		private final ImageView photo;
		private BlurhashCrossfadeDrawable crossfadeDrawable=new BlurhashCrossfadeDrawable();
		private boolean didClear;

		public LinkViewHolder(){
			super(getActivity(), R.layout.item_trending_link, list);
			name=findViewById(R.id.name);
			title=findViewById(R.id.title);
			subtitle=findViewById(R.id.subtitle);
			photo=findViewById(R.id.photo);
			photo.setOutlineProvider(OutlineProviders.roundedRect(2));
			photo.setClipToOutline(true);
		}

		@Override
		public void onBind(Card item){
			name.setText(item.providerName);
			title.setText(item.title);
			int num=item.history.get(0).uses;
			if(item.history.size()>1)
				num+=item.history.get(1).uses;
			subtitle.setText(getResources().getQuantityString(R.plurals.discussed_x_times, num, num));
			crossfadeDrawable.setSize(item.width, item.height);
			crossfadeDrawable.setBlurhashDrawable(item.blurhashPlaceholder);
			crossfadeDrawable.setCrossfadeAlpha(0f);
			photo.setImageDrawable(null);
			photo.setImageDrawable(crossfadeDrawable);
			didClear=false;
		}

		@Override
		public void setImage(int index, Drawable drawable){
			crossfadeDrawable.setImageDrawable(drawable);
			if(didClear)
				crossfadeDrawable.animateAlpha(0f);
		}

		@Override
		public void clearImage(int index){
			crossfadeDrawable.setCrossfadeAlpha(1f);
			didClear=true;
		}

		@Override
		public void onClick(){
			UiUtils.launchWebBrowser(getActivity(), item.url);
		}
	}
}
