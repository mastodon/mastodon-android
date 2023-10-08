package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.lists.DeleteList;
import org.joinmastodon.android.api.requests.lists.GetListAccounts;
import org.joinmastodon.android.api.requests.lists.UpdateList;
import org.joinmastodon.android.events.ListDeletedEvent;
import org.joinmastodon.android.events.ListUpdatedEvent;
import org.joinmastodon.android.fragments.settings.BaseSettingsFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.FollowList;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.viewmodel.AvatarPileListItem;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.views.FloatingHintEditTextLayout;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class EditListFragment extends BaseSettingsFragment<Void>{
	private FollowList followList;
	private AvatarPileListItem<Void> membersItem;
	private CheckableListItem<Void> exclusiveItem;
	private FloatingHintEditTextLayout titleEditLayout;
	private EditText titleEdit;
	private Spinner showRepliesSpinner;
	private APIRequest<?> getMembersRequest;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		followList=Parcels.unwrap(getArguments().getParcelable("list"));
		setTitle(R.string.edit_list);
		onDataLoaded(List.of(
				membersItem=new AvatarPileListItem<>(getString(R.string.list_members), null, List.of(), 0, i->onMembersClick(), null, false),
				exclusiveItem=new CheckableListItem<>(R.string.list_exclusive, R.string.list_exclusive_subtitle, CheckableListItem.Style.SWITCH, followList.exclusive, this::toggleCheckableItem)
		));
		loadMembers();
		setHasOptionsMenu(true);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		if(getMembersRequest!=null)
			getMembersRequest.cancel();
		String newTitle=titleEdit.getText().toString();
		FollowList.RepliesPolicy newRepliesPolicy=FollowList.RepliesPolicy.values()[showRepliesSpinner.getSelectedItemPosition()];
		boolean newExclusive=exclusiveItem.checked;
		if(!newTitle.equals(followList.title) || newRepliesPolicy!=followList.repliesPolicy || newExclusive!=followList.exclusive){
			new UpdateList(followList.id, newTitle, newRepliesPolicy, newExclusive)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(FollowList result){
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

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		LinearLayout topView=new LinearLayout(getActivity());
		topView.setOrientation(LinearLayout.VERTICAL);

		titleEditLayout=(FloatingHintEditTextLayout) getActivity().getLayoutInflater().inflate(R.layout.floating_hint_edit_text, topView, false);
		titleEdit=titleEditLayout.findViewById(R.id.edit);
		titleEdit.setHint(R.string.list_name);
		titleEditLayout.updateHint();
		if(followList!=null)
			titleEdit.setText(followList.title);
		topView.addView(titleEditLayout);

		FloatingHintEditTextLayout showRepliesLayout=(FloatingHintEditTextLayout) getActivity().getLayoutInflater().inflate(R.layout.floating_hint_spinner, topView, false);
		showRepliesSpinner=showRepliesLayout.findViewById(R.id.spinner);
		showRepliesLayout.setHint(R.string.list_show_replies_to);
		topView.addView(showRepliesLayout);
		ArrayAdapter<String> spinnerAdapter=new ArrayAdapter<>(getActivity(), R.layout.item_spinner, List.of(
				getString(R.string.list_replies_no_one),
				getString(R.string.list_replies_members),
				getString(R.string.list_replies_anyone)
		));
		showRepliesSpinner.setAdapter(spinnerAdapter);
		showRepliesSpinner.setSelection(switch(followList.repliesPolicy){
			case FOLLOWED -> 2;
			case LIST -> 1;
			case NONE -> 0;
		});
		ViewGroup.MarginLayoutParams llp=(ViewGroup.MarginLayoutParams)showRepliesLayout.getLabel().getLayoutParams();
		llp.setMarginStart(llp.getMarginStart()+V.dp(16));

		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(new SingleViewRecyclerAdapter(topView));
		adapter.addAdapter(super.getAdapter());
		return adapter;
	}

	@Override
	protected int indexOfItemsAdapter(){
		return 1;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		menu.add(R.string.delete_list);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.delete_list)
				.setMessage(getString(R.string.delete_list_confirm, followList.title))
				.setPositiveButton(R.string.delete, (dlg, which)->doDeleteList())
				.setNegativeButton(R.string.cancel, null)
				.show();
		return true;
	}

	private void doDeleteList(){
		new DeleteList(followList.id)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Void result){
						E.post(new ListDeletedEvent(accountID, followList.id));
						Nav.finish(EditListFragment.this);
					}

					@Override
					public void onError(ErrorResponse error){
						Activity activity=getActivity();
						if(activity==null)
							return;
						error.showToast(activity);
					}
				})
				.wrapProgress(getActivity(), R.string.loading, true)
				.exec(accountID);
	}

	private void onMembersClick(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("list", Parcels.wrap(followList));
		Nav.go(getActivity(), ListMembersFragment.class, args);
	}

	private void loadMembers(){
		getMembersRequest=new GetListAccounts(followList.id, null, 3)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(HeaderPaginationList<Account> result){
						getMembersRequest=null;
						membersItem.avatars=new ArrayList<>();
						for(int i=0;i<Math.min(3, result.size());i++){
							Account acc=result.get(i);
							membersItem.avatars.add(new UrlImageLoaderRequest(acc.avatarStatic, V.dp(32), V.dp(32)));
						}
						rebindItem(membersItem);
						imgLoader.updateImages();
					}

					@Override
					public void onError(ErrorResponse error){
						getMembersRequest=null;
					}
				})
				.exec(accountID);
	}
}
