package org.joinmastodon.android.fragments.discover;

import android.graphics.Rect;
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
import org.joinmastodon.android.model.viewmodel.CardViewModel;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.drawables.BlurhashCrossfadeDrawable;
import org.joinmastodon.android.ui.utils.DiscoverInfoBannerHelper;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ListImageLoaderAdapter;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.RecyclerViewDelegate;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class DiscoverNewsFragment extends BaseRecyclerFragment<CardViewModel> implements ScrollableToTop{
	private String accountID;
	private DiscoverInfoBannerHelper bannerHelper;
	private MergeRecyclerAdapter mergeAdapter;
	private UsableRecyclerView cardsList;
	private ArrayList<CardViewModel> top3=new ArrayList<>();
	private CardLinksAdapter cardsAdapter;

	public DiscoverNewsFragment(){
		super(10);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		bannerHelper=new DiscoverInfoBannerHelper(DiscoverInfoBannerHelper.BannerType.TRENDING_LINKS, accountID);
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetTrendingLinks()
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Card> result){
						top3.clear();
						top3.addAll(result.subList(0, Math.min(3, result.size())).stream().map(card->new CardViewModel(card, 280, 140)).collect(Collectors.toList()));
						cardsAdapter.notifyDataSetChanged();

						onDataLoaded(result.subList(top3.size(), result.size()).stream()
								.map(card->new CardViewModel(card, 56, 56))
								.collect(Collectors.toList()), false);
						bannerHelper.onBannerBecameVisible();
					}
				})
				.exec(accountID);
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		cardsList=new UsableRecyclerView(getActivity());
		cardsList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
		ListImageLoaderWrapper cardsImageLoader=new ListImageLoaderWrapper(getActivity(), cardsList, new RecyclerViewDelegate(cardsList), this);
		cardsList.setAdapter(cardsAdapter=new CardLinksAdapter(cardsImageLoader, top3));
		cardsList.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(256)));
		cardsList.setPadding(V.dp(16), V.dp(8), 0, 0);
		cardsList.setClipToPadding(false);
		cardsList.addItemDecoration(new RecyclerView.ItemDecoration(){
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				outRect.right=V.dp(16);
			}
		});
		cardsList.setSelector(R.drawable.bg_rect_12dp_ripple);
		cardsList.setDrawSelectorOnTop(true);

		mergeAdapter=new MergeRecyclerAdapter();
		bannerHelper.maybeAddBanner(list, mergeAdapter);
		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(cardsList));
		mergeAdapter.addAdapter(new LinksAdapter(imgLoader, data));
		return mergeAdapter;
	}

	@Override
	public void scrollToTop(){
		smoothScrollRecyclerViewToTop(list);
	}

	private class LinksAdapter extends UsableRecyclerView.Adapter<BaseLinkViewHolder> implements ImageLoaderRecyclerAdapter{
		private final List<CardViewModel> data;

		public LinksAdapter(ListImageLoaderWrapper imgLoader, List<CardViewModel> data){
			super(imgLoader);
			this.data=data;
		}

		@NonNull
		@Override
		public BaseLinkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new LinkViewHolder();
		}

		@Override
		public int getItemCount(){
			return data.size();
		}

		@Override
		public void onBindViewHolder(BaseLinkViewHolder holder, int position){
			holder.bind(data.get(position).card);
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getImageCountForItem(int position){
			return data.get(position).imageRequest==null ? 0 : 1;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return data.get(position).imageRequest;
		}
	}

	private class CardLinksAdapter extends LinksAdapter{
		public CardLinksAdapter(ListImageLoaderWrapper imgLoader, List<CardViewModel> data){
			super(imgLoader, data);
		}

		@NonNull
		@Override
		public BaseLinkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new LinkCardViewHolder();
		}
	}

	private class BaseLinkViewHolder extends BindableViewHolder<Card> implements UsableRecyclerView.Clickable, ImageLoaderViewHolder{
		protected final TextView name, title;
		protected final ImageView photo;
		private BlurhashCrossfadeDrawable crossfadeDrawable=new BlurhashCrossfadeDrawable();
		private boolean didClear;

		public BaseLinkViewHolder(int layout){
			super(getActivity(), layout, list);
			name=findViewById(R.id.name);
			title=findViewById(R.id.title);
			photo=findViewById(R.id.photo);
		}

		@Override
		public void onBind(Card item){
			name.setText(item.providerName);
			title.setText(item.title);
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

	private class LinkViewHolder extends BaseLinkViewHolder{
		public LinkViewHolder(){
			super(R.layout.item_trending_link);
			photo.setOutlineProvider(OutlineProviders.roundedRect(12));
			photo.setClipToOutline(true);
		}
	}

	private class LinkCardViewHolder extends BaseLinkViewHolder{
		public LinkCardViewHolder(){
			super(R.layout.item_trending_link_card);
			itemView.setOutlineProvider(OutlineProviders.roundedRect(12));
			itemView.setClipToOutline(true);
		}
	}
}
