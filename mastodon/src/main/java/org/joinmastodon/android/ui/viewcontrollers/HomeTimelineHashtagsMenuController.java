package org.joinmastodon.android.ui.viewcontrollers;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.tags.GetFollowedTags;
import org.joinmastodon.android.fragments.ManageFollowedHashtagsFragment;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.ui.utils.HideableSingleViewRecyclerAdapter;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.V;

public class HomeTimelineHashtagsMenuController extends DropdownSubmenuController{
	private HideableSingleViewRecyclerAdapter largeProgressAdapter;
	private HideableSingleViewRecyclerAdapter emptyAdapter;
	private APIRequest<?> currentRequest;

	public HomeTimelineHashtagsMenuController(ToolbarDropdownMenuController dropdownController){
		super(dropdownController);
		items=new ArrayList<>();
		loadHashtags();
	}

	@Override
	protected void createView(){
		super.createView();
		emptyAdapter=createEmptyView(R.drawable.ic_tag_24px, R.string.no_followed_hashtags_title, R.string.no_followed_hashtags_subtitle);
		FrameLayout largeProgressView=new FrameLayout(dropdownController.getActivity());
		int pad=V.dp(32);
		largeProgressView.setPadding(0, pad, 0, pad);
		largeProgressView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		ProgressBar progress=new ProgressBar(dropdownController.getActivity());
		largeProgressView.addView(progress, new FrameLayout.LayoutParams(V.dp(48), V.dp(48), Gravity.CENTER));
		largeProgressAdapter=new HideableSingleViewRecyclerAdapter(largeProgressView);
		mergeAdapter.addAdapter(0, largeProgressAdapter);
		emptyAdapter.setVisible(false);
		mergeAdapter.addAdapter(0, emptyAdapter);
	}

	@Override
	protected CharSequence getBackItemTitle(){
		return dropdownController.getActivity().getString(R.string.followed_hashtags);
	}

	@Override
	public void onDismiss(){
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
		}
	}

	private void onTagClick(Item<Hashtag> item){
		dropdownController.dismiss();
		UiUtils.openHashtagTimeline(dropdownController.getActivity(), dropdownController.getAccountID(), item.parentObject);
	}

	private void onManageTagsClick(){
		dropdownController.dismiss();
		Bundle args=new Bundle();
		args.putString("account", dropdownController.getAccountID());
		Nav.go(dropdownController.getActivity(), ManageFollowedHashtagsFragment.class, args);
	}

	private void loadHashtags(){
		currentRequest=new GetFollowedTags(null, 200)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(HeaderPaginationList<Hashtag> result){
						currentRequest=null;
						dropdownController.resizeOnNextFrame();
						largeProgressAdapter.setVisible(false);
						((List<Hashtag>) result).sort(Comparator.comparing(tag->tag.name, Collator.getInstance()));
						int prevSize=items.size();
						for(Hashtag tag:result){
							items.add(new Item<>("#"+tag.name, false, false, tag, HomeTimelineHashtagsMenuController.this::onTagClick));
						}
						items.add(new Item<Void>(R.string.manage_hashtags, false, true, i->onManageTagsClick()));
						itemsAdapter.notifyItemRangeInserted(prevSize, result.size()+1);
						emptyAdapter.setVisible(result.isEmpty());
					}

					@Override
					public void onError(ErrorResponse error){
						currentRequest=null;
						Activity activity=dropdownController.getActivity();
						if(activity!=null)
							error.showToast(activity);
						dropdownController.popSubmenuController();

					}
				})
				.exec(dropdownController.getAccountID());
	}
}
