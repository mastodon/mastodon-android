package org.joinmastodon.android.updater;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.FileNotFoundException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SelfUpdateContentProvider extends ContentProvider{
	@Override
	public boolean onCreate(){
		return true;
	}

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder){
		return null;
	}

	@Nullable
	@Override
	public String getType(@NonNull Uri uri){
		if(isCorrectUri(uri))
			return "application/vnd.android.package-archive";
		return null;
	}

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, @Nullable ContentValues values){
		return null;
	}

	@Override
	public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs){
		return 0;
	}

	@Override
	public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs){
		return 0;
	}

	@Nullable
	@Override
	public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException{
		if(isCorrectUri(uri)){
			return ParcelFileDescriptor.open(((GithubSelfUpdaterImpl)GithubSelfUpdater.getInstance()).getUpdateApkFile(), ParcelFileDescriptor.MODE_READ_ONLY);
		}
		throw new FileNotFoundException();
	}

	private boolean isCorrectUri(Uri uri){
		return "/update.apk".equals(uri.getPath());
	}
}
