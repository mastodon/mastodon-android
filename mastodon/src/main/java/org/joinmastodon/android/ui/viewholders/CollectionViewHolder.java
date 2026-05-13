package org.joinmastodon.android.ui.viewholders;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.collections.CollectionFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.viewmodel.CollectionViewModel;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.BlurView;
import org.parceler.Parcels;

import java.util.function.Consumer;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class CollectionViewHolder extends BindableViewHolder<CollectionViewModel> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable{
	private final TextView title, author, accountCount;
	public final ImageButton menuButton;
	private final ImageView[] avatars;
	private final ImageView sensitiveOverlay;
	private final BlurView blurContainer;
	private final String accountID;
	private final PopupMenu menu;
	private final Consumer<String> onBlockClick;
	private final AccountAndRelationshipProvider accountProvider;
	private Account account;
	private Relationship relationship;

	public CollectionViewHolder(Context context, ViewGroup parent, String accountID, Consumer<String> onBlockClick, AccountAndRelationshipProvider accountProvider){
		super(context, R.layout.item_account_collection, parent);
		this.accountID=accountID;
		this.onBlockClick=onBlockClick;
		title=findViewById(R.id.title);
		author=findViewById(R.id.author);
		accountCount=findViewById(R.id.account_count);
		menuButton=findViewById(R.id.options_btn);
		avatars=new ImageView[]{
				findViewById(R.id.avatar1),
				findViewById(R.id.avatar2),
				findViewById(R.id.avatar3),
				findViewById(R.id.avatar4),
		};
		sensitiveOverlay=findViewById(R.id.sensitive_overlay);
		sensitiveOverlay.setOutlineProvider(OutlineProviders.roundedRect(8));
		sensitiveOverlay.setClipToOutline(true);
		blurContainer=findViewById(R.id.blur);
		menuButton.setOnClickListener(v->showMenu());

		menu=new PopupMenu(context, menuButton);
		Menu m=menu.getMenu();
		m.add(0, 0, 0, R.string.view_collection);
		m.add(0, 1, 1, R.string.button_share);
		m.add(1, 2, 2, UiUtils.makeRedString(context, R.string.report_collection));
		m.add(1, 3 ,3, UiUtils.makeRedString(context, R.string.block_account));
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P && !UiUtils.isEMUI() && !UiUtils.isMagic()){
			m.setGroupDividerEnabled(true);
		}

		menu.setOnMenuItemClickListener(this::onMenuItemClick);
		this.accountProvider=accountProvider;
	}

	@Override
	public void onBind(CollectionViewModel item){
		title.setText(item.collection.name);
		author.setText(itemView.getContext().getString(R.string.collection_by_author, "@"+item.author.getUsername()));
		accountCount.setText(itemView.getContext().getResources().getQuantityString(R.plurals.x_accounts, item.collection.itemCount, item.collection.itemCount));
		if(item.accounts.size()<4){
			for(int i=item.accounts.size();i<4;i++){
				avatars[i].setImageResource(R.drawable.image_placeholder);
			}
		}
		sensitiveOverlay.setVisibility(item.collection.sensitive ? View.VISIBLE : View.GONE);
		blurContainer.setBlurEnabled(item.collection.sensitive);
	}

	@Override
	public void onClick(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("collection", item.collection.id);
		args.putString("collectionTitle", item.collection.name);
		args.putString("authorUsername", item.author.getUsername());
		Nav.go((Activity)itemView.getContext(), CollectionFragment.class, args);
	}

	@Override
	public void setImage(int index, Drawable image){
		avatars[index].setImageDrawable(image);
		if(image instanceof Animatable anim)
			anim.start();
	}

	@Override
	public void clearImage(int index){
		avatars[index].setImageResource(R.drawable.image_placeholder);
	}

	private void showMenu(){
		account=accountProvider.getAccount(item.collection.accountId);
		relationship=accountProvider.getRelationship(item.collection.accountId);
		boolean isSelf=AccountSessionManager.getInstance().isSelf(accountID, item.author);
		if(account!=null && (isSelf || relationship!=null)){
			menu.getMenu().setGroupVisible(1, !isSelf);
			if(!isSelf)
				menu.getMenu().getItem(3).setVisible(!relationship.blocking);
			menu.show();
		}
	}

	private boolean onMenuItemClick(MenuItem item){
		switch(item.getItemId()){
			case 0 -> onClick();
			case 1 -> UiUtils.openSystemShareSheet(itemView.getContext(), this.item);

			case 2 -> {
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putParcelable("reportAccount", Parcels.wrap(account));
				args.putParcelable("relationship", Parcels.wrap(relationship));
				args.putString("collectionID", this.item.collection.id);
				Nav.go((Activity) itemView.getContext(), ReportReasonChoiceFragment.class, args);
			}
			case 3 -> onBlockClick.accept(this.item.collection.accountId);
		}
		return true;
	}

	// I'm not proud of this. TODO relationships and accounts should be cached globally
	public interface AccountAndRelationshipProvider{
		Account getAccount(String id);
		Relationship getRelationship(String id);
	}
}
