package org.joinmastodon.android.ui.viewcontrollers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.model.viewmodel.CollectionViewModel;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.CollectionViewHolder;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;

public class ShareCollectionAlertController{
	private final Context context;
	private final CollectionViewModel collection;
	private final String accountID;

	public ShareCollectionAlertController(Context context, CollectionViewModel collection, String accountID){
		this.context=context;
		this.collection=collection;
		this.accountID=accountID;
	}

	public void show(){
		AlertDialog alert=new M3AlertDialogBuilder(context)
				.setTitle(R.string.share_collection)
				.setView(R.layout.alert_share_collection)
				.setNegativeButton(R.string.post_collection_on_mastodon, (dlg, which)->{
					Bundle args=UiUtils.makeAccountArgs(accountID);
					args.putString("prefilledText", context.getString(R.string.share_collection_post_text)+"\n\n"+collection.collection.url);
					Nav.go((Activity) context, ComposeFragment.class, args);
				})
				.setPositiveButton(R.string.share_via_other_app, (dlg, which)->UiUtils.openSystemShareSheet(context, collection))
				.create();

		TextView url=alert.findViewById(R.id.url);
		ImageButton copyLink=alert.findViewById(R.id.copy_link);
		url.setText(collection.collection.url);
		copyLink.setOnClickListener(v->{
			context.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, collection.collection.url));
			UiUtils.maybeShowTextCopiedToast(context);
		});

		FrameLayout wrap=alert.findViewById(R.id.collection_wrap);
		CollectionViewHolder holder=new CollectionViewHolder(context, wrap, null, null, null);
		holder.bind(collection);
		int i=0;
		for(ImageLoaderRequest req:collection.avatarRequests){
			int[] index={i};
			ViewImageLoader.load(new ViewImageLoader.Target(){
				@Override
				public void setImageDrawable(Drawable d){
					holder.setImage(index[0], d);
				}

				@Override
				public View getView(){
					return holder.itemView;
				}
			}, null, req, false, true);
			i++;
		}
		wrap.addView(holder.itemView);

		alert.show();
	}
}
