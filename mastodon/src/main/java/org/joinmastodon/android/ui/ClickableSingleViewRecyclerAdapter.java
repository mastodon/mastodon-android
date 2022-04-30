package org.joinmastodon.android.ui;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.views.UsableRecyclerView;

public class ClickableSingleViewRecyclerAdapter extends SingleViewRecyclerAdapter{
	private final Runnable onClick;

	public ClickableSingleViewRecyclerAdapter(View view, Runnable onClick){
		super(view);
		this.onClick=onClick;
	}

	@NonNull
	@Override
	public ViewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
		return new ClickableViewViewHolder(view);
	}

	public class ClickableViewViewHolder extends ViewViewHolder implements UsableRecyclerView.Clickable{
		public ClickableViewViewHolder(@NonNull View itemView){
			super(itemView);
		}

		@Override
		public void onClick(){
			onClick.run();
		}
	}
}
