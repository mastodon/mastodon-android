package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.View;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.GetStatusContext;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusDeletedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.StatusContext;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.Collections;

import me.grishka.appkit.api.SimpleCallback;

public class ThreadFragment extends StatusListFragment{
	private Status mainStatus;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		mainStatus=Parcels.unwrap(getArguments().getParcelable("status"));
		Account inReplyToAccount=Parcels.unwrap(getArguments().getParcelable("inReplyToAccount"));
		if(inReplyToAccount!=null)
			knownAccounts.put(inReplyToAccount.id, inReplyToAccount);
		data.add(mainStatus);
		onAppendItems(Collections.singletonList(mainStatus));
		setTitle(HtmlParser.parseCustomEmoji(getString(R.string.post_from_user, mainStatus.account.displayName), mainStatus.account.emojis));
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetStatusContext(mainStatus.id)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(StatusContext result){
						if(refreshing){
							data.clear();
							displayItems.clear();
							data.add(mainStatus);
							onAppendItems(Collections.singletonList(mainStatus));
						}
						footerProgress.setVisibility(View.GONE);
						data.addAll(result.descendants);
						int prevCount=displayItems.size();
						onAppendItems(result.descendants);
						int count=displayItems.size();
						if(!refreshing)
							adapter.notifyItemRangeInserted(prevCount, count-prevCount);
						prependItems(result.ancestors, !refreshing);
						dataLoaded();
						if(refreshing){
							refreshDone();
							adapter.notifyDataSetChanged();
						}
						list.scrollToPosition(displayItems.size()-count);
					}
				})
				.exec(accountID);
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading){
			dataLoading=true;
			doLoadData();
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		UiUtils.loadCustomEmojiInTextView(toolbarTitleView);
		showContent();
		if(!loaded)
			footerProgress.setVisibility(View.VISIBLE);
	}

	@Override
	@Subscribe
	public void onStatusCountersUpdated(StatusCountersUpdatedEvent ev){
		super.onStatusCountersUpdated(ev);
	}

	@Override
	@Subscribe
	public void onStatusDeleted(StatusDeletedEvent ev){
		super.onStatusDeleted(ev);
	}
}
