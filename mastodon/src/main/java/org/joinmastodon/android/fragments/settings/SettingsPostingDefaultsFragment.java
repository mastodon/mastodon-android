package org.joinmastodon.android.fragments.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Preferences;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.model.StatusQuotePolicy;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.HideableSingleViewRecyclerAdapter;
import org.joinmastodon.android.ui.viewcontrollers.ComposeLanguageAlertViewController;

import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;

public class SettingsPostingDefaultsFragment extends BaseSettingsFragment<Void>{
	private ListItem<Void> languageItem, visibilityItem, quotePolicyItem;
	private Locale postLanguage;
	private ComposeLanguageAlertViewController.SelectedOption newPostLanguage;
	private StatusPrivacy visibility=StatusPrivacy.PUBLIC, newVisibility;
	private StatusQuotePolicy quotePolicy=StatusQuotePolicy.PUBLIC, newQuotePolicy;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.settings_posting_defaults);
		AccountSession account=AccountSessionManager.get(accountID);
		if(account.preferences!=null){
			if(account.preferences.postingDefaultLanguage!=null)
				postLanguage=Locale.forLanguageTag(account.preferences.postingDefaultLanguage);
			if(account.preferences.postingDefaultVisibility!=null)
				visibility=account.preferences.postingDefaultVisibility;
			if(account.preferences.postingDefaultQuotePolicy!=null)
				quotePolicy=account.preferences.postingDefaultQuotePolicy;
		}
		ArrayList<ListItem<Void>> items=new ArrayList<>();
		items.add(visibilityItem=new ListItem<>(getString(R.string.default_post_visibility), getVisibilitySubtitle(), R.drawable.ic_visibility_24px, this::onVisibilityClick));
		if(account.getInstanceInfo().supportsQuotePostAuthoring())
			items.add(quotePolicyItem=new ListItem<>(getString(R.string.compose_quote_policy), getQuotePolicySubtitle(), R.drawable.ic_format_quote_fill1_24px, this::onQuotePolicyClick));
		items.add(languageItem=new ListItem<>(getString(R.string.default_post_language), postLanguage!=null ? postLanguage.getDisplayName(Locale.getDefault()) : null, R.drawable.ic_language_24px, this::onDefaultLanguageClick));
		updateQuotePolicyItem(visibility);
		onDataLoaded(items);
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected void onHidden(){
		super.onHidden();
		if(newPostLanguage!=null || newVisibility!=null || newQuotePolicy!=null){
			AccountSession s=AccountSessionManager.get(accountID);
			if(s.preferences==null)
				s.preferences=new Preferences();
			if(newPostLanguage!=null)
				s.preferences.postingDefaultLanguage=newPostLanguage.locale.toLanguageTag();
			if(newVisibility!=null)
				s.preferences.postingDefaultVisibility=newVisibility;
			if(newQuotePolicy!=null)
				s.preferences.postingDefaultQuotePolicy=newQuotePolicy;
			s.savePreferencesLater();
		}
		AccountSessionManager.get(accountID).savePreferencesIfPending();
	}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		View banner=getActivity().getLayoutInflater().inflate(R.layout.item_settings_banner, list, false);
		TextView bannerText=banner.findViewById(R.id.text);
		ImageView bannerIcon=banner.findViewById(R.id.icon);
		SingleViewRecyclerAdapter bannerAdapter=new SingleViewRecyclerAdapter(banner);
		banner.findViewById(R.id.button).setVisibility(View.GONE);
		banner.findViewById(R.id.button2).setVisibility(View.GONE);
		banner.findViewById(R.id.title).setVisibility(View.GONE);
		bannerText.setText(R.string.settings_posting_defaults_explanation);
		bannerIcon.setImageResource(R.drawable.ic_info_24px);

		MergeRecyclerAdapter mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(bannerAdapter);
		mergeAdapter.addAdapter(super.getAdapter());
		return mergeAdapter;
	}

	@Override
	protected int indexOfItemsAdapter(){
		return 1;
	}

	private String getVisibilitySubtitle(){
		return getString(switch(newVisibility==null ? visibility : newVisibility){
			case UNLISTED -> R.string.visibility_unlisted;
			case PRIVATE -> R.string.visibility_followers_only;
			default -> R.string.visibility_public;
		});
	}

	private String getQuotePolicySubtitle(){
		return getString(switch(newQuotePolicy==null ? quotePolicy : newQuotePolicy){
			case PUBLIC -> R.string.quote_policy_public;
			case FOLLOWERS -> R.string.quote_policy_followers;
			case NOBODY -> R.string.quote_policy_nobody;
		});
	}

	private void onDefaultLanguageClick(ListItem<?> item){
		ComposeLanguageAlertViewController vc=new ComposeLanguageAlertViewController(getActivity(), null, newPostLanguage==null ? new ComposeLanguageAlertViewController.SelectedOption(-1, postLanguage, null) : newPostLanguage, null);
		AlertDialog dlg=new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.default_post_language)
				.setView(vc.getView())
				.setPositiveButton(R.string.cancel, null)
				.show();
		vc.setSelectionListener(opt->{
			if(!opt.locale.equals(postLanguage)){
				newPostLanguage=opt;
				languageItem.subtitle=newPostLanguage.locale.getDisplayLanguage(Locale.getDefault());
				rebindItem(languageItem);
			}
			dlg.dismiss();
		});
	}

	private void onVisibilityClick(ListItem<?> item){
		ArrayAdapter<CharSequence> adapter=new ArrayAdapter<>(getActivity(), R.layout.item_alert_single_choice_2lines_but_different, R.id.text, new String[]{
				getString(R.string.visibility_public),
				getString(R.string.visibility_unlisted),
				getString(R.string.visibility_followers_only)
		}){
			@NonNull
			@Override
			public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
				View view=super.getView(position, convertView, parent);
				TextView subtitle=view.findViewById(R.id.subtitle);
				subtitle.setText(switch(position){
					case 0 -> R.string.visibility_subtitle_public;
					case 1 -> R.string.visibility_subtitle_unlisted;
					case 2 -> R.string.visibility_subtitle_followers;
					default -> throw new IllegalStateException("Unexpected value: " + position);
				});
				return view;
			}
		};
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.default_post_visibility)
				.setSingleChoiceItems(adapter, (newVisibility==null ? visibility : newVisibility).ordinal(), (dialog, which)->{
					newVisibility=StatusPrivacy.values()[which];
					visibilityItem.subtitle=getVisibilitySubtitle();
					rebindItem(visibilityItem);
					updateQuotePolicyItem(newVisibility);
					dialog.dismiss();
				})
				.show();
	}

	private void onQuotePolicyClick(ListItem<?> item){
		M3AlertDialogBuilder alert=new M3AlertDialogBuilder(getActivity());
		if((newVisibility==null ? visibility : newVisibility)==StatusPrivacy.UNLISTED){
			alert.setSupportingText(R.string.quote_policy_for_unlisted_explanation);
		}
		alert.setTitle(R.string.compose_quote_policy)
				.setSingleChoiceItems(new String[]{
						getString(R.string.quote_policy_public),
						getString(R.string.quote_policy_followers),
						getString(R.string.quote_policy_nobody)
				}, (newQuotePolicy==null ? quotePolicy : newQuotePolicy).ordinal(), (dialog, which)->{
					newQuotePolicy=StatusQuotePolicy.values()[which];
					quotePolicyItem.subtitle=getQuotePolicySubtitle();
					rebindItem(quotePolicyItem);
					dialog.dismiss();
				})
				.show();
	}

	private void updateQuotePolicyItem(StatusPrivacy visibility){
		if(quotePolicyItem==null)
			return;
		if(visibility==StatusPrivacy.PRIVATE){
			quotePolicyItem.isEnabled=false;
			quotePolicyItem.subtitle=getString(R.string.quote_policy_for_private_explanation);
		}else{
			quotePolicyItem.isEnabled=true;
			quotePolicyItem.subtitle=getQuotePolicySubtitle();
		}
		rebindItem(quotePolicyItem);
	}
}
