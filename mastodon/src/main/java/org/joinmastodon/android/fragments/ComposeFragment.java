package org.joinmastodon.android.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.icu.text.BreakIterator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
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
import org.joinmastodon.android.ui.CustomEmojiPopupKeyboard;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.PopupKeyboard;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.views.ReorderableLinearLayout;
import org.joinmastodon.android.ui.views.SizeListenerLinearLayout;
import org.parceler.Parcels;

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
	private static final int MAX_POLL_OPTIONS=4;

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
	private LinearLayout attachmentsView;
	private TextView replyText;
	private ReorderableLinearLayout pollOptionsView;
	private View pollWrap;
	private View addPollOptionBtn;
	private TextView pollDurationView;

	private ArrayList<DraftPollOption> pollOptions=new ArrayList<>();

	private ArrayList<DraftMediaAttachment> queuedAttachments=new ArrayList<>(), failedAttachments=new ArrayList<>(), attachments=new ArrayList<>();
	private DraftMediaAttachment uploadingAttachment;

	private List<EmojiCategory> customEmojis;
	private CustomEmojiPopupKeyboard emojiKeyboard;
	private Status replyTo;
	private String initialText;
	private String uuid;
	private int pollDuration=24*3600;
	private String pollDurationStr;

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
		if(getArguments().containsKey("replyTo"))
			replyTo=Parcels.unwrap(getArguments().getParcelable("replyTo"));
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
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

		// TODO save and restore media attachments (when design is ready)

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
			mentions.add('@'+replyTo.account.acct);
			for(Mention mention : replyTo.mentions){
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
		FrameLayout wrap=new FrameLayout(getActivity());
		wrap.addView(publishButton, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP|Gravity.LEFT));
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
		if(!attachments.isEmpty()){
			req.mediaIds=attachments.stream().map(a->a.serverAttachment.id).collect(Collectors.toList());
		}
		if(replyTo!=null){
			req.inReplyToId=replyTo.id;
			req.visibility=replyTo.visibility; // TODO
		}
		if(!pollOptions.isEmpty()){
			req.poll=new CreateStatus.Request.Poll();
			req.poll.expiresIn=pollDuration;
			for(DraftPollOption opt:pollOptions)
				req.poll.options.add(opt.edit.getText().toString());
		}
		if(uuid==null)
			uuid=UUID.randomUUID().toString();
		ProgressDialog progress=new ProgressDialog(getActivity());
		progress.setMessage(getString(R.string.publishing));
		progress.setCancelable(false);
		progress.show();
		new CreateStatus(req, uuid)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Status result){
						progress.dismiss();
						Nav.finish(ComposeFragment.this);
						E.post(new StatusCreatedEvent(result));
						if(replyTo!=null){
							replyTo.repliesCount++;
							E.post(new StatusCountersUpdatedEvent(replyTo));
						}
					}

					@Override
					public void onError(ErrorResponse error){
						progress.dismiss();
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
		pollBtn.setEnabled(false);
		View thumb=getActivity().getLayoutInflater().inflate(R.layout.compose_media_thumb, attachmentsView, false);
		ImageView img=thumb.findViewById(R.id.thumb);
		ViewImageLoader.load(img, null, new UrlImageLoaderRequest(uri, V.dp(250), V.dp(250)));
		attachmentsView.addView(thumb);

		DraftMediaAttachment draft=new DraftMediaAttachment();
		draft.uri=uri;
		draft.view=thumb;
		draft.progressBar=thumb.findViewById(R.id.progress);
		Button btn=thumb.findViewById(R.id.remove_btn);
		btn.setTag(draft);
		btn.setOnClickListener(this::onRemoveMediaAttachmentClick);

		if(uploadingAttachment==null){
			uploadMediaAttachment(draft);
		}else{
			queuedAttachments.add(draft);
		}
		updatePublishButtonState();
	}

	private void uploadMediaAttachment(DraftMediaAttachment attachment){
		if(uploadingAttachment!=null)
			throw new IllegalStateException("there is already an attachment being uploaded");
		uploadingAttachment=attachment;
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
					}

					@Override
					public void onError(ErrorResponse error){
						attachment.uploadRequest=null;
						uploadingAttachment=null;
						failedAttachments.add(attachment);
						error.showToast(getActivity());
						// TODO show the error state in the attachment view

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
		attachmentsView.removeView(att.view);
		updatePublishButtonState();
		pollBtn.setEnabled(attachments.isEmpty() && queuedAttachments.isEmpty() && failedAttachments.isEmpty() && uploadingAttachment==null);
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

	private static class DraftMediaAttachment{
		public Attachment serverAttachment;
		public Uri uri;
		public UploadAttachment uploadRequest;

		public View view;
		public ProgressBar progressBar;
	}

	private static class DraftPollOption{
		public EditText edit;
		public View view;
		public View dragger;
	}
}
