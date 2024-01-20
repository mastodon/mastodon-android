package org.joinmastodon.android.ui.wrapstodon;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.AnnualReport;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.drawables.InnerShadowDrawable;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class ArchetypeWrapScene extends AnnualWrapScene{
	private String username, domain, avaURL;
	private AnnualReport.Archetype archetype;

	private Paint photoEffectPaint=new Paint();
	private Property<View, Float> photoDevelopEffectProp=new Property<>(Float.class, "fdsafdsa"){
		@Override
		public Float get(View object){
			return null;
		}

		@Override
		public void set(View object, Float value){
			int colorMod=-Math.round((1f-value)*255);
			photoEffectPaint.setColorFilter(new ColorMatrixColorFilter(new float[]{
					1, 0, 0, 0, colorMod,
					0, 1, 0, 0, colorMod,
					0, 0, 1, 0, colorMod,
					0, 0, 0, value, 0
			}));
			object.setLayerType(View.LAYER_TYPE_HARDWARE, photoEffectPaint);
		}
	};

	public ArchetypeWrapScene(String username, String domain, String avaURL, AnnualReport.Archetype archetype){
		this.username=username;
		this.domain=domain;
		this.archetype=archetype;
		this.avaURL=avaURL;

		photoEffectPaint.setAlpha(255);
	}

	@SuppressLint("SetTextI18n")
	@Override
	protected View onCreateContentView(Context context){
		View view=context.getSystemService(LayoutInflater.class).inflate(R.layout.wrap_archetype, null);
		TextView subtitle=view.findViewById(R.id.subtitle);
		TextView username=view.findViewById(R.id.username);
		TextView domain=view.findViewById(R.id.domain);
		TextView archetypeTitle=view.findViewById(R.id.archetype_text);
		TextView archetypeExplanation=view.findViewById(R.id.archetype_explanation);
		View frame=view.findViewById(R.id.picture_frame);
		ImageView photo=view.findViewById(R.id.photo);
		RoundedFrameLayout photoWrap=view.findViewById(R.id.photo_wrap);

		subtitle.setText(context.getString(R.string.yearly_wrap_archetype_subtitle, year));
		username.setText("@"+this.username);
		domain.setText("@"+this.domain);
		archetypeTitle.setText(switch(archetype){
			case LURKER -> R.string.yearly_wrap_archetype_lurker;
			case BOOSTER -> R.string.yearly_wrap_archetype_booster;
			case REPLIER -> R.string.yearly_wrap_archetype_replier;
			case POLLSTER -> R.string.yearly_wrap_archetype_pollster;
			case ORACLE -> R.string.yearly_wrap_archetype_oracle;
		});
		archetypeExplanation.setText("TBD");

		frame.setOutlineProvider(OutlineProviders.roundedRect(4));
		frame.setClipToOutline(true);
		frame.setBackground(new RoundRectClippingDrawable(frame.getBackground()));
		photoWrap.setCornerRadius(V.dp(2));
		photoWrap.setForeground(new InnerShadowDrawable(V.dp(2), V.dp(1), 0x40000000, 0, V.dp(1)));
		photoWrap.setBackgroundColor(0xFF000000);
		if(avaURL!=null){
			ViewImageLoader.loadWithoutAnimation(photo, null, new UrlImageLoaderRequest(Bitmap.Config.ARGB_8888, 0, 0, List.of(), Uri.parse(avaURL)));
		}

		AnimatorSet set=new AnimatorSet();
		set.playTogether(
				ObjectAnimator.ofFloat(frame, View.ROTATION, -5, 2),
				ObjectAnimator.ofFloat(frame, View.SCALE_X, 1, 0.9f),
				ObjectAnimator.ofFloat(frame, View.SCALE_Y, 1, 0.9f),
				ObjectAnimator.ofFloat(photo, photoDevelopEffectProp, 0, 1)
		);
		set.setDuration(5000);
		set.setInterpolator(CubicBezierInterpolator.DEFAULT);
		set.start();
		set.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				photo.setLayerType(View.LAYER_TYPE_NONE, null);
			}
		});

		return view;
	}

	@Override
	protected void onDestroyContentView(){

	}

	private static class RoundRectClippingDrawable extends Drawable{
		private final Drawable inner;
		private Path path=new Path();
		private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
		private RectF tmpRect=new RectF();

		private RoundRectClippingDrawable(Drawable inner){
			this.inner=inner;
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			paint.setColor(0xff000000);
		}

		@Override
		public void draw(@NonNull Canvas canvas){
			if(canvas.isHardwareAccelerated()){
				inner.draw(canvas);
				return;
			}
			tmpRect.set(getBounds());
			canvas.saveLayer(tmpRect, null);
			inner.draw(canvas);
			canvas.drawPath(path, paint);
			canvas.restore();
		}

		@Override
		protected void onBoundsChange(@NonNull Rect bounds){
			super.onBoundsChange(bounds);
			inner.setBounds(bounds);
			path.rewind();
			path.addRoundRect(bounds.left, bounds.top, bounds.right, bounds.bottom, V.dp(4), V.dp(4), Path.Direction.CW);
			if(!path.isInverseFillType())
				path.toggleInverseFillType();
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
