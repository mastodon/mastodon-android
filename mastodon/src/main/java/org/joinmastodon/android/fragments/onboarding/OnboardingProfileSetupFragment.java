package org.joinmastodon.android.fragments.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentials;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HomeFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.SingleImagePhotoViewerListener;
import org.joinmastodon.android.ui.adapters.GenericListItemsAdapter;
import org.joinmastodon.android.ui.photoviewer.AvatarCropper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewholders.ListItemViewHolder;
import org.joinmastodon.android.ui.views.ReorderableLinearLayout;
import org.joinmastodon.android.utils.ElevationOnScrollListener;

import java.util.ArrayList;
import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class OnboardingProfileSetupFragment extends ToolbarFragment{
	private Button btn;
	private View buttonBar;
	private String accountID;
	private ElevationOnScrollListener onScrollListener;
	private ScrollView scroller;
	private EditText nameEdit, bioEdit;
	private ImageView avaImage, coverImage;
	private Uri avatarUri, coverUri;
	private LinearLayout scrollContent;
	private CheckableListItem<Void> discoverableItem;
	private View avaBorder;

	private static final int AVATAR_RESULT=348;
	private static final int COVER_RESULT=183;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setNavigationBarColor(UiUtils.getThemeColor(activity, R.attr.colorM3Surface));
		accountID=getArguments().getString("account");
		setTitle(R.string.profile_setup);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_onboarding_profile_setup, container, false);

		scroller=view.findViewById(R.id.scroller);
		nameEdit=view.findViewById(R.id.display_name);
		bioEdit=view.findViewById(R.id.bio);
		avaImage=view.findViewById(R.id.avatar);
		coverImage=view.findViewById(R.id.header);
		avaBorder=view.findViewById(R.id.avatar_border);

		btn=view.findViewById(R.id.btn_next);
		btn.setOnClickListener(v->onButtonClick());
		buttonBar=view.findViewById(R.id.button_bar);

		avaImage.setOutlineProvider(OutlineProviders.roundedRect(24));
		avaImage.setClipToOutline(true);

		Account account=AccountSessionManager.getInstance().getAccount(accountID).self;
		if(savedInstanceState==null){
			nameEdit.setText(account.displayName);
		}

		avaImage.setOnClickListener(v->startActivityForResult(UiUtils.getMediaPickerIntent(new String[]{"image/*"}, 1), AVATAR_RESULT));
		coverImage.setOnClickListener(v->startActivityForResult(UiUtils.getMediaPickerIntent(new String[]{"image/*"}, 1), COVER_RESULT));

		scrollContent=view.findViewById(R.id.scrollable_content);
		discoverableItem=new CheckableListItem<>(R.string.make_profile_discoverable, 0, CheckableListItem.Style.SWITCH_SEPARATED, true, R.drawable.ic_campaign_24px, item->showDiscoverabilityAlert());
		GenericListItemsAdapter<Void> fakeAdapter=new GenericListItemsAdapter<>(List.of(discoverableItem));
		ListItemViewHolder<?> holder=fakeAdapter.onCreateViewHolder(scrollContent, fakeAdapter.getItemViewType(0));
		fakeAdapter.bindViewHolder(holder, 0);
		holder.itemView.setBackground(UiUtils.getThemeDrawable(getActivity(), android.R.attr.selectableItemBackground));
		holder.itemView.setOnClickListener(v->holder.onClick());
		scrollContent.addView(holder.itemView);

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		scroller.setOnScrollChangeListener(onScrollListener=new ElevationOnScrollListener((FragmentRootLinearLayout) view, buttonBar, getToolbar()));
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		if(onScrollListener!=null){
			onScrollListener.setViews(buttonBar, getToolbar());
		}
	}

	protected void onButtonClick(){
		new UpdateAccountCredentials(nameEdit.getText().toString(), bioEdit.getText().toString(), avatarUri, coverUri, null)
				.setDiscoverableIndexable(discoverableItem.checked, discoverableItem.checked)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						AccountSessionManager.getInstance().updateAccountInfo(accountID, result);
						Bundle args=new Bundle();
						args.putString("account", accountID);
						Nav.goClearingStack(getActivity(), HomeFragment.class, args);
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.saving, true)
				.exec(accountID);
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		super.onApplyWindowInsets(UiUtils.applyBottomInsetToFixedView(buttonBar, insets));
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(resultCode!=Activity.RESULT_OK)
			return;
		Uri uri=data.getData();
		int size;
		if(requestCode==AVATAR_RESULT){
			if(!isTablet){
				getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
			int radius=V.dp(25);
			new AvatarCropper(getActivity(), data.getData(), new SingleImagePhotoViewerListener(avaImage, avaBorder, new int[]{radius, radius, radius, radius}, this, ()->{}, null, null, null), (thumbnail, newUri)->{
				getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
				avaImage.setImageDrawable(thumbnail);
				avaImage.setForeground(null);
				avatarUri=newUri;
			}, ()->getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)).show();
		}else{
			coverUri=uri;
			size=V.dp(1000);
			ViewImageLoader.load(coverImage, null, new UrlImageLoaderRequest(uri, size, size));
			coverImage.setForeground(null);
		}
	}

	private void showDiscoverabilityAlert(){
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.discoverability)
				.setMessage(R.string.discoverability_help)
				.setPositiveButton(R.string.ok, null)
				.show();
	}
}
