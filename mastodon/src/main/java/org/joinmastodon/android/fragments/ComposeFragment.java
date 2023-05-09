package org.joinmastodon.android.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
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
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.icu.text.BreakIterator;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import org.joinmastodon.android.api.requests.statuses.UpdateAttachment;
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
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.PopupKeyboard;
import org.joinmastodon.android.ui.drawables.EmptyDrawable;
import org.joinmastodon.android.ui.drawables.SpoilerStripesDrawable;
import org.joinmastodon.android.ui.text.ComposeAutocompleteSpan;
import org.joinmastodon.android.ui.text.ComposeHashtagOrMentionSpan;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.text.LengthLimitHighlighter;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CheckableLinearLayout;
import org.joinmastodon.android.ui.views.ComposeEditText;
import org.joinmastodon.android.ui.views.ReorderableLinearLayout;
import org.joinmastodon.android.ui.views.SizeListenerLinearLayout;
import org.joinmastodon.android.utils.TransferSpeedTracker;
import org.parceler.Parcel;
import org.parceler.Parcels;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class ComposeFragment extends MastodonToolbarFragment implements OnBackPressedListener, ComposeEditText.SelectionListener{

	private static final int MEDIA_RESULT=717;
	private static final int IMAGE_DESCRIPTION_RESULT=363;
	private static final int MAX_ATTACHMENTS=4;
	private static final String TAG="ComposeFragment";
	private static final int[] POLL_LENGTH_OPTIONS={
			5*60,
			30*60,
			3600,
			6*3600,
			24*3600,
			3*24*3600,
			7*24*3600,
	};

	private static final Pattern MENTION_PATTERN=Pattern.compile("(^|[^\\/\\w])@(([a-z0-9_]+)@[a-z0-9\\.\\-]+[a-z0-9]+)", Pattern.CASE_INSENSITIVE);

	// from https://github.com/mastodon/mastodon-ios/blob/main/Mastodon/Helper/MastodonRegex.swift
	private static final Pattern AUTO_COMPLETE_PATTERN=Pattern.compile("(?<!\\w)(?:@([a-zA-Z0-9_]+)(@[a-zA-Z0-9_.-]+)?|#([^\\s.]+)|:([a-zA-Z0-9_]+))");
	private static final Pattern HIGHLIGHT_PATTERN=Pattern.compile("(?<!\\w)(?:@([a-zA-Z0-9_]+)(@[a-zA-Z0-9_.-]+)?|#([^\\s.]+))");

	@SuppressLint("NewApi") // this class actually exists on 6.0
	private final BreakIterator breakIterator=BreakIterator.getCharacterInstance();

	private LinearLayout mainLayout;
	private SizeListenerLinearLayout contentView;
	private TextView selfName, selfUsername;
	private ImageView selfAvatar;
	private Account self;
	private String instanceDomain;

	private ComposeEditText mainEditText;
	private TextView charCounter;
	private String accountID;
	private int charCount, charLimit, trimmedCharCount;

	private ImageButton mediaBtn, pollBtn, emojiBtn, spoilerBtn;
	private ReorderableLinearLayout attachmentsView;
	private HorizontalScrollView attachmentsScroller;
	private TextView replyText;
	private ReorderableLinearLayout pollOptionsView;
	private ViewGroup pollWrap;
	private View addPollOptionBtn;
	private Button visibilityBtn;
	private LinearLayout bottomBar;
	private ImageView deletePollOptionBtn;
	private ViewGroup pollSettingsView;
	private View pollPoof;
	private View autocompleteDivider;

	private View pollDurationButton, pollStyleButton;
	private TextView pollDurationValue, pollStyleValue;

	private ArrayList<DraftPollOption> pollOptions=new ArrayList<>();

	private ArrayList<DraftMediaAttachment> attachments=new ArrayList<>();

	private List<EmojiCategory> customEmojis;
	private CustomEmojiPopupKeyboard emojiKeyboard;
	private Status replyTo;
	private String initialText;
	private String uuid;
	private int pollDuration=24*3600;
	private boolean pollIsMultipleChoice;
	private EditText spoilerEdit;
	private View spoilerWrap;
	private boolean hasSpoiler;
	private ProgressBar sendProgress;
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
	private MenuItem publishButton;
	private boolean wasDetached;

	private BackgroundColorSpan overLimitBG;
	private ForegroundColorSpan overLimitFG;

	private int maxPollOptions=4;
	private int maxPollOptionLength=50;

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

		if(instance.configuration!=null && instance.configuration.polls!=null && instance.configuration.polls.maxOptions>0)
			maxPollOptions=instance.configuration.polls.maxOptions;
		if(instance.configuration!=null && instance.configuration.polls!=null && instance.configuration.polls.maxCharactersPerOption>0)
			maxPollOptionLength=instance.configuration.polls.maxCharactersPerOption;

		if (editingStatus == null) loadDefaultStatusVisibility(savedInstanceState);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		for(DraftMediaAttachment att:attachments){
			if(att.isUploadingOrProcessing())
				att.cancelUpload();
		}
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
		setTitle(R.string.new_post);
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
		emojiKeyboard.setListener(this::onCustomEmojiClick);

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
		replyText=view.findViewById(R.id.reply_text);

		mediaBtn.setOnClickListener(v->openFilePicker());
		pollBtn.setOnClickListener(v->togglePoll());
		emojiBtn.setOnClickListener(v->emojiKeyboard.toggleKeyboardPopup(mainEditText));
		spoilerBtn.setOnClickListener(v->toggleSpoiler());
		visibilityBtn.setOnClickListener(this::onVisibilityClick);
		Drawable arrow=getResources().getDrawable(R.drawable.ic_baseline_arrow_drop_down_18, getActivity().getTheme()).mutate();
		arrow.setTint(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurface));
		visibilityBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, arrow, null);
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
		attachmentsScroller=view.findViewById(R.id.attachments_scroller);
		pollOptionsView=view.findViewById(R.id.poll_options);
		pollWrap=view.findViewById(R.id.poll_wrap);
		addPollOptionBtn=view.findViewById(R.id.add_poll_option);
		deletePollOptionBtn=view.findViewById(R.id.delete_poll_option);
		pollSettingsView=view.findViewById(R.id.poll_settings);
		pollPoof=view.findViewById(R.id.poll_poof);
		attachmentsView.setDividerDrawable(new EmptyDrawable(V.dp(8), 0));
		attachmentsView.setDragListener(new ReorderableLinearLayout.OnDragListener(){
			private final HashMap<View, Animator> currentAnimations=new HashMap<>();

			@Override
			public void onSwapItems(int oldIndex, int newIndex){
				attachments.add(newIndex, attachments.remove(oldIndex));
			}

			@Override
			public void onDragStart(View view){
				if(currentAnimations.containsKey(view))
					currentAnimations.get(view).cancel();
				mainLayout.setClipChildren(false);
				AnimatorSet set=new AnimatorSet();
				DraftMediaAttachment att=(DraftMediaAttachment) view.getTag();
				att.dragLayer.setVisibility(View.VISIBLE);
				set.playTogether(
						ObjectAnimator.ofFloat(view, View.TRANSLATION_Z, V.dp(3)),
						ObjectAnimator.ofFloat(att.dragLayer, View.ALPHA, 0.16f)
				);
				set.setDuration(150);
				set.setInterpolator(CubicBezierInterpolator.DEFAULT);
				set.addListener(new AnimatorListenerAdapter(){
					@Override
					public void onAnimationEnd(Animator animation){
						currentAnimations.remove(view);
					}
				});
				currentAnimations.put(view, set);
				set.start();
			}

			@Override
			public void onDragEnd(View view){
				if(currentAnimations.containsKey(view))
					currentAnimations.get(view).cancel();
				AnimatorSet set=new AnimatorSet();
				DraftMediaAttachment att=(DraftMediaAttachment) view.getTag();
				set.playTogether(
						ObjectAnimator.ofFloat(att.dragLayer, View.ALPHA, 0),
						ObjectAnimator.ofFloat(view, View.TRANSLATION_Z, 0),
						ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0),
						ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0)
				);
				set.setDuration(200);
				set.setInterpolator(CubicBezierInterpolator.DEFAULT);
				set.addListener(new AnimatorListenerAdapter(){
					@Override
					public void onAnimationEnd(Animator animation){
						if(currentAnimations.isEmpty())
							mainLayout.setClipChildren(true);
						att.dragLayer.setVisibility(View.GONE);
						currentAnimations.remove(view);
					}
				});
				currentAnimations.put(view, set);
				set.start();
			}
		});
		attachmentsView.setMoveInBothDimensions(true);

		addPollOptionBtn.setOnClickListener(v->{
			createDraftPollOption().edit.requestFocus();
			updatePollOptionHints();
		});
		pollOptionsView.setMoveInBothDimensions(true);
		pollOptionsView.setDragListener(new ReorderableLinearLayout.OnDragListener(){
			private boolean isOverDelete;
			private RectF rect1=new RectF(), rect2=new RectF();
			private Animator deletionStateAnimator;

			@Override
			public void onSwapItems(int oldIndex, int newIndex){
				ComposeFragment.this.onSwapPollOptions(oldIndex, newIndex);
			}

			@Override
			public void onDragStart(View view){
				isOverDelete=false;
				ReorderableLinearLayout.OnDragListener.super.onDragStart(view);
				DraftPollOption dpo=(DraftPollOption) view.getTag();
				int color=UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurface);
				ObjectAnimator anim=ObjectAnimator.ofArgb(dpo.edit, "backgroundColor", color & 0xffffff, color & 0x29ffffff);
				anim.setDuration(150);
				anim.setInterpolator(CubicBezierInterpolator.DEFAULT);
				anim.start();
				mainLayout.setClipChildren(false);
				if(pollOptions.size()>2){
//					UiUtils.beginLayoutTransition(pollSettingsView);
					deletePollOptionBtn.setVisibility(View.VISIBLE);
					addPollOptionBtn.setVisibility(View.GONE);
				}
			}

			@Override
			public void onDragEnd(View view){
				if(pollOptions.size()>2){
//					UiUtils.beginLayoutTransition(pollSettingsView);
					deletePollOptionBtn.setVisibility(View.GONE);
					addPollOptionBtn.setVisibility(View.VISIBLE);
				}

				DraftPollOption dpo=(DraftPollOption) view.getTag();
				if(isOverDelete){
					pollPoof.setVisibility(View.VISIBLE);
					AnimatorSet set=new AnimatorSet();
					set.playTogether(
							ObjectAnimator.ofFloat(pollPoof, View.ALPHA, 0f, 0.7f, 1f, 0f),
							ObjectAnimator.ofFloat(pollPoof, View.SCALE_X, 1f, 4f),
							ObjectAnimator.ofFloat(pollPoof, View.SCALE_Y, 1f, 4f),
							ObjectAnimator.ofFloat(pollPoof, View.ROTATION, 0f, 60f)
					);
					set.setDuration(600);
					set.setInterpolator(CubicBezierInterpolator.DEFAULT);
					set.addListener(new AnimatorListenerAdapter(){
						@Override
						public void onAnimationEnd(Animator animation){
							pollPoof.setVisibility(View.INVISIBLE);
						}
					});
					set.start();
					UiUtils.beginLayoutTransition(pollWrap);
					pollOptions.remove(dpo);
					pollOptionsView.removeView(view);
					addPollOptionBtn.setEnabled(pollOptions.size()<maxPollOptions);
					return;
				}
				ReorderableLinearLayout.OnDragListener.super.onDragEnd(view);
				int color=UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurface);
				ObjectAnimator anim=ObjectAnimator.ofArgb(dpo.edit, "backgroundColor", color & 0x29ffffff, color & 0xffffff);
				anim.setDuration(200);
				anim.setInterpolator(CubicBezierInterpolator.DEFAULT);
				anim.addListener(new AnimatorListenerAdapter(){
					@Override
					public void onAnimationEnd(Animator animation){
						mainLayout.setClipChildren(true);
					}
				});
				anim.start();
			}

			@Override
			public void onDragMove(View view){
				if(pollOptions.size()<3)
					return;
				DraftPollOption dpo=(DraftPollOption) view.getTag();
				// Yes, I don't like this either.
				float draggerX=view.getX()+dpo.dragger.getX()+pollOptionsView.getX();
				float deleteButtonX=pollSettingsView.getX()+deletePollOptionBtn.getX();
				rect1.set(deleteButtonX, pollOptionsView.getHeight(), deleteButtonX+deletePollOptionBtn.getWidth(), pollWrap.getHeight());
				rect2.set(draggerX, view.getY(), draggerX+dpo.dragger.getWidth(), view.getY()+view.getHeight());
				boolean newOverDelete=rect1.intersect(rect2);
				if(newOverDelete!=isOverDelete){
					if(deletionStateAnimator!=null)
						deletionStateAnimator.cancel();
					isOverDelete=newOverDelete;
					if(newOverDelete)
						view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
					dpo.view.setForeground(getResources().getDrawable(newOverDelete || dpo.edit.length()>maxPollOptionLength ? R.drawable.bg_m3_outlined_text_field_error_nopad : R.drawable.bg_m3_outlined_text_field_nopad, getActivity().getTheme()));
					int errorContainer=UiUtils.getThemeColor(getActivity(), R.attr.colorM3ErrorContainer);
					int surface=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Surface);
					AnimatorSet set=new AnimatorSet();
					set.playTogether(
							ObjectAnimator.ofFloat(view, View.ALPHA, newOverDelete ? .85f : 1),
							ObjectAnimator.ofArgb(view, "backgroundColor", newOverDelete ? surface : errorContainer, newOverDelete ? errorContainer : surface)
					);
					set.setDuration(150);
					set.setInterpolator(CubicBezierInterpolator.DEFAULT);
					deletionStateAnimator=set;
					set.addListener(new AnimatorListenerAdapter(){
						@Override
						public void onAnimationEnd(Animator animation){
							deletionStateAnimator=null;
						}
					});
					set.start();
				}
			}
		});
		pollOptionsView.setDividerDrawable(new EmptyDrawable(1, V.dp(8)));
		pollDurationButton=view.findViewById(R.id.poll_duration);
		pollDurationValue=view.findViewById(R.id.poll_duration_value);
		pollDurationButton.setOnClickListener(v->showPollDurationAlert());
		pollStyleButton=view.findViewById(R.id.poll_style);
		pollStyleValue=view.findViewById(R.id.poll_style_value);
		pollStyleButton.setOnClickListener(v->showPollStyleAlert());

		pollOptions.clear();
		if(!wasDetached && savedInstanceState!=null && savedInstanceState.containsKey("pollOptions")){
			pollBtn.setSelected(true);
			mediaBtn.setEnabled(false);
			pollWrap.setVisibility(View.VISIBLE);
			for(String oldText:savedInstanceState.getStringArrayList("pollOptions")){
				DraftPollOption opt=createDraftPollOption();
				opt.edit.setText(oldText);
			}
			updatePollOptionHints();
			pollDurationValue.setText(formatPollDuration(pollDuration));
			pollStyleValue.setText(pollIsMultipleChoice ? R.string.compose_poll_multiple_choice : R.string.compose_poll_single_choice);
		}else if(wasDetached && savedInstanceState==null && editingStatus!=null && editingStatus.poll!=null){
			pollBtn.setSelected(true);
			mediaBtn.setEnabled(false);
			pollWrap.setVisibility(View.VISIBLE);
			for(Poll.Option eopt:editingStatus.poll.options){
				DraftPollOption opt=createDraftPollOption();
				opt.edit.setText(eopt.title);
			}
			pollDuration=(int)editingStatus.poll.expiresAt.minus(editingStatus.createdAt.toEpochMilli(), ChronoUnit.MILLIS).getEpochSecond();
			updatePollOptionHints();
			pollDurationValue.setText(formatPollDuration(pollDuration));
			pollIsMultipleChoice=editingStatus.poll.multiple;
			pollStyleValue.setText(pollIsMultipleChoice ? R.string.compose_poll_multiple_choice : R.string.compose_poll_single_choice);
		}else{
			pollDurationValue.setText(formatPollDuration(24*3600));
			pollStyleValue.setText(R.string.compose_poll_single_choice);
		}

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

		if(!wasDetached && savedInstanceState!=null && savedInstanceState.containsKey("attachments")){
			ArrayList<Parcelable> serializedAttachments=savedInstanceState.getParcelableArrayList("attachments");
			for(Parcelable a:serializedAttachments){
				DraftMediaAttachment att=Parcels.unwrap(a);
				attachmentsView.addView(createMediaAttachmentView(att));
				attachments.add(att);
			}
			attachmentsScroller.setVisibility(View.VISIBLE);
			updateMediaAttachmentsLayout();
		}else if(!attachments.isEmpty()){
			attachmentsScroller.setVisibility(View.VISIBLE);
			for(DraftMediaAttachment att:attachments){
				attachmentsView.addView(createMediaAttachmentView(att));
			}
			updateMediaAttachmentsLayout();
		}

		if(editingStatus!=null && editingStatus.visibility!=null) {
			statusVisibility=editingStatus.visibility;
		}
		updateVisibilityIcon();

		autocompleteViewController=new ComposeAutocompleteViewController(getActivity(), accountID);
		autocompleteViewController.setCompletionSelectedListener(this::onAutocompleteOptionSelected);
		View autocompleteView=autocompleteViewController.getView();
		autocompleteView.setVisibility(View.INVISIBLE);
		bottomBar.addView(autocompleteView, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(56)));
		autocompleteDivider=view.findViewById(R.id.bottom_bar_autocomplete_divider);

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
				if(!editingStatus.mediaAttachments.isEmpty()){
					attachmentsScroller.setVisibility(View.VISIBLE);
					for(Attachment att:editingStatus.mediaAttachments){
						DraftMediaAttachment da=new DraftMediaAttachment();
						da.serverAttachment=att;
						da.description=att.description;
						da.uri=att.previewUrl!=null ? Uri.parse(att.previewUrl) : null;
						da.state=AttachmentUploadState.DONE;
						attachmentsView.addView(createMediaAttachmentView(da));
						attachments.add(da);
					}
					updateMediaAttachmentsLayout();
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
		inflater.inflate(R.menu.compose, menu);
		publishButton=menu.findItem(R.id.publish);
		updatePublishButtonState();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getItemId()==R.id.publish){
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
		if(getActivity().getCurrentFocus() instanceof EditText edit){
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
	}

	@Override
	protected int getNavigationIconDrawableResource(){
		return R.drawable.ic_baseline_close_24;
	}

	@Override
	public boolean wantsCustomNavigationIcon(){
		return true;
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

		ArrayList<UpdateAttachment> updateAltTextRequests=new ArrayList<>();
		for(DraftMediaAttachment att:attachments){
			if(!att.descriptionSaved){
				UpdateAttachment req=new UpdateAttachment(att.serverAttachment.id, att.description);
				req.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Attachment result){
								att.descriptionSaved=true;
								att.serverAttachment=result;
								updateAltTextRequests.remove(req);
								if(updateAltTextRequests.isEmpty())
									actuallyPublish();
							}

							@Override
							public void onError(ErrorResponse error){
								handlePublishError(error);
							}
						})
						.exec(accountID);
				updateAltTextRequests.add(req);
			}
		}
		if(updateAltTextRequests.isEmpty())
			actuallyPublish();
	}

	private void actuallyPublish(){
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
			req.poll.multiple=pollIsMultipleChoice;
			for(DraftPollOption opt:pollOptions)
				req.poll.options.add(opt.edit.getText().toString());
		}
		if(hasSpoiler && spoilerEdit.length()>0){
			req.spoilerText=spoilerEdit.getText().toString();
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
			String attID=result.getString("attachment");
			String text=result.getString("text");
			for(DraftMediaAttachment att:attachments){
				if(att.serverAttachment.id.equals(attID)){
					att.descriptionSaved=false;
					att.description=text;
					att.setDescriptionToTitle();
					break;
				}
			}
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
			intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, MAX_ATTACHMENTS-getMediaAttachmentsCount());
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
		int size;
		try(Cursor cursor=MastodonApp.context.getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)){
			cursor.moveToFirst();
			size=cursor.getInt(0);
		}catch(Exception x){
			Log.w("ComposeFragment", x);
			return false;
		}
		if(instance!=null && instance.configuration!=null && instance.configuration.mediaAttachments!=null){
			if(instance.configuration.mediaAttachments.supportedMimeTypes!=null && !instance.configuration.mediaAttachments.supportedMimeTypes.contains(type)){
				showMediaAttachmentError(getString(R.string.media_attachment_unsupported_type, UiUtils.getFileName(uri)));
				return false;
			}
			if(!type.startsWith("image/")){
				int sizeLimit=instance.configuration.mediaAttachments.videoSizeLimit;
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
		draft.fileSize=size;

		UiUtils.beginLayoutTransition(attachmentsScroller);
		attachmentsView.addView(createMediaAttachmentView(draft), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT));
		attachments.add(draft);
		attachmentsScroller.setVisibility(View.VISIBLE);
		updateMediaAttachmentsLayout();
//		draft.setOverlayVisible(true, false);

		if(!areThereAnyUploadingAttachments()){
			uploadNextQueuedAttachment();
		}
		updatePublishButtonState();
		if(getMediaAttachmentsCount()==MAX_ATTACHMENTS)
			mediaBtn.setEnabled(false);
		return true;
	}

	private void updateMediaAttachmentsLayout(){
		int newWidth=attachments.size()>2 ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
		if(newWidth!=attachmentsView.getLayoutParams().width){
			attachmentsView.getLayoutParams().width=newWidth;
			attachmentsScroller.requestLayout();
		}
		for(DraftMediaAttachment att:attachments){
			LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams) att.view.getLayoutParams();
			if(attachments.size()<3){
				lp.width=0;
				lp.weight=1f;
			}else{
				lp.width=V.dp(200);
				lp.weight=0f;
			}
		}
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
			if(draft.serverAttachment.previewUrl!=null)
				ViewImageLoader.load(img, draft.serverAttachment.blurhashPlaceholder, new UrlImageLoaderRequest(draft.serverAttachment.previewUrl, V.dp(250), V.dp(250)));
		}else{
			if(draft.mimeType.startsWith("image/")){
				ViewImageLoader.load(img, null, new UrlImageLoaderRequest(draft.uri, V.dp(250), V.dp(250)));
			}else if(draft.mimeType.startsWith("video/")){
				loadVideoThumbIntoView(img, draft.uri);
			}
		}

		draft.view=thumb;
		draft.imageView=img;
		draft.progressBar=thumb.findViewById(R.id.progress);
		draft.titleView=thumb.findViewById(R.id.title);
		draft.subtitleView=thumb.findViewById(R.id.subtitle);
		draft.removeButton=thumb.findViewById(R.id.delete);
		draft.editButton=thumb.findViewById(R.id.edit);
		draft.dragLayer=thumb.findViewById(R.id.drag_layer);

		draft.removeButton.setTag(draft);
		draft.removeButton.setOnClickListener(this::onRemoveMediaAttachmentClick);
		draft.editButton.setTag(draft);

		thumb.setOutlineProvider(OutlineProviders.roundedRect(12));
		thumb.setClipToOutline(true);
		img.setOutlineProvider(OutlineProviders.roundedRect(12));
		img.setClipToOutline(true);

		thumb.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Surface));
		thumb.setOnLongClickListener(v->{
			if(!v.hasTransientState() && attachments.size()>1){
				attachmentsView.startDragging(v);
				return true;
			}
			return false;
		});
		thumb.setTag(draft);


		int subtitleRes=switch(Objects.requireNonNullElse(draft.mimeType, "").split("/")[0]){
			case "image" -> R.string.attachment_description_image;
			case "video" -> R.string.attachment_description_video;
			case "audio" -> R.string.attachment_description_audio;
			default -> R.string.attachment_description_unknown;
		};
		draft.titleView.setText(getString(R.string.attachment_x_percent_uploaded, 0));
		draft.subtitleView.setText(getString(subtitleRes, UiUtils.formatFileSize(getActivity(), draft.fileSize, true)));
		draft.removeButton.setImageResource(R.drawable.ic_baseline_close_24);

		if(draft.state==AttachmentUploadState.ERROR){
			draft.titleView.setText(R.string.upload_failed);
			draft.editButton.setImageResource(R.drawable.ic_restart_alt_24px);
			draft.editButton.setOnClickListener(ComposeFragment.this::onRetryOrCancelMediaUploadClick);
			draft.progressBar.setVisibility(View.GONE);
			draft.setUseErrorColors(true);
		}else if(draft.state==AttachmentUploadState.DONE){
			draft.setDescriptionToTitle();
			draft.progressBar.setVisibility(View.GONE);
			draft.editButton.setOnClickListener(this::onEditMediaDescriptionClick);
		}else{
			draft.editButton.setVisibility(View.GONE);
			draft.removeButton.setImageResource(R.drawable.ic_baseline_close_24);
			if(draft.state==AttachmentUploadState.PROCESSING){
				draft.titleView.setText(R.string.upload_processing);
			}else{
				draft.titleView.setText(getString(R.string.attachment_x_percent_uploaded, 0));
			}
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
		attachmentsScroller.setVisibility(View.VISIBLE);
		updateMediaAttachmentsLayout();
	}

	private void uploadMediaAttachment(DraftMediaAttachment attachment){
		if(areThereAnyUploadingAttachments()){
			 throw new IllegalStateException("there is already an attachment being uploaded");
		}
		attachment.state=AttachmentUploadState.UPLOADING;
		attachment.progressBar.setVisibility(View.VISIBLE);
		int maxSize=0;
		String contentType=getActivity().getContentResolver().getType(attachment.uri);
		if(contentType!=null && contentType.startsWith("image/")){
			maxSize=2_073_600; // TODO get this from instance configuration when it gets added there
		}
		attachment.progressBar.setProgress(0);
		attachment.speedTracker.reset();
		attachment.speedTracker.addSample(0);
		attachment.uploadRequest=(UploadAttachment) new UploadAttachment(attachment.uri, maxSize, attachment.description)
				.setProgressListener(new ProgressListener(){
					@Override
					public void onProgress(long transferred, long total){
						float progressFraction=transferred/(float)total;
						int progress=Math.round(progressFraction*attachment.progressBar.getMax());
						if(Build.VERSION.SDK_INT>=24)
							attachment.progressBar.setProgress(progress, true);
						else
							attachment.progressBar.setProgress(progress);

						attachment.titleView.setText(getString(R.string.attachment_x_percent_uploaded, Math.round(progressFraction*100f)));

						attachment.speedTracker.setTotalBytes(total);
//						attachment.uploadStateTitle.setText(getString(R.string.file_upload_progress, UiUtils.formatFileSize(getActivity(), transferred, true), UiUtils.formatFileSize(getActivity(), total, true)));
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
							attachment.titleView.setText(R.string.upload_processing);
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
						attachment.state=AttachmentUploadState.ERROR;
						attachment.titleView.setText(R.string.upload_failed);
//						if(error instanceof MastodonErrorResponse er){
//							if(er.underlyingException instanceof SocketException || er.underlyingException instanceof UnknownHostException || er.underlyingException instanceof InterruptedIOException)
//								attachment.uploadStateText.setText(R.string.upload_error_connection_lost);
//							else
//								attachment.uploadStateText.setText(er.error);
//						}else{
//							attachment.uploadStateText.setText("");
//						}
//						attachment.retryButton.setImageResource(R.drawable.ic_fluent_arrow_clockwise_24_filled);
//						attachment.retryButton.setContentDescription(getString(R.string.retry_upload));

						V.setVisibilityAnimated(attachment.editButton, View.VISIBLE);
						attachment.editButton.setImageResource(R.drawable.ic_restart_alt_24px);
						attachment.editButton.setOnClickListener(ComposeFragment.this::onRetryOrCancelMediaUploadClick);
						attachment.setUseErrorColors(true);
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
		if(!areThereAnyUploadingAttachments())
			uploadNextQueuedAttachment();
		if(!attachments.isEmpty())
			UiUtils.beginLayoutTransition(attachmentsScroller);
		attachmentsView.removeView(att.view);
		if(getMediaAttachmentsCount()==0){
			attachmentsScroller.setVisibility(View.GONE);
		}else{
			updateMediaAttachmentsLayout();
		}
		updatePublishButtonState();
		pollBtn.setEnabled(attachments.isEmpty());
		mediaBtn.setEnabled(true);
	}

	private void onRetryOrCancelMediaUploadClick(View v){
		DraftMediaAttachment att=(DraftMediaAttachment) v.getTag();
		if(att.state==AttachmentUploadState.ERROR){
//			att.retryButton.setImageResource(R.drawable.ic_fluent_dismiss_24_filled);
//			att.retryButton.setContentDescription(getString(R.string.cancel));
			V.setVisibilityAnimated(att.progressBar, View.VISIBLE);
			V.setVisibilityAnimated(att.editButton, View.GONE);
			att.titleView.setText(getString(R.string.attachment_x_percent_uploaded, 0));
			att.state=AttachmentUploadState.QUEUED;
			att.setUseErrorColors(false);
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
		attachment.editButton.setImageResource(R.drawable.ic_edit_24px);
		attachment.removeButton.setImageResource(R.drawable.ic_delete_24px);
		attachment.editButton.setOnClickListener(this::onEditMediaDescriptionClick);
		V.setVisibilityAnimated(attachment.progressBar, View.GONE);
		V.setVisibilityAnimated(attachment.editButton, View.VISIBLE);
		attachment.setDescriptionToTitle();
		if(!areThereAnyUploadingAttachments())
			uploadNextQueuedAttachment();
		updatePublishButtonState();
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

	private void onEditMediaDescriptionClick(View v){
		DraftMediaAttachment att=(DraftMediaAttachment) v.getTag();
		if(att.serverAttachment==null)
			return;
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("attachment", att.serverAttachment.id);
		args.putParcelable("uri", att.uri);
		args.putString("existingDescription", att.description);
		args.putString("attachmentType", att.serverAttachment.type.toString());
		Drawable img=att.imageView.getDrawable();
		if(img!=null){
			args.putInt("width", img.getIntrinsicWidth());
			args.putInt("height", img.getIntrinsicHeight());
		}
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
		option.view.setOutlineProvider(OutlineProviders.roundedRect(4));
		option.view.setClipToOutline(true);
		option.view.setTag(option);

		UiUtils.beginLayoutTransition(pollWrap);
		pollOptionsView.addView(option.view);
		pollOptions.add(option);
		addPollOptionBtn.setEnabled(pollOptions.size()<maxPollOptions);
		option.edit.addTextChangedListener(new LengthLimitHighlighter(getActivity(), maxPollOptionLength).setListener(isOverLimit->{
			option.view.setForeground(getResources().getDrawable(isOverLimit ? R.drawable.bg_m3_outlined_text_field_error_nopad : R.drawable.bg_m3_outlined_text_field_nopad, getActivity().getTheme()));
		}));
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

	private void showPollDurationAlert(){
		String[] options=new String[POLL_LENGTH_OPTIONS.length];
		int selectedOption=-1;
		for(int i=0;i<POLL_LENGTH_OPTIONS.length;i++){
			int l=POLL_LENGTH_OPTIONS[i];
			options[i]=formatPollDuration(l);
			if(l==pollDuration)
				selectedOption=i;
		}
		int[] chosenOption={0};
		new M3AlertDialogBuilder(getActivity())
				.setSingleChoiceItems(options, selectedOption, (dialog, which)->chosenOption[0]=which)
				.setTitle(R.string.poll_length)
				.setPositiveButton(R.string.ok, (dialog, which)->{
					pollDuration=POLL_LENGTH_OPTIONS[chosenOption[0]];
					pollDurationValue.setText(formatPollDuration(pollDuration));
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private String formatPollDuration(int seconds){
		if(seconds<3600){
			int minutes=seconds/60;
			return getResources().getQuantityString(R.plurals.x_minutes, minutes, minutes);
		}else if(seconds<24*3600){
			int hours=seconds/3600;
			return getResources().getQuantityString(R.plurals.x_hours, hours, hours);
		}else{
			int days=seconds/(24*3600);
			return getResources().getQuantityString(R.plurals.x_days, days, days);
		}
	}

	private void showPollStyleAlert(){
		final int[] option={pollIsMultipleChoice ? R.id.multiple_choice : R.id.single_choice};
		AlertDialog alert=new M3AlertDialogBuilder(getActivity())
				.setView(R.layout.poll_style)
				.setTitle(R.string.poll_style_title)
				.setPositiveButton(R.string.ok, (dlg, which)->{
					pollIsMultipleChoice=option[0]==R.id.multiple_choice;
					pollStyleValue.setText(pollIsMultipleChoice ? R.string.compose_poll_multiple_choice : R.string.compose_poll_single_choice);
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
		CheckableLinearLayout multiple=alert.findViewById(R.id.multiple_choice);
		CheckableLinearLayout single=alert.findViewById(R.id.single_choice);
		single.setChecked(!pollIsMultipleChoice);
		multiple.setChecked(pollIsMultipleChoice);
		View.OnClickListener listener=v->{
			int id=v.getId();
			if(id==option[0])
				return;
			((Checkable) alert.findViewById(option[0])).setChecked(false);
			((Checkable) v).setChecked(true);
			option[0]=id;
		};
		single.setOnClickListener(listener);
		multiple.setOnClickListener(listener);
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

	private int getMediaAttachmentsCount(){
		return attachments.size();
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
		return addMediaAttachment(uri, description);
	}

	private void startAutocomplete(ComposeAutocompleteSpan span){
		currentAutocompleteSpan=span;
		Editable e=mainEditText.getText();
		String spanText=e.toString().substring(e.getSpanStart(span), e.getSpanEnd(span));
		autocompleteViewController.setText(spanText);
		UiUtils.beginLayoutTransition(bottomBar);
		View autocompleteView=autocompleteViewController.getView();
		bottomBar.getLayoutParams().height=ViewGroup.LayoutParams.WRAP_CONTENT;
		bottomBar.requestLayout();
		autocompleteView.setVisibility(View.VISIBLE);
		autocompleteDivider.setVisibility(View.VISIBLE);
	}

	private void finishAutocomplete(){
		if(currentAutocompleteSpan==null)
			return;
		autocompleteViewController.setText(null);
		currentAutocompleteSpan=null;
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
		public int fileSize;
		public boolean descriptionSaved=true;

		public transient View view;
		public transient ProgressBar progressBar;
		public transient ImageButton removeButton, editButton;
		public transient Runnable processingPollingRunnable;
		public transient ImageView imageView;
		public transient TextView titleView, subtitleView;
		public transient TransferSpeedTracker speedTracker=new TransferSpeedTracker();
		private transient boolean errorColors;
		private transient Animator errorTransitionAnimator;
		public transient View dragLayer;

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

		public void setDescriptionToTitle(){
			if(TextUtils.isEmpty(description)){
				titleView.setText(R.string.add_alt_text);
				titleView.setTextColor(UiUtils.getThemeColor(titleView.getContext(), R.attr.colorM3OnSurfaceVariant));
			}else{
				titleView.setText(description);
				titleView.setTextColor(UiUtils.getThemeColor(titleView.getContext(), R.attr.colorM3OnSurface));
			}
		}

		public void setUseErrorColors(boolean use){
			if(errorColors==use)
				return;
			errorColors=use;
			if(errorTransitionAnimator!=null)
				errorTransitionAnimator.cancel();
			AnimatorSet set=new AnimatorSet();
			int color1, color2, color3;
			if(use){
				color1=UiUtils.getThemeColor(view.getContext(), R.attr.colorM3ErrorContainer);
				color2=UiUtils.getThemeColor(view.getContext(), R.attr.colorM3Error);
				color3=UiUtils.getThemeColor(view.getContext(), R.attr.colorM3OnErrorContainer);
			}else{
				color1=UiUtils.getThemeColor(view.getContext(), R.attr.colorM3Surface);
				color2=UiUtils.getThemeColor(view.getContext(), R.attr.colorM3OnSurface);
				color3=UiUtils.getThemeColor(view.getContext(), R.attr.colorM3OnSurfaceVariant);
			}
			set.playTogether(
					ObjectAnimator.ofArgb(view, "backgroundColor", ((ColorDrawable)view.getBackground()).getColor(), color1),
					ObjectAnimator.ofArgb(titleView, "textColor", titleView.getCurrentTextColor(), color2),
					ObjectAnimator.ofArgb(subtitleView, "textColor", subtitleView.getCurrentTextColor(), color3),
					ObjectAnimator.ofArgb(removeButton.getDrawable(), "tint", subtitleView.getCurrentTextColor(), color3)
			);
			editButton.getDrawable().setTint(color3);
			set.setDuration(250);
			set.setInterpolator(CubicBezierInterpolator.DEFAULT);
			set.addListener(new AnimatorListenerAdapter(){
				@Override
				public void onAnimationEnd(Animator animation){
					errorTransitionAnimator=null;
				}
			});
			set.start();
			errorTransitionAnimator=set;
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
