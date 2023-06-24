package org.joinmastodon.android.ui.viewcontrollers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.drawables.EmptyDrawable;
import org.joinmastodon.android.ui.text.LengthLimitHighlighter;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CheckableLinearLayout;
import org.joinmastodon.android.ui.views.ReorderableLinearLayout;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class ComposePollViewController{
	private static final int[] POLL_LENGTH_OPTIONS={
			5*60,
			30*60,
			3600,
			6*3600,
			24*3600,
			3*24*3600,
			7*24*3600,
	};
	
	private final ComposeFragment fragment;
	private ViewGroup pollWrap;

	private ReorderableLinearLayout pollOptionsView;
	private View addPollOptionBtn;
	private ImageView deletePollOptionBtn;
	private ViewGroup pollSettingsView;
	private View pollPoof;
	private View pollDurationButton, pollStyleButton;
	private TextView pollDurationValue, pollStyleValue;

	private int pollDuration=24*3600;
	private boolean pollIsMultipleChoice;
	private ArrayList<DraftPollOption> pollOptions=new ArrayList<>();
	private boolean pollChanged;

	private int maxPollOptions=4;
	private int maxPollOptionLength=50;

	public ComposePollViewController(ComposeFragment fragment){
		this.fragment=fragment;
	}

	public void setView(View view, Bundle savedInstanceState){
		pollWrap=view.findViewById(R.id.poll_wrap);

		Instance instance=fragment.instance;
		if(instance.configuration!=null && instance.configuration.polls!=null && instance.configuration.polls.maxOptions>0)
			maxPollOptions=instance.configuration.polls.maxOptions;
		if(instance.configuration!=null && instance.configuration.polls!=null && instance.configuration.polls.maxCharactersPerOption>0)
			maxPollOptionLength=instance.configuration.polls.maxCharactersPerOption;

		pollOptionsView=pollWrap.findViewById(R.id.poll_options);
		addPollOptionBtn=pollWrap.findViewById(R.id.add_poll_option);
		deletePollOptionBtn=pollWrap.findViewById(R.id.delete_poll_option);
		pollSettingsView=pollWrap.findViewById(R.id.poll_settings);
		pollPoof=pollWrap.findViewById(R.id.poll_poof);

		addPollOptionBtn.setOnClickListener(v->{
			createDraftPollOption(true).edit.requestFocus();
			updatePollOptionHints();
		});
		pollOptionsView.setMoveInBothDimensions(true);
		pollOptionsView.setDragListener(new OptionDragListener());
		pollOptionsView.setDividerDrawable(new EmptyDrawable(1, V.dp(8)));
		pollDurationButton=pollWrap.findViewById(R.id.poll_duration);
		pollDurationValue=pollWrap.findViewById(R.id.poll_duration_value);
		pollDurationButton.setOnClickListener(v->showPollDurationAlert());
		pollStyleButton=pollWrap.findViewById(R.id.poll_style);
		pollStyleValue=pollWrap.findViewById(R.id.poll_style_value);
		pollStyleButton.setOnClickListener(v->showPollStyleAlert());

		if(!fragment.getWasDetached() && savedInstanceState!=null && savedInstanceState.containsKey("pollOptions")){ // Fragment was recreated without retaining instance
			pollWrap.setVisibility(View.VISIBLE);
			for(String oldText:savedInstanceState.getStringArrayList("pollOptions")){
				DraftPollOption opt=createDraftPollOption(false);
				opt.edit.setText(oldText);
			}
			updatePollOptionHints();
			pollDuration=savedInstanceState.getInt("pollDuration");
			pollIsMultipleChoice=savedInstanceState.getBoolean("pollMultiple");
			pollDurationValue.setText(UiUtils.formatDuration(fragment.getContext(), pollDuration));
			pollStyleValue.setText(pollIsMultipleChoice ? R.string.compose_poll_multiple_choice : R.string.compose_poll_single_choice);
		}else if(savedInstanceState!=null && !pollOptions.isEmpty()){ // Fragment was recreated but instance was retained
			pollWrap.setVisibility(View.VISIBLE);
			ArrayList<DraftPollOption> oldOptions=new ArrayList<>(pollOptions);
			pollOptions.clear();
			for(DraftPollOption oldOpt:oldOptions){
				DraftPollOption opt=createDraftPollOption(false);
				opt.edit.setText(oldOpt.edit.getText());
			}
			updatePollOptionHints();
			pollDurationValue.setText(UiUtils.formatDuration(fragment.getContext(), pollDuration));
			pollStyleValue.setText(pollIsMultipleChoice ? R.string.compose_poll_multiple_choice : R.string.compose_poll_single_choice);
		}else if(savedInstanceState==null && fragment.editingStatus!=null && fragment.editingStatus.poll!=null){
			pollWrap.setVisibility(View.VISIBLE);
			for(Poll.Option eopt:fragment.editingStatus.poll.options){
				DraftPollOption opt=createDraftPollOption(false);
				opt.edit.setText(eopt.title);
			}
			pollDuration=(int)fragment.editingStatus.poll.expiresAt.minus(fragment.editingStatus.createdAt.toEpochMilli(), ChronoUnit.MILLIS).getEpochSecond();
			updatePollOptionHints();
			pollDurationValue.setText(UiUtils.formatDuration(fragment.getContext(), pollDuration));
			pollIsMultipleChoice=fragment.editingStatus.poll.multiple;
			pollStyleValue.setText(pollIsMultipleChoice ? R.string.compose_poll_multiple_choice : R.string.compose_poll_single_choice);
		}else{
			pollDurationValue.setText(UiUtils.formatDuration(fragment.getContext(), 24*3600));
			pollStyleValue.setText(R.string.compose_poll_single_choice);
		}
	}

	private DraftPollOption createDraftPollOption(boolean animated){
		DraftPollOption option=new DraftPollOption();
		option.view=LayoutInflater.from(fragment.getActivity()).inflate(R.layout.compose_poll_option, pollOptionsView, false);
		option.edit=option.view.findViewById(R.id.edit);
		option.dragger=option.view.findViewById(R.id.dragger_thingy);

		option.dragger.setOnLongClickListener(v->{
			pollOptionsView.startDragging(option.view);
			return true;
		});
		option.edit.addTextChangedListener(new SimpleTextWatcher(e->{
			if(!fragment.isCreatingView())
				pollChanged=true;
			fragment.updatePublishButtonState();
		}));
		option.view.setOutlineProvider(OutlineProviders.roundedRect(4));
		option.view.setClipToOutline(true);
		option.view.setTag(option);

		if(animated)
			UiUtils.beginLayoutTransition(pollWrap);
		pollOptionsView.addView(option.view);
		pollOptions.add(option);
		addPollOptionBtn.setEnabled(pollOptions.size()<maxPollOptions);
		option.edit.addTextChangedListener(new LengthLimitHighlighter(fragment.getActivity(), maxPollOptionLength).setListener(isOverLimit->{
			option.view.setForeground(fragment.getResources().getDrawable(isOverLimit ? R.drawable.bg_m3_outlined_text_field_error_nopad : R.drawable.bg_m3_outlined_text_field_nopad, fragment.getActivity().getTheme()));
		}));
		return option;
	}

	private void updatePollOptionHints(){
		int i=0;
		for(DraftPollOption option:pollOptions){
			option.edit.setHint(fragment.getString(R.string.poll_option_hint, ++i));
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
			options[i]=UiUtils.formatDuration(fragment.getContext(), l);
			if(l==pollDuration)
				selectedOption=i;
		}
		int[] chosenOption={0};
		new M3AlertDialogBuilder(fragment.getActivity())
				.setSingleChoiceItems(options, selectedOption, (dialog, which)->chosenOption[0]=which)
				.setTitle(R.string.poll_length)
				.setPositiveButton(R.string.ok, (dialog, which)->{
					pollDuration=POLL_LENGTH_OPTIONS[chosenOption[0]];
					pollDurationValue.setText(UiUtils.formatDuration(fragment.getContext(), pollDuration));
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void showPollStyleAlert(){
		final int[] option={pollIsMultipleChoice ? R.id.multiple_choice : R.id.single_choice};
		AlertDialog alert=new M3AlertDialogBuilder(fragment.getActivity())
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
	
	public void onSaveInstanceState(Bundle outState){
		if(!pollOptions.isEmpty()){
			ArrayList<String> opts=new ArrayList<>();
			for(DraftPollOption opt:pollOptions){
				opts.add(opt.edit.getText().toString());
			}
			outState.putStringArrayList("pollOptions", opts);
			outState.putInt("pollDuration", pollDuration);
			outState.putBoolean("pollMultiple", pollIsMultipleChoice);
		}
	}

	public boolean isEmpty(){
		return pollOptions.isEmpty();
	}

	public int getNonEmptyOptionsCount(){
		int nonEmptyPollOptionsCount=0;
		for(DraftPollOption opt:pollOptions){
			if(opt.edit.length()>0)
				nonEmptyPollOptionsCount++;
		}
		return nonEmptyPollOptionsCount;
	}

	public void toggle(){
		if(pollOptions.isEmpty()){
			pollWrap.setVisibility(View.VISIBLE);
			for(int i=0;i<2;i++)
				createDraftPollOption(false);
			updatePollOptionHints();
		}else{
			pollWrap.setVisibility(View.GONE);
			addPollOptionBtn.setVisibility(View.VISIBLE);
			pollOptionsView.removeAllViews();
			pollOptions.clear();
			pollDuration=24*3600;
		}
	}

	public boolean isShown(){
		return !pollOptions.isEmpty();
	}

	public boolean isPollChanged(){
		return pollChanged;
	}

	public CreateStatus.Request.Poll getPollForRequest(){
		CreateStatus.Request.Poll poll=new CreateStatus.Request.Poll();
		poll.expiresIn=pollDuration;
		poll.multiple=pollIsMultipleChoice;
		for(DraftPollOption opt:pollOptions)
			poll.options.add(opt.edit.getText().toString());
		return poll;
	}

	private static class DraftPollOption{
		public EditText edit;
		public View view;
		public View dragger;
	}

	private class OptionDragListener implements ReorderableLinearLayout.OnDragListener{
		private boolean isOverDelete;
		private RectF rect1, rect2;
		private Animator deletionStateAnimator;

		public OptionDragListener(){
			rect1=new RectF();
			rect2=new RectF();
		}

		@Override
		public void onSwapItems(int oldIndex, int newIndex){
			onSwapPollOptions(oldIndex, newIndex);
		}

		@Override
		public void onDragStart(View view){
			isOverDelete=false;
			ReorderableLinearLayout.OnDragListener.super.onDragStart(view);
			DraftPollOption dpo=(DraftPollOption) view.getTag();
			int color=UiUtils.getThemeColor(fragment.getActivity(), R.attr.colorM3OnSurface);
			ObjectAnimator anim=ObjectAnimator.ofArgb(dpo.edit, "backgroundColor", color & 0xffffff, color & 0x29ffffff);
			anim.setDuration(150);
			anim.setInterpolator(CubicBezierInterpolator.DEFAULT);
			anim.start();
			fragment.mainLayout.setClipChildren(false);
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
			int color=UiUtils.getThemeColor(fragment.getActivity(), R.attr.colorM3OnSurface);
			ObjectAnimator anim=ObjectAnimator.ofArgb(dpo.edit, "backgroundColor", color & 0x29ffffff, color & 0xffffff);
			anim.setDuration(200);
			anim.setInterpolator(CubicBezierInterpolator.DEFAULT);
			anim.addListener(new AnimatorListenerAdapter(){
				@Override
				public void onAnimationEnd(Animator animation){
					fragment.mainLayout.setClipChildren(true);
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
				dpo.view.setForeground(fragment.getResources().getDrawable(newOverDelete || dpo.edit.length()>maxPollOptionLength ? R.drawable.bg_m3_outlined_text_field_error_nopad : R.drawable.bg_m3_outlined_text_field_nopad, fragment.getActivity().getTheme()));
				int errorContainer=UiUtils.getThemeColor(fragment.getActivity(), R.attr.colorM3ErrorContainer);
				int surface=UiUtils.getThemeColor(fragment.getActivity(), R.attr.colorM3Surface);
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
	}
}
