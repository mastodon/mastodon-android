package org.joinmastodon.android.fragments.onboarding;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.views.UsableRecyclerView;

public class InstanceCatalogSignupFragment extends InstanceCatalogFragment{
	private View headerView;
	private MastodonAPIRequest<?> getCategoriesRequest;
	private TabLayout categoriesList;
	private String currentCategory="all";
	private List<CatalogCategory> categories=new ArrayList<>();


	public InstanceCatalogSignupFragment(){
		super(R.layout.fragment_onboarding_common, 10);
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
						if(getActivity()==null)
							return;
						onDataLoaded(sortInstances(result), false);
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
		view.findViewById(R.id.btn_back).setOnClickListener(v->Nav.finish(this));
		list.setItemAnimator(new BetterItemAnimator());
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorPollVoted, 1, 16, 16, DividerItemDecoration.NOT_FIRST));
		view.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorBackgroundLight));
		setStatusBarColor(UiUtils.getThemeColor(getActivity(), R.attr.colorBackgroundLight));
	}

	@Override
	protected void proceedWithAuthOrSignup(Instance instance){
		getActivity().getSystemService(InputMethodManager.class).hideSoftInputFromWindow(contentView.getWindowToken(), 0);
		if(isSignup){
			if(!instance.registrations){
				new M3AlertDialogBuilder(getActivity())
						.setTitle(R.string.error)
						.setMessage(R.string.instance_signup_closed)
						.setPositiveButton(R.string.ok, null)
						.show();
				return;
			}
			Bundle args=new Bundle();
			args.putParcelable("instance", Parcels.wrap(instance));
			Nav.go(getActivity(), InstanceRulesFragment.class, args);
		}else{
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

	@Override
	protected void updateFilteredList(){
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


	private class InstancesAdapter extends UsableRecyclerView.Adapter<InstanceCatalogSignupFragment.InstanceViewHolder>{
		public InstancesAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public InstanceCatalogSignupFragment.InstanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new InstanceCatalogSignupFragment.InstanceViewHolder();
		}

		@Override
		public void onBindViewHolder(InstanceCatalogSignupFragment.InstanceViewHolder holder, int position){
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
					if(holder instanceof InstanceCatalogSignupFragment.InstanceViewHolder ivh){
						ivh.radioButton.setChecked(false);
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
