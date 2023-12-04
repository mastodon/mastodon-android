package org.joinmastodon.android.ui.photoviewer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;

public class PhotoViewerInfoSheet extends BottomSheet{
	private final Attachment attachment;
	private final View buttonsContainer;
	private final TextView altText;
	private final ImageButton backButton, infoButton;
	private final Button boostBtn, favoriteBtn, bookmarkBtn;
	private final Listener listener;
	private String statusID;

	public PhotoViewerInfoSheet(@NonNull Context context, Attachment attachment, int toolbarHeight, Listener listener){
		super(context);
		this.attachment=attachment;
		this.listener=listener;

		dimAmount=0;
		View content=context.getSystemService(LayoutInflater.class).inflate(R.layout.sheet_photo_viewer_info, null);
		setContentView(content);
		setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(context, R.attr.colorM3Surface),
				UiUtils.getThemeColor(context, R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());

		buttonsContainer=findViewById(R.id.buttons_container);
		altText=findViewById(R.id.alt_text);

		if(TextUtils.isEmpty(attachment.description)){
			findViewById(R.id.alt_text).setVisibility(View.GONE);
			findViewById(R.id.alt_text_title).setVisibility(View.GONE);
			findViewById(R.id.divider).setVisibility(View.GONE);
		}else{
			altText.setText(attachment.description);
			findViewById(R.id.alt_text_help).setOnClickListener(v->showAltTextHelp());
		}

		backButton=new ImageButton(context);
		backButton.setImageResource(R.drawable.ic_arrow_back);
		backButton.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(context, R.attr.colorM3OnSurfaceVariant)));
		backButton.setBackgroundResource(R.drawable.bg_button_m3_tonal_icon);
		backButton.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
		backButton.setElevation(V.dp(2));
		backButton.setAlpha(0f);
		backButton.setOnClickListener(v->{
			listener.onDismissEntireViewer();
			dismiss();
		});

		infoButton=new ImageButton(context);
		infoButton.setImageResource(R.drawable.ic_info_fill1_24px);
		infoButton.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(context, R.attr.colorM3OnPrimary)));
		infoButton.setBackgroundResource(R.drawable.bg_button_m3_filled_icon);
		infoButton.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
		infoButton.setElevation(V.dp(2));
		infoButton.setAlpha(0f);
		infoButton.setSelected(true);
		infoButton.setOnClickListener(v->dismiss());

		FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(V.dp(48), V.dp(48));
		lp.topMargin=toolbarHeight/2-V.dp(24);
		lp.leftMargin=lp.rightMargin=V.dp(4);
		lp.gravity=Gravity.START | Gravity.TOP;
		container.addView(backButton, lp);

		lp=new FrameLayout.LayoutParams(lp);
		lp.leftMargin=lp.rightMargin=0;
		lp.gravity=Gravity.END | Gravity.TOP;
		container.addView(infoButton, lp);

		boostBtn=findViewById(R.id.btn_boost);
		favoriteBtn=findViewById(R.id.btn_favorite);
		bookmarkBtn=findViewById(R.id.btn_bookmark);
		View.OnClickListener clickListener=v->listener.onButtonClick(v.getId());

		boostBtn.setOnClickListener(clickListener);
		favoriteBtn.setOnClickListener(clickListener);
		findViewById(R.id.btn_share).setOnClickListener(clickListener);
		bookmarkBtn.setOnClickListener(clickListener);
		findViewById(R.id.btn_download).setOnClickListener(clickListener);
	}

	private void showAltTextHelp(){
		new M3AlertDialogBuilder(getContext())
				.setTitle(R.string.what_is_alt_text)
				.setMessage(UiUtils.fixBulletListInString(getContext(), R.string.alt_text_help))
				.setPositiveButton(R.string.ok, null)
				.show();
	}

	@Override
	public void dismiss(){
		if(dismissed)
			return;
		int height=content.getHeight();
		int duration=Math.max(60, (int) (180 * (height - content.getTranslationY()) / (float) height));
		listener.onBeforeDismiss(duration);
		backButton.animate().alpha(0).setDuration(duration).setInterpolator(CubicBezierInterpolator.EASE_OUT).start();
		infoButton.animate().alpha(0).setDuration(duration).setInterpolator(CubicBezierInterpolator.EASE_OUT).start();
		super.dismiss();
		E.unregister(this);
	}

	@Override
	public void show(){
		super.show();
		E.register(this);
		content.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				content.getViewTreeObserver().removeOnPreDrawListener(this);
				backButton.animate().alpha(1).setDuration(300).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
				infoButton.animate().alpha(1).setDuration(300).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
				return true;
			}
		});
	}

	public void setStatus(Status status){
		statusID=status.id;
		boostBtn.setCompoundDrawablesWithIntrinsicBounds(0, switch(status.visibility){
			case DIRECT -> R.drawable.ic_boost_disabled_24px;
			case PUBLIC, UNLISTED -> R.drawable.ic_boost;
			case PRIVATE -> R.drawable.ic_boost_private;
		}, 0, 0);
		boostBtn.setEnabled(status.visibility!=StatusPrivacy.DIRECT);
		setButtonStates(status.reblogged, status.favourited, status.bookmarked);
	}

	@Subscribe
	public void onCountersUpdated(StatusCountersUpdatedEvent ev){
		if(ev.id.equals(statusID)){
			setButtonStates(ev.reblogged, ev.favorited, ev.bookmarked);
		}
	}

	private void setButtonStates(boolean reblogged, boolean favorited, boolean bookmarked){
		boostBtn.setText(reblogged ? R.string.button_reblogged : R.string.button_reblog);
		boostBtn.setSelected(reblogged);

		favoriteBtn.setText(favorited ? R.string.button_favorited : R.string.button_favorite);
		favoriteBtn.setSelected(favorited);

		bookmarkBtn.setText(bookmarked ? R.string.bookmarked : R.string.add_bookmark);
		bookmarkBtn.setSelected(bookmarked);
	}

	public interface Listener{
		void onBeforeDismiss(int duration);
		void onDismissEntireViewer();
		void onButtonClick(int id);
	}
}
