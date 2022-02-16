package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toolbar;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.polls.SubmitPollVote;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.DisplayItemsParent;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.displayitems.FooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ImageStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.PhotoStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.PollFooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.PollOptionStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.TextStatusDisplayItem;
import org.joinmastodon.android.ui.photoviewer.PhotoViewer;
import org.joinmastodon.android.ui.photoviewer.PhotoViewerHost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class BaseStatusListFragment<T extends DisplayItemsParent> extends BaseRecyclerFragment<T> implements PhotoViewerHost{
	protected ArrayList<StatusDisplayItem> displayItems=new ArrayList<>();
	protected DisplayItemsAdapter adapter;
	protected String accountID;
	protected PhotoViewer currentPhotoViewer;
	protected HashMap<String, Account> knownAccounts=new HashMap<>();

	public BaseStatusListFragment(){
		super(20);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		return adapter=new DisplayItemsAdapter();
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		accountID=getArguments().getString("account");
	}

	@Override
	public void onAppendItems(List<T> items){
		super.onAppendItems(items);
		for(T s:items){
			addAccountToKnown(s);
		}
		for(T s:items){
			displayItems.addAll(buildDisplayItems(s));
		}
	}

	@Override
	public void onClearItems(){
		super.onClearItems();
		displayItems.clear();
	}

	protected void prependItems(List<T> items){
		data.addAll(0, items);
		int offset=0;
		for(T s:items){
			addAccountToKnown(s);
		}
		for(T s:items){
			List<StatusDisplayItem> toAdd=buildDisplayItems(s);
			displayItems.addAll(offset, toAdd);
			offset+=toAdd.size();
		}
		adapter.notifyItemRangeInserted(0, offset);
	}

	protected String getMaxID(){
		if(!preloadedData.isEmpty())
			return preloadedData.get(preloadedData.size()-1).getID();
		else if(!data.isEmpty())
			return data.get(data.size()-1).getID();
		else
			return null;
	}

	protected abstract List<StatusDisplayItem> buildDisplayItems(T s);
	protected abstract void addAccountToKnown(T s);

	@Override
	protected void onHidden(){
		super.onHidden();
		// Clear any loaded images from the list to make it possible for the GC to deallocate them.
		// The delay avoids blank image views showing up in the app switcher.
		content.postDelayed(()->{
			if(!isHidden())
				return;
			imgLoader.deactivate();
			UsableRecyclerView list=(UsableRecyclerView) this.list;
			for(int i=0; i<list.getChildCount(); i++){
				RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
				if(holder instanceof ImageLoaderViewHolder){
					for(int j=0; j<list.getImageCountForItem(holder.getAbsoluteAdapterPosition()); j++){
						((ImageLoaderViewHolder) holder).clearImage(j);
					}
				}
			}
		}, 100);
	}

	@Override
	protected void onShown(){
		super.onShown();
		imgLoader.activate();
	}

	@Override
	public void openPhotoViewer(String parentID, Status _status, int attachmentIndex){
		final Status status=_status.reblog!=null ? _status.reblog : _status;
		currentPhotoViewer=new PhotoViewer(getActivity(), status.mediaAttachments, attachmentIndex, new PhotoViewer.Listener(){
			private ImageStatusDisplayItem.Holder transitioningHolder;

			@Override
			public void setPhotoViewVisibility(int index, boolean visible){
				ImageStatusDisplayItem.Holder holder=findPhotoViewHolder(index);
				if(holder!=null)
					holder.photo.setAlpha(visible ? 1f : 0f);
			}

			@Override
			public boolean startPhotoViewTransition(int index, @NonNull Rect outRect, @NonNull int[] outCornerRadius){
				ImageStatusDisplayItem.Holder holder=findPhotoViewHolder(index);
				if(holder!=null){
					transitioningHolder=holder;
					View view=transitioningHolder.photo;
					int[] pos={0, 0};
					view.getLocationOnScreen(pos);
					outRect.set(pos[0], pos[1], pos[0]+view.getWidth(), pos[1]+view.getHeight());
					list.setClipChildren(false);
					transitioningHolder.itemView.setElevation(1f);
					return true;
				}
				return false;
			}

			@Override
			public void setTransitioningViewTransform(float translateX, float translateY, float scale){
				View view=transitioningHolder.photo;
				view.setTranslationX(translateX);
				view.setTranslationY(translateY);
				view.setScaleX(scale);
				view.setScaleY(scale);
			}

			@Override
			public void endPhotoViewTransition(){
				View view=transitioningHolder.photo;
				view.setTranslationX(0f);
				view.setTranslationY(0f);
				view.setScaleX(1f);
				view.setScaleY(1f);
				transitioningHolder.itemView.setElevation(0f);
				list.setClipChildren(true);
				transitioningHolder=null;
			}

			@Override
			public Drawable getPhotoViewCurrentDrawable(int index){
				ImageStatusDisplayItem.Holder holder=findPhotoViewHolder(index);
				if(holder!=null)
					return holder.photo.getDrawable();
				return null;
			}

			@Override
			public void photoViewerDismissed(){
				currentPhotoViewer=null;
			}

			private ImageStatusDisplayItem.Holder findPhotoViewHolder(int index){
				int offset=0;
				for(StatusDisplayItem item:displayItems){
					if(item.parentID.equals(parentID)){
						if(item instanceof ImageStatusDisplayItem){
							RecyclerView.ViewHolder holder=list.findViewHolderForAdapterPosition(getMainAdapterOffset()+offset+index);
							if(holder instanceof ImageStatusDisplayItem.Holder){
								return (ImageStatusDisplayItem.Holder) holder;
							}
							return null;
						}
					}
					offset++;
				}
				return null;
			}
		});
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addOnScrollListener(new RecyclerView.OnScrollListener(){
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
				if(currentPhotoViewer!=null)
					currentPhotoViewer.offsetView(-dx, -dy);
			}
		});
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			private Paint paint=new Paint();
			{
				paint.setColor(0xFFD0D5DD);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(V.dp(1));
			}

			@Override
			public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				for(int i=0;i<parent.getChildCount();i++){
					View child=parent.getChildAt(i);
					RecyclerView.ViewHolder holder=parent.getChildViewHolder(child);
					if(holder instanceof FooterStatusDisplayItem.Holder){
						float y=child.getY()+child.getHeight()-V.dp(.5f);
						c.drawLine(child.getX(), y, child.getX()+child.getWidth(), y, paint);
					}
				}
			}
		});
		((UsableRecyclerView)list).setSelectorBoundsProvider(new UsableRecyclerView.SelectorBoundsProvider(){
			@Override
			public void getSelectorBounds(View view, Rect outRect){
				outRect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
				RecyclerView.ViewHolder holder=list.getChildViewHolder(view);
				if(holder instanceof StatusDisplayItem.Holder){
					String id=((StatusDisplayItem.Holder<?>) holder).getItemID();
					for(int i=0;i<list.getChildCount();i++){
						View child=list.getChildAt(i);
						holder=list.getChildViewHolder(child);
						if(holder instanceof StatusDisplayItem.Holder){
							String otherID=((StatusDisplayItem.Holder<?>) holder).getItemID();
							if(otherID.equals(id)){
								outRect.left=Math.min(outRect.left, child.getLeft());
								outRect.top=Math.min(outRect.top, child.getTop());
								outRect.right=Math.max(outRect.right, child.getRight());
								outRect.bottom=Math.max(outRect.bottom, child.getBottom());
							}
						}
					}
				}
			}
		});
		list.setItemAnimator(new BetterItemAnimator());
		updateToolbar();
	}

	@Override
	protected RecyclerView.LayoutManager onCreateLayoutManager(){
		GridLayoutManager lm=new GridLayoutManager(getActivity(), 2);
		lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup(){
			@Override
			public int getSpanSize(int position){
				position-=getMainAdapterOffset();
				if(position>=0 && position<displayItems.size()){
					StatusDisplayItem item=displayItems.get(position);
					if(item instanceof ImageStatusDisplayItem){
						int total=((ImageStatusDisplayItem) item).totalPhotos;
						if(total>1){
							int index=((ImageStatusDisplayItem) item).index;
							return 1;
						}
					}
				}
				return 2;
			}
		});
		return lm;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbar();
	}

	private void updateToolbar(){
		Toolbar toolbar=getToolbar();
		if(toolbar==null)
			return;
		toolbar.setOnClickListener(v->{
			if(list.getChildCount()>0 && list.getChildAdapterPosition(list.getChildAt(0))>10){
				list.scrollToPosition(0);
				list.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
						list.getViewTreeObserver().removeOnPreDrawListener(this);
						list.scrollBy(0, V.dp(300));
						list.smoothScrollToPosition(0);
						return true;
					}
				});
			}else{
				list.smoothScrollToPosition(0);
			}
		});
	}

	protected int getMainAdapterOffset(){
		return 0;
	}

	public abstract void onItemClick(String id);

	protected void updatePoll(String itemID, Poll poll){
		int firstOptionIndex=-1, footerIndex=-1;
		int i=0;
		for(StatusDisplayItem item:displayItems){
			if(item.parentID.equals(itemID)){
				if(item instanceof PollOptionStatusDisplayItem && firstOptionIndex==-1){
					firstOptionIndex=i;
				}else if(item instanceof PollFooterStatusDisplayItem){
					footerIndex=i;
					break;
				}
			}
			i++;
		}
		if(firstOptionIndex==-1 || footerIndex==-1)
			throw new IllegalStateException("Can't find all poll items in displayItems");
		List<StatusDisplayItem> pollItems=displayItems.subList(firstOptionIndex, footerIndex+1);
		int prevSize=pollItems.size();
		pollItems.clear();
		StatusDisplayItem.buildPollItems(itemID, this, poll, pollItems);
		if(prevSize!=pollItems.size()){
			adapter.notifyItemRangeRemoved(firstOptionIndex, prevSize);
			adapter.notifyItemRangeInserted(firstOptionIndex, pollItems.size());
		}else{
			adapter.notifyItemRangeChanged(firstOptionIndex, pollItems.size());
		}
	}

	public void onPollOptionClick(PollOptionStatusDisplayItem.Holder holder){
		Poll poll=holder.getItem().poll;
		Poll.Option option=holder.getItem().option;
		if(poll.multiple){
			if(poll.selectedOptions==null)
				poll.selectedOptions=new ArrayList<>();
			if(poll.selectedOptions.contains(option)){
				poll.selectedOptions.remove(option);
				holder.itemView.setSelected(false);
			}else{
				poll.selectedOptions.add(option);
				holder.itemView.setSelected(true);
			}
			for(int i=0;i<list.getChildCount();i++){
				RecyclerView.ViewHolder vh=list.getChildViewHolder(list.getChildAt(i));
				if(vh instanceof PollFooterStatusDisplayItem.Holder){
					PollFooterStatusDisplayItem.Holder footer=(PollFooterStatusDisplayItem.Holder) vh;
					if(footer.getItemID().equals(holder.getItemID())){
						footer.rebind();
						break;
					}
				}
			}
		}else{
			submitPollVote(holder.getItemID(), poll.id, Collections.singletonList(poll.options.indexOf(option)));
		}
	}

	public void onPollVoteButtonClick(PollFooterStatusDisplayItem.Holder holder){
		Poll poll=holder.getItem().poll;
		submitPollVote(holder.getItemID(), poll.id, poll.selectedOptions.stream().map(opt->poll.options.indexOf(opt)).collect(Collectors.toList()));
	}

	protected void submitPollVote(String parentID, String pollID, List<Integer> choices){
		if(refreshing)
			return;
		new SubmitPollVote(pollID, choices)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Poll result){
						updatePoll(parentID, result);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.loading, false)
				.exec(accountID);
	}

	public void onRevealSpoilerClick(TextStatusDisplayItem.Holder holder){
		Status status=holder.getItem().status;
		revealSpoiler(status, holder.getItemID());
	}

	public void onRevealSpoilerClick(ImageStatusDisplayItem.Holder<?> holder){
		Status status=holder.getItem().status;
		revealSpoiler(status, holder.getItemID());
	}

	protected void revealSpoiler(Status status, String itemID){
		status.spoilerRevealed=true;
		TextStatusDisplayItem.Holder text=findHolderOfType(itemID, TextStatusDisplayItem.Holder.class);
		if(text!=null)
			adapter.notifyItemChanged(text.getAbsoluteAdapterPosition());
		HeaderStatusDisplayItem.Holder header=findHolderOfType(itemID, HeaderStatusDisplayItem.Holder.class);
		if(header!=null)
			header.rebind();
		for(ImageStatusDisplayItem.Holder photo:(List<ImageStatusDisplayItem.Holder>)findAllHoldersOfType(itemID, ImageStatusDisplayItem.Holder.class)){
			photo.setRevealed(true);
		}
	}

	public void onVisibilityIconClick(HeaderStatusDisplayItem.Holder holder){
		Status status=holder.getItem().status;
		status.spoilerRevealed=!status.spoilerRevealed;
		if(!TextUtils.isEmpty(status.spoilerText)){
			TextStatusDisplayItem.Holder text=findHolderOfType(holder.getItemID(), TextStatusDisplayItem.Holder.class);
			if(text!=null){
				adapter.notifyItemChanged(text.getAbsoluteAdapterPosition());
			}
		}
		holder.rebind();
		for(ImageStatusDisplayItem.Holder<?> photo:(List<ImageStatusDisplayItem.Holder>)findAllHoldersOfType(holder.getItemID(), ImageStatusDisplayItem.Holder.class)){
			photo.setRevealed(status.spoilerRevealed);
		}
	}

	@Nullable
	protected <I extends StatusDisplayItem> I findItemOfType(String id, Class<I> type){
		for(StatusDisplayItem item:displayItems){
			if(item.parentID.equals(id) && type.isInstance(item))
				return type.cast(item);
		}
		return null;
	}

	@Nullable
	protected <I extends StatusDisplayItem, H extends StatusDisplayItem.Holder<I>> H findHolderOfType(String id, Class<H> type){
		for(int i=0;i<list.getChildCount();i++){
			RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
			if(holder instanceof StatusDisplayItem.Holder && ((StatusDisplayItem.Holder<?>) holder).getItemID().equals(id) && type.isInstance(holder))
				return type.cast(holder);
		}
		return null;
	}

	protected <I extends StatusDisplayItem, H extends StatusDisplayItem.Holder<I>> List<H> findAllHoldersOfType(String id, Class<H> type){
		ArrayList<H> holders=new ArrayList<>();
		for(int i=0;i<list.getChildCount();i++){
			RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
			if(holder instanceof StatusDisplayItem.Holder && ((StatusDisplayItem.Holder<?>) holder).getItemID().equals(id) && type.isInstance(holder))
				holders.add(type.cast(holder));
		}
		return holders;
	}

	protected class DisplayItemsAdapter extends UsableRecyclerView.Adapter<BindableViewHolder<StatusDisplayItem>> implements ImageLoaderRecyclerAdapter{

		public DisplayItemsAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public BindableViewHolder<StatusDisplayItem> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return (BindableViewHolder<StatusDisplayItem>) StatusDisplayItem.createViewHolder(StatusDisplayItem.Type.values()[viewType], getActivity(), parent);
		}

		@Override
		public void onBindViewHolder(BindableViewHolder<StatusDisplayItem> holder, int position){
			holder.bind(displayItems.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			return displayItems.size();
		}

		@Override
		public int getItemViewType(int position){
			return displayItems.get(position).getType().ordinal();
		}

		@Override
		public int getImageCountForItem(int position){
			return displayItems.get(position).getImageCount();
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return displayItems.get(position).getImageRequest(image);
		}

//		@Override
//		public void onViewDetachedFromWindow(@NonNull BindableViewHolder<StatusDisplayItem> holder){
//			if(holder instanceof ImageLoaderViewHolder){
//				int count=holder.getItem().getImageCount();
//				for(int i=0;i<count;i++){
//					((ImageLoaderViewHolder) holder).clearImage(i);
//				}
//			}
//		}
	}
}
