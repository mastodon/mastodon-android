package org.joinmastodon.android.fragments.onboarding;

import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toolbar;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.catalog.GetCatalogInstances;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.catalog.CatalogInstance;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class InstanceChooserLoginFragment extends InstanceCatalogFragment{
	private View headerView;
	private boolean loadedAutocomplete;
	private ImageButton clearBtn;

	public InstanceChooserLoginFragment(){
		super(R.layout.fragment_login, 10);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		dataLoaded();
		setTitle(R.string.login_title);
		if(!loadedAutocomplete){
			loadAutocompleteServers();
		}
	}

	@Override
	protected void proceedWithAuthOrSignup(Instance instance){
		AccountSessionManager.getInstance().authenticate(getActivity(), instance);
	}

	@Override
	protected void updateFilteredList(){
		ArrayList<CatalogInstance> prevData=new ArrayList<>(filteredData);
		filteredData.clear();
		if(currentSearchQuery.length()>0){
			boolean foundExactMatch=false;
			for(CatalogInstance inst:data){
				if(inst.normalizedDomain.contains(currentSearchQuery)){
					filteredData.add(inst);
					if(inst.normalizedDomain.equals(currentSearchQuery))
						foundExactMatch=true;
				}
			}
			if(!foundExactMatch)
				filteredData.add(0, fakeInstance);
		}
		UiUtils.updateList(prevData, filteredData, list, adapter, Objects::equals);
		for(int i=0;i<list.getChildCount();i++){
			list.getChildAt(i).invalidateOutline();
		}
	}

	@Override
	protected void doLoadData(int offset, int count){

	}

	private void loadAutocompleteServers(){
		loadedAutocomplete=true;
		new GetCatalogInstances(null, null)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<CatalogInstance> result){
						data.clear();
						data.addAll(sortInstances(result));
					}

					@Override
					public void onError(ErrorResponse error){

					}
				})
				.execNoAuth("");
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		Toolbar toolbar=getToolbar();
		toolbar.setElevation(0);
		toolbar.setBackground(null);
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		headerView=getActivity().getLayoutInflater().inflate(R.layout.header_onboarding_login, list, false);
		clearBtn=headerView.findViewById(R.id.search_clear);
		searchEdit=headerView.findViewById(R.id.search_edit);
		searchEdit.setOnEditorActionListener(this::onSearchEnterPressed);
		searchEdit.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){
				searchEdit.removeCallbacks(searchDebouncer);
				searchEdit.postDelayed(searchDebouncer, 300);

				if(s.length()>0){
					fakeInstance.domain=fakeInstance.normalizedDomain=s.toString();
					fakeInstance.description=getString(R.string.loading_instance);
					if(filteredData.size()>0 && filteredData.get(0)==fakeInstance){
						if(list.findViewHolderForAdapterPosition(1) instanceof InstanceViewHolder ivh){
							ivh.rebind();
						}
					}
					if(filteredData.isEmpty()){
						filteredData.add(fakeInstance);
						adapter.notifyItemInserted(0);
					}
					clearBtn.setVisibility(View.VISIBLE);
				}else{
					clearBtn.setVisibility(View.GONE);
				}
			}

			@Override
			public void afterTextChanged(Editable s){
			}
		});
		clearBtn.setOnClickListener(v->searchEdit.setText(""));

		mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(headerView));
		mergeAdapter.addAdapter(adapter=new InstancesAdapter());
		return mergeAdapter;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		setStatusBarColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Background));

		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				if(parent.getChildViewHolder(view) instanceof InstanceViewHolder){
					outRect.left=outRect.right=V.dp(16);
				}
			}
		});
		((UsableRecyclerView)list).setDrawSelectorOnTop(true);
	}

	private class InstancesAdapter extends UsableRecyclerView.Adapter<InstanceViewHolder>{
		public InstancesAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public InstanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new InstanceViewHolder();
		}

		@Override
		public void onBindViewHolder(InstanceViewHolder holder, int position){
			holder.bind(filteredData.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			return filteredData.size();
		}

		@Override
		public int getItemViewType(int position){
			return -1;
		}
	}

	private class InstanceViewHolder extends BindableViewHolder<CatalogInstance> implements UsableRecyclerView.Clickable{
		private final TextView title, description;
		private final RadioButton radioButton;

		public InstanceViewHolder(){
			super(getActivity(), R.layout.item_instance_login, list);
			title=findViewById(R.id.title);
			description=findViewById(R.id.description);
			radioButton=findViewById(R.id.radiobtn);
			radioButton.setMinWidth(0);
			radioButton.setMinHeight(0);

			itemView.setOutlineProvider(new ViewOutlineProvider(){
				@Override
				public void getOutline(View view, Outline outline){
					outline.setRoundRect(0, getAbsoluteAdapterPosition()==1 ? 0 : V.dp(-4), view.getWidth(), view.getHeight()+(getAbsoluteAdapterPosition()==filteredData.size() ? 0 : V.dp(4)), V.dp(4));
				}
			});
			itemView.setClipToOutline(true);
		}

		@Override
		public void onBind(CatalogInstance item){
			title.setText(item.normalizedDomain);
			description.setText(item.description);
			radioButton.setChecked(chosenInstance==item);
		}

		@Override
		public void onClick(){
			if(chosenInstance==item)
				return;
			if(chosenInstance!=null){
				int idx=filteredData.indexOf(chosenInstance);
				if(idx!=-1){
					for(int i=0;i<list.getChildCount();i++){
						RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
						if(holder.getAbsoluteAdapterPosition()==mergeAdapter.getPositionForAdapter(adapter)+idx && holder instanceof InstanceViewHolder ivh){
							ivh.radioButton.setChecked(false);
							break;
						}
					}
				}
			}
			radioButton.setChecked(true);
			if(chosenInstance==null)
				nextButton.setEnabled(true);
			chosenInstance=item;
			loadInstanceInfo(chosenInstance.domain, false);
		}
	}
}
