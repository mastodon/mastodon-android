package org.joinmastodon.android.fragments;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.ui.displayitems.HashtagStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import androidx.recyclerview.widget.RecyclerView;

public class FeaturedHashtagsListFragment extends BaseStatusListFragment<Hashtag>{
	private Account account;
	private String accountID;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		account=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
		onDataLoaded(getArguments().getParcelableArrayList("hashtags").stream().map(p->(Hashtag)Parcels.unwrap(p)).collect(Collectors.toList()), false);
		setTitle(R.string.hashtags);
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Hashtag s){
		return Collections.singletonList(new HashtagStatusDisplayItem(s.name, this, s));
	}

	@Override
	protected void addAccountToKnown(Hashtag s){

	}

	@Override
	public void onItemClick(String id){
		UiUtils.openHashtagTimeline(getActivity(), accountID, Objects.requireNonNull(findItemOfType(id, HashtagStatusDisplayItem.class)).tag);
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected void drawDivider(View child, View bottomSibling, RecyclerView.ViewHolder holder, RecyclerView.ViewHolder siblingHolder, RecyclerView parent, Canvas c, Paint paint){
		// no-op
	}
}
