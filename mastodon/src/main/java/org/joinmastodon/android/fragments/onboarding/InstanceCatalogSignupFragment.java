package org.joinmastodon.android.fragments.onboarding;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.requests.accounts.CheckInviteLink;
import org.joinmastodon.android.api.requests.catalog.GetCatalogCategories;
import org.joinmastodon.android.api.requests.catalog.GetCatalogInstances;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.catalog.CatalogCategory;
import org.joinmastodon.android.model.catalog.CatalogInstance;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.FilterChipView;
import org.joinmastodon.android.utils.ElevationOnScrollListener;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class InstanceCatalogSignupFragment extends InstanceCatalogFragment implements OnBackPressedListener{
	private MastodonAPIRequest<?> getCategoriesRequest;
	private String currentCategory="all";
	private List<CatalogCategory> categories=new ArrayList<>();
	private View topBar;

	private List<String> languages=Collections.emptyList();
	private PopupMenu langFilterMenu, speedFilterMenu;
	private SignupSpeedFilter currentSignupSpeedFilter=SignupSpeedFilter.ANY;
	private String currentLanguage=null;
	private boolean searchQueryMode;
	private LinearLayout filtersWrap;
	private HorizontalScrollView filtersScroll;
	private ImageButton backBtn, clearSearchBtn;
	private View focusThing;

	private FilterChipView categoryGeneral, categorySpecialInterests;
	private List<FilterChipView> regionalFilters;
	private CatalogInstance.Region chosenRegion;
	private CategoryChoice categoryChoice=CategoryChoice.GENERAL;

	private String inviteCode, inviteCodeHost;
	private AlertDialog currentInviteLinkAlert;

	public InstanceCatalogSignupFragment(){
		super(R.layout.fragment_onboarding_common, 10);
	}

	@Override
	public void onAttach(Context context){
		super.onAttach(context);
		setRefreshEnabled(false);
		setRetainInstance(true);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
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

						if(langFilterMenu!=null){
							Menu menu=langFilterMenu.getMenu();
							menu.clear();
							menu.add(0, 0, 0, R.string.server_filter_any_language);
							languages=result.stream().map(i->i.language).distinct().filter(s->s.length()>0).sorted().collect(Collectors.toList());
							int i=1;
							for(String lang:languages){
								Locale locale=Locale.forLanguageTag(lang);
								String name=locale.getDisplayLanguage(locale);
								if(name.equals(lang))
									name=lang.toUpperCase();
								else
									name=name.substring(0, 1).toUpperCase()+name.substring(1);
								menu.add(0, i, 0, name);
								i++;
							}
						}

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
//		categoriesList.removeAllTabs();
//		for(CatalogCategory cat:categories){
//			int titleRes=getTitleForCategory(cat.category);
//			TabLayout.Tab tab=categoriesList.newTab().setText(titleRes!=0 ? getString(titleRes) : cat.category).setCustomView(R.layout.item_instance_category);
//			ImageView emoji=tab.getCustomView().findViewById(R.id.emoji);
//			emoji.setImageResource(getEmojiForCategory(cat.category));
//			categoriesList.addTab(tab);
//		}
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		if(getCategoriesRequest!=null)
			getCategoriesRequest.cancel();
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		View headerView=new View(getActivity());
		headerView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

		mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(headerView));
		mergeAdapter.addAdapter(adapter=new InstancesAdapter());
		return mergeAdapter;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		backBtn=view.findViewById(R.id.btn_back);
		backBtn.setOnClickListener(v->{
			if(searchQueryMode){
				setSearchQueryMode(false);
			}else{
				Nav.finish(this);
			}
		});
		clearSearchBtn=view.findViewById(R.id.clear);
		clearSearchBtn.setOnClickListener(v->searchEdit.setText(""));
		nextButton.setEnabled(true);
		list.setItemAnimator(new BetterItemAnimator());
		setStatusBarColor(0);
		topBar=view.findViewById(R.id.top_bar);

		list.addOnScrollListener(new ElevationOnScrollListener(null, topBar, buttonBar));

		searchEdit=view.findViewById(R.id.search_edit);
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
				if((clearSearchBtn.getVisibility()==View.VISIBLE)!=(s.length()>0)){
					clearSearchBtn.setVisibility(s.length()>0 ? View.VISIBLE : View.GONE);
				}
			}
		});
		searchEdit.setOnFocusChangeListener((v, hasFocus)->{
			if(hasFocus && !searchQueryMode){
				setSearchQueryMode(true);
			}
		});

		FilterChipView langFilter=new FilterChipView(getActivity());
		langFilter.setDrawableEnd(R.drawable.ic_baseline_arrow_drop_down_18);
		if(currentLanguage==null){
			langFilter.setText(R.string.server_filter_any_language);
		}else{
			Locale locale=Locale.forLanguageTag(currentLanguage);
			langFilter.setText(locale.getDisplayLanguage(locale));
			langFilter.setSelected(true);
		}
		langFilterMenu=new PopupMenu(getContext(), langFilter);
		langFilter.setOnTouchListener(langFilterMenu.getDragToOpenListener());
		langFilter.setOnClickListener(v->langFilterMenu.show());
		filtersWrap=view.findViewById(R.id.filters_container);
		filtersScroll=view.findViewById(R.id.filters_scroll);
		filtersWrap.addView(langFilter, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		FilterChipView speedFilter=new FilterChipView(getActivity());
		speedFilter.setDrawableEnd(R.drawable.ic_baseline_arrow_drop_down_18);
		speedFilterMenu=new PopupMenu(getContext(), speedFilter);
		speedFilterMenu.getMenu().add(0, 0, 0, R.string.server_filter_any_signup_speed);
		speedFilterMenu.getMenu().add(0, 1, 0, R.string.server_filter_instant_signup);
		speedFilterMenu.getMenu().add(0, 2, 0, R.string.server_filter_manual_review);
		speedFilter.setOnTouchListener(speedFilterMenu.getDragToOpenListener());
		speedFilter.setOnClickListener(v->speedFilterMenu.show());
		speedFilter.setText(switch(currentSignupSpeedFilter){
			case ANY -> R.string.server_filter_any_signup_speed;
			case INSTANT -> R.string.server_filter_instant_signup;
			case REVIEWED -> R.string.server_filter_manual_review;
		});
		speedFilter.setSelected(currentSignupSpeedFilter!=SignupSpeedFilter.ANY);
		filtersWrap.addView(speedFilter, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		speedFilterMenu.setOnMenuItemClickListener(item->{
			speedFilter.setText(item.getTitle());
			speedFilter.setSelected(item.getItemId()>0);
			currentSignupSpeedFilter=SignupSpeedFilter.values()[item.getItemId()];
			updateFilteredList();
			return true;
		});
		langFilterMenu.setOnMenuItemClickListener(item->{
			langFilter.setText(item.getTitle());
			langFilter.setSelected(item.getItemId()>0);
			currentLanguage=item.getItemId()==0 ? null : languages.get(item.getItemId()-1);
			updateFilteredList();
			return true;
		});

		View divider=new View(getActivity());
		divider.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Outline));
		filtersWrap.addView(divider, new LinearLayout.LayoutParams(V.dp(.5f), ViewGroup.LayoutParams.MATCH_PARENT));

		categoryGeneral=new FilterChipView(getActivity());
		categoryGeneral.setText(R.string.category_general);
		categoryGeneral.setTag(CategoryChoice.GENERAL);
		categoryGeneral.setOnClickListener(this::onCategoryFilterClick);
		categoryGeneral.setSelected(categoryChoice==CategoryChoice.GENERAL);
		filtersWrap.addView(categoryGeneral, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		categorySpecialInterests=new FilterChipView(getActivity());
		categorySpecialInterests.setText(R.string.category_special_interests);
		categorySpecialInterests.setTag(CategoryChoice.SPECIAL);
		categorySpecialInterests.setOnClickListener(this::onCategoryFilterClick);
		categorySpecialInterests.setSelected(categoryChoice==CategoryChoice.SPECIAL);
		filtersWrap.addView(categorySpecialInterests, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		regionalFilters=Arrays.stream(CatalogInstance.Region.values()).map(r->{
			FilterChipView fv=new FilterChipView(getActivity());
			fv.setTag(r);
			fv.setText(switch(r){
				case EUROPE -> R.string.server_filter_region_europe;
				case NORTH_AMERICA -> R.string.server_filter_region_north_america;
				case SOUTH_AMERICA -> R.string.server_filter_region_south_america;
				case AFRICA -> R.string.server_filter_region_africa;
				case ASIA -> R.string.server_filter_region_asia;
				case OCEANIA -> R.string.server_filter_region_oceania;
			});
			fv.setSelected(r==chosenRegion);
			fv.setOnClickListener(this::onRegionFilterClick);
			filtersWrap.addView(fv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			return fv;
		}).collect(Collectors.toList());
		focusThing=view.findViewById(R.id.focus_thing);
		focusThing.requestFocus();

		view.findViewById(R.id.btn_use_invite).setOnClickListener(this::onUseInviteClick);
		nextButton.setEnabled(chosenInstance!=null);
	}

	private void onRegionFilterClick(View v){
		CatalogInstance.Region r=(CatalogInstance.Region) v.getTag();
		if(chosenRegion==r){
			chosenRegion=null;
			v.setSelected(false);
		}else{
			if(chosenRegion!=null)
				filtersWrap.findViewWithTag(chosenRegion).setSelected(false);
			chosenRegion=r;
			v.setSelected(true);
		}
		updateFilteredList();
	}

	private void onCategoryFilterClick(View v){
		CategoryChoice c=(CategoryChoice) v.getTag();
		if(categoryChoice==c){
			categoryChoice=null;
			v.setSelected(false);
		}else{
			if(categoryChoice!=null)
				filtersWrap.findViewWithTag(categoryChoice).setSelected(false);
			categoryChoice=c;
			v.setSelected(true);
		}
		updateFilteredList();
	}

	@Override
	protected void proceedWithAuthOrSignup(Instance instance){
		if(currentInviteLinkAlert!=null){
			currentInviteLinkAlert.dismiss();
		}else if(!TextUtils.isEmpty(currentSearchQuery) && HtmlParser.INVITE_LINK_PATTERN.matcher(currentSearchQueryButWithCasePreserved).find()){
			if(TextUtils.isEmpty(inviteCode) || !Objects.equals(instance.uri, inviteCodeHost)){
				Uri inviteLink=Uri.parse(currentSearchQueryButWithCasePreserved);
				new CheckInviteLink(inviteLink.getPath())
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(CheckInviteLink.Response result){
								inviteCodeHost=inviteLink.getHost();
								inviteCode=result.inviteCode;
								proceedWithAuthOrSignup(instance);
							}

							@Override
							public void onError(ErrorResponse error){
								if(getActivity()==null)
									return;
								if(error instanceof MastodonErrorResponse mer){
									switch(mer.httpStatus){
										case 401 -> new M3AlertDialogBuilder(getActivity())
												.setTitle(R.string.expired_invite_link)
												.setMessage(getString(R.string.expired_clipboard_invite_link_alert, inviteLink.getHost(), getArguments().getString("defaultServer")))
												.setPositiveButton(R.string.ok, null)
												.show();
										case 404 -> new M3AlertDialogBuilder(getActivity())
												.setTitle(R.string.invalid_invite_link)
												.setMessage(getString(R.string.invalid_clipboard_invite_link_alert, inviteLink.getHost(), getArguments().getString("defaultServer")))
												.setPositiveButton(R.string.ok, null)
												.show();
										default -> error.showToast(getActivity());
									}
								}
							}
						})
						.wrapProgress(getActivity(), R.string.loading_instance, true)
						.execNoAuth(inviteLink.getHost());
				return;
			}
		}
		getActivity().getSystemService(InputMethodManager.class).hideSoftInputFromWindow(contentView.getWindowToken(), 0);
		if(!instance.registrations && (TextUtils.isEmpty(inviteCode) || !Objects.equals(instance.uri, inviteCodeHost))){
			if(instance.invitesEnabled){
				showInviteLinkAlert(instance.uri);
			}else{
				new M3AlertDialogBuilder(getActivity())
						.setTitle(R.string.error)
						.setMessage(R.string.instance_signup_closed)
						.setPositiveButton(R.string.ok, null)
						.show();
			}
			return;
		}
		Bundle args=new Bundle();
		args.putParcelable("instance", Parcels.wrap(instance));
		if(!TextUtils.isEmpty(inviteCode) && Objects.equals(instance.uri, inviteCodeHost))
			args.putString("inviteCode", inviteCode);
		Nav.go(getActivity(), InstanceRulesFragment.class, args);
	}

	private void onUseInviteClick(View v){
		showInviteLinkAlert(null);
	}

	private void showInviteLinkAlert(String domain){
		AlertDialog alert=new M3AlertDialogBuilder(getActivity())
				.setView(R.layout.alert_invite_link)
				.setPositiveButton(R.string.next, null)
				.setNegativeButton(R.string.cancel, null)
				.create();

		Button next=alert.getButton(AlertDialog.BUTTON_POSITIVE);
		EditText edit=alert.findViewById(R.id.edit);
		TextView supportingText=alert.findViewById(R.id.supporting_text);
		TextView label=alert.findViewById(R.id.label);
		TextView subtitle=alert.findViewById(R.id.subtitle);
		ImageButton clear=alert.findViewById(R.id.clear);
		clear.setVisibility(View.GONE);

		if(TextUtils.isEmpty(domain)){
			subtitle.setVisibility(View.GONE);
		}else{
			subtitle.setText(getString(R.string.need_invite_to_join_server, domain));
		}

		Consumer<String> errorSetter=err->{
			supportingText.setText(err);
			int errorColor=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Error);
			supportingText.setTextColor(errorColor);
			label.setTextColor(errorColor);
			edit.setBackgroundResource(R.drawable.bg_m3_filled_text_field_error);
		};

		next.setOnClickListener(_v->{
			Uri inviteLink=Uri.parse(edit.getText().toString());
			if(TextUtils.isEmpty(inviteLink.getHost()) || TextUtils.isEmpty(inviteLink.getPath())){
				errorSetter.accept(getString(R.string.this_invite_is_invalid));
				return;
			}
			UiUtils.showProgressForAlertButton(next, true);
			new CheckInviteLink(inviteLink.getPath())
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(CheckInviteLink.Response result){
							if(getActivity()==null || !alert.isShowing())
								return;

							String host=inviteLink.getHost();
							inviteCode=result.inviteCode;
							inviteCodeHost=host;

							Instance instance=instancesCache.get(normalizeInstanceDomain(host));
							if(instance==null){
								loadInstanceInfo(host, false, err->{
									String errorStr;
									if(err instanceof String str){
										errorStr=str;
									}else if(err instanceof Throwable x){
										errorStr=x.getMessage();
									}else if(err instanceof MastodonErrorResponse mer){
										errorStr=mer.error;
									}else{
										errorStr=getString(R.string.error);
									}
									errorSetter.accept(errorStr);
									UiUtils.showProgressForAlertButton(next, false);
								});
							}else{
								proceedWithAuthOrSignup(instance);
							}
						}

						@Override
						public void onError(ErrorResponse error){
							if(getActivity()==null || !alert.isShowing())
								return;
							UiUtils.showProgressForAlertButton(next, false);
							if(error instanceof MastodonErrorResponse mer){
								errorSetter.accept(switch(mer.httpStatus){
									case 404 -> getString(R.string.this_invite_is_invalid);
									case 401 -> getString(R.string.this_invite_has_expired);
									default -> mer.error;
								});
							}
						}
					})
					.execNoAuth(inviteLink.getHost());
		});
		next.setEnabled(false);
		edit.addTextChangedListener(new SimpleTextWatcher(e->{
			boolean wasEmpty=!next.isEnabled();
			next.setEnabled(e.length()>0);
			if(supportingText.length()>0){
				supportingText.setText("");
				int regularColor=UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant);
				supportingText.setTextColor(regularColor);
				label.setTextColor(regularColor);
				edit.setBackgroundResource(R.drawable.bg_m3_filled_text_field);
			}
			if(wasEmpty!=(e.length()==0)){
				int padEnd;
				if(e.length()==0){
					clear.setVisibility(View.GONE);
					padEnd=V.dp(16);
				}else{
					clear.setVisibility(View.VISIBLE);
					padEnd=V.dp(48);
				}
				edit.setPaddingRelative(edit.getPaddingStart(), edit.getPaddingTop(), padEnd, edit.getPaddingBottom());
			}
		}));
		clear.setOnClickListener(_v->edit.setText(""));

		ClipData clipData=getActivity().getSystemService(ClipboardManager.class).getPrimaryClip();
		if(clipData!=null && clipData.getItemCount()>0){
			CharSequence clipText=clipData.getItemAt(0).coerceToText(getActivity());
			if(HtmlParser.INVITE_LINK_PATTERN.matcher(clipText).find()){
				edit.setText(clipText);
				supportingText.setText(R.string.invite_link_pasted);
			}
		}

		currentInviteLinkAlert=alert;
		alert.setOnDismissListener(dialog->currentInviteLinkAlert=null);
		alert.show();
	}

	@Override
	protected void updateFilteredList(){
		ArrayList<CatalogInstance> prevData=new ArrayList<>(filteredData);
		filteredData.clear();
		if(searchQueryMode){
			if(!TextUtils.isEmpty(currentSearchQuery)){
				String actualQuery;
				if(currentSearchQuery.startsWith("https:")){
					actualQuery=Uri.parse(currentSearchQuery).getHost();
				}else{
					actualQuery=currentSearchQuery;
				}
				for(CatalogInstance instance:data){
					if(instance.domain.contains(actualQuery)){
						filteredData.add(instance);
					}
				}
			}
		}else{
			for(CatalogInstance instance:data){
				if(categoryChoice==null || categoryChoice.matches(instance.category)){
					if(chosenRegion==null || instance.region==chosenRegion){
						boolean signupSpeedMatches=switch(currentSignupSpeedFilter){
							case ANY -> true;
							case INSTANT -> !instance.approvalRequired;
							case REVIEWED -> instance.approvalRequired;
						};
						if(signupSpeedMatches){
							if(currentLanguage==null || instance.languages.contains(currentLanguage)){
								filteredData.add(instance);
							}
						}
					}
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

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		topBar.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
		super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
	}

	@Override
	public boolean onBackPressed(){
		if(searchQueryMode){
			setSearchQueryMode(false);
			return true;
		}
		return false;
	}

	private void setSearchQueryMode(boolean enabled){
		searchQueryMode=enabled;
		RelativeLayout.LayoutParams lp=(RelativeLayout.LayoutParams) searchEdit.getLayoutParams();
		if(searchQueryMode){
			filtersScroll.setVisibility(View.GONE);
			lp.removeRule(RelativeLayout.END_OF);
			backBtn.setScaleX(0.83333333f);
			backBtn.setScaleY(0.83333333f);
			backBtn.setTranslationX(V.dp(8));
			searchEdit.setCompoundDrawableTintList(ColorStateList.valueOf(0));
		}else{
			filtersScroll.setVisibility(View.VISIBLE);
			focusThing.requestFocus();
			searchEdit.setText("");
			lp.addRule(RelativeLayout.END_OF, R.id.btn_back);
			getActivity().getSystemService(InputMethodManager.class).hideSoftInputFromWindow(searchEdit.getWindowToken(), 0);
			backBtn.setScaleX(1);
			backBtn.setScaleY(1);
			backBtn.setTranslationX(0);
			searchEdit.setCompoundDrawableTintList(ColorStateList.valueOf(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant)));
		}
		updateFilteredList();
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!searchQueryMode){
			// Prevent search view automatically getting focused when the user returns to this fragment
			focusThing.requestFocus();
		}
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

	private class InstanceViewHolder extends BindableViewHolder<CatalogInstance> implements UsableRecyclerView.DisableableClickable{
		private final TextView title, description;
		private final RadioButton radioButton;
		private boolean enabled;

		public InstanceViewHolder(){
			super(getActivity(), R.layout.item_instance_catalog, list);
			title=findViewById(R.id.title);
			description=findViewById(R.id.description);
			radioButton=findViewById(R.id.radiobtn);
		}

		@Override
		public void onBind(CatalogInstance item){
			title.setText(item.normalizedDomain);
			radioButton.setChecked(chosenInstance==item);
			Instance realInstance=instancesCache.get(item.normalizedDomain);
			float alpha;
			if(realInstance!=null && !realInstance.registrations){
				alpha=0.38f;
				description.setText(R.string.not_accepting_new_members);
				enabled=false;
			}else{
				alpha=1f;
				description.setText(item.description);
				enabled=true;
			}
			title.setAlpha(alpha);
			description.setAlpha(alpha);
			radioButton.setAlpha(alpha);
		}

		@Override
		public void onClick(){
			if(chosenInstance==item)
				return;
			if(chosenInstance!=null){
				int idx=filteredData.indexOf(chosenInstance);
				if(idx!=-1){
					boolean found=false;
					for(int i=0;i<list.getChildCount();i++){
						RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
						if(holder.getAbsoluteAdapterPosition()==mergeAdapter.getPositionForAdapter(adapter)+idx && holder instanceof InstanceViewHolder ivh){
							ivh.radioButton.setChecked(false);
							found=true;
							break;
						}
					}
					if(!found)
						adapter.notifyItemChanged(idx);
				}
			}
			if(!nextButton.isEnabled()){
				nextButton.setEnabled(true);
			}
			radioButton.setChecked(true);
			if(chosenInstance==null)
				nextButton.setEnabled(true);
			chosenInstance=item;
			loadInstanceInfo(chosenInstance.domain, false);
		}

		@Override
		public boolean isEnabled(){
			return enabled;
		}
	}

	private enum SignupSpeedFilter{
		ANY,
		INSTANT,
		REVIEWED
	}

	private enum CategoryChoice{
		GENERAL,
		SPECIAL;

		public boolean matches(String category){
			boolean isGeneral=(category==null || "general".equals(category));
			return (this==GENERAL)==isGeneral;
		}
	}

}
