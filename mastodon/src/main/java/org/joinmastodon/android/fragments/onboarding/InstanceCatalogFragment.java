package org.joinmastodon.android.fragments.onboarding;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.requests.GetInstance;
import org.joinmastodon.android.api.requests.catalog.GetCatalogCategories;
import org.joinmastodon.android.api.requests.catalog.GetCatalogInstances;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.catalog.CatalogCategory;
import org.joinmastodon.android.model.catalog.CatalogInstance;

import java.net.IDN;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.views.UsableRecyclerView;

public class InstanceCatalogFragment extends BaseRecyclerFragment<CatalogInstance>{
	private InstancesAdapter adapter;
	private MergeRecyclerAdapter mergeAdapter;
	private View headerView;
	private CatalogInstance chosenInstance;
	private List<CatalogInstance> filteredData=new ArrayList<>();
	private Button nextButton;
	private MastodonAPIRequest<?> getCategoriesRequest;
	private EditText searchEdit;
	private UsableRecyclerView categoriesList;
	private Runnable searchDebouncer=this::onSearchChangedDebounced;
	private String currentSearchQuery;
	private String currentCategory="all";
	private List<CatalogCategory> categories=new ArrayList<>();
	private String loadingInstanceDomain;
	private GetInstance loadingInstanceRequest;
	private HashMap<String, Instance> instancesCache=new HashMap<>();
	private ProgressDialog instanceProgressDialog;

	private boolean isSignup;

	public InstanceCatalogFragment(){
		super(R.layout.fragment_onboarding_common, 10);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		isSignup=getArguments().getBoolean("signup");
	}

	@Override
	public void onAttach(Context context){
		super.onAttach(context);
		setRefreshEnabled(false);
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetCatalogInstances(null, null)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<CatalogInstance> result){
						Map<String, List<CatalogInstance>> byLang=result.stream().collect(Collectors.groupingBy(ci->ci.language));
						// get the list of user-configured system languages
						List<String> userLangs;
						if(Build.VERSION.SDK_INT<24){
							userLangs=Collections.singletonList(getResources().getConfiguration().locale.getLanguage());
						}else{
							LocaleList ll=getResources().getConfiguration().getLocales();
							userLangs=new ArrayList<>(ll.size());
							for(int i=0;i<ll.size();i++){
								userLangs.add(ll.get(i).getLanguage());
							}
						}
						// add instances in preferred languages to the top of the list, in the order of preference
						ArrayList<CatalogInstance> sortedList=new ArrayList<>();
						for(String lang:userLangs){
							List<CatalogInstance> langInstances=byLang.remove(lang);
							if(langInstances!=null){
								sortedList.addAll(langInstances);
							}
						}
						// sort the remaining language groups by aggregate lastWeekUsers
						class InstanceGroup{
							public int activeUsers;
							public List<CatalogInstance> instances;
						}
						byLang.values().stream().map(il->{
							InstanceGroup group=new InstanceGroup();
							group.instances=il;
							for(CatalogInstance instance:il){
								group.activeUsers+=instance.lastWeekUsers;
							}
							return group;
						}).sorted(Comparator.comparingInt(g->g.activeUsers)).forEachOrdered(ig->sortedList.addAll(ig.instances));
						onDataLoaded(sortedList, false);
						updateFilteredList();
					}

					@Override
					public void onError(ErrorResponse error){
						InstanceCatalogFragment.this.onError(error);
					}
				})
				.execNoAuth("");
		getCategoriesRequest=new GetCatalogCategories(null)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<CatalogCategory> result){
						getCategoriesRequest=null;
						CatalogCategory all=new CatalogCategory();
						all.category="all";
						categories.add(all);
						categories.addAll(result);
						categoriesList.getAdapter().notifyItemRangeInserted(0, categories.size());
					}

					@Override
					public void onError(ErrorResponse error){
						getCategoriesRequest=null;
						error.showToast(getActivity());
					}
				})
				.execNoAuth("");
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		if(getCategoriesRequest!=null)
			getCategoriesRequest.cancel();
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		headerView=getActivity().getLayoutInflater().inflate(R.layout.header_onboarding_instance_catalog, list, false);
		searchEdit=headerView.findViewById(R.id.search_edit);
		categoriesList=headerView.findViewById(R.id.categories_list);
		searchEdit.setOnEditorActionListener(this::onSearchEnterPressed);
		searchEdit.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){
				searchEdit.removeCallbacks(searchDebouncer);
				searchEdit.postDelayed(searchDebouncer, 300);
			}

			@Override
			public void afterTextChanged(Editable s){
			}
		});
		categoriesList.setAdapter(new CategoriesAdapter());
		categoriesList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));

		mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(headerView));
		mergeAdapter.addAdapter(adapter=new InstancesAdapter());
		return mergeAdapter;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		nextButton=view.findViewById(R.id.btn_next);
		nextButton.setOnClickListener(this::onNextClick);
	}

	private void onNextClick(View v){
		String domain=chosenInstance.domain;
		Instance instance=instancesCache.get(domain);
		if(instance!=null){
			proceedWithAuthOrSignup(instance);
		}else{
			showProgressDialog();
			if(!domain.equals(loadingInstanceDomain)){
				loadInstanceInfo(domain);
			}
		}
	}

	private void proceedWithAuthOrSignup(Instance instance){
		if(isSignup){
			Toast.makeText(getActivity(), "not implemented yet", Toast.LENGTH_SHORT).show();
		}else{
			AccountSessionManager.getInstance().authenticate(getActivity(), instance);
		}
	}

	private boolean onSearchEnterPressed(TextView v, int actionId, KeyEvent event){
		if(event!=null && event.getAction()!=KeyEvent.ACTION_DOWN)
			return true;
		searchEdit.removeCallbacks(searchDebouncer);
		Instance instance=instancesCache.get(currentSearchQuery);
		if(instance==null){
			showProgressDialog();
			loadInstanceInfo(currentSearchQuery);
		}else{
			proceedWithAuthOrSignup(instance);
		}
		return true;
	}

	private void onSearchChangedDebounced(){
		currentSearchQuery=searchEdit.getText().toString().toLowerCase();
		updateFilteredList();
		loadInstanceInfo(currentSearchQuery);
	}

	private void updateFilteredList(){
		ArrayList<CatalogInstance> prevData=new ArrayList<>(filteredData);
		filteredData.clear();
		for(CatalogInstance instance:data){
			if(currentCategory.equals("all") || instance.categories.contains(currentCategory)){
				if(TextUtils.isEmpty(currentSearchQuery) || instance.domain.contains(currentSearchQuery)){
					if(instance.domain.equals(currentSearchQuery) || !instance.approvalRequired)
						filteredData.add(instance);
				}
			}
		}
		DiffUtil.calculateDiff(new DiffUtil.Callback(){
			@Override
			public int getOldListSize(){
				return prevData.size();
			}

			@Override
			public int getNewListSize(){
				return filteredData.size();
			}

			@Override
			public boolean areItemsTheSame(int oldItemPosition, int newItemPosition){
				return prevData.get(oldItemPosition)==filteredData.get(newItemPosition);
			}

			@Override
			public boolean areContentsTheSame(int oldItemPosition, int newItemPosition){
				return prevData.get(oldItemPosition)==filteredData.get(newItemPosition);
			}
		}).dispatchUpdatesTo(adapter);
	}

	private void showProgressDialog(){
		instanceProgressDialog=new ProgressDialog(getActivity());
		instanceProgressDialog.setMessage(getString(R.string.loading_instance));
		instanceProgressDialog.setOnCancelListener(dialog->{
			loadingInstanceRequest.cancel();
			loadingInstanceRequest=null;
			loadingInstanceDomain=null;
		});
		instanceProgressDialog.show();
	}

	private void loadInstanceInfo(String _domain){
		String domain;
		try{
			domain=IDN.toASCII(_domain);
		}catch(IllegalArgumentException x){
			return;
		}
		Instance cachedInstance=instancesCache.get(domain);
		if(cachedInstance!=null){
			boolean found=false;
			for(CatalogInstance ci:filteredData){
				if(ci.domain.equals(currentSearchQuery)){
					found=true;
					break;
				}
			}
			if(!found){
				CatalogInstance ci=cachedInstance.toCatalogInstance();
				filteredData.add(0, ci);
				adapter.notifyItemInserted(0);
			}
			return;
		}
		if(loadingInstanceDomain!=null){
			if(loadingInstanceDomain.equals(domain))
				return;
			else
				loadingInstanceRequest.cancel();
		}
		loadingInstanceDomain=domain;
		loadingInstanceRequest=new GetInstance();
		loadingInstanceRequest.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Instance result){
						loadingInstanceRequest=null;
						loadingInstanceDomain=null;
						result.uri=domain; // needed for instances that use domain redirection
						instancesCache.put(domain, result);
						if(instanceProgressDialog!=null){
							instanceProgressDialog.dismiss();
							instanceProgressDialog=null;
							proceedWithAuthOrSignup(result);
						}
						if(domain.equals(currentSearchQuery)){
							boolean found=false;
							for(CatalogInstance ci:filteredData){
								if(ci.domain.equals(currentSearchQuery)){
									found=true;
									break;
								}
							}
							if(!found){
								CatalogInstance ci=result.toCatalogInstance();
								filteredData.add(0, ci);
								adapter.notifyItemInserted(0);
							}
						}
					}

					@Override
					public void onError(ErrorResponse error){
						loadingInstanceRequest=null;
						loadingInstanceDomain=null;
						if(instanceProgressDialog!=null){
							instanceProgressDialog.dismiss();
							instanceProgressDialog=null;
							new AlertDialog.Builder(getActivity())
									.setTitle(R.string.error)
									.setMessage(getString(R.string.not_a_mastodon_instance, domain)+"\n\n"+((MastodonErrorResponse)error).error)
									.setPositiveButton(R.string.ok, null)
									.show();
						}
					}
				}).execNoAuth(domain);
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
			return 1;
		}
	}

	private class InstanceViewHolder extends BindableViewHolder<CatalogInstance> implements UsableRecyclerView.Clickable{
		private final TextView title, description, userCount, lang;
		private final RadioButton radioButton;

		public InstanceViewHolder(){
			super(getActivity(), R.layout.item_instance_catalog, list);
			title=findViewById(R.id.title);
			description=findViewById(R.id.description);
			userCount=findViewById(R.id.user_count);
			lang=findViewById(R.id.lang);
			radioButton=findViewById(R.id.radiobtn);
		}

		@Override
		public void onBind(CatalogInstance item){
			title.setText(item.normalizedDomain);
			description.setText(item.description);
			userCount.setText(""+item.totalUsers);
			lang.setText(item.language.toUpperCase());
			radioButton.setChecked(chosenInstance==item);
		}

		@Override
		public void onClick(){
			if(chosenInstance==item)
				return;
			if(chosenInstance!=null){
				int idx=filteredData.indexOf(chosenInstance);
				if(idx!=-1){
					RecyclerView.ViewHolder holder=list.findViewHolderForAdapterPosition(mergeAdapter.getPositionForAdapter(adapter)+idx);
					if(holder instanceof InstanceViewHolder){
						((InstanceViewHolder)holder).radioButton.setChecked(false);
					}
				}
			}
			radioButton.setChecked(true);
			if(chosenInstance==null)
				nextButton.setEnabled(true);
			chosenInstance=item;
			loadInstanceInfo(chosenInstance.domain);
		}
	}

	private class CategoriesAdapter extends RecyclerView.Adapter<CategoryViewHolder>{
		@NonNull
		@Override
		public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new CategoryViewHolder();
		}

		@Override
		public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position){
			holder.bind(categories.get(position));
		}

		@Override
		public int getItemCount(){
			return categories.size();
		}
	}

	private class CategoryViewHolder extends BindableViewHolder<CatalogCategory> implements UsableRecyclerView.Clickable{
		private final RadioButton radioButton;

		public CategoryViewHolder(){
			super(getActivity(), R.layout.item_instance_category, categoriesList);
			radioButton=findViewById(R.id.radiobtn);
		}

		@Override
		public void onBind(CatalogCategory item){
			radioButton.setText(item.category);
			radioButton.setChecked(item.category.equals(currentCategory));
		}

		@Override
		public void onClick(){
			if(currentCategory.equals(item.category))
				return;
			int i=0;
			for(CatalogCategory c:categories){
				if(c.category.equals(currentCategory)){
					RecyclerView.ViewHolder holder=categoriesList.findViewHolderForAdapterPosition(i);
					if(holder!=null){
						((CategoryViewHolder)holder).radioButton.setChecked(false);
					}
					break;
				}
				i++;
			}
			currentCategory=item.category;
			radioButton.setChecked(true);
			updateFilteredList();
		}
	}
}
