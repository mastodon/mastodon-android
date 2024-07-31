package org.joinmastodon.android.fragments.discover;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.joinmastodon.android.api.requests.trends.GetTrendingLinks;
import org.joinmastodon.android.fragments.ScrollableToTop;
import org.joinmastodon.android.model.Card;
import org.joinmastodon.android.model.viewmodel.CardViewModel;
import org.joinmastodon.android.ui.utils.DiscoverInfoBannerHelper;
import org.joinmastodon.android.ui.viewholders.LinkCardHolder;

import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class DiscoverNewsFragment extends BaseRecyclerFragment<DiscoverNewsFragment.CardItem> implements ScrollableToTop{
	private String accountID;
	private DiscoverInfoBannerHelper bannerHelper;
	private MergeRecyclerAdapter mergeAdapter;

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
		currentRequest=new GetTrendingLinks(40)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Card> result){
						int[] index={0};
						onDataLoaded(result.stream()
								.map(card->{
									int actualIndex=index[0]+(refreshing ? 0 : (data.size()+preloadedData.size()));
									index[0]++;
									int size=actualIndex==0 ? 1000 : 192;
									return new CardItem(new CardViewModel(card, size, size, card, accountID));
								})
								.collect(Collectors.toList()), false);
						bannerHelper.onBannerBecameVisible();
					}
				})
				.exec(accountID);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected RecyclerView.Adapter getAdapter(){
		mergeAdapter=new MergeRecyclerAdapter();
		bannerHelper.maybeAddBanner(list, mergeAdapter);
		mergeAdapter.addAdapter(new LinksAdapter(imgLoader));
		return mergeAdapter;
	}

	@Override
	public void scrollToTop(){
		smoothScrollRecyclerViewToTop(list);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				if(parent.getChildAdapterPosition(view)==0 && !bannerHelper.isBannerShown()){
					outRect.top=V.dp(16);
				}
				if(parent.getChildViewHolder(view) instanceof LinkCardHolder<?>){
					outRect.bottom=V.dp(8);
				}
			}
		});
	}

	public static class CardItem implements LinkCardHolder.LinkCardProvider{
		public final CardViewModel card;

		private CardItem(CardViewModel card){
			this.card=card;
		}

		@Override
		public CardViewModel getCard(){
			return card;
		}
	}

	private class LinksAdapter extends UsableRecyclerView.Adapter<LinkCardHolder<CardItem>> implements ImageLoaderRecyclerAdapter{
		public LinksAdapter(ListImageLoaderWrapper imgLoader){
			super(imgLoader);
		}

		@NonNull
		@Override
		public LinkCardHolder<CardItem> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			LinkCardHolder<CardItem> vh=new LinkCardHolder<>(getActivity(), list, viewType==1, accountID);
			vh.setTryResolving(false);
			return vh;
		}

		@Override
		public int getItemCount(){
			return data.size();
		}

		@Override
		public void onBindViewHolder(LinkCardHolder<CardItem> holder, int position){
			holder.bind(data.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getImageCountForItem(int position){
			return data.get(position).card.getImageCount();
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return data.get(position).card.getImageRequest(image);
		}

		@Override
		public int getItemViewType(int position){
			return position==0 ? -1 : -2;
		}
	}
}
