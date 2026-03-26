package org.joinmastodon.android.fragments.profile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.tags.GetAccountFeaturedHashtags;
import org.joinmastodon.android.api.requests.tags.RemoveFeaturedHashtag;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.OwnFeaturedHashtagAddedEvent;
import org.joinmastodon.android.events.OwnFeaturedHashtagRemovedEvent;
import org.joinmastodon.android.fragments.settings.BaseSettingsFragment;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.model.viewmodel.ListItemWithTrailingIconButton;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.List;
import java.util.stream.Collectors;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class ProfileEditFeaturedHashtagsFragment extends BaseSettingsFragment<Hashtag>{
	private String accountID;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		setTitle(R.string.edit_profile_featured_hashtags);
		setRefreshEnabled(false);
		loadData();
		E.register(this);
	}

	@Override
	public void onDestroy(){
		E.unregister(this);
		super.onDestroy();
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetAccountFeaturedHashtags(AccountSessionManager.get(accountID).self.id)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Hashtag> result){
						if(getActivity()==null)
							return;
						onDataLoaded(result.stream().map(ProfileEditFeaturedHashtagsFragment.this::makeItem).collect(Collectors.toList()));
					}
				})
				.exec(accountID);
	}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(super.getAdapter());

		Button addButton=new Button(getActivity(), null, 0, R.style.Widget_Mastodon_M3_Button_Tonal);
		addButton.setText(R.string.add_featured_hashtag);
		FrameLayout buttonWrap=new FrameLayout(getActivity());
		buttonWrap.addView(addButton, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START));
		buttonWrap.setPadding(V.dp(16), V.dp(4), V.dp(16), V.dp(4));
		adapter.addAdapter(new SingleViewRecyclerAdapter(buttonWrap));
		addButton.setOnClickListener(v->{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), ProfileEditAddHashtagFragment.class, args);
		});

		return adapter;
	}

	@Subscribe
	public void onHashtagAdded(OwnFeaturedHashtagAddedEvent ev){
		if(!accountID.equals(ev.accountID()))
			return;
		data.add(makeItem(ev.tag()));
		itemsAdapter.notifyItemInserted(data.size()-1);
	}

	private ListItem<Hashtag> makeItem(Hashtag tag){
		ListItemWithTrailingIconButton<Hashtag> item=new ListItemWithTrailingIconButton<>('#'+tag.name,
				getResources().getQuantityString(R.plurals.hashtag_used_in_x_posts, tag.statusesCount, tag.statusesCount), R.drawable.ic_delete_24px, null);
		item.parentObject=tag;
		item.isEnabled=true;
		item.iconColorAttr=R.attr.colorM3Error;
		item.buttonOnClickListener=ProfileEditFeaturedHashtagsFragment.this::onDeleteClick;
		item.buttonContentDescription=getString(R.string.delete);
		return item;
	}

	private void onDeleteClick(ListItemWithTrailingIconButton<Hashtag> item){
		AlertDialog alert=new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.confirm_remove_featured_hashtag)
				.setPositiveButton(R.string.remove, null)
				.setNegativeButton(R.string.cancel, null)
				.create();
		Button positiveBtn=alert.getButton(AlertDialog.BUTTON_POSITIVE);
		Button negativeBtn=alert.getButton(DialogInterface.BUTTON_NEGATIVE);
		positiveBtn.setOnClickListener(v->{
			alert.setCancelable(false);
			UiUtils.showProgressForAlertButton(positiveBtn, true);
			negativeBtn.setEnabled(false);
			new RemoveFeaturedHashtag(item.parentObject.id)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Void result){
							int index=data.indexOf(item);
							if(index>=0){
								data.remove(item);
								itemsAdapter.notifyItemRemoved(index);
							}
							alert.dismiss();
							E.post(new OwnFeaturedHashtagRemovedEvent(accountID, item.parentObject, data.size()));
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(getActivity());
							alert.setCancelable(true);
							UiUtils.showProgressForAlertButton(positiveBtn, false);
							negativeBtn.setEnabled(true);
						}
					})
					.exec(accountID);
		});
		alert.show();
	}
}
