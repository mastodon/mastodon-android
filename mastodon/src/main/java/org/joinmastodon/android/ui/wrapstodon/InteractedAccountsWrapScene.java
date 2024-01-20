package org.joinmastodon.android.ui.wrapstodon;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AnnualReport;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class InteractedAccountsWrapScene extends AnnualWrapScene{
	private final Account self;
	private final Map<String, Account> allAccounts;
	private final List<AnnualReport.AccountAndCount> accounts;
	private List<ImageView> avatarViews=new ArrayList<>();
	private ImageView avatarToFollowAround;
	private final Runnable followAroundAnimationUpdater=this::updateFollowAroundAnimation;
	private View content;
	private FrameLayout contentWrap;
	private FrameLayout orbit;
	private ImageView selfAva;
	private View orbitBGView;
	private TextView title, subtitle;
	private View detailsOverlay;
	private TextView detailsNumber, detailsText;
	private Animator currentZoomAnim;

	public InteractedAccountsWrapScene(Account self, Map<String, Account> allAccounts, List<AnnualReport.AccountAndCount> accounts){
		this.self=self;
		this.allAccounts=allAccounts;
		this.accounts=accounts;
	}

	@Override
	protected View onCreateContentView(Context context){
		LayoutInflater inflater=LayoutInflater.from(context);
		contentWrap=(FrameLayout) inflater.inflate(R.layout.wrap_interacted_accounts, null);
		content=contentWrap.findViewById(R.id.content);

		selfAva=content.findViewById(R.id.self_ava);
		ViewImageLoader.loadWithoutAnimation(selfAva, null, new UrlImageLoaderRequest(Bitmap.Config.ARGB_8888, V.dp(160), V.dp(160), List.of(), Uri.parse(self.avatarStatic)));
		title=content.findViewById(R.id.title);
		subtitle=content.findViewById(R.id.subtitle);
		detailsOverlay=contentWrap.findViewById(R.id.details_overlay);
		detailsNumber=contentWrap.findViewById(R.id.details_number);
		detailsText=contentWrap.findViewById(R.id.details_text);

		orbit=content.findViewById(R.id.orbit);
		orbitBGView=content.findViewById(R.id.orbit_bg);
		LayerDrawable orbitBG=new LayerDrawable(new Drawable[]{
				new OrbitBackgroundDrawable(),
				new OrbitBackgroundDrawable()
		});
		orbitBG.setLayerSize(0, V.dp(409+4), V.dp(409+4));
		orbitBG.setLayerSize(1, V.dp(256+4), V.dp(256+4));
		orbitBG.setLayerGravity(0, Gravity.CENTER);
		orbitBG.setLayerGravity(1, Gravity.CENTER);
		orbitBGView.setBackground(orbitBG);
		FrameLayout innerRing=content.findViewById(R.id.inner_ring);
		FrameLayout outerRing=content.findViewById(R.id.outer_ring);

		List<AnnualReport.AccountAndCount> forInnerRing=accounts.subList(0, Math.min(6, accounts.size()));
		List<AnnualReport.AccountAndCount> forOuterRing=accounts.size()>6 ? accounts.subList(6, Math.min(18, accounts.size())) : List.of();
		if(forInnerRing.isEmpty())
			return content;

		final long innerDuration=15000, outerDuration=20000;
		ArrayList<Animator> anims=new ArrayList<>();
		anims.add(repeat(ObjectAnimator.ofFloat(innerRing, View.ROTATION, 0, 360).setDuration(innerDuration)));
		anims.add(repeat(ObjectAnimator.ofFloat(outerRing, View.ROTATION, 0, 360).setDuration(outerDuration)));

		double angle=0;
		double deltaAngle=2*Math.PI/forInnerRing.size();
		for(AnnualReport.AccountAndCount acc:forInnerRing){
			Account account=allAccounts.get(acc.accountId);
			if(account==null)
				continue;
			ImageView iv=makeImageView(context, account);
			innerRing.addView(iv, new FrameLayout.LayoutParams(V.dp(40), V.dp(40), Gravity.CENTER));
			float r=V.dp(128);
			iv.setTranslationX(r*(float)Math.cos(angle));
			iv.setTranslationY(r*(float)Math.sin(angle));
			iv.setOnClickListener(this::onAvatarClick);
			iv.setTag(acc);
			anims.add(repeat(ObjectAnimator.ofFloat(iv, View.ROTATION, 0, -360).setDuration(innerDuration)));
			avatarViews.add(iv);
			angle+=deltaAngle;
		}

		if(!forOuterRing.isEmpty()){
			angle=0;
			deltaAngle=2*Math.PI/forOuterRing.size();
			for(AnnualReport.AccountAndCount acc:forOuterRing){
				Account account=allAccounts.get(acc.accountId);
				if(account==null)
					continue;
				ImageView iv=makeImageView(context, account);
				outerRing.addView(iv, new FrameLayout.LayoutParams(V.dp(40), V.dp(40), Gravity.CENTER));
				float r=V.dp(204.5f);
				iv.setTranslationX(r*(float)Math.cos(angle));
				iv.setTranslationY(r*(float)Math.sin(angle));
				iv.setOnClickListener(this::onAvatarClick);
				iv.setTag(acc);
				anims.add(repeat(ObjectAnimator.ofFloat(iv, View.ROTATION, 0, -360).setDuration(outerDuration)));
				avatarViews.add(iv);
				angle+=deltaAngle;
			}
		}

		detailsOverlay.setVisibility(View.GONE);

		AnimatorSet set=new AnimatorSet();
		set.playTogether(anims);
		set.setInterpolator(new LinearInterpolator());
		set.start();

		return contentWrap;
	}

	// Because Android's animation API is stupid
	private ObjectAnimator repeat(ObjectAnimator anim){
		anim.setRepeatCount(ObjectAnimator.INFINITE);
		return anim;
	}

	private ImageView makeImageView(Context context, Account account){
		RoundedImageView iv=new RoundedImageView(context);
		iv.setCornerRadius(V.dp(20));
		iv.setForeground(iv.getResources().getDrawable(R.drawable.wrap_ava_border_1dp, iv.getContext().getTheme()));
		ViewImageLoader.loadWithoutAnimation(iv, null, new UrlImageLoaderRequest(Bitmap.Config.ARGB_8888, V.dp(40), V.dp(40), List.of(), Uri.parse(account.avatarStatic)));
		return iv;
	}

	@Override
	protected void onDestroyContentView(){

	}

	private void onAvatarClick(View v){
		if(currentZoomAnim!=null)
			currentZoomAnim.cancel();
		ImageView iv=(ImageView) v;
		AnnualReport.AccountAndCount acc=(AnnualReport.AccountAndCount) v.getTag();
		((ViewGroup)content).setClipChildren(false);
		orbit.setClipChildren(false);
		contentWrap.setClipChildren(false);
		avatarToFollowAround=iv;
		content.setPivotX(0);
		content.setPivotY(0);
		detailsNumber.setText(NumberFormat.getInstance().format(acc.count));
		detailsOverlay.setVisibility(View.VISIBLE);
		detailsOverlay.setAlpha(0);
		SpannableStringBuilder ssb=new SpannableStringBuilder(v.getResources().getQuantityString(R.plurals.wrap_replies_exchanged, acc.count));
		int index=ssb.toString().indexOf("%s");
		if(index!=-1){
			String name=Objects.requireNonNull(allAccounts.get(acc.accountId)).displayName;
			ssb.replace(index, index+2, name);
			ssb.setSpan(new ForegroundColorSpan(0xFFBAFF3B), index, index+name.length(), 0);
		}
		detailsText.setText(ssb);
		AnimatorSet set=new AnimatorSet();
		ArrayList<Animator> anims=new ArrayList<>();
		anims.add(ObjectAnimator.ofFloat(content, View.SCALE_X, 3.2f));
		anims.add(ObjectAnimator.ofFloat(content, View.SCALE_Y, 3.2f));
		anims.add(ObjectAnimator.ofFloat(selfAva, View.ALPHA, .15f));
		anims.add(ObjectAnimator.ofFloat(orbitBGView, View.ALPHA, .15f));
		anims.add(ObjectAnimator.ofFloat(title, View.ALPHA, .15f));
		anims.add(ObjectAnimator.ofFloat(subtitle, View.ALPHA, .15f));
		for(ImageView ava:avatarViews){
			if(ava!=iv){
				anims.add(ObjectAnimator.ofFloat(ava, View.ALPHA, .15f));
			}
			ava.setEnabled(false);
		}
		for(Animator a:anims)
			a.setDuration(1000);
		ObjectAnimator detailFadeIn=ObjectAnimator.ofFloat(detailsOverlay, View.ALPHA, 1);
		detailFadeIn.setDuration(200);
		detailFadeIn.setStartDelay(800);
		anims.add(detailFadeIn);
		set.playTogether(anims);
		set.setInterpolator(CubicBezierInterpolator.DEFAULT);
		set.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				currentZoomAnim=null;
			}
		});
		currentZoomAnim=set;
		set.start();
		contentView.postOnAnimation(followAroundAnimationUpdater);
		contentWrap.setOnClickListener(v_->zoomOut());
	}

	private void updateFollowAroundAnimation(){
		if(avatarToFollowAround==null)
			return;
		int[] loc={0, 0};
		content.setTranslationX(0);
		content.setTranslationY(0);
		avatarToFollowAround.getLocationInWindow(loc);
		float avaX=loc[0], avaY=loc[1];
		content.getLocationInWindow(loc);
		float contX=loc[0], contY=loc[1];

		float xOffset=content.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL ? content.getWidth()-V.dp(21+128) : V.dp(21);
		content.setTranslationX((contX-avaX+xOffset)*(content.getScaleX()-1f)/2.2f);
		content.setTranslationY((contY-avaY+content.getHeight()/2f-V.dp(64))*(content.getScaleY()-1f)/2.2f);
		contentView.postOnAnimation(followAroundAnimationUpdater);
	}

	private void zoomOut(){
		if(currentZoomAnim!=null)
			currentZoomAnim.cancel();
		AnimatorSet set=new AnimatorSet();
		ArrayList<Animator> anims=new ArrayList<>();
		anims.add(ObjectAnimator.ofFloat(content, View.SCALE_X, 1));
		anims.add(ObjectAnimator.ofFloat(content, View.SCALE_Y, 1));
		anims.add(ObjectAnimator.ofFloat(selfAva, View.ALPHA, 1));
		anims.add(ObjectAnimator.ofFloat(orbitBGView, View.ALPHA, 1));
		anims.add(ObjectAnimator.ofFloat(title, View.ALPHA, 1));
		anims.add(ObjectAnimator.ofFloat(subtitle, View.ALPHA, 1));
		for(ImageView ava:avatarViews){
			anims.add(ObjectAnimator.ofFloat(ava, View.ALPHA, 1));
			ava.setEnabled(true);
		}
		for(Animator a:anims)
			a.setDuration(500);
		anims.add(ObjectAnimator.ofFloat(detailsOverlay, View.ALPHA, 0).setDuration(150));
		set.playTogether(anims);
		set.setInterpolator(CubicBezierInterpolator.DEFAULT);
		set.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				avatarToFollowAround=null;
				contentWrap.setOnClickListener(null);
				detailsOverlay.setVisibility(View.GONE);
				currentZoomAnim=null;
			}
		});
		currentZoomAnim=set;
		set.start();
	}

	private class OrbitBackgroundDrawable extends Drawable{
		private Paint strokePaint=new Paint(Paint.ANTI_ALIAS_FLAG), fillPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
		private RadialGradient gradient;
		private Matrix matrix=new Matrix();

		public OrbitBackgroundDrawable(){
			strokePaint.setStyle(Paint.Style.STROKE);
			strokePaint.setStrokeWidth(V.dp(4));
			strokePaint.setPathEffect(new DashPathEffect(new float[]{V.dp(4), V.dp(12)}, 0));
			strokePaint.setColor(0xFF562CFC);
			strokePaint.setStrokeCap(Paint.Cap.ROUND);

			fillPaint.setShader(gradient=new RadialGradient(0f, 0f, 1f, new int[]{0x8017063B, 0x8017063B, 0x802F0C7A}, new float[]{0f, .77f, 1f}, Shader.TileMode.CLAMP));
		}

		@Override
		public void draw(@NonNull Canvas canvas){
			Rect bounds=getBounds();
			float radius=bounds.width()/2f-V.dp(2);
			matrix.setTranslate(bounds.centerX(), bounds.centerY());
			matrix.postScale(radius, radius, bounds.centerX(), bounds.centerY());
			gradient.setLocalMatrix(matrix);
			canvas.drawCircle(bounds.centerX(), bounds.centerY(), radius, fillPaint);
			canvas.drawCircle(bounds.centerX(), bounds.centerY(), radius, strokePaint);
		}

		@Override
		public void setAlpha(int alpha){

		}

		@Override
		public void setColorFilter(@Nullable ColorFilter colorFilter){

		}

		@Override
		public int getOpacity(){
			return PixelFormat.TRANSLUCENT;
		}
	}
}
