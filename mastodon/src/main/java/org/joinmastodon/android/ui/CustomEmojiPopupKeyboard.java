package org.joinmastodon.android.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.EmojiUpdatedEvent;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.EmojiCategory;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.List;
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
	private int spanCount=6;
	private Listener listener;

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
				spanCount=Math.round((MeasureSpec.getSize(widthSpec)-V.dp(32-8))/(float)V.dp(48+8));
				lm.setSpanCount(spanCount);
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
		list.setPadding(V.dp(16), 0, V.dp(16), 0);
		imgLoader=new ListImageLoaderWrapper(activity, list, new RecyclerViewDelegate(list), null);

		for(EmojiCategory category:emojis)
			adapter.addAdapter(new SingleCategoryAdapter(category));
		list.setAdapter(adapter);
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				if(view instanceof TextView){ // section header
					outRect.left=outRect.right=V.dp(-16);
				}else{
					EmojiViewHolder evh=(EmojiViewHolder) parent.getChildViewHolder(view);
					int col=evh.positionWithinCategory%spanCount;
					if(col<spanCount-1){
						outRect.right=V.dp(8);
					}
					outRect.bottom=V.dp(8);
				}
			}
		});
		list.setSelector(null);
		list.setClipToPadding(false);
		new StickyHeadersOverlay(activity, 0).install(list);

		LinearLayout ll=new LinearLayout(activity);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setElevation(V.dp(3));
		ll.setBackgroundResource(R.drawable.bg_m3_surface1);

		ll.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		FrameLayout bottomPanel=new FrameLayout(activity);
		bottomPanel.setPadding(V.dp(16), V.dp(8), V.dp(16), V.dp(8));
		bottomPanel.setBackgroundResource(R.drawable.bg_m3_surface2);
		ll.addView(bottomPanel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		ImageButton hideKeyboard=new ImageButton(activity);
		hideKeyboard.setImageResource(R.drawable.ic_keyboard_hide_24px);
		hideKeyboard.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(activity, R.attr.colorM3OnSurfaceVariant)));
		hideKeyboard.setBackgroundResource(R.drawable.bg_round_ripple);
		hideKeyboard.setOnClickListener(v->hide());
		bottomPanel.addView(hideKeyboard, new FrameLayout.LayoutParams(V.dp(36), V.dp(36), Gravity.LEFT));

		ImageButton backspace=new ImageButton(activity);
		backspace.setImageResource(R.drawable.ic_backspace_24px);
		backspace.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(activity, R.attr.colorM3OnSurfaceVariant)));
		backspace.setBackgroundResource(R.drawable.bg_round_ripple);
		backspace.setOnClickListener(v->listener.onBackspace());
		bottomPanel.addView(backspace, new FrameLayout.LayoutParams(V.dp(36), V.dp(36), Gravity.RIGHT));

		return ll;
	}

	public void setListener(Listener listener){
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
			requests=category.emojis.stream().map(e->new UrlImageLoaderRequest(e.url, V.dp(24), V.dp(24))).collect(Collectors.toList());
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return viewType==0 ? new SectionHeaderViewHolder() : new EmojiViewHolder();
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder holder, int position){
			if(holder instanceof EmojiViewHolder evh){
				evh.bind(category.emojis.get(position-1));
				evh.positionWithinCategory=position-1;
			}else if(holder instanceof SectionHeaderViewHolder shvh){
				shvh.bind(TextUtils.isEmpty(category.title) ? domain : category.title);
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

	private class SectionHeaderViewHolder extends BindableViewHolder<String> implements StickyHeadersOverlay.HeaderViewHolder{
		private Drawable background;

		public SectionHeaderViewHolder(){
			super(activity, R.layout.item_emoji_section, list);
			background=new ColorDrawable(UiUtils.alphaBlendThemeColors(activity, R.attr.colorM3Surface, R.attr.colorM3Primary, .08f));
			itemView.setBackground(background);
		}

		@Override
		public void onBind(String item){
			((TextView)itemView).setText(item);
			setStickyFactor(0);
		}

		@Override
		public void setStickyFactor(float factor){
			background.setAlpha(Math.round(255*factor));
		}
	}

	private class EmojiViewHolder extends BindableViewHolder<Emoji> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable{
		public int positionWithinCategory;
		public EmojiViewHolder(){
			super(new ImageView(activity));
			ImageView img=(ImageView) itemView;
			img.setLayoutParams(new RecyclerView.LayoutParams(V.dp(48), V.dp(48)));
			img.setScaleType(ImageView.ScaleType.FIT_CENTER);
			int pad=V.dp(12);
			img.setPadding(pad, pad, pad, pad);
			img.setBackgroundResource(R.drawable.bg_custom_emoji);
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
			listener.onEmojiSelected(item);
		}
	}

	public interface Listener{
		void onEmojiSelected(Emoji emoji);
		void onBackspace();
	}
}
