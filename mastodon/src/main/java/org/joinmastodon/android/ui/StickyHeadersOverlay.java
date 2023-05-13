package org.joinmastodon.android.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class StickyHeadersOverlay{
	private static final String TAG="StickyHeadersOverlay";

	private FrameLayout headerWrapper;
	private Context context;
	private RecyclerView parent;
	private RecyclerView.ViewHolder currentHeaderHolder;
	private int headerViewType;

	public StickyHeadersOverlay(Context context, int headerViewType){
		this.context=context;
		this.headerViewType=headerViewType;
		headerWrapper=new FrameLayout(context);
	}

	public void install(RecyclerView parent){
		if(this.parent!=null)
			throw new IllegalStateException();
		this.parent=parent;
		parent.getViewTreeObserver().addOnPreDrawListener(()->{
			if(parent.getWidth()!=headerWrapper.getWidth() || parent.getHeight()!=headerWrapper.getHeight()){
				headerWrapper.measure(parent.getWidth() | View.MeasureSpec.EXACTLY, parent.getHeight() | View.MeasureSpec.EXACTLY);
				headerWrapper.layout(0, 0, parent.getWidth(), parent.getHeight());
			}
			return true;
		});
		parent.getOverlay().add(headerWrapper);

		parent.addOnScrollListener(new RecyclerView.OnScrollListener(){
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
				if(currentHeaderHolder==null){
					currentHeaderHolder=parent.getAdapter().createViewHolder(parent, headerViewType);
					headerWrapper.addView(currentHeaderHolder.itemView);
				}
				int firstVisiblePos=parent.getChildAdapterPosition(parent.getChildAt(0));
				RecyclerView.Adapter<RecyclerView.ViewHolder> adapter=Objects.requireNonNull(parent.getAdapter());
				// Go backwards from the first visible position to find the previous header
				for(int i=firstVisiblePos;i>=0;i--){
					if(adapter.getItemViewType(i)==headerViewType){
						if(currentHeaderHolder.getAbsoluteAdapterPosition()!=i){
							adapter.bindViewHolder(currentHeaderHolder, i);
						}
						break;
					}
				}
				if(currentHeaderHolder instanceof HeaderViewHolder hvh){
					hvh.setStickyFactor(firstVisiblePos==0 && parent.getChildAt(0).getTop()==0 ? 0 : 1);
				}
				// Now go forward and find the next header view to possibly offset the current one
				for(int i=firstVisiblePos+1;i<adapter.getItemCount();i++){
					if(adapter.getItemViewType(i)==headerViewType){
						RecyclerView.ViewHolder holder=parent.findViewHolderForAdapterPosition(i);
						if(holder!=null){
							float factor;
							if(holder.itemView.getTop()<currentHeaderHolder.itemView.getBottom()){
								currentHeaderHolder.itemView.setTranslationY(holder.itemView.getTop()-currentHeaderHolder.itemView.getBottom());
								factor=1f-holder.itemView.getTop()/(float)currentHeaderHolder.itemView.getBottom();
							}else{
								currentHeaderHolder.itemView.setTranslationY(0);
								factor=0;
							}
							if(holder instanceof HeaderViewHolder hvh)
								hvh.setStickyFactor(factor);
						}
						break;
					}
				}
			}
		});
	}

	public interface HeaderViewHolder{
		void setStickyFactor(float factor);
	}
}
