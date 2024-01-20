package org.joinmastodon.android.ui.wrapstodon;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.AnnualReport;
import org.joinmastodon.android.ui.utils.NestedScrollingTouchDisallower;

import java.text.NumberFormat;
import java.util.function.Consumer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import me.grishka.appkit.utils.V;

public class ByTheNumbersWrapScene extends AnnualWrapScene{
	private static final int GRID_W=15, GRID_H=27;

	private final AnnualReport.TypeDistribution typeDistribution;
	private int boostsCircleCount, repliesCircleCount, standaloneCircleCount;
	private int revealedCircleCount;
	private Drawable boostIcon, replyIcon, postIcon;
	private RectF tmpRect=new RectF();

	public ByTheNumbersWrapScene(AnnualReport.TypeDistribution typeDistribution){
		this.typeDistribution=typeDistribution;
		int totalCircles=GRID_W*GRID_H;
		boostsCircleCount=Math.round(totalCircles*(typeDistribution.reblogs/(float)typeDistribution.total));
		repliesCircleCount=Math.round(totalCircles*(typeDistribution.replies/(float)typeDistribution.total));
		standaloneCircleCount=totalCircles-boostsCircleCount-repliesCircleCount;
	}

	@Override
	protected View onCreateContentView(Context context){
		boostIcon=prepareIcon(context, R.drawable.ic_repeat_wght700grad200fill1_20px);
		replyIcon=prepareIcon(context, R.drawable.ic_reply_wght700_20px);
		postIcon=prepareIcon(context, R.drawable.ic_chat_bubble_wght700_20px);

		ViewPager2 pager=new ViewPager2(context);
		pager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
		pager.setAdapter(new PagerAdapter());
		pager.getChildAt(0).setOnTouchListener(new NestedScrollingTouchDisallower(pager));
		BackgroundDrawable bg=new BackgroundDrawable();
		pager.setBackground(bg);
		pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){
				if(position==0){
					revealedCircleCount=Math.round(boostsCircleCount*positionOffset);
				}else if(position==1){
					revealedCircleCount=boostsCircleCount+Math.round(repliesCircleCount*positionOffset);
				}else if(position==2){
					revealedCircleCount=boostsCircleCount+repliesCircleCount+Math.round(standaloneCircleCount*positionOffset);
				}else{
					revealedCircleCount=GRID_W*GRID_H;
				}
				pager.invalidate();
			}
		});
		return pager;
	}

	@Override
	protected void onDestroyContentView(){

	}

	private Drawable prepareIcon(Context context, int res){
		Drawable d=context.getResources().getDrawable(res, context.getTheme()).mutate();
		d.setTint(0xFF17063B);
		d.setBounds(V.dp(2), V.dp(2), V.dp(14), V.dp(14));
		return d;
	}

	private class PagerAdapter extends RecyclerView.Adapter<PageViewHolder>{

		@NonNull
		@Override
		public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new PageViewHolder(parent);
		}

		@Override
		public void onBindViewHolder(@NonNull PageViewHolder holder, int position){
			Resources r=holder.itemView.getResources();
			holder.topText.setText(switch(position){
				case 0 -> r.getString(R.string.wrap_numbers_total_title);
				case 1 -> r.getString(R.string.wrap_numbers_boosts_title, typeDistribution.reblogs);
				case 2 -> r.getString(R.string.wrap_numbers_replies_title, typeDistribution.replies);
				case 3 -> r.getString(R.string.wrap_numbers_standalone_posts_title, typeDistribution.standalone);
				default -> throw new IllegalStateException("Unexpected value: "+position);
			});
			holder.number.setText(switch(position){
				case 0 -> NumberFormat.getInstance().format(typeDistribution.total);
				case 1 -> Math.round(typeDistribution.reblogs/(float)typeDistribution.total*100f)+"%";
				case 2 -> Math.round(typeDistribution.replies/(float)typeDistribution.total*100f)+"%";
				case 3 -> Math.round(typeDistribution.standalone/(float)typeDistribution.total*100f)+"%";
				default -> throw new IllegalStateException("Unexpected value: "+position);
			});
			holder.bottomText.setText(switch(position){
				case 0 -> r.getString(R.string.wrap_numbers_x_times_in_year, year);
				case 1 -> r.getString(R.string.wrap_numbers_boosts);
				case 2 -> r.getString(R.string.wrap_numbers_replies);
				case 3 -> r.getString(R.string.wrap_numbers_standalone_posts);
				default -> throw new IllegalStateException("Unexpected value: "+position);
			});
			holder.itemView.setBackgroundTintList(ColorStateList.valueOf(switch(position){
				case 0, 1 -> 0xFFFFBE2E;
				case 2 -> 0xFF858AFA;
				case 3 -> 0xFFBAFF3B;
				default -> throw new IllegalStateException("Unexpected value: "+position);
			}));
		}

		@Override
		public int getItemCount(){
			return 4;
		}
	}

	private static class PageViewHolder extends RecyclerView.ViewHolder{
		private final TextView topText, number, bottomText;

		public PageViewHolder(@NonNull ViewGroup parent){
			super(LayoutInflater.from(parent.getContext()).inflate(R.layout.wrap_numbers_page, parent, false));
			topText=itemView.findViewById(R.id.top_text);
			number=itemView.findViewById(R.id.number);
			bottomText=itemView.findViewById(R.id.bottom_text);
		}
	}

	private class BackgroundDrawable extends Drawable{
		private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);

		@Override
		public void draw(@NonNull Canvas canvas){
			Rect bounds=getBounds();
			float spacing=(bounds.width()-V.dp(16+16*GRID_W))/(float)(GRID_W-1);
			int i=0;
			float radius=V.dp(8);
			canvas.save();
			canvas.translate(radius, radius);
			for(int y=0;y<GRID_H;y++){
				canvas.save();
				for(int x=0;x<GRID_W;x++){
					int color;
					Drawable icon;
					if(i<revealedCircleCount){
						if(i<boostsCircleCount){
							color=0xFFFFBE2E;
							icon=boostIcon;
						}else if(i<boostsCircleCount+repliesCircleCount){
							color=0xFF858AFA;
							icon=replyIcon;
						}else{
							color=0xFFBAFF3B;
							icon=postIcon;
						}
					}else{
						color=0xFF2F0C7A;
						icon=null;
					}
					paint.setColor(color);
					canvas.drawCircle(radius, radius, radius, paint);
					if(icon!=null){
						icon.draw(canvas);
					}
					i++;
					canvas.translate(radius*2+spacing, 0);
				}
				canvas.restore();
				canvas.translate(0, radius*2+spacing);
			}
			canvas.restore();
		}

		@Override
		public void setAlpha(int alpha){

		}

		@Override
		public void setColorFilter(@Nullable ColorFilter colorFilter){

		}

		@Override
		public int getOpacity(){
			return PixelFormat.TRANSLUCENT;
		}
	}
}
