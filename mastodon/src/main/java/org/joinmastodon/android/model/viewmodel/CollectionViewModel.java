package org.joinmastodon.android.model.viewmodel;

import org.joinmastodon.android.model.AccountOrPartial;
import org.joinmastodon.android.model.DisplayItemsParent;
import org.joinmastodon.android.model.collections.AccountCollection;
import org.joinmastodon.android.model.collections.AccountCollections;
import org.joinmastodon.android.model.collections.CollectionItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class CollectionViewModel implements DisplayItemsParent{
	public AccountCollection collection;
	public List<AccountOrPartial> accounts;
	public AccountOrPartial author;
	public List<ImageLoaderRequest> avatarRequests;

	public CollectionViewModel(){}

	public CollectionViewModel(AccountCollection collection, Map<String, ? extends AccountOrPartial> accounts){
		this.collection=collection;
		this.accounts=new ArrayList<>();
		avatarRequests=new ArrayList<>();
		for(CollectionItem item:collection.items){
			AccountOrPartial acc=accounts.get(item.accountId);
			if(acc!=null){
				this.accounts.add(acc);
				avatarRequests.add(new UrlImageLoaderRequest(acc.getAvatar(), V.dp(50), V.dp(50)));
				if(this.accounts.size()==4)
					break;
			}
		}
		author=accounts.get(collection.accountId);
	}

	public static List<CollectionViewModel> wrap(AccountCollections collections){
		Map<String, AccountOrPartial> accounts=collections.partialAccounts.stream().collect(Collectors.toMap(pa->pa.id, Function.identity(), (a, b)->b));
		ArrayList<CollectionViewModel> models=new ArrayList<>();
		for(AccountCollection c:collections.collections){
			models.add(new CollectionViewModel(c, accounts));
		}
		return models;
	}

	@Override
	public String getID(){
		return collection.id;
	}
}
