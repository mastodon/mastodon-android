package org.joinmastodon.android.fragments;

import android.os.Bundle;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.lists.UpdateList;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.ListUpdatedEvent;
import org.joinmastodon.android.model.FollowList;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;

import java.util.List;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class EditListFragment extends BaseEditListFragment{
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.edit_list);
		loadMembers();
	}

	@Override
	protected void onDataLoaded(List<ListItem<Void>> d){
		d.add(new ListItem<>(R.string.delete_list, 0, 0, this::onDeleteListClick, R.attr.colorM3Error, false));
		super.onDataLoaded(d);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		String newTitle=titleEdit.getText().toString();
		FollowList.RepliesPolicy newRepliesPolicy=getSelectedRepliesPolicy();
		boolean newExclusive=exclusiveItem.checked;
		if(!newTitle.equals(followList.title) || newRepliesPolicy!=followList.repliesPolicy || newExclusive!=followList.exclusive){
			new UpdateList(followList.id, newTitle, newRepliesPolicy, newExclusive)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(FollowList result){
							AccountSessionManager.get(accountID).getCacheController().updateList(result);
							E.post(new ListUpdatedEvent(accountID, result));
						}

						@Override
						public void onError(ErrorResponse error){
							// TODO handle errors somehow
						}
					})
					.exec(accountID);
		}
	}

	private void onDeleteListClick(ListItem<Void> item){
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.delete_list)
				.setMessage(getString(R.string.delete_list_confirm, followList.title))
				.setPositiveButton(R.string.delete, (dlg, which)->doDeleteList())
				.setNegativeButton(R.string.cancel, null)
				.show();
	}
}
