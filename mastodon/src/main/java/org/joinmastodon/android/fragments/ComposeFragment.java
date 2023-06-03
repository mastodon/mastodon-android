package org.joinmastodon.android.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.icu.text.BreakIterator;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.twitter.twittertext.TwitterTextEmojiRegex;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.requests.accounts.GetPreferences;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.requests.statuses.EditStatus;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.events.StatusUpdatedEvent;
import org.joinmastodon.android.fragments.account_list.ComposeAccountSearchFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.EmojiCategory;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Mention;
import org.joinmastodon.android.model.Preferences;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.CustomEmojiPopupKeyboard;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.PopupKeyboard;
import org.joinmastodon.android.ui.drawables.SpoilerStripesDrawable;
import org.joinmastodon.android.ui.text.ComposeAutocompleteSpan;
import org.joinmastodon.android.ui.text.ComposeHashtagOrMentionSpan;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.viewcontrollers.ComposeAutocompleteViewController;
import org.joinmastodon.android.ui.viewcontrollers.ComposeLanguageAlertViewController;
import org.joinmastodon.android.ui.viewcontrollers.ComposeMediaViewController;
import org.joinmastodon.android.ui.viewcontrollers.ComposePollViewController;
import org.joinmastodon.android.ui.views.ComposeEditText;
import org.joinmastodon.android.ui.views.SizeListenerLinearLayout;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class ComposeFragment extends MastodonToolbarFragment implements OnBackPressedListener, ComposeEditText.SelectionListener{

	private static final int MEDIA_RESULT=717;
	public static final int IMAGE_DESCRIPTION_RESULT=363;
	private static final int AUTOCOMPLETE_ACCOUNT_RESULT=779;
	private static final String TAG="ComposeFragment";

	private static final Pattern MENTION_PATTERN=Pattern.compile("(^|[^\\/\\w])@(([a-z0-9_]+)@[a-z0-9\\.\\-]+[a-z0-9]+)", Pattern.CASE_INSENSITIVE);

	// from https://github.com/mastodon/mastodon-ios/blob/main/Mastodon/Helper/MastodonRegex.swift
	private static final Pattern AUTO_COMPLETE_PATTERN=Pattern.compile("(?<!\\w)(?:@([a-zA-Z0-9_]+)(@[a-zA-Z0-9_.-]+)?|#([^\\s.]+)|:([a-zA-Z0-9_]+))");
	private static final Pattern HIGHLIGHT_PATTERN=Pattern.compile("(?<!\\w)(?:@([a-zA-Z0-9_]+)(@[a-zA-Z0-9_.-]+)?|#([^\\s.]+))");

	@SuppressLint("NewApi") // this class actually exists on 6.0
	private final BreakIterator breakIterator=BreakIterator.getCharacterInstance();

	public LinearLayout mainLayout;
	private SizeListenerLinearLayout contentView;
	private TextView selfName, selfUsername;
	private ImageView selfAvatar;
	private Account self;
	private String instanceDomain;

	private ComposeEditText mainEditText;
	private TextView charCounter;
	private String accountID;
	private int charCount, charLimit, trimmedCharCount;

	private ImageButton mediaBtn, pollBtn, emojiBtn, spoilerBtn, languageBtn;
	private TextView replyText;
	private Button visibilityBtn;
	private LinearLayout bottomBar;
	private View autocompleteDivider;

	private List<EmojiCategory> customEmojis;
	private CustomEmojiPopupKeyboard emojiKeyboard;
	private Status replyTo;
	private String initialText;
	private String uuid;
	private EditText spoilerEdit;
	private View spoilerWrap;
	private boolean hasSpoiler;
	private ProgressBar sendProgress;
	private View sendingOverlay;
	private WindowManager wm;
	private StatusPrivacy statusVisibility=StatusPrivacy.PUBLIC;
	private ComposeAutocompleteSpan currentAutocompleteSpan;
	private FrameLayout mainEditTextWrap;
	private ComposeLanguageAlertViewController.SelectedOption postLang;

	private ComposeAutocompleteViewController autocompleteViewController;
	private ComposePollViewController pollViewController=new ComposePollViewController(this);
	private ComposeMediaViewController mediaViewController=new ComposeMediaViewController(this);
	public Instance instance;

	public Status editingStatus;
	private boolean creatingView;
	private boolean ignoreSelectionChanges=false;
	private MenuItem publishButton;
	private boolean wasDetached;

	private BackgroundColorSpan overLimitBG;
	private ForegroundColorSpan overLimitFG;

	public ComposeFragment(){
		super(R.layout.toolbar_fragment_with_progressbar);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		accountID=getArguments().getString("account");
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		self=session.self;
		instanceDomain=session.domain;
		customEmojis=AccountSessionManager.getInstance().getCustomEmojis(instanceDomain);
		instance=AccountSessionManager.getInstance().getInstanceInfo(instanceDomain);
		if(getArguments().containsKey("editStatus")){
			editingStatus=Parcels.unwrap(getArguments().getParcelable("editStatus"));
		}
		if(instance==null){
			Nav.finish(this);
			return;
		}
		if(customEmojis.isEmpty()){
			AccountSessionManager.getInstance().updateInstanceInfo(instanceDomain);
		}

		if(instance.maxTootChars>0)
			charLimit=instance.maxTootChars;
		else if(instance.configuration!=null && instance.configuration.statuses!=null && instance.configuration.statuses.maxCharacters>0)
			charLimit=instance.configuration.statuses.maxCharacters;
		else
			charLimit=500;

		setTitle(editingStatus==null ? R.string.new_post : R.string.edit_post);
		if(savedInstanceState!=null)
			postLang=Parcels.unwrap(savedInstanceState.getParcelable("postLang"));
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		mediaViewController.cancelAllUploads();
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
		wm=activity.getSystemService(WindowManager.class);

		overLimitBG=new BackgroundColorSpan(UiUtils.getThemeColor(activity, R.attr.colorM3ErrorContainer));
		overLimitFG=new ForegroundColorSpan(UiUtils.getThemeColor(activity, R.attr.colorM3Error));
	}

	@Override
	public void onDetach(){
		wasDetached=true;
		super.onDetach();
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		creatingView=true;
		emojiKeyboard=new CustomEmojiPopupKeyboard(getActivity(), customEmojis, instanceDomain);
		emojiKeyboard.setListener(new CustomEmojiPopupKeyboard.Listener(){
			@Override
			public void onEmojiSelected(Emoji emoji){
				onCustomEmojiClick(emoji);
			}

			@Override
			public void onBackspace(){
				getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
				getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
			}
		});

		View view=inflater.inflate(R.layout.fragment_compose, container, false);
		mainLayout=view.findViewById(R.id.compose_main_ll);
		mainEditText=view.findViewById(R.id.toot_text);
		mainEditTextWrap=view.findViewById(R.id.toot_text_wrap);
		charCounter=view.findViewById(R.id.char_counter);
		charCounter.setText(String.valueOf(charLimit));

		selfName=view.findViewById(R.id.name);
		selfUsername=view.findViewById(R.id.username);
		selfAvatar=view.findViewById(R.id.avatar);
		HtmlParser.setTextWithCustomEmoji(selfName, self.displayName, self.emojis);
		selfUsername.setText('@'+self.username+'@'+instanceDomain);
		ViewImageLoader.load(selfAvatar, null, new UrlImageLoaderRequest(self.avatar));
		ViewOutlineProvider roundCornersOutline=new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), V.dp(12));
			}
		};
		selfAvatar.setOutlineProvider(roundCornersOutline);
		selfAvatar.setClipToOutline(true);
		bottomBar=view.findViewById(R.id.bottom_bar);

		mediaBtn=view.findViewById(R.id.btn_media);
		pollBtn=view.findViewById(R.id.btn_poll);
		emojiBtn=view.findViewById(R.id.btn_emoji);
		spoilerBtn=view.findViewById(R.id.btn_spoiler);
		visibilityBtn=view.findViewById(R.id.btn_visibility);
		languageBtn=view.findViewById(R.id.btn_language);
		replyText=view.findViewById(R.id.reply_text);

		mediaBtn.setOnClickListener(v->openFilePicker());
		pollBtn.setOnClickListener(v->togglePoll());
		emojiBtn.setOnClickListener(v->emojiKeyboard.toggleKeyboardPopup(mainEditText));
		spoilerBtn.setOnClickListener(v->toggleSpoiler());
		languageBtn.setOnClickListener(v->showLanguageAlert());
		visibilityBtn.setOnClickListener(this::onVisibilityClick);
		Drawable arrow=getResources().getDrawable(R.drawable.ic_baseline_arrow_drop_down_18, getActivity().getTheme()).mutate();
		arrow.setTint(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurface));
		visibilityBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, arrow, null);
		emojiKeyboard.setOnIconChangedListener(new PopupKeyboard.OnIconChangeListener(){
			@Override
			public void onIconChanged(int icon){
				emojiBtn.setSelected(icon!=PopupKeyboard.ICON_HIDDEN);
				updateNavigationBarColor(icon!=PopupKeyboard.ICON_HIDDEN);
				if(autocompleteViewController.getMode()==ComposeAutocompleteViewController.Mode.EMOJIS){
					contentView.layout(contentView.getLeft(), contentView.getTop(), contentView.getRight(), contentView.getBottom());
					if(icon==PopupKeyboard.ICON_HIDDEN)
						showAutocomplete();
					else
						hideAutocomplete();
				}
			}
		});

		contentView=(SizeListenerLinearLayout) view;
		contentView.addView(emojiKeyboard.getView());

		spoilerEdit=view.findViewById(R.id.content_warning);
		spoilerWrap=view.findViewById(R.id.content_warning_wrap);
		LayerDrawable spoilerBg=(LayerDrawable) spoilerWrap.getBackground().mutate();
		spoilerBg.setDrawableByLayerId(R.id.left_drawable, new SpoilerStripesDrawable(false));
		spoilerBg.setDrawableByLayerId(R.id.right_drawable, new SpoilerStripesDrawable(false));
		spoilerWrap.setBackground(spoilerBg);
		spoilerWrap.setClipToOutline(true);
		spoilerWrap.setOutlineProvider(OutlineProviders.roundedRect(8));
		if((savedInstanceState!=null && savedInstanceState.getBoolean("hasSpoiler", false)) || hasSpoiler){
			hasSpoiler=true;
			spoilerWrap.setVisibility(View.VISIBLE);
			spoilerBtn.setSelected(true);
		}else if(editingStatus!=null && !TextUtils.isEmpty(editingStatus.spoilerText)){
			hasSpoiler=true;
			spoilerWrap.setVisibility(View.VISIBLE);
			spoilerEdit.setText(getArguments().getString("sourceSpoiler", editingStatus.spoilerText));
			spoilerBtn.setSelected(true);
		}

		if(editingStatus!=null && editingStatus.visibility!=null) {
			statusVisibility=editingStatus.visibility;
		}
		updateVisibilityIcon();

		autocompleteViewController=new ComposeAutocompleteViewController(getActivity(), accountID);
		autocompleteViewController.setCompletionSelectedListener(new ComposeAutocompleteViewController.AutocompleteListener(){
			@Override
			public void onCompletionSelected(String completion){
				onAutocompleteOptionSelected(completion);
			}

			@Override
			public void onSetEmojiPanelOpen(boolean open){
				if(open!=emojiKeyboard.isVisible())
					emojiKeyboard.toggleKeyboardPopup(mainEditText);
			}

			@Override
			public void onLaunchAccountSearch(){
				Bundle args=new Bundle();
				args.putString("account", accountID);
				Nav.goForResult(getActivity(), ComposeAccountSearchFragment.class, args, AUTOCOMPLETE_ACCOUNT_RESULT, ComposeFragment.this);
			}
		});
		View autocompleteView=autocompleteViewController.getView();
		autocompleteView.setVisibility(View.INVISIBLE);
		bottomBar.addView(autocompleteView, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(56)));
		autocompleteDivider=view.findViewById(R.id.bottom_bar_autocomplete_divider);

		pollViewController.setView(view, savedInstanceState);
		mediaViewController.setView(view, savedInstanceState);

		creatingView=false;

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		pollViewController.onSaveInstanceState(outState);
		mediaViewController.onSaveInstanceState(outState);
		outState.putBoolean("hasSpoiler", hasSpoiler);
		outState.putSerializable("visibility", statusVisibility);
		outState.putParcelable("postLang", Parcels.wrap(postLang));
		if(currentAutocompleteSpan!=null){
			Editable e=mainEditText.getText();
			outState.putInt("autocompleteStart", e.getSpanStart(currentAutocompleteSpan));
			outState.putInt("autocompleteEnd", e.getSpanEnd(currentAutocompleteSpan));
		}
	}

	@Override
	public void onResume(){
		super.onResume();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		if(editingStatus==null)
			loadDefaultStatusVisibility(savedInstanceState);
		contentView.setSizeListener(emojiKeyboard::onContentViewSizeChanged);
		InputMethodManager imm=getActivity().getSystemService(InputMethodManager.class);
		mainEditText.requestFocus();
		view.postDelayed(()->{
			imm.showSoftInput(mainEditText, 0);
		}, 100);
		sendProgress=view.findViewById(R.id.progress);
		sendProgress.setVisibility(View.GONE);

		mainEditText.setSelectionListener(this);
		mainEditText.addTextChangedListener(new TextWatcher(){
			private int lastChangeStart, lastChangeCount;

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){
				if(s.length()==0)
					return;
				lastChangeStart=start;
				lastChangeCount=count;
			}

			@Override
			public void afterTextChanged(Editable s){
				if(s.length()==0){
					updateCharCounter();
					return;
				}
				int start=lastChangeStart;
				int count=lastChangeCount;
				// offset one char back to catch an already typed '@' or '#' or ':'
				int realStart=start;
				start=Math.max(0, start-1);
				CharSequence changedText=s.subSequence(start, realStart+count);
				String raw=changedText.toString();
				Editable editable=(Editable) s;
				// 1. find mentions, hashtags, and emoji shortcodes in any freshly inserted text, and put spans over them
				if(raw.contains("@") || raw.contains("#") || raw.contains(":")){
					Matcher matcher=AUTO_COMPLETE_PATTERN.matcher(changedText);
					while(matcher.find()){
						if(editable.getSpans(start+matcher.start(), start+matcher.end(), ComposeAutocompleteSpan.class).length>0)
							continue;
						ComposeAutocompleteSpan span;
						if(TextUtils.isEmpty(matcher.group(4))){ // not an emoji
							span=new ComposeHashtagOrMentionSpan();
						}else{
							span=new ComposeAutocompleteSpan();
						}
						editable.setSpan(span, start+matcher.start(), start+matcher.end(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
					}
				}
				// 2. go over existing spans in the affected range, adjust end offsets and remove no longer valid spans
				ComposeAutocompleteSpan[] spans=editable.getSpans(realStart, realStart+count, ComposeAutocompleteSpan.class);
				for(ComposeAutocompleteSpan span:spans){
					int spanStart=editable.getSpanStart(span);
					int spanEnd=editable.getSpanEnd(span);
					if(spanStart==spanEnd){ // empty, remove
						editable.removeSpan(span);
						continue;
					}
					char firstChar=editable.charAt(spanStart);
					String spanText=s.subSequence(spanStart, spanEnd).toString();
					if(firstChar=='@' || firstChar=='#' || firstChar==':'){
						Matcher matcher=AUTO_COMPLETE_PATTERN.matcher(spanText);
						char prevChar=spanStart>0 ? editable.charAt(spanStart-1) : ' ';
						if(!matcher.find() || !Character.isWhitespace(prevChar)){ // invalid mention, remove
							editable.removeSpan(span);
							continue;
						}else if(matcher.end()+spanStart<spanEnd){ // mention with something at the end, move the end offset
							editable.setSpan(span, spanStart, spanStart+matcher.end(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
						}
					}else{
						editable.removeSpan(span);
					}
				}

				updateCharCounter();
			}
		});
		spoilerEdit.addTextChangedListener(new SimpleTextWatcher(e->updateCharCounter()));
		if(replyTo!=null){
			replyText.setText(getString(R.string.in_reply_to, replyTo.account.displayName));
			ArrayList<String> mentions=new ArrayList<>();
			String ownID=AccountSessionManager.getInstance().getAccount(accountID).self.id;
			if(!replyTo.account.id.equals(ownID))
				mentions.add('@'+replyTo.account.acct);
			for(Mention mention:replyTo.mentions){
				if(mention.id.equals(ownID))
					continue;
				String m='@'+mention.acct;
				if(!mentions.contains(m))
					mentions.add(m);
			}
			initialText=mentions.isEmpty() ? "" : TextUtils.join(" ", mentions)+" ";
			if(savedInstanceState==null){
				mainEditText.setText(initialText);
				ignoreSelectionChanges=true;
				mainEditText.setSelection(mainEditText.length());
				ignoreSelectionChanges=false;
				if(!TextUtils.isEmpty(replyTo.spoilerText) && AccountSessionManager.getInstance().isSelf(accountID, replyTo.account)){
					hasSpoiler=true;
					spoilerWrap.setVisibility(View.VISIBLE);
					spoilerEdit.setText(replyTo.spoilerText);
					spoilerBtn.setSelected(true);
				}
			}
		}else{
			replyText.setVisibility(View.GONE);
		}
		if(savedInstanceState==null){
			if(editingStatus!=null){
				initialText=getArguments().getString("sourceText", "");
				mainEditText.setText(initialText);
				ignoreSelectionChanges=true;
				mainEditText.setSelection(mainEditText.length());
				ignoreSelectionChanges=false;
				mediaViewController.onViewCreated(savedInstanceState);;
			}else{
				String prefilledText=getArguments().getString("prefilledText");
				if(!TextUtils.isEmpty(prefilledText)){
					mainEditText.setText(prefilledText);
					ignoreSelectionChanges=true;
					mainEditText.setSelection(mainEditText.length());
					ignoreSelectionChanges=false;
					initialText=prefilledText;
				}
				ArrayList<Uri> mediaUris=getArguments().getParcelableArrayList("mediaAttachments");
				if(mediaUris!=null && !mediaUris.isEmpty()){
					for(Uri uri:mediaUris){
						mediaViewController.addMediaAttachment(uri, null);
					}
				}
			}
		}

		if(editingStatus!=null){
			updateCharCounter();
			visibilityBtn.setEnabled(false);
		}
		updateMediaPollStates();
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState){
		super.onViewStateRestored(savedInstanceState);
		if(savedInstanceState!=null && savedInstanceState.containsKey("autocompleteStart")){
			int start=savedInstanceState.getInt("autocompleteStart"), end=savedInstanceState.getInt("autocompleteEnd");
			currentAutocompleteSpan=new ComposeAutocompleteSpan();
			mainEditText.getText().setSpan(currentAutocompleteSpan, start, end, Editable.SPAN_EXCLUSIVE_INCLUSIVE);
			startAutocomplete(currentAutocompleteSpan);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(editingStatus==null ? R.menu.compose : R.menu.compose_edit, menu);
		publishButton=menu.findItem(R.id.publish);
		updatePublishButtonState();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getItemId()==R.id.publish){
			if(GlobalUserPreferences.altTextReminders)
				checkAltTextsAndPublish();
			else
				publish();
		}
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		emojiKeyboard.onConfigurationChanged();
	}

	@SuppressLint("NewApi")
	private void updateCharCounter(){
		Editable text=mainEditText.getText();

		String countableText=TwitterTextEmojiRegex.VALID_EMOJI_PATTERN.matcher(
				MENTION_PATTERN.matcher(
						HtmlParser.URL_PATTERN.matcher(text).replaceAll("$2xxxxxxxxxxxxxxxxxxxxxxx")
				).replaceAll("$1@$3")
		).replaceAll("x");
		charCount=0;
		breakIterator.setText(countableText);
		while(breakIterator.next()!=BreakIterator.DONE){
			charCount++;
		}

		if(hasSpoiler){
			charCount+=spoilerEdit.length();
		}
		charCounter.setText(String.valueOf(charLimit-charCount));

		text.removeSpan(overLimitBG);
		text.removeSpan(overLimitFG);
		if(charCount>charLimit){
			charCounter.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Error));
			int start=text.length()-(charCount-charLimit);
			int end=text.length();
			text.setSpan(overLimitFG, start, end, 0);
			text.setSpan(overLimitBG, start, end, 0);
		}else{
			charCounter.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurface));
		}

		trimmedCharCount=text.toString().trim().length();
		updatePublishButtonState();
	}

	public void updatePublishButtonState(){
		uuid=null;
		if(publishButton==null)
			return;
		publishButton.setEnabled((trimmedCharCount>0 || !mediaViewController.isEmpty()) && charCount<=charLimit && mediaViewController.getNonDoneAttachmentCount()==0 && (pollViewController.isEmpty() || pollViewController.getNonEmptyOptionsCount()>1));
	}

	private void onCustomEmojiClick(Emoji emoji){
		if(getActivity().getCurrentFocus() instanceof EditText edit){
			if(edit==mainEditText && currentAutocompleteSpan!=null && autocompleteViewController.getMode()==ComposeAutocompleteViewController.Mode.EMOJIS){
				Editable text=mainEditText.getText();
				int start=text.getSpanStart(currentAutocompleteSpan);
				int end=text.getSpanEnd(currentAutocompleteSpan);
				finishAutocomplete();
				text.replace(start, end, ':'+emoji.shortcode+':');
				return;
			}
			int start=edit.getSelectionStart();
			String prefix=start>0 && !Character.isWhitespace(edit.getText().charAt(start-1)) ? " :" : ":";
			edit.getText().replace(start, edit.getSelectionEnd(), prefix+emoji.shortcode+':');
		}
	}

	@Override
	protected void updateToolbar(){
		super.updateToolbar();
		int color=UiUtils.alphaBlendThemeColors(getActivity(), R.attr.colorM3Background, R.attr.colorM3Primary, 0.11f);
		getToolbar().setBackgroundColor(color);
		setStatusBarColor(color);
		bottomBar.setBackgroundColor(color);
		updateNavigationBarColor(emojiKeyboard.isVisible());
	}

	private void updateNavigationBarColor(boolean emojiKeyboardVisible){
		int color=UiUtils.alphaBlendThemeColors(getActivity(), R.attr.colorM3Background, R.attr.colorM3Primary, emojiKeyboardVisible ? 0.08f : 0.11f);
		setNavigationBarColor(color);
	}

	@Override
	protected int getNavigationIconDrawableResource(){
		return R.drawable.ic_baseline_close_24;
	}

	@Override
	public boolean wantsCustomNavigationIcon(){
		return true;
	}

	private void checkAltTextsAndPublish(){
		int count=mediaViewController.getMissingAltTextAttachmentCount();
		if(count==0){
			publish();
		}else{
			String msg=getResources().getQuantityString(mediaViewController.areAllAttachmentsImages() ? R.plurals.alt_text_reminder_x_images : R.plurals.alt_text_reminder_x_attachments,
					count, switch(count){
						case 1 -> getString(R.string.count_one);
						case 2 -> getString(R.string.count_two);
						case 3 -> getString(R.string.count_three);
						case 4 -> getString(R.string.count_four);
						default -> String.valueOf(count);
					});
			new M3AlertDialogBuilder(getActivity())
					.setTitle(R.string.alt_text_reminder_title)
					.setMessage(msg)
					.setPositiveButton(R.string.alt_text_reminder_post_anyway, (dlg, item)->publish())
					.setNegativeButton(R.string.cancel, null)
					.show();
		}
	}

	private void publish(){
		sendingOverlay=new View(getActivity());
		WindowManager.LayoutParams overlayParams=new WindowManager.LayoutParams();
		overlayParams.type=WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
		overlayParams.flags=WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
		overlayParams.width=overlayParams.height=WindowManager.LayoutParams.MATCH_PARENT;
		overlayParams.format=PixelFormat.TRANSLUCENT;
		overlayParams.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED;
		overlayParams.token=mainEditText.getWindowToken();
		wm.addView(sendingOverlay, overlayParams);

		publishButton.setEnabled(false);
		V.setVisibilityAnimated(sendProgress, View.VISIBLE);

		mediaViewController.saveAltTextsBeforePublishing(this::actuallyPublish, this::handlePublishError);
	}

	private void actuallyPublish(){
		String text=mainEditText.getText().toString();
		CreateStatus.Request req=new CreateStatus.Request();
		req.status=text;
		req.visibility=statusVisibility;
		if(!mediaViewController.isEmpty()){
			req.mediaIds=mediaViewController.getAttachmentIDs();
		}
		if(replyTo!=null){
			req.inReplyToId=replyTo.id;
		}
		if(!pollViewController.isEmpty()){
			req.poll=pollViewController.getPollForRequest();
		}
		if(hasSpoiler && spoilerEdit.length()>0){
			req.spoilerText=spoilerEdit.getText().toString();
		}
		if(postLang!=null){
			req.language=postLang.locale.toLanguageTag();
		}
		if(uuid==null)
			uuid=UUID.randomUUID().toString();

		Callback<Status> resCallback=new Callback<>(){
			@Override
			public void onSuccess(Status result){
				wm.removeView(sendingOverlay);
				sendingOverlay=null;
				if(editingStatus==null){
					E.post(new StatusCreatedEvent(result, accountID));
					if(replyTo!=null){
						replyTo.repliesCount++;
						E.post(new StatusCountersUpdatedEvent(replyTo));
					}
				}else{
					E.post(new StatusUpdatedEvent(result));
				}
				Nav.finish(ComposeFragment.this);
			}

			@Override
			public void onError(ErrorResponse error){
				handlePublishError(error);
			}
		};

		if(editingStatus!=null){
			new EditStatus(req, editingStatus.id)
					.setCallback(resCallback)
					.exec(accountID);
		}else{
			new CreateStatus(req, uuid)
					.setCallback(resCallback)
					.exec(accountID);
		}
	}

	private void handlePublishError(ErrorResponse error){
		wm.removeView(sendingOverlay);
		sendingOverlay=null;
		V.setVisibilityAnimated(sendProgress, View.GONE);
		publishButton.setEnabled(true);
		if(error instanceof MastodonErrorResponse me){
			new M3AlertDialogBuilder(getActivity())
					.setTitle(R.string.post_failed)
					.setMessage(me.error)
					.setPositiveButton(R.string.retry, (dlg, btn)->publish())
					.setNegativeButton(R.string.cancel, null)
					.show();
		}else{
			error.showToast(getActivity());
		}
	}

	private boolean hasDraft(){
		if(editingStatus!=null){
			if(!mainEditText.getText().toString().equals(initialText))
				return true;
			List<String> existingMediaIDs=editingStatus.mediaAttachments.stream().map(a->a.id).collect(Collectors.toList());
			if(!existingMediaIDs.equals(mediaViewController.getAttachmentIDs()))
				return true;
			return pollViewController.isPollChanged();
		}
		boolean pollFieldsHaveContent=pollViewController.getNonEmptyOptionsCount()>0;
		return (mainEditText.length()>0 && !mainEditText.getText().toString().equals(initialText)) || !mediaViewController.isEmpty() || pollFieldsHaveContent;
	}

	@Override
	public boolean onBackPressed(){
		if(emojiKeyboard.isVisible()){
			emojiKeyboard.hide();
			return true;
		}
		if(hasDraft()){
			confirmDiscardDraftAndFinish();
			return true;
		}
		if(sendingOverlay!=null)
			return true;
		return false;
	}

	@Override
	public void onToolbarNavigationClick(){
		if(hasDraft()){
			confirmDiscardDraftAndFinish();
		}else{
			super.onToolbarNavigationClick();
		}
	}

	@Override
	public void onFragmentResult(int reqCode, boolean success, Bundle result){
		if(reqCode==IMAGE_DESCRIPTION_RESULT && success){
			String attID=result.getString("attachment");
			String text=result.getString("text");
			mediaViewController.setAltTextByID(attID, text);
		}else if(reqCode==AUTOCOMPLETE_ACCOUNT_RESULT && success){
			Account acc=Parcels.unwrap(result.getParcelable("selectedAccount"));
			if(currentAutocompleteSpan==null)
				return;
			Editable e=mainEditText.getText();
			int start=e.getSpanStart(currentAutocompleteSpan);
			int end=e.getSpanEnd(currentAutocompleteSpan);
			e.removeSpan(currentAutocompleteSpan);
			e.replace(start, end, '@'+acc.acct+' ');
			finishAutocomplete();
		}
	}

	private void confirmDiscardDraftAndFinish(){
		new M3AlertDialogBuilder(getActivity())
				.setTitle(editingStatus==null ? R.string.discard_draft : R.string.discard_changes)
				.setPositiveButton(R.string.discard, (dialog, which)->Nav.finish(this))
				.setNegativeButton(R.string.cancel, null)
				.show();
	}


	/**
	 * Builds the correct intent for the device version to select media.
	 *
	 * <p>For Device version > T or R_SDK_v2, use the android platform photopicker via
	 * {@link MediaStore#ACTION_PICK_IMAGES}
	 *
	 * <p>For earlier versions use the built in docs ui via {@link Intent#ACTION_GET_CONTENT}
	 */
	private void openFilePicker(){
		Intent intent;
		boolean usePhotoPicker=UiUtils.isPhotoPickerAvailable();
		if(usePhotoPicker){
			intent=new Intent(MediaStore.ACTION_PICK_IMAGES);
			intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, mediaViewController.getMaxAttachments()-mediaViewController.getMediaAttachmentsCount());
		}else{
			intent=new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("*/*");
		}
		if(!usePhotoPicker && instance.configuration!=null &&
				instance.configuration.mediaAttachments!=null &&
				instance.configuration.mediaAttachments.supportedMimeTypes!=null &&
				!instance.configuration.mediaAttachments.supportedMimeTypes.isEmpty()){
			intent.putExtra(Intent.EXTRA_MIME_TYPES,
					instance.configuration.mediaAttachments.supportedMimeTypes.toArray(
							new String[0]));
		}else{
			if(!usePhotoPicker){
				// If photo picker is being used these are the default mimetypes.
				intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
			}
		}
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		startActivityForResult(intent, MEDIA_RESULT);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode==MEDIA_RESULT && resultCode==Activity.RESULT_OK){
			Uri single=data.getData();
			if(single!=null){
				mediaViewController.addMediaAttachment(single, null);
			}else{
				ClipData clipData=data.getClipData();
				for(int i=0;i<clipData.getItemCount();i++){
					mediaViewController.addMediaAttachment(clipData.getItemAt(i).getUri(), null);
				}
			}
		}
	}


	public void updateMediaPollStates(){
		pollBtn.setSelected(pollViewController.isShown());
		mediaBtn.setEnabled(!pollViewController.isShown() && mediaViewController.canAddMoreAttachments());
		pollBtn.setEnabled(mediaViewController.isEmpty());
	}

	private void togglePoll(){
		pollViewController.toggle();
		updatePublishButtonState();
		updateMediaPollStates();
	}

	private void toggleSpoiler(){
		hasSpoiler=!hasSpoiler;
		if(hasSpoiler){
			spoilerWrap.setVisibility(View.VISIBLE);
			spoilerBtn.setSelected(true);
			spoilerEdit.requestFocus();
		}else{
			spoilerWrap.setVisibility(View.GONE);
			spoilerEdit.setText("");
			spoilerBtn.setSelected(false);
			mainEditText.requestFocus();
			updateCharCounter();
		}
	}

	private void onVisibilityClick(View v){
		PopupMenu menu=new PopupMenu(getActivity(), v);
		menu.inflate(R.menu.compose_visibility);
		menu.setOnMenuItemClickListener(item->{
			int id=item.getItemId();
			if(id==R.id.vis_public){
				statusVisibility=StatusPrivacy.PUBLIC;
			}else if(id==R.id.vis_followers){
				statusVisibility=StatusPrivacy.PRIVATE;
			}else if(id==R.id.vis_private){
				statusVisibility=StatusPrivacy.DIRECT;
			}
			item.setChecked(true);
			updateVisibilityIcon();
			return true;
		});
		menu.show();
	}

	private void loadDefaultStatusVisibility(Bundle savedInstanceState){
		if(getArguments().containsKey("replyTo")){
			replyTo=Parcels.unwrap(getArguments().getParcelable("replyTo"));
			statusVisibility=replyTo.visibility;
		}

		// A saved privacy setting from a previous compose session wins over the reply visibility
		if(savedInstanceState!=null){
			statusVisibility=(StatusPrivacy) savedInstanceState.getSerializable("visibility");
		}

		Preferences prevPrefs=AccountSessionManager.getInstance().getAccount(accountID).preferences;
		if(prevPrefs!=null){
			applyPreferencesForPostVisibility(prevPrefs, savedInstanceState);
		}
		AccountSessionManager.getInstance().getAccount(accountID).reloadPreferences(prefs->{
			applyPreferencesForPostVisibility(prefs, savedInstanceState);
		});
	}

	private void applyPreferencesForPostVisibility(Preferences prefs, Bundle savedInstanceState){
		// Only override the reply visibility if our preference is more private
		if(prefs.postingDefaultVisibility.isLessVisibleThan(statusVisibility)){
			// Map unlisted from the API onto public, because we don't have unlisted in the UI
			statusVisibility=switch(prefs.postingDefaultVisibility){
				case PUBLIC, UNLISTED -> StatusPrivacy.PUBLIC;
				case PRIVATE -> StatusPrivacy.PRIVATE;
				case DIRECT -> StatusPrivacy.DIRECT;
			};
		}

		// A saved privacy setting from a previous compose session wins over all
		if(savedInstanceState!=null){
			statusVisibility=(StatusPrivacy) savedInstanceState.getSerializable("visibility");
		}

		updateVisibilityIcon();
	}

	private void updateVisibilityIcon(){
		if(statusVisibility==null){ // TODO find out why this happens
			statusVisibility=StatusPrivacy.PUBLIC;
		}
		visibilityBtn.setText(switch(statusVisibility){
			case PUBLIC, UNLISTED -> R.string.visibility_public;
			case PRIVATE -> R.string.visibility_followers_only;
			case DIRECT -> R.string.visibility_private;
		});
		Drawable icon=getResources().getDrawable(switch(statusVisibility){
			case PUBLIC, UNLISTED -> R.drawable.ic_public_20px;
			case PRIVATE -> R.drawable.ic_group_20px;
			case DIRECT -> R.drawable.ic_alternate_email_20px;
		}, getActivity().getTheme()).mutate();
		icon.setBounds(0, 0, V.dp(18), V.dp(18));
		icon.setTint(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));
		visibilityBtn.setCompoundDrawablesRelative(icon, null, visibilityBtn.getCompoundDrawablesRelative()[2], null);
	}

	@Override
	public void onSelectionChanged(int start, int end){
		if(ignoreSelectionChanges)
			return;
		if(start==end && mainEditText.length()>0){
			ComposeAutocompleteSpan[] spans=mainEditText.getText().getSpans(start, end, ComposeAutocompleteSpan.class);
			if(spans.length>0){
				assert spans.length==1;
				ComposeAutocompleteSpan span=spans[0];
				if(currentAutocompleteSpan==null && end==mainEditText.getText().getSpanEnd(span)){
					startAutocomplete(span);
				}else if(currentAutocompleteSpan!=null){
					Editable e=mainEditText.getText();
					String spanText=e.toString().substring(e.getSpanStart(span), e.getSpanEnd(span));
					autocompleteViewController.setText(spanText);
				}
			}else if(currentAutocompleteSpan!=null){
				finishAutocomplete();
			}
		}else if(currentAutocompleteSpan!=null){
			finishAutocomplete();
		}
	}

	@Override
	public String[] onGetAllowedMediaMimeTypes(){
		if(instance!=null && instance.configuration!=null && instance.configuration.mediaAttachments!=null && instance.configuration.mediaAttachments.supportedMimeTypes!=null)
			return instance.configuration.mediaAttachments.supportedMimeTypes.toArray(new String[0]);
		return new String[]{"image/jpeg", "image/gif", "image/png", "video/mp4"};
	}

	@Override
	public boolean onAddMediaAttachmentFromEditText(Uri uri, String description){
		return mediaViewController.addMediaAttachment(uri, description);
	}

	private void startAutocomplete(ComposeAutocompleteSpan span){
		currentAutocompleteSpan=span;
		Editable e=mainEditText.getText();
		String spanText=e.toString().substring(e.getSpanStart(span), e.getSpanEnd(span));
		autocompleteViewController.setText(spanText);
		showAutocomplete();
	}

	private void finishAutocomplete(){
		if(currentAutocompleteSpan==null)
			return;
		autocompleteViewController.setText(null);
		currentAutocompleteSpan=null;
		hideAutocomplete();
	}

	private void showAutocomplete(){
		UiUtils.beginLayoutTransition(bottomBar);
		View autocompleteView=autocompleteViewController.getView();
		bottomBar.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;
		bottomBar.requestLayout();
		autocompleteView.setVisibility(View.VISIBLE);
		autocompleteDivider.setVisibility(View.VISIBLE);
	}

	private void hideAutocomplete(){
		UiUtils.beginLayoutTransition(bottomBar);
		bottomBar.getLayoutParams().height=V.dp(48);
		bottomBar.requestLayout();
		autocompleteViewController.getView().setVisibility(View.INVISIBLE);
		autocompleteDivider.setVisibility(View.INVISIBLE);
	}

	private void onAutocompleteOptionSelected(String text){
		Editable e=mainEditText.getText();
		int start=e.getSpanStart(currentAutocompleteSpan);
		int end=e.getSpanEnd(currentAutocompleteSpan);
		e.replace(start, end, text+" ");
		finishAutocomplete();
		InputConnection conn=mainEditText.getCurrentInputConnection();
		if(conn!=null)
			conn.finishComposingText();
	}

	@Override
	public CharSequence getTitle(){
		return getString(R.string.new_post);
	}

	@Override
	public boolean wantsLightStatusBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	public boolean wantsLightNavigationBar(){
		return !UiUtils.isDarkTheme();
	}

	public boolean getWasDetached(){
		return wasDetached;
	}

	public boolean isCreatingView(){
		return creatingView;
	}

	public String getAccountID(){
		return accountID;
	}

	public void addFakeMediaAttachment(Uri uri, String description){
		mediaViewController.addFakeMediaAttachment(uri, description);
	}

	private void showLanguageAlert(){
		Preferences prefs=AccountSessionManager.getInstance().getAccount(accountID).preferences;
		ComposeLanguageAlertViewController vc=new ComposeLanguageAlertViewController(getActivity(), prefs!=null ? prefs.postingDefaultLanguage : null, postLang, mainEditText.getText().toString());
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.language)
				.setView(vc.getView())
				.setPositiveButton(R.string.ok, (dialog, which)->setPostLanguage(vc.getSelectedOption()))
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void setPostLanguage(ComposeLanguageAlertViewController.SelectedOption language){
		postLang=language;
	}
}
