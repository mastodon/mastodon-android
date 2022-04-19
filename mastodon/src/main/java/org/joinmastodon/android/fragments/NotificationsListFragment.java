package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.PollUpdatedEvent;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.PaginatedResponse;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.PhotoLayoutHelper;
import org.joinmastodon.android.ui.displayitems.AccountCardStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ImageStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.LinkCardStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.V;

public class NotificationsListFragment extends BaseStatusListFragment<Notification>{
	private boolean onlyMentions;
	private String maxID;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		E.register(this);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(this);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		onlyMentions=getArguments().getBoolean("onlyMentions", false);
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Notification n){
		String extraText=switch(n.type){
			case FOLLOW -> getString(R.string.user_followed_you);
			case FOLLOW_REQUEST -> getString(R.string.user_sent_follow_request);
			case MENTION, STATUS -> null;
			case REBLOG -> getString(R.string.notification_boosted);
			case FAVORITE -> getString(R.string.user_favorited);
			case POLL -> getString(R.string.poll_ended);
		};
		HeaderStatusDisplayItem titleItem=extraText!=null ? new HeaderStatusDisplayItem(n.id, n.account, n.createdAt, this, accountID, null, extraText) : null;
		if(n.status!=null){
			ArrayList<StatusDisplayItem> items=StatusDisplayItem.buildItems(this, n.status, accountID, n, knownAccounts, titleItem!=null, titleItem==null);
			if(titleItem!=null){
				for(StatusDisplayItem item:items){
					if(item instanceof ImageStatusDisplayItem imgItem){
						imgItem.horizontalInset=V.dp(32);
					}
				}
			}
			if(titleItem!=null)
				items.add(0, titleItem);
			return items;
		}else{
			AccountCardStatusDisplayItem card=new AccountCardStatusDisplayItem(n.id, this, n.account);
			return Arrays.asList(titleItem, card);
		}
	}

	@Override
	protected void addAccountToKnown(Notification s){
		if(!knownAccounts.containsKey(s.account.id))
			knownAccounts.put(s.account.id, s.account);
		if(s.status!=null && !knownAccounts.containsKey(s.status.account.id))
			knownAccounts.put(s.status.account.id, s.status.account);
	}

	@Override
	protected void doLoadData(int offset, int count){
		AccountSessionManager.getInstance()
				.getAccount(accountID).getCacheController()
				.getNotifications(offset>0 ? maxID : null, count, onlyMentions, refreshing, new SimpleCallback<>(this){
					@Override
					public void onSuccess(PaginatedResponse<List<Notification>> result){
						if(getActivity()==null)
							return;
						if(refreshing)
							relationships.clear();
						onDataLoaded(result.items.stream().filter(n->n.type!=null).collect(Collectors.toList()), !result.items.isEmpty());
						Set<String> needRelationships=result.items.stream()
								.filter(ntf->ntf.status==null && !relationships.containsKey(ntf.account.id))
								.map(ntf->ntf.account.id)
								.collect(Collectors.toSet());
						loadRelationships(needRelationships);
						maxID=result.maxID;
					}
				});
	}

	@Override
	protected void onRelationshipsLoaded(){
		if(getActivity()==null)
			return;
		for(int i=0;i<list.getChildCount();i++){
			RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
			if(holder instanceof AccountCardStatusDisplayItem.Holder accountHolder)
				accountHolder.rebind();
		}
	}

	@Override
	protected void onShown(){
		super.onShown();
//		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
//			loadData();
	}

	@Override
	public void onItemClick(String id){
		Notification n=getNotificationByID(id);
		if(n.status!=null){
			Status status=n.status;
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("status", Parcels.wrap(status));
			if(status.inReplyToAccountId!=null && knownAccounts.containsKey(status.inReplyToAccountId))
				args.putParcelable("inReplyToAccount", Parcels.wrap(knownAccounts.get(status.inReplyToAccountId)));
			Nav.go(getActivity(), ThreadFragment.class, args);
		}else{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("profileAccount", Parcels.wrap(n.account));
			Nav.go(getActivity(), ProfileFragment.class, args);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
			private int bgColor=UiUtils.getThemeColor(getActivity(), android.R.attr.colorBackground);
			private int borderColor=UiUtils.getThemeColor(getActivity(), R.attr.colorPollVoted);
			private RectF rect=new RectF();

			@Override
			public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				int pos=0;
				for(int i=0;i<parent.getChildCount();i++){
					View child=parent.getChildAt(i);
					RecyclerView.ViewHolder holder=parent.getChildViewHolder(child);
					pos=holder.getAbsoluteAdapterPosition();
					boolean inset=(holder instanceof StatusDisplayItem.Holder<?> sdi) && sdi.getItem().inset;
					if(inset){
						if(rect.isEmpty()){
							rect.set(child.getX(), i==0 && pos>0 && displayItems.get(pos-1).inset ? V.dp(-10) : child.getY(), child.getX()+child.getWidth(), child.getY()+child.getHeight());
						}else{
							rect.bottom=Math.max(rect.bottom, child.getY()+child.getHeight());
							rect.right=Math.max(rect.right, child.getX()+child.getHeight());
						}
					}else if(!rect.isEmpty()){
						drawInsetBackground(c);
						rect.setEmpty();
					}
				}
				if(!rect.isEmpty()){
					if(pos<displayItems.size()-1 && displayItems.get(pos+1).inset){
						rect.bottom=parent.getHeight()+V.dp(10);
					}
					drawInsetBackground(c);
					rect.setEmpty();
				}
			}

			private void drawInsetBackground(Canvas c){
				paint.setStyle(Paint.Style.FILL);
				paint.setColor(bgColor);
				rect.inset(V.dp(4), V.dp(4));
				c.drawRoundRect(rect, V.dp(4), V.dp(4), paint);
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(V.dp(1));
				paint.setColor(borderColor);
				rect.inset(paint.getStrokeWidth()/2f, paint.getStrokeWidth()/2f);
				c.drawRoundRect(rect, V.dp(4), V.dp(4), paint);
			}

			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				RecyclerView.ViewHolder holder=parent.getChildViewHolder(view);
				if(holder instanceof StatusDisplayItem.Holder<?> sdi){
					boolean inset=sdi.getItem().inset;
					int pos=holder.getAbsoluteAdapterPosition();
					if(inset){
						boolean topSiblingInset=pos>0 && displayItems.get(pos-1).inset;
						boolean bottomSiblingInset=pos<displayItems.size()-1 && displayItems.get(pos+1).inset;
						int pad;
						if(holder instanceof ImageStatusDisplayItem.Holder || holder instanceof LinkCardStatusDisplayItem.Holder)
							pad=V.dp(16);
						else
							pad=V.dp(12);
						boolean insetLeft=true, insetRight=true;
						if(holder instanceof ImageStatusDisplayItem.Holder<?> img){
							PhotoLayoutHelper.TiledLayoutResult layout=img.getItem().tiledLayout;
							PhotoLayoutHelper.TiledLayoutResult.Tile tile=img.getItem().thisTile;
							// only inset those items that are on the edges of the layout
							insetLeft=tile.startCol==0;
							insetRight=tile.startCol+tile.colSpan==layout.columnSizes.length;
							// inset all items in the bottom row
							if(tile.startRow+tile.rowSpan==layout.rowSizes.length)
								bottomSiblingInset=false;
						}
						if(insetLeft)
							outRect.left=pad;
						if(insetRight)
							outRect.right=pad;
						if(!topSiblingInset)
							outRect.top=pad;
						if(!bottomSiblingInset)
							outRect.bottom=pad;
					}
				}
			}
		});
	}

	private Notification getNotificationByID(String id){
		for(Notification n:data){
			if(n.id.equals(id))
				return n;
		}
		return null;
	}

	@Subscribe
	public void onPollUpdated(PollUpdatedEvent ev){
		if(!ev.accountID.equals(accountID))
			return;
		for(Notification ntf:data){
			if(ntf.status==null)
				continue;
			Status contentStatus=ntf.status.getContentStatus();
			if(contentStatus.poll!=null && contentStatus.poll.id.equals(ev.poll.id)){
				updatePoll(ntf.id, ntf.status, ev.poll);
			}
		}
	}
}
