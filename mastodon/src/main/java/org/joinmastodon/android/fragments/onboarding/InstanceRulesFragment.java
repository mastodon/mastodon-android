package org.joinmastodon.android.fragments.onboarding;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class InstanceRulesFragment extends AppKitFragment{
	private UsableRecyclerView list;
	private MergeRecyclerAdapter adapter;
	private Button btn;
	private View buttonBar;
	private Instance instance;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setNavigationBarColor(UiUtils.getThemeColor(activity, R.attr.colorWindowBackground));
		instance=Parcels.unwrap(getArguments().getParcelable("instance"));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_onboarding_rules, container, false);

		list=view.findViewById(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		View headerView=inflater.inflate(R.layout.item_list_header, list, false);
		TextView title=headerView.findViewById(R.id.title);
		TextView subtitle=headerView.findViewById(R.id.subtitle);
		headerView.findViewById(R.id.step_counter).setVisibility(View.GONE);
		title.setText(R.string.instance_rules_title);
		subtitle.setText(getString(R.string.instance_rules_subtitle, instance.uri));

		adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(new SingleViewRecyclerAdapter(headerView));
		adapter.addAdapter(new ItemsAdapter());
		list.setAdapter(adapter);
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorPollVoted, 1, 16, 16, DividerItemDecoration.NOT_FIRST));

		btn=view.findViewById(R.id.btn_next);
		btn.setOnClickListener(v->onButtonClick());
		buttonBar=view.findViewById(R.id.button_bar);
		view.findViewById(R.id.btn_back).setOnClickListener(v->Nav.finish(this));

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		setStatusBarColor(UiUtils.getThemeColor(getActivity(), R.attr.colorBackgroundLight));
	}

	protected void onButtonClick(){
		Bundle args=new Bundle();
		args.putParcelable("instance", Parcels.wrap(instance));
		Nav.go(getActivity(), GoogleMadeMeAddThisFragment.class, args);
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

	private class ItemsAdapter extends RecyclerView.Adapter<ItemViewHolder>{

		@NonNull
		@Override
		public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new ItemViewHolder();
		}

		@Override
		public void onBindViewHolder(@NonNull ItemViewHolder holder, int position){
			holder.bind(instance.rules.get(position));
		}

		@Override
		public int getItemCount(){
			return instance.rules.size();
		}
	}

	private class ItemViewHolder extends BindableViewHolder<Instance.Rule>{
		private final TextView title, subtitle;
		private final ImageView checkbox;

		public ItemViewHolder(){
			super(getActivity(), R.layout.item_report_choice, list);
			title=findViewById(R.id.title);
			subtitle=findViewById(R.id.subtitle);
			checkbox=findViewById(R.id.checkbox);
			subtitle.setVisibility(View.GONE);
		}

		@Override
		public void onBind(Instance.Rule item){
			if(item.parsedText==null){
				item.parsedText=HtmlParser.parseLinks(item.text);
			}
			title.setText(item.parsedText);
		}
	}
}
