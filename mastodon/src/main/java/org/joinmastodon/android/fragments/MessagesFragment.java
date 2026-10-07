package org.joinmastodon.android.fragments;

import android.os.Bundle;

import org.joinmastodon.android.ui.adapters.GenericListItemsAdapter;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class MessagesFragment extends MastodonRecyclerFragment<Void>{
	public MessagesFragment(){
		super(25);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setEmptyText("Not implemented yet");
		onDataLoaded(List.of());
	}

	@Override
	protected void doLoadData(int offset, int count){

	}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		return new GenericListItemsAdapter<>(List.of());
	}
}
