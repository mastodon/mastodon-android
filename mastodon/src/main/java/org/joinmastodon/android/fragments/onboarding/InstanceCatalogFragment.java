package org.joinmastodon.android.fragments.onboarding;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.requests.instance.GetInstance;
import org.joinmastodon.android.api.requests.catalog.GetCatalogCategories;
import org.joinmastodon.android.api.requests.catalog.GetCatalogInstances;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.catalog.CatalogCategory;
import org.joinmastodon.android.model.catalog.CatalogInstance;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.tabs.TabLayout;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

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
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
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
	private TabLayout categoriesList;
	private Runnable searchDebouncer=this::onSearchChangedDebounced;
	private String currentSearchQuery;
	private String currentCategory="all";
	private List<CatalogCategory> categories=new ArrayList<>();
	private String loadingInstanceDomain;
	private GetInstance loadingInstanceRequest;
	private HashMap<String, Instance> instancesCache=new HashMap<>();
	private ProgressDialog instanceProgressDialog;
	private View buttonBar;

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
						}).sorted(Comparator.comparingInt((InstanceGroup g)->g.activeUsers).reversed()).forEachOrdered(ig->sortedList.addAll(ig.instances));
						onDataLoaded(sortedList, false);
						updateFilteredList();
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
						onDataLoaded(Collections.emptyList(), false);
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
						result.stream().sorted(Comparator.comparingInt((CatalogCategory cc)->cc.serversCount).reversed()).forEach(categories::add);
						updateCategories();
					}

					@Override
					public void onError(ErrorResponse error){
						getCategoriesRequest=null;
						error.showToast(getActivity());
						CatalogCategory all=new CatalogCategory();
						all.category="all";
						categories.add(all);
						updateCategories();
					}
				})
				.execNoAuth("");
	}

	private void updateCategories(){
		categoriesList.removeAllTabs();
		for(CatalogCategory cat:categories){
			int titleRes=getTitleForCategory(cat.category);
			TabLayout.Tab tab=categoriesList.newTab().setText(titleRes!=0 ? getString(titleRes) : cat.category).setCustomView(R.layout.item_instance_category);
			ImageView emoji=tab.getCustomView().findViewById(R.id.emoji);
			emoji.setImageResource(getEmojiForCategory(cat.category));
			categoriesList.addTab(tab);
		}
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
		categoriesList.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
			@Override
			public void onTabSelected(TabLayout.Tab tab){
				CatalogCategory category=categories.get(tab.getPosition());
				currentCategory=category.category;
				updateFilteredList();
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab){

			}

			@Override
			public void onTabReselected(TabLayout.Tab tab){

			}
		});
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
		nextButton.setEnabled(chosenInstance!=null);
		view.findViewById(R.id.btn_back).setOnClickListener(v->Nav.finish(this));
		list.setItemAnimator(new BetterItemAnimator());
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorPollVoted, 1, 16, 16, DividerItemDecoration.NOT_FIRST));
		view.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorBackgroundLight));
		buttonBar=view.findViewById(R.id.button_bar);
		setStatusBarColor(UiUtils.getThemeColor(getActivity(), R.attr.colorBackgroundLight));
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
		getActivity().getSystemService(InputMethodManager.class).hideSoftInputFromWindow(contentView.getWindowToken(), 0);
		if(isSignup){
			Bundle args=new Bundle();
			args.putParcelable("instance", Parcels.wrap(instance));
			Nav.go(getActivity(), InstanceRulesFragment.class, args);
		}else{
			AccountSessionManager.getInstance().authenticate(getActivity(), instance);
		}
	}

//	private String getEmojiForCategory(String category){
//		return switch(category){
//			case "all" -> "💬";
//			case "academia" -> "📚";
//			case "activism" -> "✊";
//			case "food" -> "🍕";
//			case "furry" -> "🦁";
//			case "games" -> "🕹";
//			case "general" -> "🐘";
//			case "journalism" -> "📰";
//			case "lgbt" -> "🏳️‍🌈";
//			case "regional" -> "📍";
//			case "art" -> "🎨";
//			case "music" -> "🎼";
//			case "tech" -> "📱";
//			default -> "❓";
//		};
//	}

	private int getEmojiForCategory(String category){
		return switch(category){
			case "all" -> R.drawable.ic_category_all;
			case "academia" -> R.drawable.ic_category_academia;
			case "activism" -> R.drawable.ic_category_activism;
			case "food" -> R.drawable.ic_category_food;
			case "furry" -> R.drawable.ic_category_furry;
			case "games" -> R.drawable.ic_category_games;
			case "general" -> R.drawable.ic_category_general;
			case "journalism" -> R.drawable.ic_category_journalism;
			case "lgbt" -> R.drawable.ic_category_lgbt;
			case "regional" -> R.drawable.ic_category_regional;
			case "art" -> R.drawable.ic_category_art;
			case "music" -> R.drawable.ic_category_music;
			case "tech" -> R.drawable.ic_category_tech;
			default -> R.drawable.ic_category_unknown;
		};
	}

	private int getTitleForCategory(String category){
		return switch(category){
			case "all" -> R.string.category_all;
			case "academia" -> R.string.category_academia;
			case "activism" -> R.string.category_activism;
			case "food" -> R.string.category_food;
			case "furry" -> R.string.category_furry;
			case "games" -> R.string.category_games;
			case "general" -> R.string.category_general;
			case "journalism" -> R.string.category_journalism;
			case "lgbt" -> R.string.category_lgbt;
			case "regional" -> R.string.category_regional;
			case "art" -> R.string.category_art;
			case "music" -> R.string.category_music;
			case "tech" -> R.string.category_tech;
			default -> 0;
		};
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
					if(instance.domain.equals(currentSearchQuery) || !isSignup || !instance.approvalRequired)
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
		if(TextUtils.isEmpty(_domain))
			return;
		String domain;
		try{
			domain=IDN.toASCII(_domain);
		}catch(IllegalArgumentException x){
			return;
		}
		Instance cachedInstance=instancesCache.get(domain);
		if(cachedInstance!=null){
			for(CatalogInstance ci:filteredData){
				if(ci.domain.equals(domain))
					return;
			}
			CatalogInstance ci=cachedInstance.toCatalogInstance();
			filteredData.add(0, ci);
			adapter.notifyItemInserted(0);
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
								if(ci.domain.equals(domain)){
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
							new M3AlertDialogBuilder(getActivity())
									.setTitle(R.string.error)
									.setMessage(getString(R.string.not_a_mastodon_instance, domain)+"\n\n"+((MastodonErrorResponse)error).error)
									.setPositiveButton(R.string.ok, null)
									.show();
						}
					}
				}).execNoAuth(domain);
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
		private final TextView title, description, userCount, lang;
		private final RadioButton radioButton;

		public InstanceViewHolder(){
			super(getActivity(), R.layout.item_instance_catalog, list);
			title=findViewById(R.id.title);
			description=findViewById(R.id.description);
			userCount=findViewById(R.id.user_count);
			lang=findViewById(R.id.lang);
			radioButton=findViewById(R.id.radiobtn);
			if(Build.VERSION.SDK_INT<Build.VERSION_CODES.N){
				UiUtils.fixCompoundDrawableTintOnAndroid6(userCount);
				UiUtils.fixCompoundDrawableTintOnAndroid6(lang);
			}
		}

		@Override
		public void onBind(CatalogInstance item){
			title.setText(item.normalizedDomain);
			description.setText(item.description);
			userCount.setText(UiUtils.abbreviateNumber(item.totalUsers));
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
}
