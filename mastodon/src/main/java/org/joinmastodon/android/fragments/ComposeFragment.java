package org.joinmastodon.android.fragments;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.icu.text.BreakIterator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.ProgressListener;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.requests.statuses.UploadAttachment;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.EmojiCategory;
import org.joinmastodon.android.model.Mention;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.CustomEmojiPopupKeyboard;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.PopupKeyboard;
import org.joinmastodon.android.ui.drawables.SpoilerStripesDrawable;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.text.SpacerSpan;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.ComposeMediaLayout;
import org.joinmastodon.android.ui.views.ReorderableLinearLayout;
import org.joinmastodon.android.ui.views.SizeListenerLinearLayout;
import org.parceler.Parcel;
import org.parceler.Parcels;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class ComposeFragment extends ToolbarFragment implements OnBackPressedListener{

	private static final int MEDIA_RESULT=717;
	private static final int IMAGE_DESCRIPTION_RESULT=363;
	private static final int MAX_POLL_OPTIONS=4;
	private static final int MAX_ATTACHMENTS=4;

	private static final Pattern MENTION_PATTERN=Pattern.compile("(^|[^\\/\\w])@(([a-z0-9_]+)@[a-z0-9\\.\\-]+[a-z0-9]+)", Pattern.CASE_INSENSITIVE);

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

	private EditText mainEditText;
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

	private ArrayList<DraftMediaAttachment> queuedAttachments=new ArrayList<>(), failedAttachments=new ArrayList<>(), attachments=new ArrayList<>(), allAttachments=new ArrayList<>();
	private DraftMediaAttachment uploadingAttachment;

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

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		accountID=getArguments().getString("account");
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		charLimit=session.tootCharLimit;
		if(charLimit==0)
			charLimit=500;
		self=session.self;
		instanceDomain=session.domain;
		customEmojis=AccountSessionManager.getInstance().getCustomEmojis(instanceDomain);
		if(getArguments().containsKey("replyTo")){
			replyTo=Parcels.unwrap(getArguments().getParcelable("replyTo"));
			statusVisibility=replyTo.visibility;
		}
		if(savedInstanceState!=null){
			statusVisibility=(StatusPrivacy) savedInstanceState.getSerializable("visibility");
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
		emojiKeyboard=new CustomEmojiPopupKeyboard(getActivity(), customEmojis, instanceDomain);
		emojiKeyboard.setListener(this::onCustomEmojiClick);

		View view=inflater.inflate(R.layout.fragment_compose, container, false);
		mainEditText=view.findViewById(R.id.toot_text);
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
		}

		if(savedInstanceState!=null && savedInstanceState.containsKey("attachments")){
			ArrayList<Parcelable> serializedAttachments=savedInstanceState.getParcelableArrayList("attachments");
			for(Parcelable a:serializedAttachments){
				DraftMediaAttachment att=Parcels.unwrap(a);
				attachmentsView.addView(createMediaAttachmentView(att));
				attachments.add(att);
			}
			attachmentsView.setVisibility(View.VISIBLE);
		}else if(!allAttachments.isEmpty()){
			attachmentsView.setVisibility(View.VISIBLE);
			for(DraftMediaAttachment att:allAttachments){
				attachmentsView.addView(createMediaAttachmentView(att));
			}
		}
		updateVisibilityIcon();

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

		mainEditText.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){

			}

			@Override
			public void afterTextChanged(Editable s){
				updateCharCounter(s);
			}
		});
		updateToolbar();
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
			initialText=TextUtils.join(" ", mentions)+" ";
			if(savedInstanceState==null){
				mainEditText.setText(initialText);
				mainEditText.setSelection(mainEditText.length());
			}
		}else{
			replyText.setVisibility(View.GONE);
		}
		if(savedInstanceState==null){
			String prefilledText=getArguments().getString("prefilledText");
			if(!TextUtils.isEmpty(prefilledText)){
				mainEditText.setText(prefilledText);
				mainEditText.setSelection(mainEditText.length());
				initialText=prefilledText;
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		publishButton=new Button(getActivity());
		publishButton.setText(R.string.publish);
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
		MenuItem item=menu.add(R.string.publish);
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
		updateToolbar();
	}

	@SuppressLint("NewApi")
	private void updateCharCounter(CharSequence text){
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
		publishButton.setEnabled((trimmedCharCount>0 || !attachments.isEmpty()) && charCount<=charLimit && uploadingAttachment==null && failedAttachments.isEmpty() && queuedAttachments.isEmpty()
				&& (pollOptions.isEmpty() || nonEmptyPollOptionsCount>1));
	}

	private void onCustomEmojiClick(Emoji emoji){
		mainEditText.getText().replace(mainEditText.getSelectionStart(), mainEditText.getSelectionEnd(), ':'+emoji.shortcode+':');
	}

	private void updateToolbar(){
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

		new CreateStatus(req, uuid)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Status result){
						wm.removeView(sendingOverlay);
						Nav.finish(ComposeFragment.this);
						E.post(new StatusCreatedEvent(result));
						if(replyTo!=null){
							replyTo.repliesCount++;
							E.post(new StatusCountersUpdatedEvent(replyTo));
						}
					}

					@Override
					public void onError(ErrorResponse error){
						wm.removeView(sendingOverlay);
						sendProgress.setVisibility(View.GONE);
						sendError.setVisibility(View.VISIBLE);
						publishButton.setEnabled(true);
						error.showToast(getActivity());
					}
				})
				.exec(accountID);
	}

	private boolean hasDraft(){
		boolean pollFieldsHaveContent=false;
		for(DraftPollOption opt:pollOptions)
			pollFieldsHaveContent|=opt.edit.length()>0;
		return (mainEditText.length()>0 && !mainEditText.getText().toString().equals(initialText)) || !attachments.isEmpty()
				|| uploadingAttachment!=null || !queuedAttachments.isEmpty() || !failedAttachments.isEmpty() || pollFieldsHaveContent;
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
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.discard_draft)
				.setPositiveButton(R.string.discard, (dialog, which)->Nav.finish(this))
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void openFilePicker(){
		Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
		intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		startActivityForResult(intent, MEDIA_RESULT);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode==MEDIA_RESULT && resultCode==Activity.RESULT_OK){
			Uri single=data.getData();
			if(single!=null){
				addMediaAttachment(single);
			}else{
				ClipData clipData=data.getClipData();
				for(int i=0;i<clipData.getItemCount();i++){
					addMediaAttachment(clipData.getItemAt(i).getUri());
				}
			}
		}
	}

	private void addMediaAttachment(Uri uri){
		if(getMediaAttachmentsCount()==MAX_ATTACHMENTS)
			return;
		pollBtn.setEnabled(false);
		DraftMediaAttachment draft=new DraftMediaAttachment();
		draft.uri=uri;

		attachmentsView.addView(createMediaAttachmentView(draft));
		allAttachments.add(draft);
		attachmentsView.setVisibility(View.VISIBLE);
		draft.overlay.setVisibility(View.VISIBLE);
		draft.infoBar.setVisibility(View.GONE);

		if(uploadingAttachment==null){
			uploadMediaAttachment(draft);
		}else{
			queuedAttachments.add(draft);
		}
		updatePublishButtonState();
		if(getMediaAttachmentsCount()==MAX_ATTACHMENTS)
			mediaBtn.setEnabled(false);
	}

	private View createMediaAttachmentView(DraftMediaAttachment draft){
		View thumb=getActivity().getLayoutInflater().inflate(R.layout.compose_media_thumb, attachmentsView, false);
		ImageView img=thumb.findViewById(R.id.thumb);
		ViewImageLoader.load(img, null, new UrlImageLoaderRequest(draft.uri, V.dp(250), V.dp(250)));
		TextView fileName=thumb.findViewById(R.id.file_name);
		fileName.setText(UiUtils.getFileName(draft.uri));

		draft.view=thumb;
		draft.progressBar=thumb.findViewById(R.id.progress);
		draft.infoBar=thumb.findViewById(R.id.info_bar);
		draft.overlay=thumb.findViewById(R.id.overlay);
		draft.descriptionView=thumb.findViewById(R.id.description);
		ImageButton btn=thumb.findViewById(R.id.remove_btn);
		btn.setTag(draft);
		btn.setOnClickListener(this::onRemoveMediaAttachmentClick);
		btn=thumb.findViewById(R.id.remove_btn2);
		btn.setTag(draft);
		btn.setOnClickListener(this::onRemoveMediaAttachmentClick);
		Button retry=thumb.findViewById(R.id.retry_upload);
		retry.setTag(draft);
		retry.setOnClickListener(this::onRetryMediaUploadClick);
		retry.setVisibility(View.GONE);
		draft.retryButton=retry;
		draft.infoBar.setTag(draft);
		draft.infoBar.setOnClickListener(this::onEditMediaDescriptionClick);

		if(!TextUtils.isEmpty(draft.description))
			draft.descriptionView.setText(draft.description);

		if(uploadingAttachment!=draft && !queuedAttachments.contains(draft)){
			draft.progressBar.setVisibility(View.GONE);
		}
		if(failedAttachments.contains(draft)){
			draft.infoBar.setVisibility(View.GONE);
			draft.overlay.setVisibility(View.VISIBLE);
		}

		return thumb;
	}

	private void uploadMediaAttachment(DraftMediaAttachment attachment){
		if(uploadingAttachment!=null)
			throw new IllegalStateException("there is already an attachment being uploaded");
		uploadingAttachment=attachment;
		attachment.progressBar.setVisibility(View.VISIBLE);
		ObjectAnimator rotationAnimator=ObjectAnimator.ofFloat(attachment.progressBar, View.ROTATION, 0f, 360f);
		rotationAnimator.setInterpolator(new LinearInterpolator());
		rotationAnimator.setDuration(1500);
		rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
		rotationAnimator.start();
		attachment.uploadRequest=(UploadAttachment) new UploadAttachment(attachment.uri)
				.setProgressListener(new ProgressListener(){
					@Override
					public void onProgress(long transferred, long total){
						int progress=Math.round(transferred/(float)total*attachment.progressBar.getMax());
						if(Build.VERSION.SDK_INT>=24)
							attachment.progressBar.setProgress(progress, true);
						else
							attachment.progressBar.setProgress(progress);
					}
				})
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Attachment result){
						attachment.serverAttachment=result;
						attachment.uploadRequest=null;
						uploadingAttachment=null;
						attachments.add(attachment);
						attachment.progressBar.setVisibility(View.GONE);
						if(!queuedAttachments.isEmpty())
							uploadMediaAttachment(queuedAttachments.remove(0));
						updatePublishButtonState();

						rotationAnimator.cancel();
						V.setVisibilityAnimated(attachment.overlay, View.GONE);
						V.setVisibilityAnimated(attachment.infoBar, View.VISIBLE);
					}

					@Override
					public void onError(ErrorResponse error){
						attachment.uploadRequest=null;
						uploadingAttachment=null;
						failedAttachments.add(attachment);
//						error.showToast(getActivity());
						Toast.makeText(getActivity(), R.string.image_upload_failed, Toast.LENGTH_SHORT).show();

						rotationAnimator.cancel();
						V.setVisibilityAnimated(attachment.retryButton, View.VISIBLE);
						V.setVisibilityAnimated(attachment.progressBar, View.GONE);

						if(!queuedAttachments.isEmpty())
							uploadMediaAttachment(queuedAttachments.remove(0));
					}
				})
				.exec(accountID);
	}

	private void onRemoveMediaAttachmentClick(View v){
		DraftMediaAttachment att=(DraftMediaAttachment) v.getTag();
		if(att==uploadingAttachment){
			att.uploadRequest.cancel();
			if(!queuedAttachments.isEmpty())
				uploadMediaAttachment(queuedAttachments.remove(0));
			else
				uploadingAttachment=null;
		}else{
			attachments.remove(att);
			queuedAttachments.remove(att);
			failedAttachments.remove(att);
		}
		allAttachments.remove(att);
		attachmentsView.removeView(att.view);
		if(getMediaAttachmentsCount()==0)
			attachmentsView.setVisibility(View.GONE);
		updatePublishButtonState();
		pollBtn.setEnabled(attachments.isEmpty() && queuedAttachments.isEmpty() && failedAttachments.isEmpty() && uploadingAttachment==null);
		mediaBtn.setEnabled(true);
	}

	private void onRetryMediaUploadClick(View v){
		DraftMediaAttachment att=(DraftMediaAttachment) v.getTag();
		if(failedAttachments.remove(att)){
			V.setVisibilityAnimated(att.retryButton, View.GONE);
			V.setVisibilityAnimated(att.progressBar, View.VISIBLE);
			if(uploadingAttachment==null)
				uploadMediaAttachment(att);
			else
				queuedAttachments.add(att);
		}
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
		option.edit.addTextChangedListener(new SimpleTextWatcher(e->updatePublishButtonState()));

		pollOptionsView.addView(option.view);
		pollOptions.add(option);
		if(pollOptions.size()==MAX_POLL_OPTIONS)
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
		}
	}

	private int getMediaAttachmentsCount(){
		return allAttachments.size();
	}

	private void onVisibilityClick(View v){
		PopupMenu menu=new PopupMenu(getActivity(), v);
		menu.inflate(R.menu.compose_visibility);
		Menu m=menu.getMenu();
		if(Build.VERSION.SDK_INT>=29){
			menu.setForceShowIcon(true);
		}else{
			try{
				Method setOptionalIconsVisible=m.getClass().getDeclaredMethod("setOptionalIconsVisible", boolean.class);
				setOptionalIconsVisible.setAccessible(true);
				setOptionalIconsVisible.invoke(m, true);
			}catch(Exception ignore){}
		}
		ColorStateList iconTint=ColorStateList.valueOf(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		for(int i=0;i<m.size();i++){
			MenuItem item=m.getItem(i);
			Drawable icon=item.getIcon().mutate();
			if(Build.VERSION.SDK_INT>=26){
				item.setIconTintList(iconTint);
			}else{
				icon.setTintList(iconTint);
			}
			icon=new InsetDrawable(icon, V.dp(8), 0, 0, 0);
			item.setIcon(icon);
			SpannableStringBuilder ssb=new SpannableStringBuilder(item.getTitle());
			ssb.insert(0, " ");
			ssb.setSpan(new SpacerSpan(V.dp(24), 1), 0, 1, 0);
			ssb.append(" ", new SpacerSpan(V.dp(8), 1), 0);
			item.setTitle(ssb);
		}
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

	private void updateVisibilityIcon(){
		visibilityBtn.setImageResource(switch(statusVisibility){
			case PUBLIC -> R.drawable.ic_fluent_earth_24_filled;
			case UNLISTED -> R.drawable.ic_fluent_people_community_24_regular;
			case PRIVATE -> R.drawable.ic_fluent_people_checkmark_24_regular;
			case DIRECT -> R.drawable.ic_at_symbol;
		});
	}

	@Parcel
	static class DraftMediaAttachment{
		public Attachment serverAttachment;
		public Uri uri;
		public transient UploadAttachment uploadRequest;
		public String description;

		public transient View view;
		public transient ProgressBar progressBar;
		public transient TextView descriptionView;
		public transient View overlay;
		public transient View infoBar;
		public transient Button retryButton;
	}

	private static class DraftPollOption{
		public EditText edit;
		public View view;
		public View dragger;
	}
}
