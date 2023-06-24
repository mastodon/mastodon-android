package org.joinmastodon.android.fragments.account_list;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Toolbar;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.fragments.MastodonRecyclerFragment;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public abstract class BaseAccountListFragment extends MastodonRecyclerFragment<AccountViewModel>{
	protected HashMap<String, Relationship> relationships=new HashMap<>();
	protected String accountID;
	protected ArrayList<APIRequest<?>> relationshipsRequests=new ArrayList<>();

	public BaseAccountListFragment(){
		super(40);
	}

	public BaseAccountListFragment(int layout, int perPage){
		super(layout, perPage);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
	}

	@Override
	protected void onDataLoaded(List<AccountViewModel> d, boolean more){
		if(refreshing){
			relationships.clear();
		}
		loadRelationships(d);
		super.onDataLoaded(d, more);
	}

	@Override
	public void onRefresh(){
		for(APIRequest<?> req:relationshipsRequests){
			req.cancel();
		}
		relationshipsRequests.clear();
		super.onRefresh();
	}

	protected void loadRelationships(List<AccountViewModel> accounts){
		Set<String> ids=accounts.stream().map(ai->ai.account.id).collect(Collectors.toSet());
		GetAccountRelationships req=new GetAccountRelationships(ids);
		relationshipsRequests.add(req);
		req.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Relationship> result){
						relationshipsRequests.remove(req);
						for(Relationship rel:result){
							relationships.put(rel.id, rel);
						}
						if(list==null)
							return;
						for(int i=0;i<list.getChildCount();i++){
							if(list.getChildViewHolder(list.getChildAt(i)) instanceof AccountViewHolder avh){
								avh.bindRelationship();
							}
						}
					}

					@Override
					public void onError(ErrorResponse error){
						relationshipsRequests.remove(req);
					}
				})
				.exec(accountID);
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		return new AccountsAdapter();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.setClipToPadding(false);
		updateToolbar();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbar();
	}

	@CallSuper
	protected void updateToolbar(){
		Toolbar toolbar=getToolbar();
		if(toolbar!=null && toolbar.getNavigationIcon()!=null){
			toolbar.setNavigationContentDescription(R.string.back);
			if(hasSubtitle()){
				toolbar.setTitleTextAppearance(getActivity(), R.style.m3_title_medium);
				toolbar.setSubtitleTextAppearance(getActivity(), R.style.m3_body_medium);
				int color=UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary);
				toolbar.setTitleTextColor(color);
				toolbar.setSubtitleTextColor(color);
			}
		}
	}

	protected boolean hasSubtitle(){
		return true;
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
			list.setPadding(0, V.dp(16), 0, V.dp(16)+insets.getSystemWindowInsetBottom());
			emptyView.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
			progress.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
			insets=insets.inset(0, 0, 0, insets.getSystemWindowInsetBottom());
		}else{
			list.setPadding(0, V.dp(16), 0, V.dp(16));
		}
		super.onApplyWindowInsets(insets);
	}

	protected void onConfigureViewHolder(AccountViewHolder holder){}

	protected class AccountsAdapter extends UsableRecyclerView.Adapter<AccountViewHolder> implements ImageLoaderRecyclerAdapter{
		public AccountsAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			AccountViewHolder holder=new AccountViewHolder(BaseAccountListFragment.this, parent, relationships);
			onConfigureViewHolder(holder);
			return holder;
		}

		@Override
		public void onBindViewHolder(AccountViewHolder holder, int position){
			holder.bind(data.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			return data.size();
		}

		@Override
		public int getImageCountForItem(int position){
			return data.get(position).emojiHelper.getImageCount()+1;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			AccountViewModel item=data.get(position);
			return image==0 ? item.avaRequest : item.emojiHelper.getImageRequest(image-1);
		}
	}
}
