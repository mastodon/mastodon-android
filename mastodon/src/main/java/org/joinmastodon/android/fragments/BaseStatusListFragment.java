package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Toolbar;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.polls.SubmitPollVote;
import org.joinmastodon.android.api.requests.statuses.TranslateStatus;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.PollUpdatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.DisplayItemsParent;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.Translation;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.PhotoLayoutHelper;
import org.joinmastodon.android.ui.displayitems.AccountStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.GapStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HashtagStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.MediaGridStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.PollFooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.PollOptionStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.SpoilerStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.TextStatusDisplayItem;
import org.joinmastodon.android.ui.photoviewer.PhotoViewer;
import org.joinmastodon.android.ui.photoviewer.PhotoViewerHost;
import org.joinmastodon.android.ui.sheets.NonMutualPreReplySheet;
import org.joinmastodon.android.ui.sheets.OldPostPreReplySheet;
import org.joinmastodon.android.ui.utils.MediaAttachmentViewController;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.MediaGridLayout;
import org.joinmastodon.android.utils.TypedObjectPool;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class BaseStatusListFragment<T extends DisplayItemsParent> extends MastodonRecyclerFragment<T> implements PhotoViewerHost, ScrollableToTop{
	protected ArrayList<StatusDisplayItem> displayItems=new ArrayList<>();
	protected DisplayItemsAdapter adapter;
	protected String accountID;
	protected PhotoViewer currentPhotoViewer;
	protected HashMap<String, Account> knownAccounts=new HashMap<>();
	protected HashMap<String, Relationship> relationships=new HashMap<>();
	protected Rect tmpRect=new Rect();
	protected TypedObjectPool<MediaGridStatusDisplayItem.GridItemType, MediaAttachmentViewController> attachmentViewsPool=new TypedObjectPool<>(this::makeNewMediaAttachmentView);
	private SpringAnimation listShakeAnimation;

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
		loadRelationships(items.stream().map(DisplayItemsParent::getAccountID).filter(Objects::nonNull).collect(Collectors.toSet()));
	}

	@Override
	public void onClearItems(){
		super.onClearItems();
		displayItems.clear();
	}

	protected void prependItems(List<T> items, boolean notify){
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
		if(notify)
			adapter.notifyItemRangeInserted(0, offset);
		loadRelationships(items.stream().map(DisplayItemsParent::getAccountID).filter(Objects::nonNull).collect(Collectors.toSet()));
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
				if(holder instanceof ImageLoaderViewHolder ivh){
					int pos=holder.getAbsoluteAdapterPosition();
					if(pos<0)
						continue;
					for(int j=0;j<list.getImageCountForItem(pos);j++){
						ivh.clearImage(j);
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
	public void openPhotoViewer(String parentID, Status _status, int attachmentIndex, MediaGridStatusDisplayItem.Holder gridHolder){
		final Status status=_status.getContentStatus();
		currentPhotoViewer=new PhotoViewer(getActivity(), this, status.mediaAttachments, attachmentIndex, status, accountID, new PhotoViewer.Listener(){
			private MediaAttachmentViewController transitioningHolder;

			@Override
			public void setPhotoViewVisibility(int index, boolean visible){
				MediaAttachmentViewController holder=findPhotoViewHolder(index);
				if(holder!=null)
					holder.photo.setAlpha(visible ? 1f : 0f);
			}

			@Override
			public boolean startPhotoViewTransition(int index, @NonNull Rect outRect, @NonNull int[] outCornerRadius){
				MediaAttachmentViewController holder=findPhotoViewHolder(index);
				if(holder!=null){
					transitioningHolder=holder;
					View view=transitioningHolder.photo;
					int[] pos={0, 0};
					view.getLocationOnScreen(pos);
					outRect.set(pos[0], pos[1], pos[0]+view.getWidth(), pos[1]+view.getHeight());
					list.setClipChildren(false);
					gridHolder.setClipChildren(false);
					transitioningHolder.view.setElevation(1f);
					int cornerMask=((MediaGridLayout.LayoutParams)holder.view.getLayoutParams()).tile.getRoundCornersMask();
					if((cornerMask & PhotoLayoutHelper.CORNER_TL)!=0)
						outCornerRadius[0]=V.dp(8);
					if((cornerMask & PhotoLayoutHelper.CORNER_TR)!=0)
						outCornerRadius[1]=V.dp(8);
					if((cornerMask & PhotoLayoutHelper.CORNER_BR)!=0)
						outCornerRadius[2]=V.dp(8);
					if((cornerMask & PhotoLayoutHelper.CORNER_BL)!=0)
						outCornerRadius[3]=V.dp(8);
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
				// fix drawable callback
				Drawable d=transitioningHolder.photo.getDrawable();
				transitioningHolder.photo.setImageDrawable(null);
				transitioningHolder.photo.setImageDrawable(d);

				View view=transitioningHolder.photo;
				view.setTranslationX(0f);
				view.setTranslationY(0f);
				view.setScaleX(1f);
				view.setScaleY(1f);
				transitioningHolder.view.setElevation(0f);
				if(list!=null)
					list.setClipChildren(true);
				gridHolder.setClipChildren(true);
				transitioningHolder=null;
			}

			@Override
			public Drawable getPhotoViewCurrentDrawable(int index){
				MediaAttachmentViewController holder=findPhotoViewHolder(index);
				if(holder!=null)
					return holder.photo.getDrawable();
				return null;
			}

			@Override
			public void photoViewerDismissed(){
				currentPhotoViewer=null;
				gridHolder.itemView.setHasTransientState(false);
			}

			@Override
			public void onRequestPermissions(String[] permissions){
				requestPermissions(permissions, PhotoViewer.PERMISSION_REQUEST);
			}

			private MediaAttachmentViewController findPhotoViewHolder(int index){
				return gridHolder.getViewController(index);
			}
		});
		gridHolder.itemView.setHasTransientState(true);
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
		list.addItemDecoration(new StatusListItemDecoration());
		((UsableRecyclerView)list).setSelectorBoundsProvider(new UsableRecyclerView.SelectorBoundsProvider(){
			private Rect tmpRect=new Rect();
			@Override
			public void getSelectorBounds(View view, Rect outRect){
				if(((UsableRecyclerView) list).isIncludeMarginsInItemHitbox()){
					list.getDecoratedBoundsWithMargins(view, outRect);
				}else{
					outRect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
				}
				RecyclerView.ViewHolder holder=list.getChildViewHolder(view);
				if(holder instanceof StatusDisplayItem.Holder<?> sih){
					if(sih.getItem() instanceof StatusDisplayItem sdi && sdi.getType()==StatusDisplayItem.Type.GAP){
						outRect.setEmpty();
						return;
					}
					String id=((StatusDisplayItem.Holder<?>) holder).getItemID();
					for(int i=0;i<list.getChildCount();i++){
						View child=list.getChildAt(i);
						holder=list.getChildViewHolder(child);
						if(holder instanceof StatusDisplayItem.Holder<?> sih2){
							String otherID=sih2.getItemID();
							if(otherID.equals(id)){
								list.getDecoratedBoundsWithMargins(child, tmpRect);
								outRect.left=Math.min(outRect.left, tmpRect.left);
								outRect.top=Math.min(outRect.top, tmpRect.top);
								outRect.right=Math.max(outRect.right, tmpRect.right);
								outRect.bottom=Math.max(outRect.bottom, tmpRect.bottom);
							}
						}
					}
				}
			}
		});
		list.setItemAnimator(new BetterItemAnimator());
		((UsableRecyclerView) list).setIncludeMarginsInItemHitbox(true);
		updateToolbar();
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
		toolbar.setOnClickListener(v->scrollToTop());
		toolbar.setNavigationContentDescription(R.string.back);
	}

	public int getMainAdapterOffset(){
		if(list.getAdapter() instanceof MergeRecyclerAdapter mergeAdapter){
			return mergeAdapter.getPositionForAdapter(adapter);
		}
		return 0;
	}

	protected void drawDivider(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder, RecyclerView parent, Canvas c, Paint paint){
		parent.getDecoratedBoundsWithMargins(child, tmpRect);
		tmpRect.offset(0, Math.round(child.getTranslationY()));
		float y=tmpRect.bottom;
		int strokeWidth=V.dp(0.5f);
		if(strokeWidth%2==1){
			y-=0.5f;
		}
		paint.setAlpha(Math.round(255*child.getAlpha()));
		c.drawLine(0, y, parent.getWidth(), y, paint);
	}

	protected boolean needDividerForExtraItem(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder){
		return false;
	}

	public abstract void onItemClick(String id);

	protected void updatePoll(String itemID, Status status, Poll poll){
		if(status.poll!=poll)
			status.poll=poll;
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
		StatusDisplayItem.buildPollItems(itemID, this, poll, status, pollItems);
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
		if(poll.selectedOptions==null)
			poll.selectedOptions=new ArrayList<>();
		if(poll.multiple){
			if(poll.selectedOptions.contains(option)){
				poll.selectedOptions.remove(option);
			}else{
				poll.selectedOptions.add(option);
			}
		}else{
			if(poll.selectedOptions.contains(option))
				return;
			if(!poll.selectedOptions.isEmpty()){
				Poll.Option previouslySelected=poll.selectedOptions.get(0);
				poll.selectedOptions.clear();
				for(int i=0;i<list.getChildCount();i++){
					RecyclerView.ViewHolder vh=list.getChildViewHolder(list.getChildAt(i));
					if(vh instanceof PollOptionStatusDisplayItem.Holder otherOption){
						if(otherOption.getItemID().equals(holder.getItemID()) && otherOption.getItem().option==previouslySelected){
							otherOption.updateCheckedState();
							break;
						}
					}
				}
			}
			poll.selectedOptions.add(option);
		}
		holder.updateCheckedState();
		for(int i=0;i<list.getChildCount();i++){
			RecyclerView.ViewHolder vh=list.getChildViewHolder(list.getChildAt(i));
			if(vh instanceof PollFooterStatusDisplayItem.Holder footer){
				if(footer.getItemID().equals(holder.getItemID())){
					footer.rebind();
					break;
				}
			}
		}
	}

	public void onPollToggleResultsClick(PollFooterStatusDisplayItem.Holder holder){
		Status status=holder.getItem().status.getContentStatus();
		status.poll.showResults=!status.poll.showResults;
		String itemID=holder.getItemID();
		if(status.poll.selectedOptions!=null)
			status.poll.selectedOptions.clear();
		int firstOptionIndex=-1, footerIndex=-1;
		int i=0;
		for(StatusDisplayItem item:displayItems){
			if(item.parentID.equals(itemID)){
				if(item instanceof PollOptionStatusDisplayItem optItem){
					if(firstOptionIndex==-1)
						firstOptionIndex=i;
					optItem.showResults=status.poll.showResults;
				}else if(item instanceof PollFooterStatusDisplayItem){
					footerIndex=i;
					break;
				}
			}
			i++;
		}
		if(firstOptionIndex==-1 || footerIndex==-1)
			throw new IllegalStateException("Can't find all poll items in displayItems");
		adapter.notifyItemRangeChanged(firstOptionIndex, status.poll.options.size());
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
						E.post(new PollUpdatedEvent(accountID, result));
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.loading, true)
				.exec(accountID);
	}

	public void onRevealSpoilerClick(SpoilerStatusDisplayItem.Holder holder){
		Status status=holder.getItem().status;
		SpoilerStatusDisplayItem spoilerItem=holder.getItem();
		if(status.revealedSpoilers.contains(spoilerItem.spoilerType))
			status.revealedSpoilers.remove(spoilerItem.spoilerType);
		else
			status.revealedSpoilers.add(spoilerItem.spoilerType);

		holder.rebind();

		int index=displayItems.indexOf(spoilerItem);
		if(status.revealedSpoilers.contains(spoilerItem.spoilerType)){
			int itemCount=spoilerItem.contentItems.size();
			displayItems.addAll(index+1, spoilerItem.contentItems);
			if(spoilerItem.spoilerType==Status.SpoilerType.FILTER && spoilerItem.contentItems.get(0) instanceof SpoilerStatusDisplayItem nestedSpoiler
					&& nestedSpoiler.spoilerType==Status.SpoilerType.CONTENT_WARNING && !GlobalUserPreferences.showCWs){
				status.revealedSpoilers.add(Status.SpoilerType.CONTENT_WARNING);
				displayItems.addAll(index+1+itemCount, nestedSpoiler.contentItems);
				itemCount+=nestedSpoiler.contentItems.size();
			}
			adapter.notifyItemRangeInserted(index+1, itemCount);
		}else{
			int itemCount=spoilerItem.contentItems.size();
			if(spoilerItem.contentItems.get(0) instanceof SpoilerStatusDisplayItem nestedSpoiler && status.revealedSpoilers.contains(nestedSpoiler.spoilerType)){
				status.revealedSpoilers.remove(nestedSpoiler.spoilerType);
				itemCount+=nestedSpoiler.contentItems.size();
			}
			displayItems.subList(index+1, index+1+itemCount).clear();
			adapter.notifyItemRangeRemoved(index+1, itemCount);
		}
		list.invalidateItemDecorations();
	}

	public void onGapClick(GapStatusDisplayItem.Holder item){}

	public String getAccountID(){
		return accountID;
	}

	public Relationship getRelationship(String id){
		return relationships.get(id);
	}

	public void putRelationship(String id, Relationship rel){
		relationships.put(id, rel);
	}

	protected void loadRelationships(Set<String> ids){
		if(ids.isEmpty())
			return;
		ids=ids.stream().filter(id->!relationships.containsKey(id)).collect(Collectors.toSet());
		if(ids.isEmpty())
			return;
		// TODO somehow manage these and cancel outstanding requests on refresh
		new GetAccountRelationships(ids)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Relationship> result){
						for(Relationship r:result)
							relationships.put(r.id, r);
						onRelationshipsLoaded();
					}

					@Override
					public void onError(ErrorResponse error){

					}
				})
				.exec(accountID);
	}

	protected void onRelationshipsLoaded(){}

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
			if(holder instanceof StatusDisplayItem.Holder<?> itemHolder && itemHolder.getItemID().equals(id) && type.isInstance(holder))
				return type.cast(holder);
		}
		return null;
	}

	protected <I extends StatusDisplayItem, H extends StatusDisplayItem.Holder<I>> List<H> findAllHoldersOfType(String id, Class<H> type){
		ArrayList<H> holders=new ArrayList<>();
		for(int i=0;i<list.getChildCount();i++){
			RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
			if(holder instanceof StatusDisplayItem.Holder<?> itemHolder && itemHolder.getItemID().equals(id) && type.isInstance(holder))
				holders.add(type.cast(holder));
		}
		return holders;
	}

	@Override
	public void scrollToTop(){
		smoothScrollRecyclerViewToTop(list);
	}

	protected int getListWidthForMediaLayout(){
		return list.getWidth();
	}

	protected boolean wantsOverlaySystemNavigation(){
		return true;
	}

	protected void onSetFabBottomInset(int inset){

	}

	public boolean isItemEnabled(String id){
		return true;
	}

	public ArrayList<StatusDisplayItem> getDisplayItems(){
		return displayItems;
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0 && wantsOverlaySystemNavigation()){
			list.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
			onSetFabBottomInset(insets.getSystemWindowInsetBottom());
			insets=insets.inset(0, 0, 0, insets.getSystemWindowInsetBottom());
		}else{
			onSetFabBottomInset(0);
		}
		super.onApplyWindowInsets(insets);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
		if(requestCode==PhotoViewer.PERMISSION_REQUEST && currentPhotoViewer!=null){
			currentPhotoViewer.onRequestPermissionsResult(permissions, grantResults);
		}
	}

	@Override
	public void onPause(){
		super.onPause();
		if(currentPhotoViewer!=null)
			currentPhotoViewer.onPause();
	}

	private MediaAttachmentViewController makeNewMediaAttachmentView(MediaGridStatusDisplayItem.GridItemType type){
		return new MediaAttachmentViewController(getActivity(), type);
	}

	public TypedObjectPool<MediaGridStatusDisplayItem.GridItemType, MediaAttachmentViewController> getAttachmentViewsPool(){
		return attachmentViewsPool;
	}

	public void togglePostTranslation(Status status, String itemID){
		switch(status.translationState){
			case LOADING -> {
				return;
			}
			case SHOWN -> {
				status.translationState=Status.TranslationState.HIDDEN;
			}
			case HIDDEN -> {
				if(status.translation!=null){
					status.translationState=Status.TranslationState.SHOWN;
				}else{
					status.translationState=Status.TranslationState.LOADING;
					new TranslateStatus(status.getContentStatus().id, Locale.getDefault().getLanguage())
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(Translation result){
									if(getActivity()==null)
										return;
									status.translation=result;
									status.translationState=Status.TranslationState.SHOWN;
									updateTranslation(itemID);
								}

								@Override
								public void onError(ErrorResponse error){
									if(getActivity()==null)
										return;
									status.translationState=Status.TranslationState.HIDDEN;
									updateTranslation(itemID);
									new M3AlertDialogBuilder(getActivity())
											.setTitle(R.string.error)
											.setMessage(R.string.translation_failed)
											.setPositiveButton(R.string.ok, null)
											.show();
								}
							})
							.exec(accountID);
				}
			}
		}
		updateTranslation(itemID);
	}

	private void updateTranslation(String itemID) {
		TextStatusDisplayItem.Holder text=findHolderOfType(itemID, TextStatusDisplayItem.Holder.class);
		if(text!=null){
			text.updateTranslation(true);
			imgLoader.bindViewHolder((ImageLoaderRecyclerAdapter) list.getAdapter(), text, text.getAbsoluteAdapterPosition());
		}

		SpoilerStatusDisplayItem.Holder spoiler=findHolderOfType(itemID, SpoilerStatusDisplayItem.Holder.class);
		if(spoiler!=null){
			spoiler.rebind();
		}

		MediaGridStatusDisplayItem.Holder media=findHolderOfType(itemID, MediaGridStatusDisplayItem.Holder.class);
		if (media!=null) {
			media.rebind();
		}

		for(int i=0;i<list.getChildCount();i++){
			if(list.getChildViewHolder(list.getChildAt(i)) instanceof PollOptionStatusDisplayItem.Holder item){
				item.rebind();
			}
		}
	}

	public void rebuildAllDisplayItems(){
		displayItems.clear();
		for(T item:data){
			displayItems.addAll(buildDisplayItems(item));
		}
		adapter.notifyDataSetChanged();
	}

	public void maybeShowPreReplySheet(Status status, Runnable proceed){
		Relationship rel=getRelationship(status.account.id);
		if(!GlobalUserPreferences.isOptedOutOfPreReplySheet(GlobalUserPreferences.PreReplySheetType.NON_MUTUAL, status.account, accountID) &&
				!status.account.id.equals(AccountSessionManager.get(accountID).self.id) && rel!=null && !rel.followedBy && status.account.followingCount>=1){
			new NonMutualPreReplySheet(getActivity(), notAgain->{
				GlobalUserPreferences.optOutOfPreReplySheet(GlobalUserPreferences.PreReplySheetType.NON_MUTUAL, notAgain ? null : status.account, accountID);
				proceed.run();
			}, status.account, accountID).show();
		}else if(!GlobalUserPreferences.isOptedOutOfPreReplySheet(GlobalUserPreferences.PreReplySheetType.OLD_POST, null, null) &&
				status.createdAt.isBefore(Instant.now().minus(90, ChronoUnit.DAYS)) && !status.account.id.equals(AccountSessionManager.get(accountID).self.id)){
			new OldPostPreReplySheet(getActivity(), notAgain->{
				if(notAgain)
					GlobalUserPreferences.optOutOfPreReplySheet(GlobalUserPreferences.PreReplySheetType.OLD_POST, null, null);
				proceed.run();
			}, status).show();
		}else{
			proceed.run();
		}
	}

	protected void onModifyItemViewHolder(BindableViewHolder<StatusDisplayItem> holder){}

	public void shakeListView(){
		if(listShakeAnimation!=null)
			listShakeAnimation.cancel();
		SpringAnimation anim=new SpringAnimation(list, DynamicAnimation.TRANSLATION_X, 0);
		anim.setStartVelocity(V.dp(-500));
		anim.getSpring().setStiffness(500).setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY);
		listShakeAnimation=anim;
		anim.addEndListener((animation, canceled, value, velocity)->listShakeAnimation=null);
		anim.start();
	}

	public void retryFailedImages(){
		imgLoader.retryFailedRequests();
	}

	protected class DisplayItemsAdapter extends UsableRecyclerView.Adapter<BindableViewHolder<StatusDisplayItem>> implements ImageLoaderRecyclerAdapter{

		public DisplayItemsAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public BindableViewHolder<StatusDisplayItem> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			BindableViewHolder<StatusDisplayItem> holder=(BindableViewHolder<StatusDisplayItem>) StatusDisplayItem.createViewHolder(StatusDisplayItem.Type.values()[viewType & (~0x80000000)], getActivity(), parent, BaseStatusListFragment.this);
			onModifyItemViewHolder(holder);
			return holder;
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
			return displayItems.get(position).getType().ordinal() | 0x80000000;
		}

		@Override
		public int getImageCountForItem(int position){
			return displayItems.get(position).getImageCount();
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return displayItems.get(position).getImageRequest(image);
		}
	}

	private class StatusListItemDecoration extends RecyclerView.ItemDecoration{
		private Paint dividerPaint=new Paint();

		{
			dividerPaint.setColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
			dividerPaint.setStyle(Paint.Style.STROKE);
			dividerPaint.setStrokeWidth(V.dp(0.5f));
		}

		@Override
		public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
			for(int i=0;i<parent.getChildCount()-1;i++){
				View child=parent.getChildAt(i);
				View bottomSibling=parent.getChildAt(i+1);
				RecyclerView.ViewHolder holder=parent.getChildViewHolder(child);
				RecyclerView.ViewHolder siblingHolder=parent.getChildViewHolder(bottomSibling);
				if(needDrawDivider(holder, siblingHolder)){
					drawDivider(child, bottomSibling, holder, siblingHolder, parent, c, dividerPaint);
				}
			}
		}

		private boolean needDrawDivider(RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder){
			if(needDividerForExtraItem(holder.itemView, siblingHolder.itemView, holder, siblingHolder))
				return true;
			if(holder instanceof StatusDisplayItem.Holder<?> ih && siblingHolder instanceof StatusDisplayItem.Holder<?> sh){
				// Do not draw dividers between hashtag and/or account rows
				if((ih instanceof HashtagStatusDisplayItem.Holder || ih instanceof AccountStatusDisplayItem.Holder) && (sh instanceof HashtagStatusDisplayItem.Holder || sh instanceof AccountStatusDisplayItem.Holder))
					return false;
				return !ih.getItemID().equals(sh.getItemID()) && ih.getItem() instanceof StatusDisplayItem sdi && sdi.getType()!=StatusDisplayItem.Type.GAP;
			}
			return false;
		}
	}
}
