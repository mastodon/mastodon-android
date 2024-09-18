package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CheckableLinearLayout;

import java.util.Locale;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class PollOptionStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;
	private CharSequence translatedText;
	public final Poll.Option option;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	public boolean showResults;
	private float votesFraction; // 0..1
	private boolean isMostVoted;
	private final int optionIndex;
	public final Poll poll;
	public final Status status;


	public PollOptionStatusDisplayItem(String parentID, Poll poll, int optionIndex, BaseStatusListFragment parentFragment, Status status){
		super(parentID, parentFragment);
		this.optionIndex=optionIndex;
		option=poll.options.get(optionIndex);
		this.poll=poll;
		this.status=status;
		text=HtmlParser.parseCustomEmoji(option.title, poll.emojis);
		emojiHelper.setText(text);
		showResults=poll.showResults;
		int total=poll.votersCount>0 ? poll.votersCount : poll.votesCount;
		if(option.votesCount!=null && total>0){
			votesFraction=(float)option.votesCount/(float)total;
			int mostVotedCount=0;
			for(Poll.Option opt:poll.options)
				mostVotedCount=Math.max(mostVotedCount, opt.votesCount);
			isMostVoted=option.votesCount==mostVotedCount;
		}
	}

	@Override
	public Type getType(){
		return Type.POLL_OPTION;
	}

	@Override
	public int getImageCount(){
		return emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return emojiHelper.getImageRequest(index);
	}

	public static class Holder extends StatusDisplayItem.Holder<PollOptionStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView text, percent;
		private final View check;
		private final Drawable progressBg;
		private final Drawable checkboxIcon, radioIcon;
		private final CheckableLinearLayout button;
		private LinearGradient textShader, percentShader;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_poll_option, parent);
			text=findViewById(R.id.text);
			percent=findViewById(R.id.percent);
			check=findViewById(R.id.checkbox);
			button=findViewById(R.id.button);
			progressBg=activity.getResources().getDrawable(R.drawable.bg_poll_option_voted, activity.getTheme()).mutate();
			itemView.setOnClickListener(this::onButtonClick);

			checkboxIcon=new CheckBox(activity).getButtonDrawable();
			radioIcon=new RadioButton(activity).getButtonDrawable();
			Matrix matrix=new Matrix();
			button.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)->{
				if(textShader!=null){
					int width=right-left;
					matrix.setScale(width, 1f, 0f, 0f);
					matrix.postTranslate(-text.getLeft(), 0);
					textShader.setLocalMatrix(matrix);
					matrix.setScale(width, 1f, 0f, 0f);
					matrix.postTranslate(-percent.getLeft(), 0);
					percentShader.setLocalMatrix(matrix);
				}
			});
		}

		@Override
		public void onBind(PollOptionStatusDisplayItem item){
			itemView.setPaddingRelative(V.dp(item.fullWidth ? 16 : 64), itemView.getPaddingTop(), itemView.getPaddingEnd(), itemView.getPaddingBottom());
			if (item.status.translation != null && item.status.translationState == Status.TranslationState.SHOWN) {
				if(item.translatedText==null){
					item.translatedText=item.status.translation.poll.options[item.optionIndex].title;
				}
				text.setText(item.translatedText);
			} else {
				text.setText(item.text);
			}
			percent.setVisibility(item.showResults ? View.VISIBLE : View.GONE);
			check.setVisibility(item.showResults ? View.GONE : View.VISIBLE);
			itemView.setClickable(!item.showResults);
			if(item.showResults){
				Drawable bg=progressBg;
				int drawableLevel=Math.round(10000f*item.votesFraction);
				bg.setLevel(drawableLevel);
				button.setBackground(bg);
				button.setChecked(item.isMostVoted);
				percent.setText(String.format(Locale.getDefault(), "%d%%", Math.round(item.votesFraction*100f)));
				text.setTextColor(UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3Outline));
				if(item.isMostVoted){
					int leftColor=UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3OnSurfaceInverse);
					int rightColor=UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3Outline);
					text.getPaint().setShader(textShader=new LinearGradient(0, 0, 1, 0, new int[]{leftColor, rightColor}, new float[]{item.votesFraction, item.votesFraction+Float.MIN_VALUE}, Shader.TileMode.CLAMP));
					percent.getPaint().setShader(percentShader=new LinearGradient(0, 0, 1, 0, new int[]{leftColor, rightColor}, new float[]{item.votesFraction, item.votesFraction+Float.MIN_VALUE}, Shader.TileMode.CLAMP));
				}else{
					textShader=percentShader=null;
					text.getPaint().setShader(null);
					percent.getPaint().setShader(null);
				}
			}else{
				textShader=percentShader=null;
				text.getPaint().setShader(null);
				percent.getPaint().setShader(null);
				button.setBackgroundResource(R.drawable.bg_poll_option_clickable);
				check.setForeground(item.poll.multiple ? checkboxIcon : radioIcon);
				text.setTextColor(UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3Primary));
				updateCheckedState();
			}
		}

		public void updateCheckedState(){
			button.setChecked(item.poll.selectedOptions!=null && item.poll.selectedOptions.contains(item.option));
		}

		@Override
		public void setImage(int index, Drawable image){
			item.emojiHelper.setImageDrawable(index, image);
			text.invalidate();
			if(image instanceof Animatable){
				((Animatable) image).start();
			}
		}

		@Override
		public void clearImage(int index){
			item.emojiHelper.setImageDrawable(index, null);
			text.invalidate();
		}

		private void onButtonClick(View v){
			item.parentFragment.onPollOptionClick(this);
		}
	}
}
