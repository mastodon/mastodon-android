package org.joinmastodon.android.api;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;

import org.joinmastodon.android.MastodonApp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;
import okhttp3.MediaType;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class ResizedImageRequestBody extends CountingRequestBody{
	private File tempFile;
	private Uri uri;
	private String contentType;

	public ResizedImageRequestBody(Uri uri, int maxSize, ProgressListener progressListener) throws IOException{
		super(progressListener);
		this.uri=uri;
		contentType=MastodonApp.context.getContentResolver().getType(uri);
		BitmapFactory.Options opts=new BitmapFactory.Options();
		opts.inJustDecodeBounds=true;
		try(InputStream in=MastodonApp.context.getContentResolver().openInputStream(uri)){
			BitmapFactory.decodeStream(in, null, opts);
		}
		if(opts.outWidth*opts.outHeight>maxSize){
			Bitmap bitmap;
			if(Build.VERSION.SDK_INT>=29){
				bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(MastodonApp.context.getContentResolver(), uri), (decoder, info, source)->{
					int targetWidth=Math.round((float)Math.sqrt((float)maxSize*((float)info.getSize().getWidth()/info.getSize().getHeight())));
					int targetHeight=Math.round((float)Math.sqrt((float)maxSize*((float)info.getSize().getHeight()/info.getSize().getWidth())));
					decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
					decoder.setTargetSize(targetWidth, targetHeight);
				});
			}else{
				int targetWidth=Math.round((float)Math.sqrt((float)maxSize*((float)opts.outWidth/opts.outHeight)));
				int targetHeight=Math.round((float)Math.sqrt((float)maxSize*((float)opts.outHeight/opts.outWidth)));
				float factor=opts.outWidth/(float)targetWidth;
				opts=new BitmapFactory.Options();
				opts.inSampleSize=(int)factor;
				try(InputStream in=MastodonApp.context.getContentResolver().openInputStream(uri)){
					bitmap=BitmapFactory.decodeStream(in, null, opts);
				}
				if(factor%1f!=0f){
					Bitmap scaled=Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
					new Canvas(scaled).drawBitmap(bitmap, null, new Rect(0, 0, targetWidth, targetHeight), new Paint(Paint.FILTER_BITMAP_FLAG));
					bitmap=scaled;
				}
				if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
					int rotation;
					try(InputStream in=MastodonApp.context.getContentResolver().openInputStream(uri)){
						ExifInterface exif=new ExifInterface(in);
						int orientation=exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
						rotation=switch(orientation){
							case ExifInterface.ORIENTATION_ROTATE_90 -> 90;
							case ExifInterface.ORIENTATION_ROTATE_180 -> 180;
							case ExifInterface.ORIENTATION_ROTATE_270 -> 270;
							default -> 0;
						};
					}
					if(rotation!=0){
						Matrix matrix=new Matrix();
						matrix.setRotate(rotation);
						bitmap=Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
					}
				}
			}

			tempFile=new File(MastodonApp.context.getCacheDir(), "tmp_upload_image");
			try(FileOutputStream out=new FileOutputStream(tempFile)){
				if("image/png".equals(contentType)){
					bitmap.compress(Bitmap.CompressFormat.PNG, 0, out);
				}else{
					bitmap.compress(Bitmap.CompressFormat.JPEG, 97, out);
					contentType="image/jpeg";
				}
			}
			length=tempFile.length();
		}else{
			try(Cursor cursor=MastodonApp.context.getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)){
				cursor.moveToFirst();
				length=cursor.getInt(0);
			}
		}
	}

	@Override
	protected Source openSource() throws IOException{
		if(tempFile==null){
			return Okio.source(MastodonApp.context.getContentResolver().openInputStream(uri));
		}else{
			return Okio.source(tempFile);
		}
	}

	@Override
	public MediaType contentType(){
		return MediaType.get(contentType);
	}

	@Override
	public void writeTo(BufferedSink sink) throws IOException{
		try{
			super.writeTo(sink);
		}finally{
			if(tempFile!=null){
				tempFile.delete();
			}
		}
	}
}
