package org.joinmastodon.android.ui.viewholders;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.model.Card;
import org.joinmastodon.android.model.viewmodel.CardViewModel;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.drawables.BlurhashCrossfadeDrawable;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.Objects;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.utils.V;

public class LinkCardHolder<T extends LinkCardHolder.LinkCardProvider> extends StatusDisplayItem.Holder<T> implements ImageLoaderViewHolder{
	private final TextView title, description, domain, timestamp, authorBefore, authorAfter, authorName;
	private final ImageView photo, authorAva;
	private BlurhashCrossfadeDrawable crossfadeDrawable=new BlurhashCrossfadeDrawable();
	private boolean didClear;
	private final View inner, authorFooter, authorChip;
	private final boolean isLarge;
	private final Drawable logoIcon;
	private final String accountID;
	private Activity activity;
	private boolean tryResolving=true;

	public LinkCardHolder(Activity context, ViewGroup parent, boolean isLarge, String accountID){
		super(context, isLarge ? R.layout.display_item_link_card : R.layout.display_item_link_card_compact, parent);
		this.isLarge=isLarge;
		this.accountID=accountID;
		activity=context;
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
		}else{
			photo.setOutlineProvider(OutlineProviders.topRoundedRect(7));
		}
		photo.setClipToOutline(true);
		authorAva.setOutlineProvider(OutlineProviders.roundedRect(3));
		authorAva.setClipToOutline(true);
		authorChip.setOnClickListener(this::onAuthorChipClick);

		logoIcon=context.getResources().getDrawable(R.drawable.ic_ntf_logo, context.getTheme()).mutate();
		logoIcon.setBounds(0, 0, V.dp(17), V.dp(17));
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBind(T item){
		CardViewModel cardVM=item.getCard();
		Card card=cardVM.card;
		title.setText(card.title);
		if(description!=null){
			description.setText(card.description);
			description.setVisibility(TextUtils.isEmpty(card.description) ? View.GONE : View.VISIBLE);
		}
		String cardDomain=HtmlParser.normalizeDomain(Objects.requireNonNull(Uri.parse(card.url).getHost()));
		domain.setText(TextUtils.isEmpty(card.providerName) ? cardDomain : card.providerName);
		String authorName=card.authors!=null && !card.authors.isEmpty() ? card.authors.get(0).name : card.authorName;

		if(cardVM.parsedAuthorName!=null){
			authorFooter.setVisibility(View.VISIBLE);
			authorChip.setVisibility(View.VISIBLE);
			authorBefore.setVisibility(View.VISIBLE);
			String[] authorParts=itemView.getContext().getString(R.string.article_by_author, "{author}").split("\\{author\\}");
			String before=authorParts.length>0 ? authorParts[0].trim() : "";
			String after=authorParts.length>1 ? authorParts[1].trim() : "";
			authorBefore.setText(before);
			if(TextUtils.isEmpty(after)){
				authorAfter.setVisibility(View.GONE);
			}else{
				authorAfter.setVisibility(View.VISIBLE);
				authorAfter.setText(after);
			}
			this.authorName.setText(cardVM.parsedAuthorName);
			authorBefore.setCompoundDrawablesRelative(logoIcon, null, null, null);
		}else if(!TextUtils.isEmpty(authorName)){
			authorFooter.setVisibility(View.VISIBLE);
			authorBefore.setVisibility(View.VISIBLE);
			authorBefore.setCompoundDrawables(null, null, null, null);
			authorChip.setVisibility(View.GONE);
			authorAfter.setVisibility(View.GONE);
			authorBefore.setText(itemView.getContext().getString(R.string.article_by_author, authorName));
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
		if(cardVM.imageRequest!=null){
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
			CardViewModel card=item.getCard();
			// Make sure the image is not stretched if the server returned wrong dimensions
			if(drawable!=null && (drawable.getIntrinsicWidth()!=card.card.width || drawable.getIntrinsicHeight()!=card.card.height)){
				photo.setImageDrawable(null);
				photo.setImageDrawable(crossfadeDrawable);
			}
		}else if(index==1){
			authorAva.setImageDrawable(drawable);
		}else{
			item.getCard().authorNameEmojiHelper.setImageDrawable(index-2, drawable);
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

	public void setTryResolving(boolean tryResolving){
		this.tryResolving=tryResolving;
	}

	private void onClick(View v){
		CardViewModel card=item.getCard();
		if(tryResolving)
			UiUtils.openURL(activity, accountID, card.card.url, card.parentObject);
		else
			UiUtils.launchWebBrowser(activity, card.card.url);
	}

	private void onAuthorChipClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("profileAccount", Parcels.wrap(item.getCard().getAuthorAccount()));
		Nav.go(activity, ProfileFragment.class, args);
	}

	public interface LinkCardProvider{
		CardViewModel getCard();
	}
}
