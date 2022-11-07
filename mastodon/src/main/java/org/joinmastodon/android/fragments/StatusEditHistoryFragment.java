package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.GetStatusEditHistory;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.ReblogOrReplyLineStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.utils.InsetStatusItemDecoration;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import me.grishka.appkit.api.SimpleCallback;

public class StatusEditHistoryFragment extends StatusListFragment{
	private String id;


	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		id=getArguments().getString("id");
		loadData();
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle(R.string.edit_history);
	}

	@Override
	protected void doLoadData(int offset, int count){
		new GetStatusEditHistory(id)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						Collections.sort(result, Comparator.comparing((Status s)->s.createdAt).reversed());
						onDataLoaded(result, false);
					}
				})
				.exec(accountID);
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Status s){
		List<StatusDisplayItem> items=StatusDisplayItem.buildItems(this, s, accountID, s, knownAccounts, true, false);
		int idx=data.indexOf(s);
		if(idx>=0){
			String date=UiUtils.DATE_TIME_FORMATTER.format(s.createdAt.atZone(ZoneId.systemDefault()));
			String action="";
			if(idx==data.size()-1){
				action=getString(R.string.edit_original_post);
			}else{
				enum StatusEditChangeType{
					TEXT_CHANGED,
					SPOILER_ADDED,
					SPOILER_REMOVED,
					SPOILER_CHANGED,
					POLL_ADDED,
					POLL_REMOVED,
					POLL_CHANGED,
					MEDIA_ADDED,
					MEDIA_REMOVED,
					MEDIA_REORDERED,
					MARKED_SENSITIVE,
					MARKED_NOT_SENSITIVE
				}
				EnumSet<StatusEditChangeType> changes=EnumSet.noneOf(StatusEditChangeType.class);
				Status prev=data.get(idx+1);

				if(!Objects.equals(s.content, prev.content)){
					changes.add(StatusEditChangeType.TEXT_CHANGED);
				}
				if(!Objects.equals(s.spoilerText, prev.spoilerText)){
					if(s.spoilerText==null){
						changes.add(StatusEditChangeType.SPOILER_REMOVED);
					}else if(prev.spoilerText==null){
						changes.add(StatusEditChangeType.SPOILER_ADDED);
					}else{
						changes.add(StatusEditChangeType.SPOILER_CHANGED);
					}
				}
				if(s.poll!=null || prev.poll!=null){
					if(s.poll==null){
						changes.add(StatusEditChangeType.POLL_REMOVED);
					}else if(prev.poll==null){
						changes.add(StatusEditChangeType.POLL_ADDED);
					}else if(!s.poll.id.equals(prev.poll.id)){
						changes.add(StatusEditChangeType.POLL_CHANGED);
					}
				}
				List<String> newAttachmentIDs=s.mediaAttachments.stream().map(att->att.id).collect(Collectors.toList());
				List<String> prevAttachmentIDs=s.mediaAttachments.stream().map(att->att.id).collect(Collectors.toList());
				boolean addedOrRemoved=false;
				if(!newAttachmentIDs.containsAll(prevAttachmentIDs)){
					changes.add(StatusEditChangeType.MEDIA_REMOVED);
					addedOrRemoved=true;
				}
				if(!prevAttachmentIDs.containsAll(newAttachmentIDs)){
					changes.add(StatusEditChangeType.MEDIA_ADDED);
					addedOrRemoved=true;
				}
				if(!addedOrRemoved && !newAttachmentIDs.equals(prevAttachmentIDs)){
					changes.add(StatusEditChangeType.MEDIA_REORDERED);
				}
				if(s.sensitive && !prev.sensitive){
					changes.add(StatusEditChangeType.MARKED_SENSITIVE);
				}else if(prev.sensitive && !s.sensitive){
					changes.add(StatusEditChangeType.MARKED_NOT_SENSITIVE);
				}

				if(changes.size()==1){
					action=getString(switch(changes.iterator().next()){
						case TEXT_CHANGED -> R.string.edit_text_edited;
						case SPOILER_ADDED -> R.string.edit_spoiler_added;
						case SPOILER_REMOVED -> R.string.edit_spoiler_removed;
						case SPOILER_CHANGED -> R.string.edit_spoiler_edited;
						case POLL_ADDED -> R.string.edit_poll_added;
						case POLL_REMOVED -> R.string.edit_poll_removed;
						case POLL_CHANGED -> R.string.edit_poll_edited;
						case MEDIA_ADDED -> R.string.edit_media_added;
						case MEDIA_REMOVED -> R.string.edit_media_removed;
						case MEDIA_REORDERED -> R.string.edit_media_reordered;
						case MARKED_SENSITIVE -> R.string.edit_marked_sensitive;
						case MARKED_NOT_SENSITIVE -> R.string.edit_marked_not_sensitive;
					});
				}else{
					action=getString(R.string.edit_multiple_changed);
				}
			}
			items.add(0, new ReblogOrReplyLineStatusDisplayItem(s.id, this, action+" · "+date, Collections.emptyList(), 0));
		}
		return items;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addItemDecoration(new InsetStatusItemDecoration(this));
	}

	@Override
	public boolean isItemEnabled(String id){
		return false;
	}
}
