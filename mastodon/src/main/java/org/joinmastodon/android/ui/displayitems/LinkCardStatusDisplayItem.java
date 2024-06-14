package org.joinmastodon.android.ui.displayitems;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.model.Card;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.drawables.BlurhashCrossfadeDrawable;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.Objects;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class LinkCardStatusDisplayItem extends StatusDisplayItem{
	private final Status status;
	private final UrlImageLoaderRequest imgRequest, authorAvaRequest;
	private final SpannableStringBuilder parsedAuthorName;
	private final CustomEmojiHelper authorNameEmojiHelper=new CustomEmojiHelper();

	public LinkCardStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Status status){
		super(parentID, parentFragment);
		this.status=status;
		if(status.card.image!=null)
			imgRequest=new UrlImageLoaderRequest(status.card.image, 1000, 1000);
		else
			imgRequest=null;

		if(status.card.authorAccount!=null){
			parsedAuthorName=new SpannableStringBuilder(status.card.authorAccount.displayName);
			if(AccountSessionManager.get(parentFragment.getAccountID()).getLocalPreferences().customEmojiInNames)
				HtmlParser.parseCustomEmoji(parsedAuthorName, status.card.authorAccount.emojis);
			authorNameEmojiHelper.setText(parsedAuthorName);
			authorAvaRequest=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? status.card.authorAccount.avatar : status.card.authorAccount.avatarStatic, V.dp(50), V.dp(50));
		}else{
			parsedAuthorName=null;
			authorAvaRequest=null;
		}
	}

	@Override
	public Type getType(){
		return status.card.type==Card.Type.VIDEO || (status.card.image!=null && status.card.width>status.card.height) ? Type.CARD_LARGE : Type.CARD_COMPACT;
	}

	@Override
	public int getImageCount(){
		return 1+(status.card.authorAccount!=null ? (1+authorNameEmojiHelper.getImageCount()) : 0);
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return switch(index){
			case 0 -> imgRequest;
			case 1 -> authorAvaRequest;
			default -> authorNameEmojiHelper.getImageRequest(index-2);
		};
	}

	public static class Holder extends StatusDisplayItem.Holder<LinkCardStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView title, description, domain, timestamp, authorBefore, authorAfter, authorName;
		private final ImageView photo, authorAva;
		private BlurhashCrossfadeDrawable crossfadeDrawable=new BlurhashCrossfadeDrawable();
		private boolean didClear;
		private final View inner, authorFooter, authorChip;
		private final boolean isLarge;
		private final Drawable logoIcon;

		public Holder(Context context, ViewGroup parent, boolean isLarge){
			super(context, isLarge ? R.layout.display_item_link_card : R.layout.display_item_link_card_compact, parent);
			this.isLarge=isLarge;
			title=findViewById(R.id.title);
			description=findViewById(R.id.description);
			domain=findViewById(R.id.domain);
			timestamp=findViewById(R.id.timestamp);
			photo=findViewById(R.id.photo);
			inner=findViewById(R.id.inner);
			authorBefore=findViewById(R.id.author_before);
			authorAfter=findViewById(R.id.author_after);
			authorName=findViewById(R.id.author_name);
			authorAva=findViewById(R.id.author_ava);
			authorChip=findViewById(R.id.author_chip);
			authorFooter=findViewById(R.id.author_footer);

			inner.setOnClickListener(this::onClick);
			inner.setOutlineProvider(OutlineProviders.roundedRect(8));
			inner.setClipToOutline(true);
			if(!isLarge){
				photo.setOutlineProvider(OutlineProviders.roundedRect(4));
				photo.setClipToOutline(true);
			}
			authorAva.setOutlineProvider(OutlineProviders.roundedRect(3));
			authorAva.setClipToOutline(true);
			authorChip.setOnClickListener(this::onAuthorChipClick);

			logoIcon=context.getResources().getDrawable(R.drawable.ic_ntf_logo, context.getTheme()).mutate();
			logoIcon.setBounds(0, 0, V.dp(17), V.dp(17));
		}

		@SuppressLint("SetTextI18n")
		@Override
		public void onBind(LinkCardStatusDisplayItem item){
			Card card=item.status.card;
			title.setText(card.title);
			if(description!=null){
				description.setText(card.description);
				description.setVisibility(TextUtils.isEmpty(card.description) ? View.GONE : View.VISIBLE);
			}
			String cardDomain=HtmlParser.normalizeDomain(Objects.requireNonNull(Uri.parse(card.url).getHost()));
			domain.setText(TextUtils.isEmpty(card.providerName) ? cardDomain : card.providerName);
			if(card.authorAccount!=null){
				authorFooter.setVisibility(View.VISIBLE);
				authorChip.setVisibility(View.VISIBLE);
				authorBefore.setVisibility(View.VISIBLE);
				String[] authorParts=itemView.getContext().getString(R.string.article_by_author, "{author}").split("\\{author\\}");
				String before=authorParts[0].trim();
				String after=authorParts.length>1 ? authorParts[1].trim() : "";
				if(!TextUtils.isEmpty(before)){
					authorBefore.setText(before);
				}
				if(TextUtils.isEmpty(after)){
					authorAfter.setVisibility(View.GONE);
				}else{
					authorAfter.setVisibility(View.VISIBLE);
					authorAfter.setText(after);
				}
				authorName.setText(item.parsedAuthorName);
				authorBefore.setCompoundDrawablesRelative(logoIcon, null, null, null);
			}else if(!TextUtils.isEmpty(card.authorName)){
				authorFooter.setVisibility(View.VISIBLE);
				authorBefore.setVisibility(View.VISIBLE);
				authorBefore.setCompoundDrawables(null, null, null, null);
				authorChip.setVisibility(View.GONE);
				authorAfter.setVisibility(View.GONE);
				authorBefore.setText(itemView.getContext().getString(R.string.article_by_author, card.authorName));
			}else{
				authorFooter.setVisibility(View.GONE);
			}

			if(card.publishedAt!=null){
				timestamp.setVisibility(View.VISIBLE);
				timestamp.setText(" · "+UiUtils.formatRelativeTimestamp(itemView.getContext(), card.publishedAt));
			}else{
				timestamp.setVisibility(View.GONE);
			}

			photo.setImageDrawable(null);
			if(item.imgRequest!=null){
				photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
				photo.setBackground(null);
				photo.setImageTintList(null);
				crossfadeDrawable.setSize(card.width, card.height);
				crossfadeDrawable.setBlurhashDrawable(card.blurhashPlaceholder);
				crossfadeDrawable.setCrossfadeAlpha(0f);
				photo.setImageDrawable(null);
				photo.setImageDrawable(crossfadeDrawable);
				didClear=false;
			}else{
				photo.setBackgroundColor(UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3SurfaceVariant));
				photo.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3Outline)));
				photo.setScaleType(ImageView.ScaleType.CENTER);
				photo.setImageResource(R.drawable.ic_feed_48px);
			}
		}

		@Override
		public void setImage(int index, Drawable drawable){
			if(index==0){
				crossfadeDrawable.setImageDrawable(drawable);
				if(didClear)
					crossfadeDrawable.animateAlpha(0f);
				Card card=item.status.card;
				// Make sure the image is not stretched if the server returned wrong dimensions
				if(drawable!=null && (drawable.getIntrinsicWidth()!=card.width || drawable.getIntrinsicHeight()!=card.height)){
					photo.setImageDrawable(null);
					photo.setImageDrawable(crossfadeDrawable);
				}
			}else if(index==1){
				authorAva.setImageDrawable(drawable);
			}else{
				item.authorNameEmojiHelper.setImageDrawable(index-2, drawable);
				authorName.invalidate();
			}
		}

		@Override
		public void clearImage(int index){
			if(index==0){
				crossfadeDrawable.setCrossfadeAlpha(1f);
				didClear=true;
			}else{
				setImage(index, null);
			}
		}

		private void onClick(View v){
			UiUtils.openURL(itemView.getContext(), item.parentFragment.getAccountID(), item.status.card.url, item.status);
		}

		private void onAuthorChipClick(View v){
			Bundle args=new Bundle();
			args.putString("account", item.parentFragment.getAccountID());
			args.putParcelable("profileAccount", Parcels.wrap(item.status.card.authorAccount));
			Nav.go(item.parentFragment.getActivity(), ProfileFragment.class, args);
		}
	}
}
