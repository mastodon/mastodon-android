package org.joinmastodon.android.ui.wrapstodon;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.R;
import org.joinmastodon.android.model.AnnualReport;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.FixedAspectRatioImageView;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import me.grishka.appkit.utils.V;

public class FavoriteHashtagsWrapScene extends AnnualWrapScene{
	private final List<AnnualReport.NameAndCount> tags;
	private int[] buffer;
	private Paint paint=new Paint();
	private ByteBuffer pixelBuffer;

	public FavoriteHashtagsWrapScene(List<AnnualReport.NameAndCount> tags){
		this.tags=tags.stream().sorted(Comparator.comparingInt((AnnualReport.NameAndCount t)->t.count).reversed()).collect(Collectors.toList());
	}

	@Override
	protected View onCreateContentView(Context context){
		LinearLayout ll=new LinearLayout(context);
		ll.setOrientation(LinearLayout.VERTICAL);
		LayoutInflater inflater=LayoutInflater.from(context);

		View header=inflater.inflate(R.layout.wrap_faves_header, ll, false);
		TextView title=header.findViewById(R.id.title);
		TextView subtitle=header.findViewById(R.id.subtitle);
		title.setText(replaceBoldWithColor(context.getResources().getText(R.string.wrap_favorite_tags_title), 0xFFBAFF3B));
		subtitle.setText(R.string.wrap_favorite_tags_subtitle);
		ll.addView(header);

		FrameLayout tagsCanvas=new FrameLayout(context);
		ll.addView(tagsCanvas, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(360)));

		Typeface font;
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
			font=context.getResources().getFont(R.font.manrope_w400);
		}else{
			font=Typeface.DEFAULT;
		}
		int max=tags.get(0).count;
		int min=tags.get(tags.size()-1).count;
		buffer=new int[12*360];
		paint.setTypeface(font);
		paint.setColor(0xFF000000);
		paint.setShadowLayer(1, 0, 0, 0xFF000000);
		pixelBuffer=ByteBuffer.allocate(120*384);
		int[] spriteSize={0, 0};
		int[] xy={0, 0};
		Random rand=new Random();
//		int i=0;
		for(AnnualReport.NameAndCount tag:tags){
//			if(i==20) break;
			int size;
			if(max==min){
				size=80;
			}else{
				float fraction=(tag.count-min)/(float)(max-min);
				size=UiUtils.lerp(15, 80, fraction*fraction*fraction/*fraction*/);
				paint.setShadowLayer(fraction, 0, 0, 0xFF000000);
			}
			paint.setTextSize(size);
			while(paint.measureText(tag.name)>360){
				size/=2;
				paint.setTextSize(size);
			}

			int[] sprite=getWordSprite(tag.name, size, spriteSize);
//			int startX=360/2-spriteSize[0]/2;
//			int startY=360/2-spriteSize[1]/2;
			int startX=rand.nextInt(360-spriteSize[0]);
			int startY=rand.nextInt(360-spriteSize[1]);
			double dt=rand.nextBoolean() ? 1 : -1;
			double t=-dt;
			int maxDelta=509;
			int dx, dy;
			boolean placed=false;
			int x, y;
			do{
				spiral(t+=dt, xy);
				dx=xy[0];
				dy=xy[1];
				x=startX+dx;
				y=startY+dy;
				if(x<0 || y<0 || x+spriteSize[0]>360 || y+spriteSize[1]>360)
					continue;
				if(!collide(sprite, x, y, spriteSize[0], spriteSize[1])){
					addMask(sprite, x, y, spriteSize[0], spriteSize[1]);
					placed=true;
					break;
				}
			}while(Math.max(Math.abs(dx), Math.abs(dy))<maxDelta);
//			i++;
			if(!placed)
				break;

			TextView text=new TextView(context);
			text.setTypeface(font);
			text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
			text.setTextColor(0xFFCCCFFF);
			text.setSingleLine();
			text.setText(tag.name);
			text.setTranslationX(V.dp(x));
			text.setTranslationY(V.dp(y));
			tagsCanvas.addView(text, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP|Gravity.LEFT));
		}

		if(BuildConfig.DEBUG){
			tagsCanvas.setOnClickListener(v->{
				FixedAspectRatioImageView vv=new FixedAspectRatioImageView(context);
				vv.setAspectRatio(1);
				vv.setBackground(new BitmapDrawable(getDebugBitmap()));
				new AlertDialog.Builder(context)
						.setView(vv)
						.show();
			});
		}

		return ll;
	}

	@Override
	protected void onDestroyContentView(){

	}

	private int[] getWordSprite(String text, int size, int[] outSize){
		// Makes a binary image of the string
		paint.setTextSize(size);
		int w=Math.min(384, (int)paint.measureText(text)+2);
		if(w%32!=0){
			w+=32-w%32;
		}
		int h=(int)(paint.descent()-paint.ascent());
		Bitmap bmp=Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8);
		new Canvas(bmp).drawText(text, 1, -paint.ascent(), paint);
		bmp.copyPixelsToBuffer(pixelBuffer);
		pixelBuffer.rewind();
		outSize[0]=w;
		outSize[1]=h;
		int spriteW=w >> 5;
		int[] sprite=new int[spriteW*h];
		byte[] pixelData=pixelBuffer.array();
		int offsetIntoPixelData=0;
		int stride=bmp.getRowBytes();
		for(int y=0;y<h;y++){
			for(int x=0;x<spriteW;x++){
				int slice=0;
				for(int i=0;i<32;i++){
					slice<<=1;
					if(pixelData[offsetIntoPixelData+(x << 5)+i]!=0)
						slice|=1;
				}
				sprite[y*spriteW+x]=slice;
			}
			offsetIntoPixelData+=stride;
		}
		return sprite;
	}

	private void spiral(double t, int[] outXY){
		t*=3;
		outXY[0]=(int)Math.round(t*Math.cos(t));
		outXY[1]=(int)Math.round(t*Math.sin(t));
	}

	private boolean collide(int[] sprite, int x, int y, int w, int h){
		int packedWidth=w>>5, packedX=x>>5;
		int shiftX=x & 0x7f, invShiftX=32-shiftX;
		int last;
		for(int iy=0;iy<h;iy++){
			last=0;
			for(int ix=0;ix<=packedWidth;ix++){
				if((((last << invShiftX) | (ix<packedWidth ? (last=sprite[iy*packedWidth+ix]) >>> shiftX : 0)) & buffer[(iy+y)*12+ix+packedX])!=0)
					return true;
			}
		}
		return false;
	}

	private void addMask(int[] sprite, int x, int y, int w, int h){
		int packedWidth=w>>5, packedX=x>>5;
		int shiftX=x & 0x7f, invShiftX=32-shiftX;
		int last;
		for(int iy=0;iy<h;iy++){
			last=0;
			for(int ix=0;ix<=packedWidth;ix++){
				buffer[(iy+y)*12+ix+packedX]|=(last << invShiftX) | (ix<packedWidth ? (last=sprite[iy*packedWidth+ix]) >>> shiftX : 0);
			}
		}
	}

	private Bitmap getDebugBitmap(){
		Bitmap bmp=Bitmap.createBitmap(384, 360, Bitmap.Config.ALPHA_8);
		for(int y=0;y<360;y++){
			for(int x=0;x<12;x++){
				int packed=buffer[y*12+x];
				for(int i=0;i<32;i++){
					if((packed & 0x80000000)!=0){
						bmp.setPixel(x*32+i, y, 0xFF000000);
					}
					packed<<=1;
				}
			}
		}
		return bmp;
	}
}
