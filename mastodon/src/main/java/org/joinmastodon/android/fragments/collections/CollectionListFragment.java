package org.joinmastodon.android.fragments.collections;

import android.os.Bundle;
import android.view.ViewGroup;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.accounts.GetAccountsByIDs;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.MastodonRecyclerFragment;
import org.joinmastodon.android.fragments.profile.ProfileFeaturedFragment;
import org.joinmastodon.android.fragments.profile.ProfileFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountOrPartial;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.collections.AccountCollection;
import org.joinmastodon.android.model.collections.AccountCollections;
import org.joinmastodon.android.model.collections.CollectionItem;
import org.joinmastodon.android.model.viewmodel.CollectionViewModel;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.CollectionViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;

public abstract class CollectionListFragment extends MastodonRecyclerFragment<CollectionViewModel>{
	protected String accountID;
	private Map<String, Account> extraAccountsForCollections=new HashMap<>();
	private Map<String, Relationship> relationships=new HashMap<>();
	private ArrayList<APIRequest<?>> currentRequests=new ArrayList<>();
	protected Account profileAccount;
	protected CollectionsAdapter adapter;

	public CollectionListFragment(){
		super(40);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getAccountIdArg();
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		for(APIRequest<?> req:currentRequests){
			req.cancel();
		}
	}

	@Override
	public RecyclerView.Adapter<CollectionViewHolder> getAdapter(){
		adapter=new CollectionsAdapter();
		return adapter;
	}

	protected abstract MastodonAPIRequest<AccountCollections> getRequest(int offset, int count);

	@Override
	public void doLoadData(int offset, int count){
		if(offset==0){
			for(APIRequest<?> req:currentRequests){
				req.cancel();
			}
		}
		currentRequest=getRequest(offset, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(AccountCollections rawCollections){
						if(getActivity()==null)
							return;
						if(offset==0){
							extraAccountsForCollections.clear();
							if(profileAccount!=null)
								extraAccountsForCollections.put(profileAccount.id, profileAccount);
						}
						List<CollectionViewModel> collections=CollectionViewModel.wrap(rawCollections, (Map<String, AccountOrPartial>)(Object) extraAccountsForCollections);
						HashSet<String> needAccounts=new HashSet<>(), needRelationships=new HashSet<>();
						for(AccountCollection c:rawCollections.collections){
							needAccounts.add(c.accountId);
							needRelationships.add(c.accountId);
							int i=0;
							for(CollectionItem item:c.items){
								needAccounts.add(item.accountId);
								i++;
								if(i==4)
									break;
							}
						}
						needAccounts.removeAll(extraAccountsForCollections.keySet());
						needRelationships.removeAll(relationships.keySet());
						needRelationships.remove(AccountSessionManager.get(accountID).self.id);
						if(!needAccounts.isEmpty())
							loadAccountsForCollections(needAccounts);
						if(!needRelationships.isEmpty())
							loadRelationships(needRelationships);
						onDataLoaded(collections, rawCollections.hasNextPage);
						currentRequest=null;
					}
				})
				.exec(accountID);
	}

	private void loadAccountsForCollections(Set<String> ids){
		GetAccountsByIDs req=new GetAccountsByIDs(ids);
		currentRequests.add(req);
		req.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Account> result){
						currentRequests.remove(req);
						for(Account acc:result)
							extraAccountsForCollections.put(acc.id, acc);
						for(CollectionViewModel cvm:data){
							cvm.updateAccounts((Map<String, AccountOrPartial>)(Object) extraAccountsForCollections);
						}
						rebindAllItems();
						imgLoader.forceUpdateImages();
					}

					@Override
					public void onError(ErrorResponse error){
						currentRequests.remove(req);
					}
				})
				.exec(accountID);
	}

	private void loadRelationships(Set<String> ids){
		GetAccountRelationships req=new GetAccountRelationships(ids);
		currentRequests.add(req);
		req.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Relationship> result){
						currentRequests.remove(req);
						for(Relationship r:result){
							relationships.put(r.id, r);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						currentRequests.remove(req);
					}
				})
				.exec(accountID);
	}

	private void rebindAllItems(){
		for(int i=0;i<list.getChildCount();i++){
			if(list.getChildViewHolder(list.getChildAt(i)) instanceof CollectionViewHolder cvh)
				cvh.rebind();
		}
	}

	private void onBlockCollectionAuthorClick(String id){
		Relationship rel=relationships.get(id);
		if(rel==null)
			return;
		UiUtils.confirmToggleBlockUser(getActivity(), accountID, profileAccount, rel.blocking, newRel->{
			relationships.put(id, newRel);
		});
	}

	protected class CollectionsAdapter extends RecyclerView.Adapter<CollectionViewHolder> implements ImageLoaderRecyclerAdapter{
		@NonNull
		@Override
		public CollectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new CollectionViewHolder(getActivity(), list, accountID, CollectionListFragment.this::onBlockCollectionAuthorClick, new CollectionViewHolder.AccountAndRelationshipProvider(){
				@Override
				public Account getAccount(String id){
					return extraAccountsForCollections.get(id);
				}

				@Override
				public Relationship getRelationship(String id){
					return relationships.get(id);
				}
			});
		}

		@Override
		public void onBindViewHolder(@NonNull CollectionViewHolder holder, int position){
			holder.bind(data.get(position));
		}

		@Override
		public int getItemCount(){
			return data.size();
		}

		@Override
		public int getImageCountForItem(int position){
			return data.get(position).accounts.size();
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return data.get(position).avatarRequests.get(image);
		}
	}
}
