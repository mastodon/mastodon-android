package org.joinmastodon.android.api;

import android.graphics.Rect;
import android.net.Uri;

import java.io.IOException;

public class AvatarResizedImageRequestBody extends ResizedImageRequestBody{
	public AvatarResizedImageRequestBody(Uri uri, ProgressListener progressListener) throws IOException{
		super(uri, 0, progressListener);
	}

	@Override
	protected int[] getTargetSize(int srcWidth, int srcHeight){
		float factor=400f/Math.min(srcWidth, srcHeight);
		return new int[]{Math.round(srcWidth*factor), Math.round(srcHeight*factor)};
	}

	@Override
	protected boolean needResize(int srcWidth, int srcHeight){
		return srcHeight>400 || srcWidth!=srcHeight;
	}

	@Override
	protected boolean needCrop(int srcWidth, int srcHeight){
		return srcWidth!=srcHeight;
	}

	@Override
	protected Rect getCropBounds(int srcWidth, int srcHeight){
		Rect rect=new Rect();
		if(srcWidth>srcHeight){
			rect.set(srcWidth/2-srcHeight/2, 0, srcWidth/2-srcHeight/2+srcHeight, srcHeight);
		}else{
			rect.set(0, srcHeight/2-srcWidth/2, srcWidth, srcHeight/2-srcWidth/2+srcWidth);
		}
		return rect;
	}
}
