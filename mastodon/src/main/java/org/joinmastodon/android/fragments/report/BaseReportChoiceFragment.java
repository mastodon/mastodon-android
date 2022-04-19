package org.joinmastodon.android.fragments.report;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class BaseReportChoiceFragment extends ToolbarFragment{
	private UsableRecyclerView list;
	private MergeRecyclerAdapter adapter;
	private Button btn;
	private View buttonBar;
	protected ArrayList<Item> items=new ArrayList<>();
	protected boolean isMultipleChoice;
	protected ArrayList<String> selectedIDs=new ArrayList<>();
	protected String accountID;
	protected Account reportAccount;
	protected Status reportStatus;

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
		Item header=getHeaderItem();
		View headerView=inflater.inflate(R.layout.item_list_header, list, false);
		TextView title=headerView.findViewById(R.id.title);
		TextView subtitle=headerView.findViewById(R.id.subtitle);
		TextView stepCounter=headerView.findViewById(R.id.step_counter);
		title.setText(header.title);
		subtitle.setText(header.subtitle);
		stepCounter.setText(getString(R.string.step_x_of_n, getStepNumber(), 3));

		adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(new SingleViewRecyclerAdapter(headerView));
		adapter.addAdapter(new ItemsAdapter());
		list.setAdapter(adapter);
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorPollVoted, 1, 16, 16, DividerItemDecoration.NOT_FIRST));

		btn=view.findViewById(R.id.btn_next);
		btn.setEnabled(!selectedIDs.isEmpty());
		btn.setOnClickListener(v->onButtonClick());
		buttonBar=view.findViewById(R.id.button_bar);

		return view;
	}

	protected abstract Item getHeaderItem();
	protected abstract void populateItems();
	protected abstract void onButtonClick();
	protected abstract int getStepNumber();

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

	protected static class Item{
		public String title, subtitle, id;

		public Item(String title, String subtitle, String id){
			this.title=title;
			this.subtitle=subtitle;
			this.id=id;
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
			holder.bind(items.get(position));
		}

		@Override
		public int getItemCount(){
			return items.size();
		}
	}

	private class ItemViewHolder extends BindableViewHolder<Item> implements UsableRecyclerView.Clickable{
		private final TextView title, subtitle;
		private final ImageView checkbox;

		public ItemViewHolder(){
			super(getActivity(), R.layout.item_report_choice, list);
			title=findViewById(R.id.title);
			subtitle=findViewById(R.id.subtitle);
			checkbox=findViewById(R.id.checkbox);
		}

		@Override
		public void onBind(Item item){
			title.setText(item.title);
			if(TextUtils.isEmpty(item.subtitle)){
				subtitle.setVisibility(View.GONE);
			}else{
				subtitle.setVisibility(View.VISIBLE);
				subtitle.setText(item.subtitle);
			}
			checkbox.setSelected(selectedIDs.contains(item.id));
		}

		@Override
		public void onClick(){
			if(isMultipleChoice){
				if(selectedIDs.contains(item.id))
					selectedIDs.remove(item.id);
				else
					selectedIDs.add(item.id);
				rebind();
			}else{
				if(!selectedIDs.contains(item.id)){
					if(!selectedIDs.isEmpty()){
						String prev=selectedIDs.remove(0);
						for(int i=0;i<list.getChildCount();i++){
							RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
							if(holder instanceof ItemViewHolder ivh && ivh.getItem().id.equals(prev)){
								ivh.rebind();
								break;
							}
						}
					}
					selectedIDs.add(item.id);
					rebind();
				}
			}
			btn.setEnabled(!selectedIDs.isEmpty());
		}
	}
}
