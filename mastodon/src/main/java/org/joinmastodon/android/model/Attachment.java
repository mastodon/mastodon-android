package org.joinmastodon.android.model;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.ui.utils.BlurHashDecoder;
import org.joinmastodon.android.ui.utils.BlurHashDrawable;
import org.parceler.Parcel;
import org.parceler.ParcelConstructor;
import org.parceler.ParcelProperty;

@Parcel
public class Attachment extends BaseModel{
	@RequiredField
	public String id;
	@RequiredField
	public Type type;
	@RequiredField
	public String url;
	public String previewUrl;
	public String remoteUrl;
	public String description;
	@ParcelProperty("blurhash")
	public String blurhash;
	public Metadata meta;

	public transient Drawable blurhashPlaceholder;

	public Attachment(){}

	@ParcelConstructor
	public Attachment(@ParcelProperty("blurhash") String blurhash){
		this.blurhash=blurhash;
		if(blurhash!=null){
			Bitmap placeholder=BlurHashDecoder.decode(blurhash, 16, 16);
			if(placeholder!=null)
				blurhashPlaceholder=new BlurHashDrawable(placeholder, getWidth(), getHeight());
		}
	}

	public int getWidth(){
		if(meta==null)
			return 0;
		if(meta.width>0)
			return meta.width;
		if(meta.original!=null && meta.original.width>0)
			return meta.original.width;
		if(meta.small!=null && meta.small.width>0)
			return meta.small.width;
		return 0;
	}

	public int getHeight(){
		if(meta==null)
			return 0;
		if(meta.height>0)
			return meta.height;
		if(meta.original!=null && meta.original.height>0)
			return meta.original.height;
		if(meta.small!=null && meta.small.height>0)
			return meta.small.height;
		return 0;
	}

	public double getDuration(){
		if(meta==null)
			return 0;
		if(meta.duration>0)
			return meta.duration;
		if(meta.original!=null && meta.original.duration>0)
			return meta.original.duration;
		return 0;
	}

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(blurhash!=null){
			Bitmap placeholder=BlurHashDecoder.decode(blurhash, 16, 16);
			if(placeholder!=null)
				blurhashPlaceholder=new BlurHashDrawable(placeholder, getWidth(), getHeight());
		}
	}

	@Override
	public String toString(){
		return "Attachment{"+
				"id='"+id+'\''+
				", type="+type+
				", url='"+url+'\''+
				", previewUrl='"+previewUrl+'\''+
				", remoteUrl='"+remoteUrl+'\''+
				", description='"+description+'\''+
				", blurhash='"+blurhash+'\''+
				", meta="+meta+
				'}';
	}

	public enum Type{
		@SerializedName("image")
		IMAGE,
		@SerializedName("gifv")
		GIFV,
		@SerializedName("video")
		VIDEO,
		@SerializedName("audio")
		AUDIO,
		@SerializedName("unknown")
		UNKNOWN;

		public boolean isImage(){
			return this==IMAGE || this==GIFV || this==VIDEO;
		}
	}

	@Parcel
	public static class Metadata{
		public double duration;
		public int width;
		public int height;
		public double aspect;
		public PointF focus;
		public SizeMetadata original;
		public SizeMetadata small;
		public ColorsMetadata colors;

		@Override
		public String toString(){
			return "Metadata{"+
					"duration="+duration+
					", width="+width+
					", height="+height+
					", aspect="+aspect+
					", focus="+focus+
					", original="+original+
					", small="+small+
					", colors="+colors+
					'}';
		}
	}

	@Parcel
	public static class SizeMetadata{
		public int width;
		public int height;
		public double aspect;
		public double duration;
		public int bitrate;

		@Override
		public String toString(){
			return "SizeMetadata{"+
					"width="+width+
					", height="+height+
					", aspect="+aspect+
					", duration="+duration+
					", bitrate="+bitrate+
					'}';
		}
	}

	@Parcel
	public static class ColorsMetadata{
		public String background;
		public String foreground;
		public String accent;

		@Override
		public String toString(){
			return "ColorsMetadata{"+
					"background='"+background+'\''+
					", foreground='"+foreground+'\''+
					", accent='"+accent+'\''+
					'}';
		}
	}
}
