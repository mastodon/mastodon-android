package org.joinmastodon.android.fragments;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.lists.AddAccountsToList;
import org.joinmastodon.android.api.requests.lists.GetListAccounts;
import org.joinmastodon.android.api.requests.lists.RemoveAccountsFromList;
import org.joinmastodon.android.events.FinishListCreationFragmentEvent;
import org.joinmastodon.android.fragments.account_list.AddNewListMembersFragment;
import org.joinmastodon.android.fragments.account_list.BaseAccountListFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.FollowList;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;
import org.joinmastodon.android.ui.views.CurlyArrowEmptyView;
import org.parceler.Parcels;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class CreateListAddMembersFragment extends BaseAccountListFragment implements AddNewListMembersFragment.Listener{
	private FollowList followList;
	private Button nextButton;
	private View buttonBar;
	private FragmentRootLinearLayout rootView;
	private FrameLayout searchFragmentContainer;
	private FrameLayout fragmentContentWrap;
	private AddNewListMembersFragment searchFragment;
	private WindowInsets lastInsets;
	private boolean dismissingSearchFragment;
	private HashSet<String> accountIDsInList=new HashSet<>();
	private Runnable searchFragmentDismisser=this::dismissSearchFragment;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.manage_list_members);
		setSubtitle(getString(R.string.step_x_of_y, 2, 2));
		setLayout(R.layout.fragment_login);
		setEmptyText(R.string.list_no_members);
		setHasOptionsMenu(true);

		followList=Parcels.unwrap(getArguments().getParcelable("list"));
		if(savedInstanceState!=null || getArguments().getBoolean("needLoadMembers", false)){
			loadData();
		}else{
			onDataLoaded(List.of());
		}
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetListAccounts(followList.id, null, 0)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(HeaderPaginationList<Account> result){
						for(Account acc:result)
							accountIDsInList.add(acc.id);
						onDataLoaded(result.stream().map(a->new AccountViewModel(a, accountID, getActivity())).collect(Collectors.toList()));
					}
				})
				.exec(accountID);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=super.onCreateView(inflater, container, savedInstanceState);
		FrameLayout wrapper=new FrameLayout(getActivity());
		wrapper.addView(view);
		rootView=(FragmentRootLinearLayout) view;
		fragmentContentWrap=wrapper;
		return wrapper;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		nextButton=view.findViewById(R.id.btn_next);
		nextButton.setOnClickListener(this::onNextClick);
		nextButton.setText(R.string.done);
		buttonBar=view.findViewById(R.id.button_bar);

		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		lastInsets=insets;
		if(searchFragment!=null)
			searchFragment.onApplyWindowInsets(insets);
		insets=UiUtils.applyBottomInsetToFixedView(buttonBar, insets);
		rootView.dispatchApplyWindowInsets(insets);
	}

	@Override
	protected List<View> getViewsForElevationEffect(){
		return List.of(getToolbar(), buttonBar);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		MenuItem item=menu.add(R.string.add_list_member);
		item.setIcon(R.drawable.ic_add_24px);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(searchFragmentContainer!=null)
			return true;

		searchFragmentContainer=new FrameLayout(getActivity());
		searchFragmentContainer.setId(R.id.search_fragment);
		fragmentContentWrap.addView(searchFragmentContainer);

		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("list", Parcels.wrap(followList));
		args.putBoolean("_can_go_back", true);
		searchFragment=new AddNewListMembersFragment(this);
		searchFragment.setArguments(args);
		getChildFragmentManager().beginTransaction().add(R.id.search_fragment, searchFragment).commit();
		getChildFragmentManager().executePendingTransactions();
		if(lastInsets!=null)
			searchFragment.onApplyWindowInsets(lastInsets);
		searchFragmentContainer.setTranslationX(V.dp(100));
		searchFragmentContainer.setAlpha(0f);
		searchFragmentContainer.animate().translationX(0).alpha(1).setDuration(300).withLayer().setInterpolator(CubicBezierInterpolator.DEFAULT).withEndAction(()->{
			rootView.setVisibility(View.GONE);
		}).start();
		addBackCallback(searchFragmentDismisser);
		return true;
	}

	@Override
	protected void initializeEmptyView(View contentView){
		ViewStub emptyStub=contentView.findViewById(R.id.empty);
		emptyStub.setLayoutResource(R.layout.empty_with_arrow);
		super.initializeEmptyView(contentView);
		TextView emptySecondary=contentView.findViewById(R.id.empty_text_secondary);
		emptySecondary.setText(R.string.list_find_users);
		CurlyArrowEmptyView arrowView=(CurlyArrowEmptyView) emptyView;
		arrowView.setGravityAndOffsets(Gravity.TOP | Gravity.END, 24, 2);
	}

	@Override
	protected void setStatusBarColor(int color){
		rootView.setStatusBarColor(color);
	}

	@Override
	protected void setNavigationBarColor(int color){
		rootView.setNavigationBarColor(color);
	}

	private void dismissSearchFragment(){
		if(searchFragment==null || dismissingSearchFragment)
			return;
		removeBackCallback(searchFragmentDismisser);
		dismissingSearchFragment=true;
		rootView.setVisibility(View.VISIBLE);
		searchFragmentContainer.animate().translationX(V.dp(100)).alpha(0).setDuration(200).withLayer().setInterpolator(CubicBezierInterpolator.DEFAULT).withEndAction(()->{
			getChildFragmentManager().beginTransaction().remove(searchFragment).commit();
			getChildFragmentManager().executePendingTransactions();
			fragmentContentWrap.removeView(searchFragmentContainer);
			searchFragmentContainer=null;
			searchFragment=null;
			dismissingSearchFragment=false;
		}).start();
		getActivity().getSystemService(InputMethodManager.class).hideSoftInputFromWindow(contentView.getWindowToken(), 0);
	}

	private void onNextClick(View v){
		E.post(new FinishListCreationFragmentEvent(accountID, followList.id));
		Nav.finish(this);
	}

	@Override
	public boolean isAccountInList(AccountViewModel account){
		return accountIDsInList.contains(account.account.id);
	}

	@Override
	public void addAccountToList(AccountViewModel account, Runnable onDone){
		new AddAccountsToList(followList.id, Set.of(account.account.id))
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Void result){
						accountIDsInList.add(account.account.id);
						if(onDone!=null)
							onDone.run();
						int i=0;
						for(AccountViewModel acc:data){
							if(acc.account.id.equals(account.account.id)){
								list.getAdapter().notifyItemChanged(i);
								return;
							}
							i++;
						}
						int pos=data.size();
						data.add(account);
						list.getAdapter().notifyItemInserted(pos);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.exec(accountID);
	}

	@Override
	public void removeAccountAccountFromList(AccountViewModel account, Runnable onDone){
		new RemoveAccountsFromList(followList.id, Set.of(account.account.id))
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Void result){
						accountIDsInList.remove(account.account.id);
						if(onDone!=null)
							onDone.run();
						int i=0;
						for(AccountViewModel acc:data){
							if(acc.account.id.equals(account.account.id)){
								list.getAdapter().notifyItemChanged(i);
								return;
							}
							i++;
						}
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.exec(accountID);
	}

	@Override
	protected void onConfigureViewHolder(AccountViewHolder holder){
		holder.setStyle(AccountViewHolder.AccessoryType.CUSTOM_BUTTON, false);
		holder.setOnLongClickListener(vh->false);
		Button button=holder.getButton();
		button.setPadding(V.dp(24), 0, V.dp(24), 0);
		button.setMinimumWidth(0);
		button.setMinWidth(0);
		button.setOnClickListener(v->{
			holder.setActionProgressVisible(true);
			holder.itemView.setHasTransientState(true);
			Runnable onDone=()->{
				holder.setActionProgressVisible(false);
				holder.itemView.setHasTransientState(false);
			};
			AccountViewModel account=holder.getItem();
			if(isAccountInList(account)){
				removeAccountAccountFromList(account, onDone);
			}else{
				addAccountToList(account, onDone);
			}
		});
	}

	@Override
	protected void onBindViewHolder(AccountViewHolder holder){
		Button button=holder.getButton();
		int textRes, styleRes;
		if(isAccountInList(holder.getItem())){
			textRes=R.string.remove;
			styleRes=R.style.Widget_Mastodon_M3_Button_Tonal_Error;
		}else{
			textRes=R.string.add;
			styleRes=R.style.Widget_Mastodon_M3_Button_Filled;
		}
		button.setText(textRes);
		TypedArray ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.background});
		button.setBackground(ta.getDrawable(0));
		ta.recycle();
		ta=button.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.textColor});
		button.setTextColor(ta.getColorStateList(0));
		ta.recycle();
	}

	@Override
	protected void loadRelationships(List<AccountViewModel> accounts){
		// no-op
	}
}
