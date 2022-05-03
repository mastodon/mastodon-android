package org.joinmastodon.android.ui;

import android.app.Fragment;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.joinmastodon.android.ui.photoviewer.PhotoViewer;

import java.util.function.Supplier;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SingleImagePhotoViewerListener implements PhotoViewer.Listener{
	private final View sourceView, transformView;
	private final int[] cornerRadius;
	private final Runnable onDismissed;
	private final Fragment parentFragment;
	private final Supplier<Drawable> currentDrawableSupplier;
	private final Runnable onStart, onEnd;

	private float origAlpha;

	public SingleImagePhotoViewerListener(View sourceView, View transformView, int[] cornerRadius, Fragment parentFragment, Runnable onDismissed, Supplier<Drawable> currentDrawableSupplier, Runnable onStart, Runnable onEnd){
		this.sourceView=sourceView;
		this.transformView=transformView;
		this.cornerRadius=cornerRadius;
		this.onDismissed=onDismissed;
		this.parentFragment=parentFragment;
		this.currentDrawableSupplier=currentDrawableSupplier;
		this.onStart=onStart;
		this.onEnd=onEnd;
		if(cornerRadius!=null && cornerRadius.length!=4)
			throw new IllegalArgumentException("Corner radius must be null or have length of 4");
	}

	@Override
	public void setPhotoViewVisibility(int index, boolean visible){
		transformView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
	}

	@Override
	public boolean startPhotoViewTransition(int index, @NonNull Rect outRect, @NonNull int[] outCornerRadius){
		int[] loc={0, 0};
		sourceView.getLocationOnScreen(loc);
		outRect.set(loc[0], loc[1], loc[0]+sourceView.getWidth(), loc[1]+sourceView.getHeight());
		if(cornerRadius!=null)
			System.arraycopy(cornerRadius, 0, outCornerRadius, 0, 4);
		transformView.setTranslationZ(1);
		if(onStart!=null)
			onStart.run();
		return true;
	}

	@Override
	public void setTransitioningViewTransform(float translateX, float translateY, float scale){
		transformView.setTranslationX(translateX);
		transformView.setTranslationY(translateY);
		transformView.setScaleX(scale);
		transformView.setScaleY(scale);
	}

	@Override
	public void endPhotoViewTransition(){
		setTransitioningViewTransform(0f, 0f, 1f);
		transformView.setTranslationZ(0);
		if(onEnd!=null)
			onEnd.run();
	}

	@Nullable
	@Override
	public Drawable getPhotoViewCurrentDrawable(int index){
		return currentDrawableSupplier.get();
	}

	@Override
	public void photoViewerDismissed(){
		onDismissed.run();
	}

	@Override
	public void onRequestPermissions(String[] permissions){
		parentFragment.requestPermissions(permissions, PhotoViewer.PERMISSION_REQUEST);
	}
}
