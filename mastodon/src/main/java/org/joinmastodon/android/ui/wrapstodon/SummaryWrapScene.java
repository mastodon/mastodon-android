package org.joinmastodon.android.ui.wrapstodon;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AnnualReport;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class SummaryWrapScene extends AnnualWrapScene{
	private final Map<String, Account> allAccounts;
	private final Map<String, Status> allStatuses;
	private final AnnualReport report;
	private final Account self;
	private Drawable postIcon;

	public SummaryWrapScene(Account self, Map<String, Account> allAccounts, Map<String, Status> allStatuses, AnnualReport report){
		this.allAccounts=allAccounts;
		this.allStatuses=allStatuses;
		this.report=report;
		this.self=self;
	}


	@SuppressLint("SetTextI18n")
	@Override
	protected View onCreateContentView(Context context){
		LayoutInflater inflater=LayoutInflater.from(context);
		View content=inflater.inflate(R.layout.wrap_summary, null);

		ImageView appIcon=content.findViewById(R.id.app_icon);
		ImageView selfAva=content.findViewById(R.id.self_ava);
		TextView selfName=content.findViewById(R.id.self_name);
		TextView mostBoostedText=content.findViewById(R.id.most_boosted_post_text);
		TextView plusFollowers=content.findViewById(R.id.plus_followers);
		TextView followersLabel=content.findViewById(R.id.followers_label);
		TextView totalFollowers=content.findViewById(R.id.followers_total);
		View followersChart=content.findViewById(R.id.followers_chart);
		TextView hashtag=content.findViewById(R.id.most_used_hashtag);
		TextView followersPercent=content.findViewById(R.id.followers_percentile);
		TextView newPostsCount=content.findViewById(R.id.new_posts);
		TextView newPostsLabel=content.findViewById(R.id.new_posts_label);
		View newPostsBlock=content.findViewById(R.id.new_posts_block);
		postIcon=prepareIcon(context, R.drawable.ic_chat_bubble_wght700_20px);

		if(!report.mostUsedApps.isEmpty()){
			AnnualReport.NameAndCount app=report.mostUsedApps.get(0);
			if("Mastodon for Android".equals(app.name) || "Mastodon for iOS".equals(app.name)){
				appIcon.setImageResource(R.mipmap.ic_launcher);
			}else{
				String url=AppsWrapScene.getIconUrl(app.name);
				if(url==null)
					url=AppsWrapScene.getIconUrl(app.name.split(" ")[0]);
				if(url!=null){
					ViewImageLoader.loadWithoutAnimation(appIcon, null, new UrlImageLoaderRequest(Bitmap.Config.ARGB_8888, V.dp(96), V.dp(96), List.of(), Uri.parse(url)));
				}
			}
		}
		ViewImageLoader.loadWithoutAnimation(selfAva, null, new UrlImageLoaderRequest(Bitmap.Config.ARGB_8888, V.dp(40), V.dp(40), List.of(), Uri.parse(self.avatarStatic)));
		selfName.setText(HtmlParser.parseCustomEmoji(self.displayName, self.emojis));
		UiUtils.loadCustomEmojiInTextView(selfName);
		Status mostBoosted=allStatuses.get(report.topStatuses.byReblogs);
		if(mostBoosted!=null){
			mostBoostedText.setText(HtmlParser.parse(mostBoosted.content, mostBoosted.emojis, mostBoosted.mentions, mostBoosted.tags, null, mostBoosted));
		}
		int newFollowers=report.timeSeries.stream().mapToInt(p->p.followers).sum();
		plusFollowers.setText("+"+UiUtils.abbreviateNumber(newFollowers));
		followersLabel.setText(content.getResources().getQuantityString(R.plurals.followers, newFollowers>1000 ? 9999 : newFollowers));
		totalFollowers.setText(context.getString(R.string.x_followers_total, UiUtils.abbreviateNumber(self.followersCount)));
		if(!report.topHashtags.isEmpty()){
			hashtag.setText("#"+report.topHashtags.get(0).name);
		}
		double followersPercentile=Objects.requireNonNullElse(report.percentiles.get("followers"), 0.0);
		followersPercent.setText(Math.max(1, Math.round(100-followersPercentile))+"%");
		newPostsCount.setText(NumberFormat.getInstance().format(report.typeDistribution.standalone));
		newPostsLabel.setText(context.getResources().getQuantityString(R.plurals.new_posts, report.typeDistribution.standalone));

		newPostsBlock.setBackground(new NewPostsBackgroundDrawable());
		followersChart.setBackground(new FollowersChartDrawable());

		return content;
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

	private class NewPostsBackgroundDrawable extends Drawable{

		private static final int GRID_W=11;
		private static final int GRID_H=6;
		private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);

		@Override
		public void draw(@NonNull Canvas canvas){
			Rect bounds=getBounds();
			paint.setColor(0xFF2F0C7A);
			canvas.drawRect(bounds, paint);
			float spacing=(bounds.width()+V.dp(16)-V.dp(16*GRID_W))/(float)(GRID_W-1);
			float radius=V.dp(8);
			canvas.saveLayerAlpha(bounds.left, bounds.top, bounds.right, bounds.bottom, 51);
			canvas.translate(-radius, -radius);
			for(int y=0;y<GRID_H;y++){
				canvas.save();
				for(int x=0;x<GRID_W;x++){
					int color;
					Drawable icon;
					color=0xFFFFBE2E;
					icon=postIcon;
					paint.setColor(color);
					canvas.drawCircle(radius, radius, radius, paint);
					if(icon!=null){
						icon.draw(canvas);
					}
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
			return PixelFormat.OPAQUE;
		}
	}

	private class FollowersChartDrawable extends Drawable{
		private Path path=new Path();
		private Paint fillPaint=new Paint(Paint.ANTI_ALIAS_FLAG), strokePaint=new Paint(Paint.ANTI_ALIAS_FLAG);

		public FollowersChartDrawable(){
			strokePaint.setStyle(Paint.Style.STROKE);
			strokePaint.setStrokeWidth(V.dp(1));
			strokePaint.setColor(0xFF562CFC);
		}

		@Override
		public void draw(@NonNull Canvas canvas){
			canvas.drawPath(path, fillPaint);
			canvas.drawPath(path, strokePaint);
		}

		@Override
		public void setAlpha(int alpha){

		}

		@Override
		public void setColorFilter(@Nullable ColorFilter colorFilter){

		}

		@Override
		protected void onBoundsChange(@NonNull Rect bounds){
			path.rewind();
			int pad=V.dp(16);
			path.moveTo(-20, bounds.height()+20);
			float dx=bounds.width()/11f;
			float x=0f;
			int max=report.timeSeries.stream().mapToInt(p->p.followers).max().orElse(1);
			for(AnnualReport.TimeSeriesPoint point:report.timeSeries){
				float fraction=point.followers/(float)max;
				path.lineTo(x, pad+(bounds.height()-pad*2)*(1f-fraction));
				x+=dx;
			}
			path.lineTo(bounds.width()+20, bounds.height()+20);
			path.close();
			fillPaint.setShader(new LinearGradient(0, pad, 0, bounds.height()-pad, 0x80562CFC, 0x00562CFC, Shader.TileMode.CLAMP));
		}

		@Override
		public int getOpacity(){
			return PixelFormat.TRANSLUCENT;
		}
	}
}
