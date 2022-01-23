package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;
import org.parceler.Parcels;

import java.time.Instant;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;

public class HeaderStatusDisplayItem extends StatusDisplayItem{
	private Account user;
	private Instant createdAt;
	private ImageLoaderRequest avaRequest;
	private Fragment parentFragment;
	private String accountID;

	public HeaderStatusDisplayItem(String parentID, Account user, Instant createdAt, Fragment parentFragment, String accountID){
		super(parentID);
		this.user=user;
		this.createdAt=createdAt;
		avaRequest=new UrlImageLoaderRequest(user.avatar);
		this.parentFragment=parentFragment;
		this.accountID=accountID;
	}

	@Override
	public Type getType(){
		return Type.HEADER;
	}

	@Override
	public int getImageCount(){
		return 1;
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return avaRequest;
	}

	public static class Holder extends BindableViewHolder<HeaderStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView name, subtitle;
		private final ImageView avatar;
		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_header, parent);
			name=findViewById(R.id.name);
			subtitle=findViewById(R.id.subtitle);
			avatar=findViewById(R.id.avatar);
			avatar.setOnClickListener(this::onAvaClick);
		}

		@Override
		public void onBind(HeaderStatusDisplayItem item){
			name.setText(item.user.displayName);
			subtitle.setText('@'+item.user.acct);
		}

		@Override
		public void setImage(int index, Drawable drawable){
			avatar.setImageDrawable(drawable);
			if(drawable instanceof Animatable)
				((Animatable) drawable).start();
		}

		@Override
		public void clearImage(int index){
			avatar.setImageBitmap(null);
		}

		private void onAvaClick(View v){
			Bundle args=new Bundle();
			args.putString("account", item.accountID);
			args.putParcelable("profileAccount", Parcels.wrap(item.user));
			Nav.go(item.parentFragment.getActivity(), ProfileFragment.class, args);
		}
	}
}
