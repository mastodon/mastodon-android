package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.grishka.appkit.fragments.ToolbarFragment;

public class SearchFragment extends ToolbarFragment{
	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		return new View(getActivity());
	}
}
