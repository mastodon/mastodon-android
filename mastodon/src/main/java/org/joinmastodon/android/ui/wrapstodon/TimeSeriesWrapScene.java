package org.joinmastodon.android.ui.wrapstodon;

import android.content.Context;
import android.content.res.Resources;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.AnnualReport;

import java.text.NumberFormat;
import java.util.List;

public class TimeSeriesWrapScene extends AnnualWrapScene{
	private final List<AnnualReport.TimeSeriesPoint> points;
	private TextView postsCounter, followersCounter, followingCounter;
	private TextView postsLabel, followersLabel, followingLabel;
	private TextView monthLabel;

	private int currentMonth, monthOnGestureStart;
	private float downX, downY, touchslop;
	private boolean trackingTouch, figuringOutWhetherWeWantThisGesture;

	public TimeSeriesWrapScene(List<AnnualReport.TimeSeriesPoint> points){
		this.points=points;
	}

	@Override
	protected View onCreateContentView(Context context){
		LayoutInflater inflater=LayoutInflater.from(context);
		View content=inflater.inflate(R.layout.wrap_time_series, null);

		postsCounter=content.findViewById(R.id.posts_counter);
		followersCounter=content.findViewById(R.id.followers_counter);
		followingCounter=content.findViewById(R.id.following_counter);
		postsLabel=content.findViewById(R.id.posts_label);
		followersLabel=content.findViewById(R.id.followers_label);
		followingLabel=content.findViewById(R.id.following_label);
		monthLabel=content.findViewById(R.id.month_label);
		setMonth(0);

		content.setOnTouchListener(this::onContentTouch);
		touchslop=ViewConfiguration.get(context).getScaledTouchSlop();

		return content;
	}

	@Override
	protected void onDestroyContentView(){

	}

	private boolean onContentTouch(View v, MotionEvent ev){
		if(ev.getAction()==MotionEvent.ACTION_DOWN){
			downX=ev.getX();
			downY=ev.getY();
			trackingTouch=false;
			figuringOutWhetherWeWantThisGesture=true;
			monthOnGestureStart=currentMonth;
			v.getParent().requestDisallowInterceptTouchEvent(true);
		}else if(ev.getAction()==MotionEvent.ACTION_MOVE){
			if(!trackingTouch){
				float dX=Math.abs(downX-ev.getX());
				float dY=Math.abs(downY-ev.getY());
				if(dX>dY && dX>=touchslop){
					trackingTouch=true;
					figuringOutWhetherWeWantThisGesture=false;
				}else if(dY>=touchslop){
					v.getParent().requestDisallowInterceptTouchEvent(false);
					figuringOutWhetherWeWantThisGesture=false;
				}
			}else{
				int monthsToAdvance=(int)((ev.getX()-downX)/(v.getWidth()/15f));
				if(monthsToAdvance!=0){
					int newCurrentMonth=Math.min(11, Math.max(0, monthOnGestureStart+monthsToAdvance));
					if(newCurrentMonth!=currentMonth){
						setMonth(newCurrentMonth);
						v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
					}
				}
			}
		}
		return trackingTouch || figuringOutWhetherWeWantThisGesture;
	}

	private void setMonth(int monthIndex){
		currentMonth=monthIndex;
		AnnualReport.TimeSeriesPoint point=points.get(monthIndex);
		postsCounter.setText(NumberFormat.getInstance().format(point.statuses));
		followersCounter.setText(NumberFormat.getInstance().format(point.followers));
		followingCounter.setText(NumberFormat.getInstance().format(point.following));
		Resources r=postsCounter.getResources();
		postsLabel.setText(r.getQuantityString(R.plurals.posts, point.statuses));
		followersLabel.setText(r.getQuantityString(R.plurals.followers, point.followers));
		followingLabel.setText(r.getQuantityString(R.plurals.following, point.following));
		monthLabel.setText(r.getStringArray(R.array.months_standalone)[monthIndex]);
	}
}
