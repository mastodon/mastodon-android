package org.joinmastodon.android.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.EmojiUpdatedEvent;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.EmojiCategory;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.RecyclerViewDelegate;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class CustomEmojiPopupKeyboard extends PopupKeyboard{
	private List<EmojiCategory> emojis;
	private UsableRecyclerView list;
	private ListImageLoaderWrapper imgLoader;
	private MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
	private String domain;
	private int gridGap;
	private int spanCount=6;
	private Consumer<Emoji> listener;

	public CustomEmojiPopupKeyboard(Activity activity, List<EmojiCategory> emojis, String domain){
		super(activity);
		this.emojis=emojis;
		this.domain=domain;
	}

	@Override
	protected View onCreateView(){
		GridLayoutManager lm=new GridLayoutManager(activity, spanCount);
		list=new UsableRecyclerView(activity){
			@Override
			protected void onMeasure(int widthSpec, int heightSpec){
				// it's important to do this in onMeasure so the child views will be measured with correct paddings already set
				spanCount=Math.round(MeasureSpec.getSize(widthSpec)/(float)V.dp(44+20));
				lm.setSpanCount(spanCount);
				int pad=V.dp(16);
				gridGap=(MeasureSpec.getSize(widthSpec)-pad*2-V.dp(44)*spanCount)/(spanCount-1);
				setPadding(pad, 0, pad-gridGap, 0);
				invalidateItemDecorations();
				super.onMeasure(widthSpec, heightSpec);
			}
		};
		lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup(){
			@Override
			public int getSpanSize(int position){
				if(adapter.getItemViewType(position)==0)
					return lm.getSpanCount();
				return 1;
			}
		});
		list.setLayoutManager(lm);
		imgLoader=new ListImageLoaderWrapper(activity, list, new RecyclerViewDelegate(list), null);

		for(EmojiCategory category:emojis)
			adapter.addAdapter(new SingleCategoryAdapter(category));
		list.setAdapter(adapter);
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				outRect.right=gridGap;
				if(view instanceof TextView){ // section header
					if(parent.getChildAdapterPosition(view)>0)
						outRect.top=-gridGap; // negate the margin added by the emojis above
				}else{
					outRect.bottom=gridGap;
				}
			}
		});
		list.setBackgroundColor(UiUtils.getThemeColor(activity, android.R.attr.colorBackground));
		list.setSelector(null);

		return list;
	}

	public void setListener(Consumer<Emoji> listener){
		this.listener=listener;
	}

	@SuppressLint("NotifyDataSetChanged")
	@Subscribe
	public void onEmojiUpdated(EmojiUpdatedEvent ev){
		if(ev.instanceDomain.equals(domain)){
			emojis=AccountSessionManager.getInstance().getCustomEmojis(domain);
			adapter.notifyDataSetChanged();
		}
	}

	private class SingleCategoryAdapter extends UsableRecyclerView.Adapter<RecyclerView.ViewHolder> implements ImageLoaderRecyclerAdapter{
		private final EmojiCategory category;
		private final List<ImageLoaderRequest> requests;

		public SingleCategoryAdapter(EmojiCategory category){
			super(imgLoader);
			this.category=category;
			requests=category.emojis.stream().map(e->new UrlImageLoaderRequest(e.url, V.dp(44), V.dp(44))).collect(Collectors.toList());
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return viewType==0 ? new SectionHeaderViewHolder() : new EmojiViewHolder();
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position){
			if(holder instanceof EmojiViewHolder emojiViewHolder){
				emojiViewHolder.bind(category.emojis.get(position-1));
				emojiViewHolder.positionWithinCategory=position-1;
			}else if(holder instanceof SectionHeaderViewHolder sectionHeaderViewHolder){
				sectionHeaderViewHolder.bind(TextUtils.isEmpty(category.title) ? domain : category.title);
			}
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			return category.emojis.size()+1;
		}

		@Override
		public int getItemViewType(int position){
			return position==0 ? 0 : 1;
		}

		@Override
		public int getImageCountForItem(int position){
			return position>0 ? 1 : 0;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return requests.get(position-1);
		}
	}

	private class SectionHeaderViewHolder extends BindableViewHolder<String>{
		public SectionHeaderViewHolder(){
			super(activity, R.layout.item_emoji_section, list);
		}

		@Override
		public void onBind(String item){
			((TextView)itemView).setText(item);
		}
	}

	private class EmojiViewHolder extends BindableViewHolder<Emoji> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable{
		public int positionWithinCategory;
		public EmojiViewHolder(){
			super(new ImageView(activity));
			ImageView img=(ImageView) itemView;
			img.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(44)));
			img.setScaleType(ImageView.ScaleType.FIT_CENTER);
		}

		@Override
		public void onBind(Emoji item){

		}

		@Override
		public void setImage(int index, Drawable image){
			((ImageView)itemView).setImageDrawable(image);
			if(image instanceof Animatable)
				((Animatable) image).start();
		}

		@Override
		public void clearImage(int index){
			((ImageView)itemView).setImageDrawable(null);
		}

		@Override
		public void onClick(){
			listener.accept(item);
		}
	}
}
