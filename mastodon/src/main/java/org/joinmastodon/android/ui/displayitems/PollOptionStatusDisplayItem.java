package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.Locale;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;

public class PollOptionStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;
	public final Poll.Option option;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private boolean showResults;
	private float votesFraction; // 0..1
	private boolean isMostVoted;
	private final int optionIndex;
	public final Poll poll;

	public PollOptionStatusDisplayItem(String parentID, Poll poll, int optionIndex, BaseStatusListFragment parentFragment){
		super(parentID, parentFragment);
		this.optionIndex=optionIndex;
		option=poll.options.get(optionIndex);
		this.poll=poll;
		text=HtmlParser.parseCustomEmoji(option.title, poll.emojis);
		emojiHelper.setText(text);
		showResults=poll.isExpired() || poll.voted;
		int total=poll.votersCount>0 ? poll.votersCount : poll.votesCount;
		if(showResults && option.votesCount!=null && total>0){
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
		private final View check, button;
		private final Drawable progressBg, progressBgInset;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_poll_option, parent);
			text=findViewById(R.id.text);
			percent=findViewById(R.id.percent);
			check=findViewById(R.id.checkbox);
			button=findViewById(R.id.button);
			progressBg=activity.getResources().getDrawable(R.drawable.bg_poll_option_voted, activity.getTheme()).mutate();
			progressBgInset=activity.getResources().getDrawable(R.drawable.bg_poll_option_voted_inset, activity.getTheme()).mutate();
			itemView.setOnClickListener(this::onButtonClick);
			button.setOutlineProvider(OutlineProviders.roundedRect(20));
			button.setClipToOutline(true);
		}

		@Override
		public void onBind(PollOptionStatusDisplayItem item){
			text.setText(item.text);
			percent.setVisibility(item.showResults ? View.VISIBLE : View.GONE);
			itemView.setClickable(!item.showResults);
			if(item.showResults){
				Drawable bg=item.inset ? progressBgInset : progressBg;
				bg.setLevel(Math.round(10000f*item.votesFraction));
				button.setBackground(bg);
				itemView.setSelected(item.isMostVoted);
				check.setSelected(item.poll.ownVotes!=null && item.poll.ownVotes.contains(item.optionIndex));
				percent.setText(String.format(Locale.getDefault(), "%d%%", Math.round(item.votesFraction*100f)));
			}else{
				itemView.setSelected(item.poll.selectedOptions!=null && item.poll.selectedOptions.contains(item.option));
				button.setBackgroundResource(item.inset ? R.drawable.bg_poll_option_clickable_inset : R.drawable.bg_poll_option_clickable);
			}
			if(item.inset){
				text.setTextColor(itemView.getContext().getColorStateList(R.color.poll_option_text_inset));
				percent.setTextColor(itemView.getContext().getColorStateList(R.color.poll_option_text_inset));
			}else{
				text.setTextColor(UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3Primary));
				percent.setTextColor(UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3OnSecondaryContainer));
			}
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
