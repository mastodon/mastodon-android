package org.joinmastodon.android.fragments.onboarding;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.adapters.InstanceRulesAdapter;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.ElevationOnScrollListener;
import org.parceler.Parcels;

import androidx.recyclerview.widget.LinearLayoutManager;
import me.grishka.appkit.Nav;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.views.FragmentRootLinearLayout;
import me.grishka.appkit.views.UsableRecyclerView;

public class InstanceRulesFragment extends ToolbarFragment{
	private UsableRecyclerView list;
	private MergeRecyclerAdapter adapter;
	private Button btn;
	private View buttonBar;
	private Instance instance;
	private ElevationOnScrollListener onScrollListener;

	private static final int RULES_REQUEST=376;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setNavigationBarColor(UiUtils.getThemeColor(activity, R.attr.colorM3Surface));
		instance=Parcels.unwrap(getArguments().getParcelable("instance"));
		setTitle(R.string.instance_rules_title);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_onboarding_rules, container, false);

		list=view.findViewById(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		View headerView=inflater.inflate(R.layout.item_list_header_simple, list, false);
		TextView text=headerView.findViewById(R.id.text);
		text.setText(Html.fromHtml(getString(R.string.instance_rules_subtitle, "<b>"+Html.escapeHtml(instance.uri)+"</b>")));

		adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(new SingleViewRecyclerAdapter(headerView));
		adapter.addAdapter(new InstanceRulesAdapter(instance.rules));
		list.setAdapter(adapter);

		btn=view.findViewById(R.id.btn_next);
		btn.setOnClickListener(v->onButtonClick());
		buttonBar=view.findViewById(R.id.button_bar);

		view.findViewById(R.id.btn_back).setOnClickListener(v->Nav.finish(this));

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addOnScrollListener(onScrollListener=new ElevationOnScrollListener((FragmentRootLinearLayout) view, buttonBar, getToolbar()));
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		if(onScrollListener!=null){
			onScrollListener.setViews(buttonBar, getToolbar());
		}
	}

	protected void onButtonClick(){
		Bundle args=new Bundle();
		args.putParcelable("instance", Parcels.wrap(instance));
		Nav.goForResult(getActivity(), GoogleMadeMeAddThisFragment.class, args, RULES_REQUEST, this);
	}

	@Override
	public void onFragmentResult(int reqCode, boolean success, Bundle result){
		super.onFragmentResult(reqCode, success, result);
		if(reqCode==RULES_REQUEST && !success){
			Nav.finish(this);
		}
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		super.onApplyWindowInsets(UiUtils.applyBottomInsetToFixedView(buttonBar, insets));
	}
}
