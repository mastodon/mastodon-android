package org.joinmastodon.android;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WrapstodonImageProvider extends ContentProvider{
	@Override
	public boolean onCreate(){
		return true;
	}

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder){
		File image=new File(getContext().getCacheDir(), "wrapstodon.png");
		if(projection==null)
			projection=new String[]{"_display_name"};
		MatrixCursor cursor=new MatrixCursor(projection);
		if(!image.exists())
			return cursor;
		Object[] values=new Object[projection.length];
		for(int i=0;i<projection.length;i++){
			if("_display_name".equals(projection[i]))
				values[i]="wrapstodon.png";
			if("_size".equals(projection[i]))
				values[i]=image.length();
		}
		cursor.addRow(values);
		return cursor;
	}

	@Nullable
	@Override
	public String getType(@NonNull Uri uri){
		return "image/png";
	}

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, @Nullable ContentValues values){
		throw new UnsupportedOperationException();
	}

	@Override
	public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs){
		throw new UnsupportedOperationException();
	}

	@Override
	public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs){
		throw new UnsupportedOperationException();
	}

	@Nullable
	@Override
	public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException{
		if(!"r".equals(mode))
			throw new FileNotFoundException("Unsupported mode");
		File image=new File(getContext().getCacheDir(), "wrapstodon.png");
		if(!image.exists())
			throw new FileNotFoundException();
		return ParcelFileDescriptor.open(image, ParcelFileDescriptor.MODE_READ_ONLY);
	}
}
