package org.joinmastodon.android;

import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileNotFoundException;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TweakedFileProvider extends FileProvider{
	private static final String TAG="TweakedFileProvider";

	@Override
	public String getType(@NonNull Uri uri){
		Log.d(TAG, "getType() called with: uri = ["+uri+"]");
		if(uri.getPathSegments().get(0).equals("image_cache")){
			Log.i(TAG, "getType: HERE!");
			return "image/jpeg"; // might as well be a png but image decoding APIs don't care, needs to be image/* though
		}
		return super.getType(uri);
	}

	@Override
	public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder){
		Log.d(TAG, "query() called with: uri = ["+uri+"], projection = ["+Arrays.toString(projection)+"], selection = ["+selection+"], selectionArgs = ["+Arrays.toString(selectionArgs)+"], sortOrder = ["+sortOrder+"]");
		return super.query(uri, projection, selection, selectionArgs, sortOrder);
	}

	@Override
	public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException{
		Log.d(TAG, "openFile() called with: uri = ["+uri+"], mode = ["+mode+"]");
		return super.openFile(uri, mode);
	}
}
