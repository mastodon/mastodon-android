package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import java.util.function.Supplier;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class NestedRecyclerScrollView extends CustomScrollView{
	private Supplier<RecyclerView> scrollableChildSupplier;

	public NestedRecyclerScrollView(Context context){
		super(context);
	}

	public NestedRecyclerScrollView(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	public NestedRecyclerScrollView(Context context, AttributeSet attrs, int defStyleAttr){
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
		if(target instanceof RecyclerView rv && ((dy < 0 && isScrolledToTop(rv)) || (dy > 0 && !isScrolledToBottom()))){
			scrollBy(0, dy);
			consumed[1] = dy;
			return;
		}
		super.onNestedPreScroll(target, dx, dy, consumed);
	}

	@Override
	public boolean onNestedPreFling(View target, float velX, float velY) {
		if (target instanceof RecyclerView rv && ((velY < 0 && isScrolledToTop(rv)) || (velY > 0 && !isScrolledToBottom()))){
			fling((int) velY);
			return true;
		}
		return super.onNestedPreFling(target, velX, velY);
	}

	private boolean isScrolledToBottom() {
		return !canScrollVertically(1);
	}

	private boolean isScrolledToTop(RecyclerView rv) {
		final LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
		return lm.findFirstVisibleItemPosition() == 0
				&& lm.findViewByPosition(0).getTop() == rv.getPaddingTop();
	}

	public void setScrollableChildSupplier(Supplier<RecyclerView> scrollableChildSupplier){
		this.scrollableChildSupplier=scrollableChildSupplier;
	}

	@Override
	protected boolean onScrollingHitEdge(float velocity){
		if(velocity>0){
			RecyclerView view=scrollableChildSupplier.get();
			if(view!=null){
				return view.fling(0, (int)velocity);
			}
		}
		return false;
	}
}
