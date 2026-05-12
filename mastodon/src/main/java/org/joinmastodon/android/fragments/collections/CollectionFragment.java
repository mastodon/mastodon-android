package org.joinmastodon.android.fragments.collections;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.collections.GetCollection;
import org.joinmastodon.android.api.requests.collections.RevokeCollectionItem;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.account_list.BaseAccountListFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.collections.AccountCollection;
import org.joinmastodon.android.model.collections.CollectionItem;
import org.joinmastodon.android.model.collections.CollectionWithAccounts;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.model.viewmodel.CollectionViewModel;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.sheets.GenericConfirmationSheet;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;
import org.parceler.Parcels;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;

public class CollectionFragment extends BaseAccountListFragment{
	private String collectionID;
	private MergeRecyclerAdapter mergeAdapter;
	private RecyclerView.Adapter<?> listAdapter;
	private SingleViewRecyclerAdapter sensitiveAdapter;
	private AccountCollection collection;
	private Account collectionAuthor;
	private CollectionItem selfItem;

	private TextView description, accountCount;
	private View selfBanner;
	private TextView selfBannerDescription;
	private Button selfBannerRemoveButton;
	private boolean sensitiveRevealed;

	public CollectionFragment(){
		addTopPadding=false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		collectionID=getArguments().getString("collection");
		setTitle(getArguments().getString("collectionTitle"));
		setSubtitle(getString(R.string.collection_by_author, "@"+getArguments().getString("authorUsername")));
		setHasOptionsMenu(true);
		loadData();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorM3Outline, 0.5f, 0, 0, vh->vh instanceof AccountViewHolder));
	}

	@Override
	protected void doLoadData(int offset, int count){
		new GetCollection(collectionID)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(CollectionWithAccounts result){
						Map<String, Account> accountsByID=result.accounts.stream().collect(Collectors.toMap(a->a.id, Function.identity(), (a, b)->b));
						ArrayList<AccountViewModel> accounts=new ArrayList<>();
						for(CollectionItem item:result.collection.items){
							Account acc=accountsByID.get(item.accountId);
							if(acc!=null){
								accounts.add(new AccountViewModel(acc, accountID, true, getActivity()));
							}
						}
						setTitle(result.collection.name);
						collectionAuthor=accountsByID.get(result.collection.accountId);
						collection=result.collection;
						if(collectionAuthor==null)
							throw new IllegalArgumentException("Collection author is unknown");

						String selfID=AccountSessionManager.get(accountID).self.id;
						selfItem=null;
						for(CollectionItem item:collection.items){
							if(item.accountId.equals(selfID)){
								selfItem=item;
								break;
							}
						}

						setSubtitle(getString(R.string.collection_by_author, "@"+collectionAuthor.acct));
						sensitiveRevealed=false;
						updateHeader();
						onDataLoaded(accounts);
						invalidateOptionsMenu();
					}
				})
				.exec(accountID);
	}

	@Override
	protected void onConfigureViewHolder(AccountViewHolder holder){
		super.onConfigureViewHolder(holder);
		holder.setStyle(AccountViewHolder.AccessoryType.BUTTON, true);
	}

	@Override
	protected AccountViewHolder createViewHolder(){
		return new CollectionAccountViewHolder(this, list, relationships);
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		listAdapter=super.getAdapter();
		mergeAdapter=new MergeRecyclerAdapter();

		View header=LayoutInflater.from(getActivity()).inflate(R.layout.header_collection, list, false);
		description=header.findViewById(R.id.description);
		accountCount=header.findViewById(R.id.account_count);
		selfBanner=header.findViewById(R.id.self_banner);
		selfBannerDescription=header.findViewById(R.id.self_banner_description);
		selfBannerRemoveButton=header.findViewById(R.id.self_banner_remove);
		OutlineProviders.clipToOval(header.findViewById(R.id.self_banner_icon));
		OutlineProviders.clipToRoundRect(selfBanner, 12);
		selfBannerRemoveButton.setOnClickListener(v->confirmAndRemoveSelf());

		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(header));
		mergeAdapter.addAdapter(listAdapter);

		View sensitiveBanner=LayoutInflater.from(getActivity()).inflate(R.layout.header_collection_sensitive, list, false);
		sensitiveBanner.findViewById(R.id.show).setOnClickListener(v->{
			sensitiveRevealed=true;
			list.setAdapter(mergeAdapter);
		});
		sensitiveAdapter=new SingleViewRecyclerAdapter(sensitiveBanner);

		return mergeAdapter;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		if(!loaded)
			return;
		inflater.inflate(R.menu.collection, menu);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P && !UiUtils.isEMUI() && !UiUtils.isMagic()){
			menu.setGroupDividerEnabled(true);
		}
		if(AccountSessionManager.getInstance().isSelf(accountID, collectionAuthor)){
			menu.setGroupVisible(R.id.menu_group2, false);
		}else{
			UiUtils.makeMenuItemRed(getActivity(), menu.findItem(R.id.report));
			UiUtils.makeMenuItemRed(getActivity(), menu.findItem(R.id.block));
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id=item.getItemId();
		if(id==R.id.open_in_browser){
			UiUtils.launchWebBrowser(getActivity(), collection.url);
		}else if(id==R.id.report){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("reportAccount", Parcels.wrap(collectionAuthor));
			//args.putParcelable("relationship", Parcels.wrap(relationship));
			args.putString("collectionID", collection.id);
			Nav.go(getActivity(), ReportReasonChoiceFragment.class, args);
		}else if(id==R.id.block){
			UiUtils.confirmToggleBlockUser(getActivity(), accountID, collectionAuthor, false, rel->Nav.finish(this));
		}else if(id==R.id.share){
			CollectionViewModel cvm=new CollectionViewModel();
			cvm.collection=collection;
			cvm.author=collectionAuthor;
			UiUtils.openSystemShareSheet(getActivity(), cvm);
		}
		return true;
	}

	private void updateHeader(){
		if(TextUtils.isEmpty(collection.description)){
			description.setVisibility(View.GONE);
		}else{
			description.setVisibility(View.VISIBLE);
			description.setText(collection.description);
		}
		accountCount.setText(getResources().getQuantityString(R.plurals.x_accounts, collection.itemCount, collection.itemCount));

		if(selfItem!=null){
			selfBanner.setVisibility(View.VISIBLE);
			String descr=getString(R.string.collection_author_added_you, collectionAuthor.displayName, UiUtils.formatDateLong(selfItem.createdAt.atZone(ZoneId.systemDefault())));
			if(GlobalUserPreferences.customEmojiInNames)
				HtmlParser.setTextWithCustomEmoji(selfBannerDescription, descr, collectionAuthor.emojis);
			else
				selfBannerDescription.setText(descr);
		}else{
			selfBanner.setVisibility(View.GONE);
		}

		if(collection.sensitive && !sensitiveRevealed){
			list.setAdapter(sensitiveAdapter);
		}else if(list.getAdapter()!=mergeAdapter){
			list.setAdapter(mergeAdapter);
		}
	}

	private void confirmAndRemoveSelf(){
		if(selfItem==null)
			return;
		GenericConfirmationSheet sheet=new GenericConfirmationSheet(getActivity());
		sheet.setTitleText(getString(R.string.confirm_remove_self_from_collection_title, collection.name))
				.setContentText(R.string.confirm_remove_self_from_collection)
				.setPrimaryButton(R.string.remove_self_from_collection, ()->{
					sheet.setPrimaryButtonProgressVisible(true);
					new RevokeCollectionItem(collectionID, selfItem.id)
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(Void result){
									if(getActivity()==null)
										return;

									sheet.dismiss();
									removeOwnAccountFromList();
								}

								@Override
								public void onError(ErrorResponse error){
									if(getActivity()==null)
										return;
									error.showToast(getActivity());
									sheet.setPrimaryButtonProgressVisible(false);
								}
							})
							.exec(accountID);
				});
		sheet.show();
	}

	private void removeOwnAccountFromList(){
		String selfID=AccountSessionManager.get(accountID).self.id;
		for(int i=0;i<data.size();i++){
			if(selfID.equals(data.get(i).account.id)){
				data.remove(i);
				listAdapter.notifyItemRemoved(i);
				collection.itemCount--;
				collection.items.remove(selfItem);
				selfItem=null;
				updateHeader();
				break;
			}
		}
	}

	private class CollectionAccountViewHolder extends AccountViewHolder{
		private final TextView postCount, lastActivity;

		public CollectionAccountViewHolder(Fragment fragment, ViewGroup list, Map<String, Relationship> relationships){
			super(fragment, list, relationships, R.layout.item_account_list_collection);
			postCount=findViewById(R.id.posts_count);
			lastActivity=findViewById(R.id.last_activity);
		}

		@Override
		public void onBind(AccountViewModel item){
			super.onBind(item);
			postCount.setText(UiUtils.abbreviateNumber(item.account.statusesCount));
			lastActivity.setText(item.account.lastStatusAt==null ? "" : UiUtils.formatDateDay(getActivity(), item.account.lastStatusAt.atStartOfDay(ZoneId.systemDefault())));
		}

		@Override
		protected void bindFollowerCount(){
			followers.setText(UiUtils.abbreviateNumber(item.account.followersCount));
		}

		@Override
		protected void bindVerifiedLink(){
			if(item.verifiedLink!=null){
				verifiedLink.setVisibility(View.VISIBLE);
				verifiedLink.setText(item.verifiedLink);
			}else{
				verifiedLink.setVisibility(View.GONE);
			}
		}
	}
}
