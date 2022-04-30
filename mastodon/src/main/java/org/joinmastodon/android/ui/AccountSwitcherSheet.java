package org.joinmastodon.android.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.oauth.RevokeOauthToken;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.SplashFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.RecyclerViewDelegate;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;
import me.grishka.appkit.views.UsableRecyclerView;

public class AccountSwitcherSheet extends BottomSheet{
	private final Activity activity;
	private UsableRecyclerView list;
	private List<WrappedAccount> accounts;
	private ListImageLoaderWrapper imgLoader;

	public AccountSwitcherSheet(@NonNull Activity activity){
		super(activity);
		this.activity=activity;

		accounts=AccountSessionManager.getInstance().getLoggedInAccounts().stream().map(WrappedAccount::new).collect(Collectors.toList());

		list=new UsableRecyclerView(activity);
		imgLoader=new ListImageLoaderWrapper(activity, list, new RecyclerViewDelegate(list), null);
		list.setClipToPadding(false);
		list.setLayoutManager(new LinearLayoutManager(activity));

		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		View handle=new View(activity);
		handle.setBackgroundResource(R.drawable.bg_bottom_sheet_handle);
		adapter.addAdapter(new SingleViewRecyclerAdapter(handle));
		adapter.addAdapter(new AccountsAdapter());
		AccountViewHolder holder=new AccountViewHolder();
		holder.more.setVisibility(View.GONE);
		holder.currentIcon.setVisibility(View.GONE);
		holder.name.setText(R.string.add_account);
		holder.avatar.setScaleType(ImageView.ScaleType.CENTER);
		holder.avatar.setImageResource(R.drawable.ic_fluent_add_circle_24_filled);
		holder.avatar.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(activity, android.R.attr.textColorPrimary)));
		adapter.addAdapter(new ClickableSingleViewRecyclerAdapter(holder.itemView, ()->{
			Nav.go(activity, SplashFragment.class, null);
			dismiss();
		}));

		list.setAdapter(adapter);
		DividerItemDecoration divider=new DividerItemDecoration(activity, R.attr.colorPollVoted, .5f, 72, 16, DividerItemDecoration.NOT_FIRST);
		divider.setDrawBelowLastItem(true);
		list.addItemDecoration(divider);

		FrameLayout content=new FrameLayout(activity);
		content.setBackgroundResource(R.drawable.bg_bottom_sheet);
		content.addView(list);
		setContentView(content);
		setNavigationBarBackground(new ColorDrawable(UiUtils.getThemeColor(activity, R.attr.colorWindowBackground)), !UiUtils.isDarkTheme());
	}

	private void confirmLogOut(String accountID){
		new M3AlertDialogBuilder(activity)
				.setTitle(R.string.log_out)
				.setMessage(R.string.confirm_log_out)
				.setPositiveButton(R.string.log_out, (dialog, which) -> logOut(accountID))
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void logOut(String accountID){
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		new RevokeOauthToken(session.app.clientId, session.app.clientSecret, session.token.accessToken)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Object result){
						onLoggedOut(accountID);
					}

					@Override
					public void onError(ErrorResponse error){
						onLoggedOut(accountID);
					}
				})
				.wrapProgress(activity, R.string.loading, false)
				.exec(accountID);
	}

	private void onLoggedOut(String accountID){
		AccountSessionManager.getInstance().removeAccount(accountID);
		dismiss();
	}

	@Override
	protected void onWindowInsetsUpdated(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=29){
			int tappableBottom=insets.getTappableElementInsets().bottom;
			int insetBottom=insets.getSystemWindowInsetBottom();
			if(tappableBottom==0 && insetBottom>0){
				list.setPadding(0, 0, 0, V.dp(48)-insetBottom);
			}else{
				list.setPadding(0, 0, 0, 0);
			}
		}
	}

	private class AccountsAdapter extends UsableRecyclerView.Adapter<AccountViewHolder> implements ImageLoaderRecyclerAdapter{
		public AccountsAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new AccountViewHolder();
		}

		@Override
		public int getItemCount(){
			return accounts.size();
		}

		@Override
		public void onBindViewHolder(AccountViewHolder holder, int position){
			holder.bind(accounts.get(position).session);
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getImageCountForItem(int position){
			return 1;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return accounts.get(position).req;
		}
	}

	private class AccountViewHolder extends BindableViewHolder<AccountSession> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable{
		private final TextView name;
		private final ImageView avatar;
		private final ImageButton more;
		private final View currentIcon;
		private final PopupMenu menu;

		public AccountViewHolder(){
			super(activity, R.layout.item_account_switcher, list);
			name=findViewById(R.id.name);
			avatar=findViewById(R.id.avatar);
			more=findViewById(R.id.more);
			currentIcon=findViewById(R.id.current);

			avatar.setOutlineProvider(OutlineProviders.roundedRect(12));
			avatar.setClipToOutline(true);

			menu=new PopupMenu(activity, more);
			menu.inflate(R.menu.account_switcher);
			menu.setOnMenuItemClickListener(item1 -> {
				confirmLogOut(item.getID());
				return true;
			});
			more.setOnClickListener(v->menu.show());
		}

		@SuppressLint("SetTextI18n")
		@Override
		public void onBind(AccountSession item){
			name.setText("@"+item.self.username+"@"+item.domain);
			if(AccountSessionManager.getInstance().getLastActiveAccountID().equals(item.getID())){
				more.setVisibility(View.GONE);
				currentIcon.setVisibility(View.VISIBLE);
			}else{
				more.setVisibility(View.VISIBLE);
				currentIcon.setVisibility(View.GONE);
			}
			menu.getMenu().findItem(R.id.log_out).setTitle(activity.getString(R.string.log_out_account, "@"+item.self.username));
			UiUtils.enablePopupMenuIcons(activity, menu);
		}

		@Override
		public void setImage(int index, Drawable image){
			avatar.setImageDrawable(image);
			if(image instanceof Animatable a)
				a.start();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}

		@Override
		public void onClick(){
			AccountSessionManager.getInstance().setLastActiveAccountID(item.getID());
			activity.finish();
			activity.startActivity(new Intent(activity, MainActivity.class));
		}
	}

	private static class WrappedAccount{
		public final AccountSession session;
		public final ImageLoaderRequest req;

		public WrappedAccount(AccountSession session){
			this.session=session;
			req=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? session.self.avatar : session.self.avatarStatic, V.dp(50), V.dp(50));
		}
	}
}
