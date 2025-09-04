package org.joinmastodon.android.fragments;

import android.app.assist.AssistContent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.GetStatusContext;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusContext;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.displayitems.ExtendedFooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.FooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.SpoilerStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.TextStatusDisplayItem;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class ThreadFragment extends StatusListFragment implements AssistContentProviderFragment{
	private Status mainStatus;
	private ImageView endMark;
	private FrameLayout replyContainer;
	private LinearLayout replyButton;
	private ImageView replyButtonAva;
	private TextView replyButtonText;
	private int lastBottomInset;
	private Paint replyLinePaint=new Paint(Paint.ANTI_ALIAS_FLAG);

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setLayout(R.layout.fragment_thread);
		mainStatus=Parcels.unwrap(getArguments().getParcelable("status"));
		Account inReplyToAccount=Parcels.unwrap(getArguments().getParcelable("inReplyToAccount"));
		if(inReplyToAccount!=null)
			knownAccounts.put(inReplyToAccount.id, inReplyToAccount);
		data.add(mainStatus);
		onAppendItems(Collections.singletonList(mainStatus));
		if(GlobalUserPreferences.customEmojiInNames)
			setTitle(HtmlParser.parseCustomEmoji(getString(R.string.post_from_user, mainStatus.account.displayName), mainStatus.account.emojis));
		else
			setTitle(getString(R.string.post_from_user, mainStatus.account.displayName));
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Status s){
		List<StatusDisplayItem> items=StatusDisplayItem.buildItems(this, getActivity(), s, accountID, s, knownAccounts, StatusDisplayItem.FLAG_NO_IN_REPLY_TO);
		if(s.id.equals(mainStatus.id)){
			for(StatusDisplayItem item:items){
				item.fullWidth=true;
				if(item instanceof TextStatusDisplayItem text){
					text.textSelectable=!item.isQuote;
					text.largerFont=true;
				}
				else if(item instanceof FooterStatusDisplayItem footer)
					footer.hideCounts=true;
				else if(item instanceof SpoilerStatusDisplayItem spoiler){
					for(StatusDisplayItem subItem:spoiler.contentItems){
						if(subItem instanceof TextStatusDisplayItem text)
							text.textSelectable=!subItem.isQuote;
					}
				}
			}
			items.add(items.size()-1, new ExtendedFooterStatusDisplayItem(s.id, this, getActivity(), s.getContentStatus(), accountID));
		}
		return items;
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetStatusContext(mainStatus.id)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(StatusContext result){
						if(getActivity()==null)
							return;
						if(refreshing){
							data.clear();
							displayItems.clear();
							data.add(mainStatus);
							onAppendItems(Collections.singletonList(mainStatus));
						}
						filterStatuses(result.descendants);
						filterStatuses(result.ancestors);
						if(footerProgress!=null)
							footerProgress.setVisibility(View.GONE);
						data.addAll(result.descendants);
						int prevCount=displayItems.size();
						onAppendItems(result.descendants);
						int count=displayItems.size();
						if(!refreshing)
							adapter.notifyItemRangeInserted(prevCount, count-prevCount);
						prependItems(result.ancestors, !refreshing);
						dataLoaded();
						if(refreshing){
							refreshDone();
							adapter.notifyDataSetChanged();
						}
						list.scrollToPosition(displayItems.size()-count);
					}
				})
				.exec(accountID);
	}

	private void filterStatuses(List<Status> statuses){
		AccountSessionManager.get(accountID).filterStatuses(statuses, FilterContext.THREAD);
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading){
			dataLoading=true;
			doLoadData();
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		replyContainer=view.findViewById(R.id.reply_button_wrapper);
		replyButton=replyContainer.findViewById(R.id.reply_button);
		replyButtonText=replyButton.findViewById(R.id.reply_btn_text);
		replyButtonAva=replyButton.findViewById(R.id.avatar);
		replyButton.setOutlineProvider(OutlineProviders.roundedRect(20));
		replyButton.setClipToOutline(true);
		replyButtonText.setText(getString(R.string.reply_to_user, mainStatus.account.displayName));
		replyButtonAva.setOutlineProvider(OutlineProviders.OVAL);
		replyButtonAva.setClipToOutline(true);
		replyButton.setOnClickListener(v->openReply());
		Account self=AccountSessionManager.get(accountID).self;
		if(!TextUtils.isEmpty(self.avatar)){
			ViewImageLoader.loadWithoutAnimation(replyButtonAva, getResources().getDrawable(R.drawable.image_placeholder, getActivity().getTheme()), new UrlImageLoaderRequest(self.avatar, V.dp(24), V.dp(24)));
		}
		UiUtils.loadCustomEmojiInTextView(toolbarTitleView);
		showContent();
		if(!loaded)
			footerProgress.setVisibility(View.VISIBLE);

		list.addItemDecoration(new ReplyLinesItemDecoration());
	}

	protected void onStatusCreated(Status status){
		if(status.inReplyToId!=null && getStatusByID(status.inReplyToId)!=null){
			onAppendItems(Collections.singletonList(status));
			data.add(status);
		}
	}

	@Override
	public boolean isItemEnabled(StatusDisplayItem item){
		return item.isQuote || !item.parentID.equals(mainStatus.id);
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		MergeRecyclerAdapter a=new MergeRecyclerAdapter();
		a.addAdapter(super.getAdapter());

		endMark=new ImageView(getActivity());
		endMark.setScaleType(ImageView.ScaleType.CENTER);
		endMark.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant)));
		endMark.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(25)));
		endMark.setImageResource(R.drawable.thread_end_mark);
		a.addAdapter(new SingleViewRecyclerAdapter(endMark));

		return a;
	}

	@Override
	protected boolean needDividerForExtraItem(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder){
		return bottomSibling==endMark;
	}

	@Override
	protected void onErrorRetryClick(){
		if(preloadingFailed){
			preloadingFailed=false;
			V.setVisibilityAnimated(footerProgress, View.VISIBLE);
			V.setVisibilityAnimated(footerError, View.GONE);
			doLoadData();
			return;
		}
		super.onErrorRetryClick();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		lastBottomInset=insets.getSystemWindowInsetBottom();
		super.onApplyWindowInsets(UiUtils.applyBottomInsetToFixedView(replyContainer, insets));
	}

	private void openReply(){
		maybeShowPreReplySheet(mainStatus, ()->{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("replyTo", Parcels.wrap(mainStatus));
			args.putBoolean("fromThreadFragment", true);
			Nav.go(getActivity(), ComposeFragment.class, args);
		});
	}

	public int getSnackbarOffset(){
		return replyContainer.getHeight()-lastBottomInset;
	}

	@Override
	protected void drawDivider(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder, RecyclerView parent, Canvas c, Paint paint){
		if(holder instanceof StatusDisplayItem.Holder<?> statusHolder && siblingHolder instanceof StatusDisplayItem.Holder<?> siblingStatusHolder){
			Status siblingStatus=getStatusByID(siblingStatusHolder.getItemID());
			if(siblingStatus==null)
				return;
			if(statusHolder.getItemID().equals(siblingStatus.inReplyToId) && siblingStatus!=mainStatus && !statusHolder.getItemID().equals(mainStatus.id))
				return;
		}
		super.drawDivider(child, bottomSibling, holder, siblingHolder, parent, c, paint);
	}

	private Status findPreviousStatus(String id){
		for(int i=0;i<data.size();i++){
			if(data.get(i).id.equals(id))
				return i>0 ? data.get(i-1) : null;
		}
		return null;
	}

	private Status findNextStatus(String id){
		for(int i=0;i<data.size();i++){
			if(data.get(i).id.equals(id))
				return i<data.size()-1 ? data.get(i+1) : null;
		}
		return null;
	}

	@Override
	public void onProvideAssistContent(AssistContent content){
		content.setWebUri(Uri.parse(mainStatus.url));
	}

	@Override
	public void navigateToStatus(Status status){
		if(status.id.equals(mainStatus.id)){
			shakeListView();
			return;
		}
		super.navigateToStatus(status);
	}

	private class ReplyLinesItemDecoration extends RecyclerView.ItemDecoration{
		private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);

		@Override
		public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
			String currentID=null;
			boolean connectUp=false, connectToRoot=false, connectReply=false;
			paint.setColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(V.dp(2));
			paint.setStrokeCap(Paint.Cap.ROUND);
			for(int i=0;i<parent.getChildCount();i++){
				View child=parent.getChildAt(i);
				if(!(parent.getChildViewHolder(child) instanceof StatusDisplayItem.Holder<?> holder) || holder.getItemID().equals(mainStatus.id))
					continue;
				String itemID=holder.getItemID();
				if(!Objects.equals(currentID, itemID)){
					currentID=itemID;
					Status current=getStatusByID(currentID);
					Status previous=findPreviousStatus(currentID);
					Status next=findNextStatus(currentID);
					if(current==null)
						continue;

					connectUp=previous!=null && previous.id.equals(current.inReplyToId);
					connectToRoot=mainStatus.id.equals(current.inReplyToId);
					connectReply=next!=null && itemID.equals(next.inReplyToId);
				}

				if(!connectUp && !connectToRoot && !connectReply)
					continue;

				float lineX=V.dp(36);
				paint.setAlpha(Math.round(255*child.getAlpha()));
				c.save();
				c.clipRect(child.getX(), child.getY(), child.getX()+child.getWidth(), child.getY()+child.getHeight());
				if(holder instanceof HeaderStatusDisplayItem.Holder){
					if(connectUp || connectToRoot){
						c.drawLine(lineX, child.getY()-V.dp(2), lineX, child.getY()+V.dp(14), paint);
					}
					if(connectReply){
						c.drawLine(lineX, child.getY()+V.dp(62), lineX, child.getY()+child.getHeight()+V.dp(2), paint);
					}
				}else if(connectReply){
					c.drawLine(lineX, child.getY(), lineX, child.getY()+child.getHeight(), paint);
				}
				c.restore();
			}
		}
	}
}
