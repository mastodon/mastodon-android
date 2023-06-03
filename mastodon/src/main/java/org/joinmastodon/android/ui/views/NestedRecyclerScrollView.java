package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;
import android.widget.ScrollView;

import org.joinmastodon.android.R;

import java.util.function.Supplier;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class NestedRecyclerScrollView extends CustomScrollView{
	private Supplier<View> scrollableChildSupplier;
	private boolean takePriorityOverChildViews;

	public NestedRecyclerScrollView(Context context){
		this(context, null);
	}

	public NestedRecyclerScrollView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public NestedRecyclerScrollView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		TypedArray ta=context.obtainStyledAttributes(attrs, R.styleable.NestedRecyclerScrollView);
		takePriorityOverChildViews=ta.getBoolean(R.styleable.NestedRecyclerScrollView_takePriorityOverChildViews, false);
		ta.recycle();
	}

	@Override
	public void onNestedPreScroll(View target, int dx, int dy, int[] consumed){
		if(takePriorityOverChildViews){
			if((dy<0 && getScrollY()>0) || (dy>0 && canScrollVertically(1))){
				scrollBy(0, dy);
				consumed[1]=dy;
				return;
			}
		}else if((dy<0 && isScrolledToTop(target)) || (dy>0 && !isScrolledToBottom())){
			scrollBy(0, dy);
			consumed[1]=dy;
			return;
		}
		super.onNestedPreScroll(target, dx, dy, consumed);
	}

	@Override
	public boolean onNestedPreFling(View target, float velX, float velY){
		if(takePriorityOverChildViews){
			if((velY<0 && getScrollY()>0) || (velY>0 && canScrollVertically(1))){
				fling((int)velY);
				return true;
			}
		}else if((velY<0 && isScrolledToTop(target)) || (velY>0 && !isScrolledToBottom())){
			fling((int) velY);
			return true;
		}
		return super.onNestedPreFling(target, velX, velY);
	}

	private boolean isScrolledToBottom(){
		return !canScrollVertically(1);
	}

	private boolean isScrolledToTop(View view){
		if(view instanceof RecyclerView rv){
			final LinearLayoutManager lm=(LinearLayoutManager) rv.getLayoutManager();
			return lm.findFirstVisibleItemPosition()==0
					&& lm.findViewByPosition(0).getTop()==rv.getPaddingTop();
		}
		return !view.canScrollVertically(-1);
	}

	public void setScrollableChildSupplier(Supplier<View> scrollableChildSupplier){
		this.scrollableChildSupplier=scrollableChildSupplier;
	}

	@Override
	protected boolean onScrollingHitEdge(float velocity){
		if(velocity>0 || takePriorityOverChildViews){
			View view=scrollableChildSupplier==null ? null : scrollableChildSupplier.get();
			if(view instanceof RecyclerView rv){
				return rv.fling(0, (int) velocity);
			}else if(view instanceof ScrollView sv){
				if(sv.canScrollVertically((int)velocity)){
					sv.fling((int)velocity);
					return true;
				}
			}else if(view instanceof CustomScrollView sv){
				if(sv.canScrollVertically((int)velocity)){
					sv.fling((int)velocity);
					return true;
				}
			}else if(view instanceof WebView wv){
				if(wv.canScrollVertically((int)velocity)){
					wv.flingScroll(0, (int)velocity);
					return true;
				}
			}
		}
		return false;
	}

	public boolean isTakePriorityOverChildViews(){
		return takePriorityOverChildViews;
	}

	public void setTakePriorityOverChildViews(boolean takePriorityOverChildViews){
		this.takePriorityOverChildViews=takePriorityOverChildViews;
	}
}
