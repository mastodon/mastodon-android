package org.joinmastodon.android.fragments.report;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.FinishReportFragmentsEvent;
import org.joinmastodon.android.fragments.StatusListFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.ReportReason;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.displayitems.LinkCardStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.MediaGridStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class ReportReasonChoiceFragment extends StatusListFragment{
	private MergeRecyclerAdapter mergeAdapter;
	private Button btn;
	private View buttonBar;
	protected ArrayList<ChoiceItem> items=new ArrayList<>();
	protected boolean isMultipleChoice;
	protected ArrayList<String> selectedIDs=new ArrayList<>();
	protected Account reportAccount;
	protected Status reportStatus;
	protected ProgressBar progressBar;
	private Relationship relationship;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setListLayoutId(R.layout.fragment_content_report_posts);
		setLayout(R.layout.fragment_report_posts);
		E.register(this);
	}

	@Override
	public void onDestroy(){
		E.unregister(this);
		super.onDestroy();
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setNavigationBarColor(UiUtils.getThemeColor(activity, R.attr.colorM3Surface));
		accountID=getArguments().getString("account");
		reportAccount=Parcels.unwrap(getArguments().getParcelable("reportAccount"));
		reportStatus=Parcels.unwrap(getArguments().getParcelable("status"));
		if(reportStatus!=null){
			Status hiddenStatus=reportStatus.clone();
			hiddenStatus.spoilerText=getString(R.string.post_hidden);
			onDataLoaded(Collections.singletonList(hiddenStatus));
			setTitle(R.string.report_title_post);
		}else{
			onDataLoaded(Collections.emptyList());
			setTitle(getString(R.string.report_title, reportAccount.acct));
		}
		relationship=Parcels.unwrap(getArguments().getParcelable("relationship"));
		if(relationship==null && reportStatus==null)
			loadRelationships(Collections.singleton(reportAccount.id));

		items.add(new ChoiceItem(getString(R.string.report_reason_personal), getString(R.string.report_reason_personal_subtitle), ReportReason.PERSONAL.name()));
		items.add(new ChoiceItem(getString(R.string.report_reason_spam), getString(R.string.report_reason_spam_subtitle), ReportReason.SPAM.name()));
		Instance inst=AccountSessionManager.getInstance().getInstanceInfo(AccountSessionManager.getInstance().getAccount(accountID).domain);
		if(inst!=null && inst.rules!=null && !inst.rules.isEmpty()){
			items.add(new ChoiceItem(getString(R.string.report_reason_violation), getString(R.string.report_reason_violation_subtitle), ReportReason.VIOLATION.name()));
		}
		items.add(new ChoiceItem(getString(R.string.report_reason_other), getString(R.string.report_reason_other_subtitle), ReportReason.OTHER.name()));
	}

	protected void onButtonClick(){
		ReportReason reason=ReportReason.valueOf(selectedIDs.get(0));
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("status", Parcels.wrap(reportStatus));
		args.putParcelable("reportAccount", Parcels.wrap(reportAccount));
		args.putString("reason", reason.name());
		args.putBoolean("fromPost", reportStatus!=null);
		args.putParcelable("relationship", Parcels.wrap(relationship));
		switch(reason){
			case PERSONAL -> {
				Nav.go(getActivity(), ReportDoneFragment.class, args);
				content.postDelayed(()->Nav.finish(this), 500);
			}
			case SPAM, OTHER -> Nav.go(getActivity(), ReportAddPostsChoiceFragment.class, args);
			case VIOLATION -> Nav.go(getActivity(), ReportRuleChoiceFragment.class, args);
		}
	}

	@Subscribe
	public void onFinishReportFragments(FinishReportFragmentsEvent ev){
		if(ev.reportAccountID.equals(reportAccount.id))
			Nav.finish(this);
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		super.onApplyWindowInsets(UiUtils.applyBottomInsetToFixedView(buttonBar, insets));
	}

	@Override
	protected void doLoadData(int offset, int count){

	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();

		LayoutInflater inflater=getActivity().getLayoutInflater();
		View headerView=inflater.inflate(R.layout.item_list_header, list, false);
		TextView title=headerView.findViewById(R.id.title);
		TextView subtitle=headerView.findViewById(R.id.subtitle);
		title.setText(reportStatus!=null ? getString(R.string.report_choose_reason) : getString(R.string.report_choose_reason_account, reportAccount.acct));
		subtitle.setText(getString(R.string.report_choose_reason_subtitle));

		adapter.addAdapter(new SingleViewRecyclerAdapter(headerView));
		adapter.addAdapter(super.getAdapter());
		adapter.addAdapter(new ChoiceItemsAdapter(getActivity(), isMultipleChoice, items, list, selectedIDs, btn::setEnabled));

		return mergeAdapter=adapter;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		btn=view.findViewById(R.id.btn_next);
		btn.setEnabled(!selectedIDs.isEmpty());
		btn.setOnClickListener(v->onButtonClick());
		buttonBar=view.findViewById(R.id.button_bar);
		progressBar=view.findViewById(R.id.top_progress);
		progressBar.setProgress(5);
		super.onViewCreated(view, savedInstanceState);
		((UsableRecyclerView)list).setIncludeMarginsInItemHitbox(false);

		if(reportStatus!=null){
			list.addItemDecoration(new RecyclerView.ItemDecoration(){
				@Override
				public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
					RecyclerView.ViewHolder holder=parent.getChildViewHolder(view);
					if(holder instanceof LinkCardStatusDisplayItem.Holder || holder instanceof MediaGridStatusDisplayItem.Holder){
						outRect.left=V.dp(16);
						outRect.right=V.dp(16);
					}
				}
			});

			list.addItemDecoration(new RecyclerView.ItemDecoration(){
				private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
				{
					paint.setStyle(Paint.Style.STROKE);
					paint.setStrokeWidth(V.dp(1));
					paint.setColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
				}

				@Override
				public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
					int firstPos=list.getChildAdapterPosition(list.getChildAt(0));
					int lastPos=-1;
					for(int i=list.getChildCount()-1;i>=0;i--){
						lastPos=list.getChildAdapterPosition(list.getChildAt(i));
						if(lastPos!=-1)
							break;
					}
					int postStart=mergeAdapter.getPositionForAdapter(adapter);
					if(lastPos<postStart || firstPos>postStart+displayItems.size()){
						return;
					}

					float top=V.dp(-12);
					float bottom=parent.getHeight()+V.dp(12);
					for(int i=0;i<parent.getChildCount();i++){
						View child=parent.getChildAt(i);
						int pos=parent.getChildAdapterPosition(child);
						if(pos==postStart)
							top=child.getY();
						if(pos==postStart+displayItems.size())
							bottom=child.getY()-V.dp(16);
					}

					float off=paint.getStrokeWidth()/2f;
					c.drawRoundRect(V.dp(16)-off, top-off, parent.getWidth()-V.dp(16)+off, bottom+off, V.dp(12), V.dp(12), paint);
				}

				@Override
				public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
					RecyclerView.ViewHolder holder=parent.getChildViewHolder(view);
					if(holder instanceof StatusDisplayItem.Holder<?>){
						outRect.left=outRect.right=V.dp(16);
					}
					int index=holder.getAbsoluteAdapterPosition()-mergeAdapter.getPositionForAdapter(adapter);
					if(index==displayItems.size()){
						outRect.top=V.dp(32);
					}
				}
			});
		}
	}

	@Override
	protected boolean wantsOverlaySystemNavigation(){
		return false;
	}

	@Override
	protected boolean wantsElevationOnScrollEffect(){
		return false;
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Status s){
		return StatusDisplayItem.buildItems(this, s, accountID, s, knownAccounts, StatusDisplayItem.FLAG_INSET | StatusDisplayItem.FLAG_NO_FOOTER);
	}

	@Override
	protected void onModifyItemViewHolder(BindableViewHolder<StatusDisplayItem> holder){
		if((Object)holder instanceof MediaGridStatusDisplayItem.Holder h){
			View layout=h.getLayout();
			layout.setOutlineProvider(OutlineProviders.roundedRect(8));
			layout.setClipToOutline(true);
			View overlay=h.getSensitiveOverlay();
			overlay.setOutlineProvider(OutlineProviders.roundedRect(8));
			overlay.setClipToOutline(true);
		}
	}

	@Override
	public void putRelationship(String id, Relationship rel){
		super.putRelationship(id, rel);
		if(id.equals(reportAccount.id))
			relationship=rel;
	}
}
