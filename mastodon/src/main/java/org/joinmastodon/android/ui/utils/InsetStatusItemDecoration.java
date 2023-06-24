package org.joinmastodon.android.ui.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.ui.displayitems.NotificationHeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ReblogOrReplyLineStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.utils.V;

public class InsetStatusItemDecoration extends RecyclerView.ItemDecoration{
	private final BaseStatusListFragment<?> listFragment;
	private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
	private int bgColor;
	private int borderColor;
	private RectF rect=new RectF();

	public InsetStatusItemDecoration(BaseStatusListFragment<?> listFragment){
		this.listFragment=listFragment;
		bgColor=UiUtils.getThemeColor(listFragment.getActivity(), R.attr.colorM3SurfaceVariant);
		borderColor=UiUtils.getThemeColor(listFragment.getActivity(), R.attr.colorM3OutlineVariant);
	}

	@Override
	public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
		List<StatusDisplayItem> displayItems=listFragment.getDisplayItems();
		int pos=0;
		for(int i=0; i<parent.getChildCount(); i++){
			View child=parent.getChildAt(i);
			RecyclerView.ViewHolder holder=parent.getChildViewHolder(child);
			pos=holder.getAbsoluteAdapterPosition();
			boolean inset=(holder instanceof StatusDisplayItem.Holder<?> sdi) && sdi.getItem().inset;
			if(inset){
				if(rect.isEmpty()){
					float childY=child.getY();
					if(pos>0 && displayItems.get(pos-1).getType()==StatusDisplayItem.Type.REBLOG_OR_REPLY_LINE){
						childY+=V.dp(8);
					}
					rect.set(child.getX(), i==0 && pos>0 && displayItems.get(pos-1).inset ? V.dp(-10) : childY, child.getX()+child.getWidth(), child.getY()+child.getHeight());
				}else{
					rect.bottom=Math.max(rect.bottom, child.getY()+child.getHeight());
				}
			}else if(!rect.isEmpty()){
				drawInsetBackground(parent, c);
				rect.setEmpty();
			}
		}
		if(!rect.isEmpty()){
			if(pos<displayItems.size()-1 && displayItems.get(pos+1).inset){
				rect.bottom=parent.getHeight()+V.dp(10);
			}
			drawInsetBackground(parent, c);
			rect.setEmpty();
		}
	}

	private void drawInsetBackground(RecyclerView list, Canvas c){
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(bgColor);
		rect.left=V.dp(16);
		rect.right=list.getWidth()-V.dp(16);
		c.drawRoundRect(rect, V.dp(4), V.dp(4), paint);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(V.dp(1));
		paint.setColor(borderColor);
		rect.inset(paint.getStrokeWidth()/2f, paint.getStrokeWidth()/2f);
		c.drawRoundRect(rect, V.dp(4), V.dp(4), paint);
	}

	@Override
	public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
		List<StatusDisplayItem> displayItems=listFragment.getDisplayItems();
		RecyclerView.ViewHolder holder=parent.getChildViewHolder(view);
		if(holder instanceof StatusDisplayItem.Holder<?> sdi){
			boolean inset=sdi.getItem().inset;
			int pos=holder.getAbsoluteAdapterPosition();
			if(inset){
				boolean topSiblingInset=pos>0 && displayItems.get(pos-1).inset;
				boolean bottomSiblingInset=pos<displayItems.size()-1 && displayItems.get(pos+1).inset;
				StatusDisplayItem.Type type=sdi.getItem().getType();
				if(type==StatusDisplayItem.Type.CARD || type==StatusDisplayItem.Type.MEDIA_GRID)
					outRect.left=outRect.right=V.dp(16);
				else
					outRect.left=outRect.right=V.dp(8);
				if(!bottomSiblingInset)
					outRect.bottom=V.dp(16);
				if(!topSiblingInset && displayItems.get(pos-1) instanceof NotificationHeaderStatusDisplayItem)
					outRect.top=V.dp(-8);
			}
		}
	}
}
