package org.joinmastodon.android.fragments.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.requests.accounts.DeleteProfileAvatar;
import org.joinmastodon.android.api.requests.accounts.DeleteProfileHeader;
import org.joinmastodon.android.api.requests.accounts.GetOwnAccount;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentials;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.OwnFeaturedHashtagAddedEvent;
import org.joinmastodon.android.events.OwnFeaturedHashtagRemovedEvent;
import org.joinmastodon.android.events.SelfAccountUpdatedEvent;
import org.joinmastodon.android.fragments.settings.BaseSettingsFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.model.viewmodel.ListItemWithTrailingIcon;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.SingleImagePhotoViewerListener;
import org.joinmastodon.android.ui.Snackbar;
import org.joinmastodon.android.ui.adapters.GenericListItemsAdapter;
import org.joinmastodon.android.ui.photoviewer.AvatarCropper;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.ListItemViewHolder;
import org.parceler.Parcels;

import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class ProfileEditFragment extends BaseSettingsFragment<Void>{
	private static final int AVATAR_RESULT=722;
	private static final int COVER_RESULT=343;

	private View header;
	private ImageView avatar, cover;
	private Account account;
	private ListItem<Void> displayNameItem, bioItem, customFieldsItem, featuredHashtagsItem, tabSettingsItem;
	private View avatarBadge, coverBadge, avatarBorder;
	private int featuredHashtagCount;
	private PopupMenu avatarMenu, coverMenu;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		featuredHashtagCount=getArguments().getInt("featuredTagCount");
		setTitle(R.string.edit_profile);
		data.add(displayNameItem=new ListItem<>(getString(R.string.edit_profile_display_name), "", 0, this::onDisplayNameClick, null, 0, true));
		data.add(bioItem=new ListItemWithTrailingIcon<>(getString(R.string.profile_bio), "", 0, this::onBioClick, null, 0, true));
		data.add(customFieldsItem=new ListItemWithTrailingIcon<>("", getString(R.string.edit_profile_custom_fields_description), R.drawable.ic_arrow_right_24px, this::onCustomFieldsClick, null, 0, true));
		data.add(featuredHashtagsItem=new ListItemWithTrailingIcon<>("", getString(R.string.edit_profile_featured_hashtags_description), R.drawable.ic_arrow_right_24px, this::onFeaturedHashtagsClick, null, 0, true));
		data.add(tabSettingsItem=new ListItemWithTrailingIcon<>(getString(R.string.edit_profile_tab_settings), null, R.drawable.ic_arrow_right_24px, this::onTabSettingsClick, null, 0, true));
		loadData();
		setRefreshEnabled(false);
		E.register(this);
	}

	@Override
	public void onDestroy(){
		E.unregister(this);
		super.onDestroy();
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetOwnAccount()
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(Account result){
						if(getActivity()==null)
							return;

						account=result;
						updateItems();
						dataLoaded();
					}
				})
				.exec(accountID);
	}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		header=LayoutInflater.from(getActivity()).inflate(R.layout.header_profile_edit, list, false);
		avatar=header.findViewById(R.id.avatar);
		cover=header.findViewById(R.id.cover);
		avatarBadge=header.findViewById(R.id.avatar_badge);
		coverBadge=header.findViewById(R.id.cover_badge);
		avatarBorder=header.findViewById(R.id.avatar_border);
		avatar.setOutlineProvider(OutlineProviders.roundedRect(16));
		avatar.setClipToOutline(true);
		avatarBadge.setOutlineProvider(OutlineProviders.OVAL);
		avatarBadge.setClipToOutline(true);
		coverBadge.setOutlineProvider(OutlineProviders.OVAL);
		coverBadge.setClipToOutline(true);
		avatar.setOnClickListener(this::onAvatarClick);
		cover.setOnClickListener(this::onCoverClick);

		avatarMenu=new PopupMenu(getActivity(), avatarBadge);
		populateImageMenu(avatarMenu);
		avatarMenu.setOnMenuItemClickListener(item->{
			if(item.getItemId()==0)
				startImagePicker(AVATAR_RESULT);
			else
				confirmAndDeleteImage(R.string.confirm_delete_avatar, new DeleteProfileAvatar(), ()->avatar.setImageResource(R.drawable.image_placeholder));
			return true;
		});
		coverMenu=new PopupMenu(getActivity(), coverBadge);
		coverMenu.setOnMenuItemClickListener(item->{
			if(item.getItemId()==0)
				startImagePicker(COVER_RESULT);
			else
				confirmAndDeleteImage(R.string.confirm_delete_cover, new DeleteProfileHeader(), ()->cover.setImageResource(R.drawable.image_placeholder));
			return true;
		});
		populateImageMenu(coverMenu);

		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(new SingleViewRecyclerAdapter(header));
		adapter.addAdapter(itemsAdapter=new GenericListItemsAdapter<>(imgLoader, data){
			@NonNull
			@Override
			public ListItemViewHolder<?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
				ListItemViewHolder<?> holder=super.onCreateViewHolder(parent, viewType);
				holder.subtitle.setSingleLine();
				holder.subtitle.setEllipsize(TextUtils.TruncateAt.END);
				return holder;
			}
		});
		return adapter;
	}

	@Override
	protected int indexOfItemsAdapter(){
		return 1;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(resultCode==Activity.RESULT_OK){
			if(requestCode==AVATAR_RESULT){
				if(!isTablet){
					getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}
				int radius=V.dp(25);
				new AvatarCropper(getActivity(), data.getData(), new SingleImagePhotoViewerListener(avatar, avatarBorder, new int[]{radius, radius, radius, radius}, this, ()->{}, null, null, null),
						this::uploadAvatar, ()->getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)).show();
			}else if(requestCode==COVER_RESULT){
				uploadCover(data.getData());
			}
		}
	}

	private void uploadAvatar(Drawable thumbnail, Uri uri, Runnable cropperDismiss){
		new UpdateAccountCredentials(null, null, uri, null, null)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
						avatar.setImageDrawable(thumbnail);
						cropperDismiss.run();
						AccountSessionManager.getInstance().updateAccountInfo(accountID, result);
					}

					@Override
					public void onError(ErrorResponse error){
						if(getActivity()==null)
							return;
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.loading, false)
				.exec(accountID);
	}

	private void uploadCover(Uri uri){
		new UpdateAccountCredentials(null, null, null, uri, null)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						ViewImageLoader.loadWithoutAnimation(cover, null, new UrlImageLoaderRequest(uri, V.dp(1000), V.dp(1000)));
						AccountSessionManager.getInstance().updateAccountInfo(accountID, result);
					}

					@Override
					public void onError(ErrorResponse error){
						if(getActivity()==null || !(error instanceof MastodonErrorResponse me))
							return;
						Snackbar[] sb={null};
						sb[0]=new Snackbar.Builder(getActivity())
								.setText(me.getErrorMessage())
								.setAction(R.string.retry, ()->{
									uploadCover(uri);
									sb[0].dismiss();
								})
								.setPersistent()
								.create();
						sb[0].show();
					}
				})
				.wrapProgress(getActivity(), R.string.loading, false)
				.exec(accountID);
	}

	@Subscribe
	public void onAccountUpdated(SelfAccountUpdatedEvent ev){
		if(ev.accountID().equals(accountID)){
			account=ev.account();
			updateItems();
		}
	}

	@Subscribe
	public void onHashtagAdded(OwnFeaturedHashtagAddedEvent ev){
		if(ev.accountID().equals(accountID)){
			featuredHashtagCount++;
			featuredHashtagsItem.title=getString(R.string.edit_profile_x_featured_hashtags, featuredHashtagCount);
			rebindItem(featuredHashtagsItem);
		}
	}

	@Subscribe
	public void onHashtagRemoved(OwnFeaturedHashtagRemovedEvent ev){
		if(ev.accountID().equals(accountID)){
			featuredHashtagCount=ev.newTotal();
			featuredHashtagsItem.title=getString(R.string.edit_profile_x_featured_hashtags, featuredHashtagCount);
			rebindItem(featuredHashtagsItem);
		}
	}

	private void populateImageMenu(PopupMenu menu){
		UiUtils.enablePopupMenuIcons(getActivity(), menu);

		MenuItem item=menu.getMenu().add(0, 0, 0, R.string.edit_profile_replace_image);
		Drawable icon=getResources().getDrawable(R.drawable.ic_replace_image_20px, getActivity().getTheme()).mutate();
		icon.setTint(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurface));
		item.setIcon(icon);

		item=menu.getMenu().add(0, 1, 0, UiUtils.makeColoredString(getString(R.string.remove), UiUtils.getThemeColor(getActivity(), R.attr.colorM3Error)));
		icon=getResources().getDrawable(R.drawable.ic_delete_20px, getActivity().getTheme()).mutate();
		icon.setTint(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Error));
		item.setIcon(icon);
	}

	private void updateItems(){
		displayNameItem.subtitle=account.displayName;
		rebindItem(displayNameItem);

		if(TextUtils.isEmpty(account.note)){
			bioItem.subtitle=null;
			bioItem.title=getString(R.string.edit_profile_add_bio);
			bioItem.iconRes=R.drawable.ic_add_24px;
		}else{
			bioItem.subtitle=HtmlParser.strip(account.note, true);
			bioItem.title=getString(R.string.profile_bio);
			bioItem.iconRes=0;
		}
		rebindItem(bioItem);

		customFieldsItem.title=getString(R.string.edit_profile_x_custom_fields, account.fields.size());
		rebindItem(customFieldsItem);

		featuredHashtagsItem.title=getString(R.string.edit_profile_x_featured_hashtags, featuredHashtagCount);
		rebindItem(featuredHashtagsItem);

		ViewImageLoader.loadWithoutAnimation(avatar, avatar.getDrawable(), new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? account.avatar : account.avatarStatic, V.dp(100), V.dp(100)));
		ViewImageLoader.loadWithoutAnimation(cover, cover.getDrawable(), new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? account.header : account.headerStatic, 1000, 1000));
	}

	private void onDisplayNameClick(ListItem<?> item){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("name", account.displayName);
		Nav.go(getActivity(), ProfileEditNameFragment.class, args);
	}

	private void onBioClick(ListItem<?> item){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("bio", account.source.note);
		Nav.go(getActivity(), ProfileEditBioFragment.class, args);
	}

	private void onCustomFieldsClick(ListItem<?> item){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelableArrayList("fields", (ArrayList<? extends Parcelable>) account.source.fields.stream().map(Parcels::wrap).collect(Collectors.toCollection(ArrayList::new)));
		Nav.go(getActivity(), ProfileEditCustomFieldsFragment.class, args);
	}

	private void onFeaturedHashtagsClick(ListItem<?> item){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), ProfileEditFeaturedHashtagsFragment.class, args);
	}

	private void onTabSettingsClick(ListItem<?> item){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), ProfileEditTabSettingsFragment.class, args);
	}

	private void onAvatarClick(View v){
		// TODO if there's no avatar
		avatarMenu.show();
	}

	private void onCoverClick(View v){
		coverMenu.show();
	}

	private void startImagePicker(int requestCode){
		Intent intent=UiUtils.getMediaPickerIntent(new String[]{"image/*"}, 1);
		startActivityForResult(intent, requestCode);
	}

	private void confirmAndDeleteImage(int confirmStr, MastodonAPIRequest<Account> req, Runnable onDone){
		AlertDialog alert=new M3AlertDialogBuilder(getActivity())
				.setTitle(confirmStr)
				.setPositiveButton(R.string.remove, null)
				.setNegativeButton(R.string.cancel, null)
				.create();
		Button positiveBtn=alert.getButton(AlertDialog.BUTTON_POSITIVE);
		Button negativeBtn=alert.getButton(DialogInterface.BUTTON_NEGATIVE);
		positiveBtn.setOnClickListener(v->{
			alert.setCancelable(false);
			UiUtils.showProgressForAlertButton(positiveBtn, true);
			negativeBtn.setEnabled(false);

			req.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Account result){
							onDone.run();
							alert.dismiss();
							AccountSessionManager.getInstance().updateAccountInfo(accountID, result);
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
