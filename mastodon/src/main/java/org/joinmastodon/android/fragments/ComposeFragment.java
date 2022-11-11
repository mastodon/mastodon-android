package org.joinmastodon.android.fragments;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.LayerDrawable;
import android.icu.text.BreakIterator;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
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
import android.widget.Toast;

import com.twitter.twittertext.Regex;
import com.twitter.twittertext.TwitterTextEmojiRegex;

import org.joinmastodon.android.E;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.ProgressListener;
import org.joinmastodon.android.api.requests.accounts.GetPreferences;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.requests.statuses.EditStatus;
import org.joinmastodon.android.api.requests.statuses.GetAttachmentByID;
import org.joinmastodon.android.api.requests.statuses.UploadAttachment;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.events.StatusUpdatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.EmojiCategory;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Mention;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.Preferences;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.ComposeAutocompleteViewController;
import org.joinmastodon.android.ui.CustomEmojiPopupKeyboard;
import org.joinmastodon.android.ui.PopupKeyboard;
import org.joinmastodon.android.ui.drawables.SpoilerStripesDrawable;
import org.joinmastodon.android.ui.text.ComposeAutocompleteSpan;
import org.joinmastodon.android.ui.text.ComposeHashtagOrMentionSpan;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.TransferSpeedTracker;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.ComposeEditText;
import org.joinmastodon.android.ui.views.ComposeMediaLayout;
import org.joinmastodon.android.ui.views.ReorderableLinearLayout;
import org.joinmastodon.android.ui.views.SizeListenerLinearLayout;
import org.parceler.Parcel;
import org.parceler.Parcels;

import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
	private static final int IMAGE_DESCRIPTION_RESULT=363;
	private static final int MAX_ATTACHMENTS=4;
	private static final String TAG="ComposeFragment";

	private static final Pattern MENTION_PATTERN=Pattern.compile("(^|[^\\/\\w])@(([a-z0-9_]+)@[a-z0-9\\.\\-]+[a-z0-9]+)", Pattern.CASE_INSENSITIVE);

	// from https://github.com/mastodon/mastodon-ios/blob/main/Mastodon/Helper/MastodonRegex.swift
	private static final Pattern AUTO_COMPLETE_PATTERN=Pattern.compile("(?<!\\w)(?:@([a-zA-Z0-9_]+)(@[a-zA-Z0-9_.-]+)?|#([^\\s.]+)|:([a-zA-Z0-9_]+))");
	private static final Pattern HIGHLIGHT_PATTERN=Pattern.compile("(?<!\\w)(?:@([a-zA-Z0-9_]+)(@[a-zA-Z0-9_.-]+)?|#([^\\s.]+))");

	private static final String VALID_URL_PATTERN_STRING =
					"(" +                                                            //  $1 total match
						"(" + Regex.URL_VALID_PRECEDING_CHARS + ")" +                        //  $2 Preceding character
						"(" +                                                          //  $3 URL
						"(https?://)" +                                             //  $4 Protocol (optional)
						"(" + Regex.URL_VALID_DOMAIN + ")" +                               //  $5 Domain(s)
						"(?::(" + Regex.URL_VALID_PORT_NUMBER + "))?" +                    //  $6 Port number (optional)
						"(/" +
						Regex.URL_VALID_PATH + "*+" +
						")?" +                                                       //  $7 URL Path and anchor
						"(\\?" + Regex.URL_VALID_URL_QUERY_CHARS + "*" +                   //  $8 Query String
						Regex.URL_VALID_URL_QUERY_ENDING_CHARS + ")?" +
						")" +
					")";
	private static final Pattern URL_PATTERN=Pattern.compile(VALID_URL_PATTERN_STRING, Pattern.CASE_INSENSITIVE);
	@SuppressLint("NewApi") // this class actually exists on 6.0
	private final BreakIterator breakIterator=BreakIterator.getCharacterInstance();

	private SizeListenerLinearLayout contentView;
	private TextView selfName, selfUsername;
	private ImageView selfAvatar;
	private Account self;
	private String instanceDomain;

	private ComposeEditText mainEditText;
	private TextView charCounter;
	private String accountID;
	private int charCount, charLimit, trimmedCharCount;

	private Button publishButton;
	private ImageButton mediaBtn, pollBtn, emojiBtn, spoilerBtn, visibilityBtn;
	private ComposeMediaLayout attachmentsView;
	private TextView replyText;
	private ReorderableLinearLayout pollOptionsView;
	private View pollWrap;
	private View addPollOptionBtn;
	private TextView pollDurationView;

	private ArrayList<DraftPollOption> pollOptions=new ArrayList<>();

	private ArrayList<DraftMediaAttachment> attachments=new ArrayList<>();

	private List<EmojiCategory> customEmojis;
	private CustomEmojiPopupKeyboard emojiKeyboard;
	private Status replyTo;
	private String initialText;
	private String uuid;
	private int pollDuration=24*3600;
	private String pollDurationStr;
	private EditText spoilerEdit;
	private boolean hasSpoiler;
	private ProgressBar sendProgress;
	private ImageView sendError;
	private View sendingOverlay;
	private WindowManager wm;
	private StatusPrivacy statusVisibility=StatusPrivacy.PUBLIC;
	private ComposeAutocompleteSpan currentAutocompleteSpan;
	private FrameLayout mainEditTextWrap;
	private ComposeAutocompleteViewController autocompleteViewController;
	private Instance instance;
	private boolean attachmentsErrorShowing;

	private Status editingStatus;
	private boolean pollChanged;
	private boolean creatingView;
	private boolean ignoreSelectionChanges=false;
	private Runnable updateUploadEtaRunnable;

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

		loadDefaultStatusVisibility(savedInstanceState);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		for(DraftMediaAttachment att:attachments){
			if(att.isUploadingOrProcessing())
				att.cancelUpload();
		}
		if(updateUploadEtaRunnable!=null){
			UiUtils.removeCallbacks(updateUploadEtaRunnable);
			updateUploadEtaRunnable=null;
		}
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
		wm=activity.getSystemService(WindowManager.class);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		creatingView=true;
		emojiKeyboard=new CustomEmojiPopupKeyboard(getActivity(), customEmojis, instanceDomain);
		emojiKeyboard.setListener(this::onCustomEmojiClick);

		View view=inflater.inflate(R.layout.fragment_compose, container, false);
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

		mediaBtn=view.findViewById(R.id.btn_media);
		pollBtn=view.findViewById(R.id.btn_poll);
		emojiBtn=view.findViewById(R.id.btn_emoji);
		spoilerBtn=view.findViewById(R.id.btn_spoiler);
		visibilityBtn=view.findViewById(R.id.btn_visibility);
		replyText=view.findViewById(R.id.reply_text);

		mediaBtn.setOnClickListener(v->openFilePicker());
		pollBtn.setOnClickListener(v->togglePoll());
		emojiBtn.setOnClickListener(v->emojiKeyboard.toggleKeyboardPopup(mainEditText));
		spoilerBtn.setOnClickListener(v->toggleSpoiler());
		visibilityBtn.setOnClickListener(this::onVisibilityClick);
		emojiKeyboard.setOnIconChangedListener(new PopupKeyboard.OnIconChangeListener(){
			@Override
			public void onIconChanged(int icon){
				emojiBtn.setSelected(icon!=PopupKeyboard.ICON_HIDDEN);
			}
		});

		contentView=(SizeListenerLinearLayout) view;
		contentView.addView(emojiKeyboard.getView());
		emojiKeyboard.getView().setElevation(V.dp(2));

		attachmentsView=view.findViewById(R.id.attachments);
		pollOptionsView=view.findViewById(R.id.poll_options);
		pollWrap=view.findViewById(R.id.poll_wrap);
		addPollOptionBtn=view.findViewById(R.id.add_poll_option);

		addPollOptionBtn.setOnClickListener(v->{
			createDraftPollOption().edit.requestFocus();
			updatePollOptionHints();
		});
		pollOptionsView.setDragListener(this::onSwapPollOptions);
		pollDurationView=view.findViewById(R.id.poll_duration);
		pollDurationView.setOnClickListener(v->showPollDurationMenu());

		pollOptions.clear();
		if(savedInstanceState!=null && savedInstanceState.containsKey("pollOptions")){
			pollBtn.setSelected(true);
			mediaBtn.setEnabled(false);
			pollWrap.setVisibility(View.VISIBLE);
			for(String oldText:savedInstanceState.getStringArrayList("pollOptions")){
				DraftPollOption opt=createDraftPollOption();
				opt.edit.setText(oldText);
			}
			updatePollOptionHints();
			pollDurationView.setText(getString(R.string.compose_poll_duration, pollDurationStr));
		}else if(savedInstanceState==null && editingStatus!=null && editingStatus.poll!=null){
			pollBtn.setSelected(true);
			mediaBtn.setEnabled(false);
			pollWrap.setVisibility(View.VISIBLE);
			for(Poll.Option eopt:editingStatus.poll.options){
				DraftPollOption opt=createDraftPollOption();
				opt.edit.setText(eopt.title);
			}
			updatePollOptionHints();
			pollDurationView.setText(getString(R.string.compose_poll_duration, pollDurationStr));
		}else{
			pollDurationView.setText(getString(R.string.compose_poll_duration, pollDurationStr=getResources().getQuantityString(R.plurals.x_days, 1, 1)));
		}

		spoilerEdit=view.findViewById(R.id.content_warning);
		LayerDrawable spoilerBg=(LayerDrawable) spoilerEdit.getBackground().mutate();
		spoilerBg.setDrawableByLayerId(R.id.left_drawable, new SpoilerStripesDrawable());
		spoilerBg.setDrawableByLayerId(R.id.right_drawable, new SpoilerStripesDrawable());
		spoilerEdit.setBackground(spoilerBg);
		if((savedInstanceState!=null && savedInstanceState.getBoolean("hasSpoiler", false)) || hasSpoiler){
			spoilerEdit.setVisibility(View.VISIBLE);
			spoilerBtn.setSelected(true);
		}else if(editingStatus!=null && !TextUtils.isEmpty(editingStatus.spoilerText)){
			hasSpoiler=true;
			spoilerEdit.setVisibility(View.VISIBLE);
			spoilerEdit.setText(getArguments().getString("sourceSpoiler", editingStatus.spoilerText));
			spoilerBtn.setSelected(true);
		}

		if(savedInstanceState!=null && savedInstanceState.containsKey("attachments")){
			ArrayList<Parcelable> serializedAttachments=savedInstanceState.getParcelableArrayList("attachments");
			for(Parcelable a:serializedAttachments){
				DraftMediaAttachment att=Parcels.unwrap(a);
				attachmentsView.addView(createMediaAttachmentView(att));
				attachments.add(att);
			}
			attachmentsView.setVisibility(View.VISIBLE);
		}else if(!attachments.isEmpty()){
			attachmentsView.setVisibility(View.VISIBLE);
			for(DraftMediaAttachment att:attachments){
				attachmentsView.addView(createMediaAttachmentView(att));
			}
		}

		if(editingStatus!=null && editingStatus.visibility!=null) {
			statusVisibility=editingStatus.visibility;
		}
		updateVisibilityIcon();

		autocompleteViewController=new ComposeAutocompleteViewController(getActivity(), accountID);
		autocompleteViewController.setCompletionSelectedListener(this::onAutocompleteOptionSelected);
		View autocompleteView=autocompleteViewController.getView();
		autocompleteView.setVisibility(View.GONE);
		mainEditTextWrap.addView(autocompleteView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(178), Gravity.TOP));

		creatingView=false;

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		if(!pollOptions.isEmpty()){
			ArrayList<String> opts=new ArrayList<>();
			for(DraftPollOption opt:pollOptions){
				opts.add(opt.edit.getText().toString());
			}
			outState.putStringArrayList("pollOptions", opts);
			outState.putInt("pollDuration", pollDuration);
			outState.putString("pollDurationStr", pollDurationStr);
		}
		outState.putBoolean("hasSpoiler", hasSpoiler);
		if(!attachments.isEmpty()){
			ArrayList<Parcelable> serializedAttachments=new ArrayList<>(attachments.size());
			for(DraftMediaAttachment att:attachments){
				serializedAttachments.add(Parcels.wrap(att));
			}
			outState.putParcelableArrayList("attachments", serializedAttachments);
		}
		outState.putSerializable("visibility", statusVisibility);
	}

	@Override
	public void onResume(){
		super.onResume();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		contentView.setSizeListener(emojiKeyboard::onContentViewSizeChanged);
		InputMethodManager imm=getActivity().getSystemService(InputMethodManager.class);
		mainEditText.requestFocus();
		view.postDelayed(()->{
			imm.showSoftInput(mainEditText, 0);
		}, 100);

		mainEditText.setSelectionListener(this);
		mainEditText.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){
				if(s.length()==0)
					return;
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
			}

			@Override
			public void afterTextChanged(Editable s){
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
					spoilerEdit.setVisibility(View.VISIBLE);
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
				if(!editingStatus.mediaAttachments.isEmpty()){
					attachmentsView.setVisibility(View.VISIBLE);
					for(Attachment att:editingStatus.mediaAttachments){
						DraftMediaAttachment da=new DraftMediaAttachment();
						da.serverAttachment=att;
						da.description=att.description;
						da.uri=Uri.parse(att.previewUrl);
						da.state=AttachmentUploadState.DONE;
						attachmentsView.addView(createMediaAttachmentView(da));
						attachments.add(da);
					}
					pollBtn.setEnabled(false);
				}
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
						addMediaAttachment(uri, null);
					}
				}
			}
		}

		if(editingStatus!=null){
			updateCharCounter();
			visibilityBtn.setEnabled(false);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		publishButton=new Button(getActivity());
		publishButton.setText(editingStatus==null ? R.string.publish : R.string.save);
		publishButton.setOnClickListener(this::onPublishClick);
		LinearLayout wrap=new LinearLayout(getActivity());
		wrap.setOrientation(LinearLayout.HORIZONTAL);

		sendProgress=new ProgressBar(getActivity());
		LinearLayout.LayoutParams progressLP=new LinearLayout.LayoutParams(V.dp(24), V.dp(24));
		progressLP.setMarginEnd(V.dp(16));
		progressLP.gravity=Gravity.CENTER_VERTICAL;
		wrap.addView(sendProgress, progressLP);

		sendError=new ImageView(getActivity());
		sendError.setImageResource(R.drawable.ic_fluent_error_circle_24_regular);
		sendError.setImageTintList(getResources().getColorStateList(R.color.error_600));
		sendError.setScaleType(ImageView.ScaleType.CENTER);
		wrap.addView(sendError, progressLP);

		sendError.setVisibility(View.GONE);
		sendProgress.setVisibility(View.GONE);

		wrap.addView(publishButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		wrap.setPadding(V.dp(16), V.dp(4), V.dp(16), V.dp(8));
		wrap.setClipToPadding(false);
		MenuItem item=menu.add(editingStatus==null ? R.string.publish : R.string.save);
		item.setActionView(wrap);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		updatePublishButtonState();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		emojiKeyboard.onConfigurationChanged();
	}

	@SuppressLint("NewApi")
	private void updateCharCounter(){
		CharSequence text=mainEditText.getText();

		String countableText=TwitterTextEmojiRegex.VALID_EMOJI_PATTERN.matcher(
				MENTION_PATTERN.matcher(
						URL_PATTERN.matcher(text).replaceAll("$2xxxxxxxxxxxxxxxxxxxxxxx")
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
		trimmedCharCount=text.toString().trim().length();
		updatePublishButtonState();
	}

	private void updatePublishButtonState(){
		uuid=null;
		int nonEmptyPollOptionsCount=0;
		for(DraftPollOption opt:pollOptions){
			if(opt.edit.length()>0)
				nonEmptyPollOptionsCount++;
		}
		if(publishButton==null)
			return;
		int nonDoneAttachmentCount=0;
		for(DraftMediaAttachment att:attachments){
			if(att.state!=AttachmentUploadState.DONE)
				nonDoneAttachmentCount++;
		}
		publishButton.setEnabled((trimmedCharCount>0 || !attachments.isEmpty()) && charCount<=charLimit && nonDoneAttachmentCount==0 && (pollOptions.isEmpty() || nonEmptyPollOptionsCount>1));
	}

	private void onCustomEmojiClick(Emoji emoji){
		int start=mainEditText.getSelectionStart();
		String prefix=start>0 && !Character.isWhitespace(mainEditText.getText().charAt(start-1)) ? " :" : ":";
		mainEditText.getText().replace(start, mainEditText.getSelectionEnd(), prefix+emoji.shortcode+':');
	}

	@Override
	protected void updateToolbar(){
		super.updateToolbar();
		getToolbar().setNavigationIcon(R.drawable.ic_fluent_dismiss_24_regular);
	}

	private void onPublishClick(View v){
		publish();
	}

	private void publish(){
		String text=mainEditText.getText().toString();
		CreateStatus.Request req=new CreateStatus.Request();
		req.status=text;
		req.visibility=statusVisibility;
		if(!attachments.isEmpty()){
			req.mediaIds=attachments.stream().map(a->a.serverAttachment.id).collect(Collectors.toList());
		}
		if(replyTo!=null){
			req.inReplyToId=replyTo.id;
		}
		if(!pollOptions.isEmpty()){
			req.poll=new CreateStatus.Request.Poll();
			req.poll.expiresIn=pollDuration;
			for(DraftPollOption opt:pollOptions)
				req.poll.options.add(opt.edit.getText().toString());
		}
		if(hasSpoiler && spoilerEdit.length()>0){
			req.spoilerText=spoilerEdit.getText().toString();
		}
		if(uuid==null)
			uuid=UUID.randomUUID().toString();

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
		sendProgress.setVisibility(View.VISIBLE);
		sendError.setVisibility(View.GONE);

		Callback<Status> resCallback=new Callback<>(){
			@Override
			public void onSuccess(Status result){
				wm.removeView(sendingOverlay);
				sendingOverlay=null;
				if(editingStatus==null){
					E.post(new StatusCreatedEvent(result));
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
				wm.removeView(sendingOverlay);
				sendingOverlay=null;
				sendProgress.setVisibility(View.GONE);
				sendError.setVisibility(View.VISIBLE);
				publishButton.setEnabled(true);
				error.showToast(getActivity());
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

	private boolean hasDraft(){
		if(editingStatus!=null){
			if(!mainEditText.getText().toString().equals(initialText))
				return true;
			List<String> existingMediaIDs=editingStatus.mediaAttachments.stream().map(a->a.id).collect(Collectors.toList());
			if(!existingMediaIDs.equals(attachments.stream().map(a->a.serverAttachment.id).collect(Collectors.toList())))
				return true;
			return pollChanged;
		}
		boolean pollFieldsHaveContent=false;
		for(DraftPollOption opt:pollOptions)
			pollFieldsHaveContent|=opt.edit.length()>0;
		return (mainEditText.length()>0 && !mainEditText.getText().toString().equals(initialText)) || !attachments.isEmpty() || pollFieldsHaveContent;
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
			Attachment updated=Parcels.unwrap(result.getParcelable("attachment"));
			for(DraftMediaAttachment att:attachments){
				if(att.serverAttachment.id.equals(updated.id)){
					att.serverAttachment=updated;
					att.description=updated.description;
					att.descriptionView.setText(att.description);
					break;
				}
			}
		}
	}

	private void confirmDiscardDraftAndFinish(){
		new AlertDialog.Builder(getActivity())
				.setTitle(editingStatus==null ? R.string.discard_draft : R.string.discard_changes)
				.setPositiveButton(R.string.discard, (dialog, which)->Nav.finish(this))
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void openFilePicker(){
		Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		if(instance.configuration!=null && instance.configuration.mediaAttachments!=null && instance.configuration.mediaAttachments.supportedMimeTypes!=null && !instance.configuration.mediaAttachments.supportedMimeTypes.isEmpty()){
			intent.putExtra(Intent.EXTRA_MIME_TYPES, instance.configuration.mediaAttachments.supportedMimeTypes.toArray(new String[0]));
		}else{
			intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
		}
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		startActivityForResult(intent, MEDIA_RESULT);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode==MEDIA_RESULT && resultCode==Activity.RESULT_OK){
			Uri single=data.getData();
			if(single!=null){
				addMediaAttachment(single, null);
			}else{
				ClipData clipData=data.getClipData();
				for(int i=0;i<clipData.getItemCount();i++){
					addMediaAttachment(clipData.getItemAt(i).getUri(), null);
				}
			}
		}
	}

	private boolean addMediaAttachment(Uri uri, String description){
		if(getMediaAttachmentsCount()==MAX_ATTACHMENTS){
			showMediaAttachmentError(getResources().getQuantityString(R.plurals.cant_add_more_than_x_attachments, MAX_ATTACHMENTS, MAX_ATTACHMENTS));
			return false;
		}
		String type=getActivity().getContentResolver().getType(uri);
		if(instance!=null && instance.configuration!=null && instance.configuration.mediaAttachments!=null){
			if(instance.configuration.mediaAttachments.supportedMimeTypes!=null && !instance.configuration.mediaAttachments.supportedMimeTypes.contains(type)){
				showMediaAttachmentError(getString(R.string.media_attachment_unsupported_type, UiUtils.getFileName(uri)));
				return false;
			}
			if(!type.startsWith("image/")){
				int sizeLimit=instance.configuration.mediaAttachments.videoSizeLimit;
				int size;
				try(Cursor cursor=MastodonApp.context.getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)){
					cursor.moveToFirst();
					size=cursor.getInt(0);
				}catch(Exception x){
					Log.w("ComposeFragment", x);
					return false;
				}
				if(size>sizeLimit){
					float mb=sizeLimit/(float) (1024*1024);
					String sMb=String.format(Locale.getDefault(), mb%1f==0f ? "%.0f" : "%.2f", mb);
					showMediaAttachmentError(getString(R.string.media_attachment_too_big, UiUtils.getFileName(uri), sMb));
					return false;
				}
			}
		}
		pollBtn.setEnabled(false);
		DraftMediaAttachment draft=new DraftMediaAttachment();
		draft.uri=uri;
		draft.mimeType=type;
		draft.description=description;

		attachmentsView.addView(createMediaAttachmentView(draft));
		attachments.add(draft);
		attachmentsView.setVisibility(View.VISIBLE);
		draft.setOverlayVisible(true, false);

		if(!areThereAnyUploadingAttachments()){
			uploadNextQueuedAttachment();
		}
		updatePublishButtonState();
		if(getMediaAttachmentsCount()==MAX_ATTACHMENTS)
			mediaBtn.setEnabled(false);
		return true;
	}

	private void showMediaAttachmentError(String text){
		if(!attachmentsErrorShowing){
			Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
			attachmentsErrorShowing=true;
			contentView.postDelayed(()->attachmentsErrorShowing=false, 2000);
		}
	}

	private View createMediaAttachmentView(DraftMediaAttachment draft){
		View thumb=getActivity().getLayoutInflater().inflate(R.layout.compose_media_thumb, attachmentsView, false);
		ImageView img=thumb.findViewById(R.id.thumb);
		if(draft.serverAttachment!=null){
			ViewImageLoader.load(img, draft.serverAttachment.blurhashPlaceholder, new UrlImageLoaderRequest(draft.serverAttachment.previewUrl, V.dp(250), V.dp(250)));
		}else{
			if(draft.mimeType.startsWith("image/")){
				ViewImageLoader.load(img, null, new UrlImageLoaderRequest(draft.uri, V.dp(250), V.dp(250)));
			}else if(draft.mimeType.startsWith("video/")){
				loadVideoThumbIntoView(img, draft.uri);
			}
		}
		TextView fileName=thumb.findViewById(R.id.file_name);
		fileName.setText(UiUtils.getFileName(draft.serverAttachment!=null ? Uri.parse(draft.serverAttachment.url) : draft.uri));

		draft.view=thumb;
		draft.imageView=img;
		draft.progressBar=thumb.findViewById(R.id.progress);
		draft.infoBar=thumb.findViewById(R.id.info_bar);
		draft.overlay=thumb.findViewById(R.id.overlay);
		draft.descriptionView=thumb.findViewById(R.id.description);
		draft.uploadStateTitle=thumb.findViewById(R.id.state_title);
		draft.uploadStateText=thumb.findViewById(R.id.state_text);
		ImageButton btn=thumb.findViewById(R.id.remove_btn);
		btn.setTag(draft);
		btn.setOnClickListener(this::onRemoveMediaAttachmentClick);
		btn=thumb.findViewById(R.id.remove_btn2);
		btn.setTag(draft);
		btn.setOnClickListener(this::onRemoveMediaAttachmentClick);
		ImageButton retry=thumb.findViewById(R.id.retry_or_cancel_upload);
		retry.setTag(draft);
		retry.setOnClickListener(this::onRetryOrCancelMediaUploadClick);
		draft.retryButton=retry;
		draft.infoBar.setTag(draft);
		draft.infoBar.setOnClickListener(this::onEditMediaDescriptionClick);

		if(!TextUtils.isEmpty(draft.description))
			draft.descriptionView.setText(draft.description);

		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
			draft.overlay.setBackgroundColor(0xA6000000);
		}

		if(draft.state==AttachmentUploadState.UPLOADING || draft.state==AttachmentUploadState.PROCESSING || draft.state==AttachmentUploadState.QUEUED){
			draft.progressBar.setVisibility(View.GONE);
		}else if(draft.state==AttachmentUploadState.ERROR){
			draft.setOverlayVisible(true, false);
		}

		return thumb;
	}

	public void addFakeMediaAttachment(Uri uri, String description){
		pollBtn.setEnabled(false);
		DraftMediaAttachment draft=new DraftMediaAttachment();
		draft.uri=uri;
		draft.description=description;
		attachmentsView.addView(createMediaAttachmentView(draft));
		attachments.add(draft);
		attachmentsView.setVisibility(View.VISIBLE);
	}

	private void uploadMediaAttachment(DraftMediaAttachment attachment){
		if(areThereAnyUploadingAttachments()){
			 throw new IllegalStateException("there is already an attachment being uploaded");
		}
		attachment.state=AttachmentUploadState.UPLOADING;
		attachment.progressBar.setVisibility(View.VISIBLE);
		ObjectAnimator rotationAnimator=ObjectAnimator.ofFloat(attachment.progressBar, View.ROTATION, 0f, 360f);
		rotationAnimator.setInterpolator(new LinearInterpolator());
		rotationAnimator.setDuration(1500);
		rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
		rotationAnimator.start();
		attachment.progressBarAnimator=rotationAnimator;
		int maxSize=0;
		String contentType=getActivity().getContentResolver().getType(attachment.uri);
		if(contentType!=null && contentType.startsWith("image/")){
			maxSize=2_073_600; // TODO get this from instance configuration when it gets added there
		}
		attachment.uploadStateTitle.setText("");
		attachment.uploadStateText.setText("");
		attachment.progressBar.setProgress(0);
		attachment.speedTracker.reset();
		attachment.speedTracker.addSample(0);
		attachment.uploadRequest=(UploadAttachment) new UploadAttachment(attachment.uri, maxSize, attachment.description)
				.setProgressListener(new ProgressListener(){
					@Override
					public void onProgress(long transferred, long total){
						if(updateUploadEtaRunnable==null){
							UiUtils.runOnUiThread(updateUploadEtaRunnable=ComposeFragment.this::updateUploadETAs, 100);
						}
						int progress=Math.round(transferred/(float)total*attachment.progressBar.getMax());
						if(Build.VERSION.SDK_INT>=24)
							attachment.progressBar.setProgress(progress, true);
						else
							attachment.progressBar.setProgress(progress);

						attachment.speedTracker.setTotalBytes(total);
						attachment.uploadStateTitle.setText(getString(R.string.file_upload_progress, UiUtils.formatFileSize(getActivity(), transferred, true), UiUtils.formatFileSize(getActivity(), total, true)));
						attachment.speedTracker.addSample(transferred);
					}
				})
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Attachment result){
						attachment.serverAttachment=result;
						if(TextUtils.isEmpty(result.url)){
							attachment.state=AttachmentUploadState.PROCESSING;
							attachment.processingPollingRunnable=()->pollForMediaAttachmentProcessing(attachment);
							if(getActivity()==null)
								return;
							attachment.uploadStateTitle.setText(R.string.upload_processing);
							attachment.uploadStateText.setText("");
							UiUtils.runOnUiThread(attachment.processingPollingRunnable, 1000);
							if(!areThereAnyUploadingAttachments())
								uploadNextQueuedAttachment();
						}else{
							finishMediaAttachmentUpload(attachment);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						attachment.uploadRequest=null;
						attachment.progressBarAnimator=null;
						attachment.state=AttachmentUploadState.ERROR;
						attachment.uploadStateTitle.setText(R.string.upload_failed);
						if(error instanceof MastodonErrorResponse er){
							if(er.underlyingException instanceof SocketException || er.underlyingException instanceof UnknownHostException || er.underlyingException instanceof InterruptedIOException)
								attachment.uploadStateText.setText(R.string.upload_error_connection_lost);
							else
								attachment.uploadStateText.setText(er.error);
						}else{
							attachment.uploadStateText.setText("");
						}
						attachment.retryButton.setImageResource(R.drawable.ic_fluent_arrow_clockwise_24_filled);
						attachment.retryButton.setContentDescription(getString(R.string.retry_upload));

						rotationAnimator.cancel();
						V.setVisibilityAnimated(attachment.retryButton, View.VISIBLE);
						V.setVisibilityAnimated(attachment.progressBar, View.GONE);

						if(!areThereAnyUploadingAttachments())
							uploadNextQueuedAttachment();
					}
				})
				.exec(accountID);
	}

	private void onRemoveMediaAttachmentClick(View v){
		DraftMediaAttachment att=(DraftMediaAttachment) v.getTag();
		if(att.isUploadingOrProcessing())
			att.cancelUpload();
		attachments.remove(att);
		uploadNextQueuedAttachment();
		attachmentsView.removeView(att.view);
		if(getMediaAttachmentsCount()==0)
			attachmentsView.setVisibility(View.GONE);
		updatePublishButtonState();
		pollBtn.setEnabled(attachments.isEmpty());
		mediaBtn.setEnabled(true);
	}

	private void onRetryOrCancelMediaUploadClick(View v){
		DraftMediaAttachment att=(DraftMediaAttachment) v.getTag();
		if(att.state==AttachmentUploadState.ERROR){
			att.retryButton.setImageResource(R.drawable.ic_fluent_dismiss_24_filled);
			att.retryButton.setContentDescription(getString(R.string.cancel));
			V.setVisibilityAnimated(att.progressBar, View.VISIBLE);
			att.state=AttachmentUploadState.QUEUED;
			if(!areThereAnyUploadingAttachments()){
				uploadNextQueuedAttachment();
			}
		}else{
			onRemoveMediaAttachmentClick(v);
		}
	}

	private void pollForMediaAttachmentProcessing(DraftMediaAttachment attachment){
		attachment.processingPollingRequest=(GetAttachmentByID) new GetAttachmentByID(attachment.serverAttachment.id)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Attachment result){
						attachment.processingPollingRequest=null;
						if(!TextUtils.isEmpty(result.url)){
							attachment.processingPollingRunnable=null;
							attachment.serverAttachment=result;
							finishMediaAttachmentUpload(attachment);
						}else if(getActivity()!=null){
							UiUtils.runOnUiThread(attachment.processingPollingRunnable, 1000);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						attachment.processingPollingRequest=null;
						if(getActivity()!=null)
							UiUtils.runOnUiThread(attachment.processingPollingRunnable, 1000);
					}
				})
				.exec(accountID);
	}

	private void finishMediaAttachmentUpload(DraftMediaAttachment attachment){
		if(attachment.state!=AttachmentUploadState.PROCESSING && attachment.state!=AttachmentUploadState.UPLOADING)
			throw new IllegalStateException("Unexpected state "+attachment.state);
		attachment.uploadRequest=null;
		attachment.state=AttachmentUploadState.DONE;
		attachment.progressBar.setVisibility(View.GONE);
		if(!areThereAnyUploadingAttachments())
			uploadNextQueuedAttachment();
		updatePublishButtonState();

		if(attachment.progressBarAnimator!=null){
			attachment.progressBarAnimator.cancel();
			attachment.progressBarAnimator=null;
		}
		attachment.setOverlayVisible(false, true);
	}

	private void uploadNextQueuedAttachment(){
		for(DraftMediaAttachment att:attachments){
			if(att.state==AttachmentUploadState.QUEUED){
				uploadMediaAttachment(att);
				return;
			}
		}
	}

	private boolean areThereAnyUploadingAttachments(){
		for(DraftMediaAttachment att:attachments){
			if(att.state==AttachmentUploadState.UPLOADING)
				return true;
		}
		return false;
	}

	private void updateUploadETAs(){
		if(!areThereAnyUploadingAttachments()){
			UiUtils.removeCallbacks(updateUploadEtaRunnable);
			updateUploadEtaRunnable=null;
			return;
		}
		for(DraftMediaAttachment att:attachments){
			if(att.state==AttachmentUploadState.UPLOADING){
				long eta=att.speedTracker.updateAndGetETA();
//				Log.i(TAG, "onProgress: transfer speed "+UiUtils.formatFileSize(getActivity(), Math.round(att.speedTracker.getLastSpeed()), false)+" average "+UiUtils.formatFileSize(getActivity(), Math.round(att.speedTracker.getAverageSpeed()), false)+" eta "+eta);
				String time=String.format("%d:%02d", eta/60, eta%60);
				att.uploadStateText.setText(getString(R.string.file_upload_time_remaining, time));
			}
		}
		UiUtils.runOnUiThread(updateUploadEtaRunnable, 100);
	}

	private void onEditMediaDescriptionClick(View v){
		DraftMediaAttachment att=(DraftMediaAttachment) v.getTag();
		if(att.serverAttachment==null)
			return;
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("attachment", att.serverAttachment.id);
		args.putParcelable("uri", att.uri);
		args.putString("existingDescription", att.description);
		Nav.goForResult(getActivity(), ComposeImageDescriptionFragment.class, args, IMAGE_DESCRIPTION_RESULT, this);
	}

	private void togglePoll(){
		if(pollOptions.isEmpty()){
			pollBtn.setSelected(true);
			mediaBtn.setEnabled(false);
			pollWrap.setVisibility(View.VISIBLE);
			for(int i=0;i<2;i++)
				createDraftPollOption();
			updatePollOptionHints();
		}else{
			pollBtn.setSelected(false);
			mediaBtn.setEnabled(true);
			pollWrap.setVisibility(View.GONE);
			addPollOptionBtn.setVisibility(View.VISIBLE);
			pollOptionsView.removeAllViews();
			pollOptions.clear();
			pollDuration=24*3600;
		}
		updatePublishButtonState();
	}

	private DraftPollOption createDraftPollOption(){
		DraftPollOption option=new DraftPollOption();
		option.view=LayoutInflater.from(getActivity()).inflate(R.layout.compose_poll_option, pollOptionsView, false);
		option.edit=option.view.findViewById(R.id.edit);
		option.dragger=option.view.findViewById(R.id.dragger_thingy);

		option.dragger.setOnLongClickListener(v->{
			pollOptionsView.startDragging(option.view);
			return true;
		});
		option.edit.addTextChangedListener(new SimpleTextWatcher(e->{
			if(!creatingView)
				pollChanged=true;
			updatePublishButtonState();
		}));
		option.edit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(instance.configuration!=null && instance.configuration.polls!=null && instance.configuration.polls.maxCharactersPerOption>0 ? instance.configuration.polls.maxCharactersPerOption : 50)});

		pollOptionsView.addView(option.view);
		pollOptions.add(option);
		if(pollOptions.size()==(instance.configuration!=null && instance.configuration.polls!=null && instance.configuration.polls.maxOptions>0 ? instance.configuration.polls.maxOptions : 4))
			addPollOptionBtn.setVisibility(View.GONE);
		return option;
	}

	private void updatePollOptionHints(){
		int i=0;
		for(DraftPollOption option:pollOptions){
			option.edit.setHint(getString(R.string.poll_option_hint, ++i));
		}
	}

	private void onSwapPollOptions(int oldIndex, int newIndex){
		pollOptions.add(newIndex, pollOptions.remove(oldIndex));
		updatePollOptionHints();
		pollChanged=true;
	}

	private void showPollDurationMenu(){
		PopupMenu menu=new PopupMenu(getActivity(), pollDurationView);
		menu.getMenu().add(0, 1, 0, getResources().getQuantityString(R.plurals.x_minutes, 5, 5));
		menu.getMenu().add(0, 2, 0, getResources().getQuantityString(R.plurals.x_minutes, 30, 30));
		menu.getMenu().add(0, 3, 0, getResources().getQuantityString(R.plurals.x_hours, 1, 1));
		menu.getMenu().add(0, 4, 0, getResources().getQuantityString(R.plurals.x_hours, 6, 6));
		menu.getMenu().add(0, 5, 0, getResources().getQuantityString(R.plurals.x_days, 1, 1));
		menu.getMenu().add(0, 6, 0, getResources().getQuantityString(R.plurals.x_days, 3, 3));
		menu.getMenu().add(0, 7, 0, getResources().getQuantityString(R.plurals.x_days, 7, 7));
		menu.setOnMenuItemClickListener(item->{
			pollDuration=switch(item.getItemId()){
				case 1 -> 5*60;
				case 2 -> 30*60;
				case 3 -> 3600;
				case 4 -> 6*3600;
				case 5 -> 24*3600;
				case 6 -> 3*24*3600;
				case 7 -> 7*24*3600;
				default -> throw new IllegalStateException("Unexpected value: "+item.getItemId());
			};
			pollDurationView.setText(getString(R.string.compose_poll_duration, pollDurationStr=item.getTitle().toString()));
			pollChanged=true;
			return true;
		});
		menu.show();
	}

	private void toggleSpoiler(){
		hasSpoiler=!hasSpoiler;
		if(hasSpoiler){
			spoilerEdit.setVisibility(View.VISIBLE);
			spoilerBtn.setSelected(true);
			spoilerEdit.requestFocus();
		}else{
			spoilerEdit.setVisibility(View.GONE);
			spoilerEdit.setText("");
			spoilerBtn.setSelected(false);
			mainEditText.requestFocus();
			updateCharCounter();
		}
	}

	private int getMediaAttachmentsCount(){
		return attachments.size();
	}

	private void onVisibilityClick(View v){
		PopupMenu menu=new PopupMenu(getActivity(), v);
		menu.inflate(R.menu.compose_visibility);
		Menu m=menu.getMenu();
		UiUtils.enablePopupMenuIcons(getActivity(), menu);
		m.setGroupCheckable(0, true, true);
		m.findItem(switch(statusVisibility){
			case PUBLIC, UNLISTED -> R.id.vis_public;
			case PRIVATE -> R.id.vis_followers;
			case DIRECT -> R.id.vis_private;
		}).setChecked(true);
		menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
			@Override
			public boolean onMenuItemClick(MenuItem item){
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
			}
		});
		menu.show();
	}

	private void loadDefaultStatusVisibility(Bundle savedInstanceState) {
		if(getArguments().containsKey("replyTo")){
			replyTo=Parcels.unwrap(getArguments().getParcelable("replyTo"));
			statusVisibility = replyTo.visibility;
		}

		// A saved privacy setting from a previous compose session wins over the reply visibility
		if(savedInstanceState !=null){
			statusVisibility = (StatusPrivacy) savedInstanceState.getSerializable("visibility");
		}

		new GetPreferences()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Preferences result){
						// Only override the reply visibility if our preference is more private
						if (result.postingDefaultVisibility.isLessVisibleThan(statusVisibility)) {
							// Map unlisted from the API onto public, because we don't have unlisted in the UI
							statusVisibility = switch (result.postingDefaultVisibility) {
								case PUBLIC, UNLISTED -> StatusPrivacy.PUBLIC;
								case PRIVATE -> StatusPrivacy.PRIVATE;
								case DIRECT -> StatusPrivacy.DIRECT;
							};
						}

						// A saved privacy setting from a previous compose session wins over all
						if(savedInstanceState !=null){
							statusVisibility = (StatusPrivacy) savedInstanceState.getSerializable("visibility");
						}

						updateVisibilityIcon ();
					}

					@Override
					public void onError(ErrorResponse error){
						Log.w(TAG, "Unable to get user preferences to set default post privacy");
					}
				})
				.exec(accountID);
	}

	private void updateVisibilityIcon(){
		if(statusVisibility==null){ // TODO find out why this happens
			statusVisibility=StatusPrivacy.PUBLIC;
		}
		visibilityBtn.setImageResource(switch(statusVisibility){
			case PUBLIC -> R.drawable.ic_fluent_earth_24_regular;
			case UNLISTED -> R.drawable.ic_fluent_people_community_24_regular;
			case PRIVATE -> R.drawable.ic_fluent_people_checkmark_24_regular;
			case DIRECT -> R.drawable.ic_at_symbol;
		});
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

				View autocompleteView=autocompleteViewController.getView();
				Layout layout=mainEditText.getLayout();
				int line=layout.getLineForOffset(start);
				int offsetY=layout.getLineBottom(line);
				FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams) autocompleteView.getLayoutParams();
				if(lp.topMargin!=offsetY){
					lp.topMargin=offsetY;
					mainEditTextWrap.requestLayout();
				}
				int offsetX=Math.round(layout.getPrimaryHorizontal(start))+mainEditText.getPaddingLeft();
				autocompleteViewController.setArrowOffset(offsetX);
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
		return addMediaAttachment(uri, description);
	}

	private void startAutocomplete(ComposeAutocompleteSpan span){
		currentAutocompleteSpan=span;
		Editable e=mainEditText.getText();
		String spanText=e.toString().substring(e.getSpanStart(span), e.getSpanEnd(span));
		autocompleteViewController.setText(spanText);
		View autocompleteView=autocompleteViewController.getView();
		autocompleteView.setVisibility(View.VISIBLE);
	}

	private void finishAutocomplete(){
		if(currentAutocompleteSpan==null)
			return;
		autocompleteViewController.setText(null);
		currentAutocompleteSpan=null;
		autocompleteViewController.getView().setVisibility(View.GONE);
	}

	private void onAutocompleteOptionSelected(String text){
		Editable e=mainEditText.getText();
		int start=e.getSpanStart(currentAutocompleteSpan);
		int end=e.getSpanEnd(currentAutocompleteSpan);
		e.replace(start, end, text+" ");
		mainEditText.setSelection(start+text.length()+1);
		finishAutocomplete();
	}

	private void loadVideoThumbIntoView(ImageView target, Uri uri){
		MastodonAPIController.runInBackground(()->{
			Context context=getActivity();
			if(context==null)
				return;
			try{
				MediaMetadataRetriever mmr=new MediaMetadataRetriever();
				mmr.setDataSource(context, uri);
				Bitmap frame=mmr.getFrameAtTime(3_000_000);
				mmr.release();
				int size=Math.max(frame.getWidth(), frame.getHeight());
				int maxSize=V.dp(250);
				if(size>maxSize){
					float factor=maxSize/(float)size;
					frame=Bitmap.createScaledBitmap(frame, Math.round(frame.getWidth()*factor), Math.round(frame.getHeight()*factor), true);
				}
				Bitmap finalFrame=frame;
				target.post(()->target.setImageBitmap(finalFrame));
			}catch(Exception x){
				Log.w(TAG, "loadVideoThumbIntoView: error getting video frame", x);
			}
		});
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

	@Parcel
	static class DraftMediaAttachment{
		public Attachment serverAttachment;
		public Uri uri;
		public transient UploadAttachment uploadRequest;
		public transient GetAttachmentByID processingPollingRequest;
		public String description;
		public String mimeType;
		public AttachmentUploadState state=AttachmentUploadState.QUEUED;

		public transient View view;
		public transient ProgressBar progressBar;
		public transient TextView descriptionView;
		public transient View overlay;
		public transient View infoBar;
		public transient ImageButton retryButton;
		public transient ObjectAnimator progressBarAnimator;
		public transient Runnable processingPollingRunnable;
		public transient ImageView imageView;
		public transient TextView uploadStateTitle, uploadStateText;
		public transient TransferSpeedTracker speedTracker=new TransferSpeedTracker();

		public void cancelUpload(){
			switch(state){
				case UPLOADING -> {
					if(uploadRequest!=null){
						uploadRequest.cancel();
						uploadRequest=null;
					}
				}
				case PROCESSING -> {
					if(processingPollingRunnable!=null){
						UiUtils.removeCallbacks(processingPollingRunnable);
						processingPollingRunnable=null;
					}
					if(processingPollingRequest!=null){
						processingPollingRequest.cancel();
						processingPollingRequest=null;
					}
				}
				default -> throw new IllegalStateException("Unexpected state "+state);
			}
		}

		public boolean isUploadingOrProcessing(){
			return state==AttachmentUploadState.UPLOADING || state==AttachmentUploadState.PROCESSING;
		}

		public void setOverlayVisible(boolean visible, boolean animated){
			if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
				if(visible){
					imageView.setRenderEffect(RenderEffect.createBlurEffect(V.dp(16), V.dp(16), Shader.TileMode.REPEAT));
				}else{
					imageView.setRenderEffect(null);
				}
			}
			int infoBarVis=visible ? View.GONE : View.VISIBLE;
			int overlayVis=visible ? View.VISIBLE : View.GONE;
			if(animated){
				V.setVisibilityAnimated(infoBar, infoBarVis);
				V.setVisibilityAnimated(overlay, overlayVis);
			}else{
				infoBar.setVisibility(infoBarVis);
				overlay.setVisibility(overlayVis);
			}
		}
	}

	enum AttachmentUploadState{
		QUEUED,
		UPLOADING,
		PROCESSING,
		ERROR,
		DONE
	}

	private static class DraftPollOption{
		public EditText edit;
		public View view;
		public View dragger;
	}
}
