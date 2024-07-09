package org.joinmastodon.android.api;

import android.database.sqlite.SQLiteDatabase;

import java.io.IOException;

@FunctionalInterface
public interface DatabaseRunnable{
	void run(SQLiteDatabase db) throws IOException;
}
