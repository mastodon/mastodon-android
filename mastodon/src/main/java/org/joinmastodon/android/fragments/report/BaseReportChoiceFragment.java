package org.joinmastodon.android.fragments.report;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.MastodonToolbarFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;

import androidx.recyclerview.widget.LinearLayoutManager;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class BaseReportChoiceFragment extends MastodonToolbarFragment{
	private UsableRecyclerView list;
	private MergeRecyclerAdapter adapter;
	private Button btn;
	private View buttonBar;
	protected ArrayList<ChoiceItem> items=new ArrayList<>();
	protected boolean isMultipleChoice;
	protected ArrayList<String> selectedIDs=new ArrayList<>();
	protected String accountID;
	protected Account reportAccount;
	protected Status reportStatus;
	protected ProgressBar progressBar;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
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
		setNavigationBarColor(UiUtils.getThemeColor(activity, R.attr.colorWindowBackground));
		accountID=getArguments().getString("account");
		reportAccount=Parcels.unwrap(getArguments().getParcelable("reportAccount"));
		reportStatus=Parcels.unwrap(getArguments().getParcelable("status"));
		setTitle(getString(R.string.report_title, reportAccount.acct));
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_report_choice, container, false);

		list=view.findViewById(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		populateItems();
		ChoiceItem header=getHeaderItem();
		View headerView=inflater.inflate(R.layout.item_list_header, list, false);
		TextView title=headerView.findViewById(R.id.title);
		TextView subtitle=headerView.findViewById(R.id.subtitle);
		title.setText(header.title);
		subtitle.setText(header.subtitle);

		btn=view.findViewById(R.id.btn_next);
		btn.setEnabled(!selectedIDs.isEmpty());
		btn.setOnClickListener(v->onButtonClick());
		buttonBar=view.findViewById(R.id.button_bar);
		progressBar=view.findViewById(R.id.top_progress);

		adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(new SingleViewRecyclerAdapter(headerView));
		adapter.addAdapter(new ChoiceItemsAdapter(getActivity(), isMultipleChoice, items, list, selectedIDs, btn::setEnabled));
		list.setAdapter(adapter);

		return view;
	}

	protected abstract ChoiceItem getHeaderItem();
	protected abstract void populateItems();
	protected abstract void onButtonClick();

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		super.onApplyWindowInsets(UiUtils.applyBottomInsetToFixedView(buttonBar, insets));
	}
}
