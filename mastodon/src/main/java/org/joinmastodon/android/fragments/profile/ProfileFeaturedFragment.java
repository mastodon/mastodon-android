package org.joinmastodon.android.fragments.profile;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowInsets;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.requests.BatchRequest;
import org.joinmastodon.android.api.requests.accounts.GetAccountEndorsements;
import org.joinmastodon.android.api.requests.accounts.GetAccountsByIDs;
import org.joinmastodon.android.api.requests.collections.GetAccountCollections;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.account_list.BaseAccountListFragment;
import org.joinmastodon.android.fragments.account_list.FeaturedAccountListFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountOrPartial;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.collections.AccountCollection;
import org.joinmastodon.android.model.collections.AccountCollections;
import org.joinmastodon.android.model.collections.CollectionItem;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.model.viewmodel.CollectionViewModel;
import org.joinmastodon.android.ui.displayitems.SectionHeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.CollectionViewHolder;
import org.parceler.Parcels;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.MergeRecyclerAdapter;

public class ProfileFeaturedFragment extends BaseAccountListFragment{
	private Account profileAccount;
	private boolean isSelf;
	private SectionHeaderStatusDisplayItem accountsHeader, collectionsHeader;
	private MergeRecyclerAdapter mergeAdapter;
	private SectionHeaderAdapter accountsHeaderAdapter, collectionsHeaderAdapter;
	private CollectionsAdapter collectionsAdapter;
	private List<CollectionViewModel> collections=List.of();
	private Relationship relationship;
	private Map<String, Account> extraAccountsForCollections=new HashMap<>();
	private APIRequest<?> collectionAccountsRequest;

	public ProfileFeaturedFragment(){
		setRefreshEnabled(false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		profileAccount=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
		isSelf=AccountSessionManager.getInstance().isSelf(accountID, profileAccount);
		setEmptyText(isSelf ? R.string.profile_featured_empty_self : R.string.profile_featured_empty);

		accountsHeader=new SectionHeaderStatusDisplayItem(StatusDisplayItem.NO_OP_CALLBACKS, getActivity(), getString(R.string.profile_endorsed_accounts), getString(R.string.view_all), null);
		collectionsHeader=new SectionHeaderStatusDisplayItem(StatusDisplayItem.NO_OP_CALLBACKS, getActivity(), getString(R.string.profile_collections), getString(R.string.view_all), null);
		extraAccountsForCollections.put(profileAccount.id, profileAccount);
	}

	@Override
	protected void doLoadData(int offset, int count){
		// Batch request because this will also load collections when those are ready
		Map<String, MastodonAPIRequest<?>> requests=new HashMap<>();
		requests.put("accounts", new GetAccountEndorsements(profileAccount.id, 5, null));
		boolean supportsCollections=AccountSessionManager.get(accountID).getInstanceInfo().supportsCollections();
		if(supportsCollections){
			requests.put("collections", new GetAccountCollections(profileAccount.id, 0, 5));
		}
		currentRequest=new BatchRequest(requests)
				.setCallback(new SimpleCallback<>(this){
					@SuppressWarnings("unchecked")
					@Override
					public void onSuccess(Map<String, Object> result){
						if(getActivity()==null)
							return;
						HeaderPaginationList<Account> accounts=Objects.requireNonNull((HeaderPaginationList<Account>) result.get("accounts"));
						accountsHeader.onButtonClick=accounts.nextPageUri==null ? null : ProfileFeaturedFragment.this::showAllEndorsedAccounts;
						accountsHeaderAdapter.setVisible(!accounts.isEmpty());

						if(supportsCollections){
							AccountCollections rawCollections=Objects.requireNonNull((AccountCollections) result.get("collections"));
							if(refreshing){
								extraAccountsForCollections.clear();
								extraAccountsForCollections.put(profileAccount.id, profileAccount);
							}
							collections=CollectionViewModel.wrap(rawCollections, (Map<String, AccountOrPartial>)(Object) extraAccountsForCollections);
							collectionsHeaderAdapter.setVisible(!rawCollections.collections.isEmpty());
							HashSet<String> needAccounts=new HashSet<>();
							for(AccountCollection c:rawCollections.collections){
								needAccounts.add(c.accountId);
								int i=0;
								for(CollectionItem item:c.items){
									needAccounts.add(item.accountId);
									i++;
									if(i==4)
										break;
								}
							}
							needAccounts.removeAll(extraAccountsForCollections.keySet());
							if(!needAccounts.isEmpty())
								loadAccountsForCollections(needAccounts);
						}else{
							collectionsHeaderAdapter.setVisible(false);
						}

						onDataLoaded(accounts.stream().map(a->new AccountViewModel(a, accountID, false, getActivity())).collect(Collectors.toList()), false);
					}
				})
				.exec(accountID);
	}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		mergeAdapter=new MergeRecyclerAdapter();
		accountsHeaderAdapter=new SectionHeaderAdapter(accountsHeader);
		collectionsHeaderAdapter=new SectionHeaderAdapter(collectionsHeader);

		mergeAdapter.addAdapter(accountsHeaderAdapter);
		mergeAdapter.addAdapter(super.getAdapter());
		mergeAdapter.addAdapter(collectionsHeaderAdapter);
		collectionsAdapter=new CollectionsAdapter();
		mergeAdapter.addAdapter(collectionsAdapter);
		return mergeAdapter;
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
			loadData();
	}

	@Override
	protected void initializeEmptyView(View contentView){
		ViewStub emptyStub=contentView.findViewById(R.id.empty);
		emptyStub.setLayoutResource(R.layout.empty_with_elephant);
		super.initializeEmptyView(contentView);
		TextView emptySecondary=contentView.findViewById(R.id.empty_text_secondary);
		if(isSelf){
			emptySecondary.setText(getString(R.string.profile_featured_empty_self_description));
		}else{
			String text=getString(R.string.profile_featured_empty_description, profileAccount.displayName);
			if(GlobalUserPreferences.customEmojiInNames){
				emptySecondary.setText(HtmlParser.parseCustomEmoji(text, profileAccount.emojis));
				UiUtils.loadCustomEmojiInTextView(emptySecondary);
			}else{
				emptySecondary.setText(text);
			}
		}
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		// no-op
	}

	@Override
	protected void refresh(){
		if(collectionAccountsRequest!=null){
			collectionAccountsRequest.cancel();
			collectionAccountsRequest=null;
		}
		super.refresh();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.getItemAnimator().setChangeDuration(0);
	}

	private void showAllEndorsedAccounts(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("targetAccount", Parcels.wrap(profileAccount));
		Nav.go(getActivity(), FeaturedAccountListFragment.class, args);
	}

	private void onBlockCollectionAuthorClick(String id){
		UiUtils.confirmToggleBlockUser(getActivity(), accountID, profileAccount, relationship.blocking, rel->{
			// TODO use the event bus for this
			if(getParentFragment() instanceof ProfileFragment pf)
				pf.updateRelationship(rel);
		});
	}

	public void setRelationship(Relationship relationship){
		this.relationship=relationship;
	}

	private void loadAccountsForCollections(Set<String> ids){
		collectionAccountsRequest=new GetAccountsByIDs(ids)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Account> result){
						collectionAccountsRequest=null;
						for(Account acc:result)
							extraAccountsForCollections.put(acc.id, acc);
						for(CollectionViewModel cvm:collections){
							cvm.updateAccounts((Map<String, AccountOrPartial>)(Object) extraAccountsForCollections);
						}
						collectionsAdapter.notifyItemRangeChanged(0, collections.size());
						imgLoader.forceUpdateImages();
					}

					@Override
					public void onError(ErrorResponse error){
						collectionAccountsRequest=null;
					}
				})
				.exec(accountID);
	}

	private static class SectionHeaderAdapter extends RecyclerView.Adapter<SectionHeaderStatusDisplayItem.Holder>{
		private boolean isVisible;
		private final SectionHeaderStatusDisplayItem item;

		private SectionHeaderAdapter(SectionHeaderStatusDisplayItem item){
			this.item=item;
		}

		@NonNull
		@Override
		public SectionHeaderStatusDisplayItem.Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new SectionHeaderStatusDisplayItem.Holder(parent.getContext(), parent);
		}

		@Override
		public void onBindViewHolder(@NonNull SectionHeaderStatusDisplayItem.Holder holder, int position){
			holder.bind(item);
		}

		@Override
		public int getItemCount(){
			return isVisible ? 1 : 0;
		}

		@Override
		public int getItemViewType(int position){
			return 1;
		}

		public void setVisible(boolean visible){
			if(visible==isVisible)
				return;
			isVisible=visible;
			if(visible){
				notifyItemInserted(0);
			}else{
				notifyItemRemoved(0);
			}
		}
	}

	private class CollectionsAdapter extends RecyclerView.Adapter<CollectionViewHolder> implements ImageLoaderRecyclerAdapter{
		@NonNull
		@Override
		public CollectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new CollectionViewHolder(getActivity(), list, accountID, ProfileFeaturedFragment.this::onBlockCollectionAuthorClick, new CollectionViewHolder.AccountAndRelationshipProvider(){
				@Override
				public Account getAccount(String id){
					return id.equals(profileAccount.id) ? profileAccount : null;
				}

				@Override
				public Relationship getRelationship(String id){
					return id.equals(profileAccount.id) ? relationship : null;
				}
			});
		}

		@Override
		public void onBindViewHolder(@NonNull CollectionViewHolder holder, int position){
			holder.bind(collections.get(position));
		}

		@Override
		public int getItemCount(){
			return collections.size();
		}

		@Override
		public int getItemViewType(int position){
			return 2;
		}

		@Override
		public int getImageCountForItem(int position){
			return collections.get(position).accounts.size();
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return collections.get(position).avatarRequests.get(image);
		}
	}
}
