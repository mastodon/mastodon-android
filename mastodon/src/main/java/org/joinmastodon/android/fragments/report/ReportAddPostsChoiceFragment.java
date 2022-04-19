package org.joinmastodon.android.fragments.report;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountStatuses;
import org.joinmastodon.android.events.FinishReportFragmentsEvent;
import org.joinmastodon.android.fragments.StatusListFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.PhotoLayoutHelper;
import org.joinmastodon.android.ui.displayitems.AudioStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ImageStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.LinkCardStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ReblogOrReplyLineStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class ReportAddPostsChoiceFragment extends StatusListFragment{

	private Button btn;
	private View buttonBar;
	private ArrayList<String> selectedIDs=new ArrayList<>();
	private String accountID;
	private Account reportAccount;
	private Status reportStatus;
	private SparseIntArray knownDisplayItemHeights=new SparseIntArray();
	private HashSet<String> postsWithKnownNonHeaderHeights=new HashSet<>();

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setListLayoutId(R.layout.fragment_content_report_posts);
		setLayout(R.layout.fragment_report_posts);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setNavigationBarColor(UiUtils.getThemeColor(activity, R.attr.colorWindowBackground));
		accountID=getArguments().getString("account");
		reportAccount=Parcels.unwrap(getArguments().getParcelable("reportAccount"));
		reportStatus=Parcels.unwrap(getArguments().getParcelable("status"));
		if(reportStatus!=null)
			selectedIDs.add(reportStatus.id);
		setTitle(getString(R.string.report_title, reportAccount.acct));
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetAccountStatuses(reportAccount.id, offset>0 ? getMaxID() : null, null, count, GetAccountStatuses.Filter.NO_REBLOGS)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, !result.isEmpty());
					}
				})
				.exec(accountID);
	}

	@Override
	public void onItemClick(String id){
		if(selectedIDs.contains(id))
			selectedIDs.remove(id);
		else
			selectedIDs.add(id);
		list.invalidate();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		btn=view.findViewById(R.id.btn_next);
		btn.setEnabled(!selectedIDs.isEmpty());
		btn.setOnClickListener(this::onButtonClick);
		view.findViewById(R.id.btn_back).setOnClickListener(this::onButtonClick);
		buttonBar=view.findViewById(R.id.button_bar);

		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			private Drawable uncheckedIcon=getResources().getDrawable(R.drawable.ic_fluent_radio_button_24_regular, getActivity().getTheme()).mutate();
			private Drawable checkedIcon=getResources().getDrawable(R.drawable.ic_fluent_checkmark_circle_24_filled, getActivity().getTheme()).mutate();
			{
				int color=UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary);
				checkedIcon.setTint(color);
				uncheckedIcon.setTint(color);
				checkedIcon.setBounds(0, 0, checkedIcon.getIntrinsicWidth(), checkedIcon.getIntrinsicHeight());
				uncheckedIcon.setBounds(0, 0, uncheckedIcon.getIntrinsicWidth(), uncheckedIcon.getIntrinsicHeight());
			}

			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				RecyclerView.ViewHolder holder=parent.getChildViewHolder(view);
				if(holder.getAbsoluteAdapterPosition()==0)
					return;
				outRect.left=V.dp(40);
				if(holder instanceof ImageStatusDisplayItem.Holder<?> imgHolder){
					PhotoLayoutHelper.TiledLayoutResult layout=imgHolder.getItem().tiledLayout;
					PhotoLayoutHelper.TiledLayoutResult.Tile tile=imgHolder.getItem().thisTile;
					String siblingID;
					if(holder.getAbsoluteAdapterPosition()<parent.getAdapter().getItemCount()-1){
						siblingID=displayItems.get(holder.getAbsoluteAdapterPosition()-getMainAdapterOffset()+1).parentID;
					}else{
						siblingID=null;
					}
					if(tile.startCol>0)
						outRect.left=0;
					outRect.left+=V.dp(16);
					outRect.right=V.dp(16);
					if(!imgHolder.getItemID().equals(siblingID) || tile.startRow+tile.rowSpan==layout.rowSizes.length)
						outRect.bottom=V.dp(16);
				}else if(holder instanceof AudioStatusDisplayItem.Holder){
					outRect.bottom=V.dp(16);
				}else if(holder instanceof LinkCardStatusDisplayItem.Holder){
					outRect.bottom=V.dp(16);
					outRect.left+=V.dp(16);
					outRect.right=V.dp(16);
				}
			}

			@Override
			public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				// 1st pass: update item heights
				for(int i=0;i<parent.getChildCount();i++){
					View child=parent.getChildAt(i);
					RecyclerView.ViewHolder holder=parent.getChildViewHolder(child);
					if(holder instanceof StatusDisplayItem.Holder sdiHolder){
						parent.getDecoratedBoundsWithMargins(child, tmpRect);
						String id=sdiHolder.getItemID();
						int height=tmpRect.height();
						if(holder instanceof ImageStatusDisplayItem.Holder<?> imgHolder){
							if(imgHolder.getItem().thisTile.startCol+imgHolder.getItem().thisTile.colSpan<imgHolder.getItem().tiledLayout.columnSizes.length)
								height=0;
						}
						if(!(holder instanceof HeaderStatusDisplayItem.Holder) && !(holder instanceof ReblogOrReplyLineStatusDisplayItem.Holder))
							postsWithKnownNonHeaderHeights.add(id);
						knownDisplayItemHeights.put(holder.getAbsoluteAdapterPosition(), height);
					}
				}
				// 2nd pass: draw checkboxes
				String lastPostID=null;
				for(int i=0;i<parent.getChildCount();i++){
					View child=parent.getChildAt(i);
					RecyclerView.ViewHolder holder=parent.getChildViewHolder(child);
					if(holder instanceof StatusDisplayItem.Holder<?> sdiHolder){
						String postID=sdiHolder.getItemID();
						if(!postID.equals(lastPostID)){
							lastPostID=postID;
							if(!postsWithKnownNonHeaderHeights.contains(postID))
								continue; // We don't know full height of this post yet
							int postHeight=0;
							int heightOffset=0;
							for(int j=holder.getAbsoluteAdapterPosition()-getMainAdapterOffset();j<displayItems.size();j++){
								StatusDisplayItem item=displayItems.get(j);
								if(!item.parentID.equals(postID))
									break;
								postHeight+=knownDisplayItemHeights.get(j+getMainAdapterOffset());
							}
							for(int j=holder.getAbsoluteAdapterPosition()-getMainAdapterOffset()-1;j>=0;j--){
								StatusDisplayItem item=displayItems.get(j);
								if(!item.parentID.equals(postID))
									break;
								int itemHeight=knownDisplayItemHeights.get(j+getMainAdapterOffset());
								postHeight+=itemHeight;
								heightOffset+=itemHeight;
							}
							int y=Math.round(child.getY())+postHeight/2-heightOffset;
							Drawable check=selectedIDs.contains(postID) ? checkedIcon : uncheckedIcon;
							c.save();
							c.translate(V.dp(16), y-check.getIntrinsicHeight()/2f);
							check.draw(c);
							c.restore();
						}
					}
				}
			}
		});
	}

	@Override
	protected int getMainAdapterOffset(){
		return 1;
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		View headerView=getActivity().getLayoutInflater().inflate(R.layout.item_list_header, list, false);
		TextView title=headerView.findViewById(R.id.title);
		TextView subtitle=headerView.findViewById(R.id.subtitle);
		TextView stepCounter=headerView.findViewById(R.id.step_counter);
		title.setText(R.string.report_choose_posts);
		subtitle.setText(R.string.report_choose_posts_subtitle);
		stepCounter.setText(getString(R.string.step_x_of_n, 2, 3));

		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(new SingleViewRecyclerAdapter(headerView));
		adapter.addAdapter(super.getAdapter());
		return adapter;
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Status s){
		List<StatusDisplayItem> items=StatusDisplayItem.buildItems(this, s, accountID, s, knownAccounts, true, false);
		for(StatusDisplayItem item:items){
			if(item instanceof ImageStatusDisplayItem isdi){
				isdi.horizontalInset=V.dp(40+32);
			}
		}
		return items;
	}

	protected void drawDivider(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder, RecyclerView parent, Canvas c, Paint paint){
		parent.getDecoratedBoundsWithMargins(child, tmpRect);
		tmpRect.offset(0, Math.round(child.getTranslationY()));
		float y=tmpRect.bottom-V.dp(.5f);
		paint.setAlpha(Math.round(255*child.getAlpha()));
		c.drawLine(V.dp(16), y, parent.getWidth()-V.dp(16), y, paint);
	}

	private void onButtonClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("reportAccount", Parcels.wrap(reportAccount));
		if(v.getId()==R.id.btn_next){
			args.putStringArrayList("statusIDs", selectedIDs);
		}else{
			ArrayList<String> ids=new ArrayList<>();
			if(reportStatus!=null)
				ids.add(reportStatus.id);
			args.putStringArrayList("statusIDs", ids);
		}
		args.putStringArrayList("ruleIDs", getArguments().getStringArrayList("ruleIDs"));
		args.putString("reason", getArguments().getString("reason"));
		Nav.go(getActivity(), ReportCommentFragment.class, args);
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

	@Subscribe
	public void onFinishReportFragments(FinishReportFragmentsEvent ev){
		if(ev.reportAccountID.equals(reportAccount.id))
			Nav.finish(this);
	}

	@Override
	protected boolean wantsOverlaySystemNavigation(){
		return false;
	}
}
