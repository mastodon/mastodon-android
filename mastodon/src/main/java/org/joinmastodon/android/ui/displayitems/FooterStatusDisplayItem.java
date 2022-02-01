package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.text.DecimalFormat;

import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;

public class FooterStatusDisplayItem extends StatusDisplayItem{
	private final Status status;
	private final String accountID;

	public FooterStatusDisplayItem(String parentID, Status status, String accountID){
		super(parentID);
		this.status=status;
		this.accountID=accountID;
	}

	@Override
	public Type getType(){
		return Type.FOOTER;
	}

	public static class Holder extends BindableViewHolder<FooterStatusDisplayItem>{
		private final TextView reply, boost, favorite;
		private final ImageView share;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_footer, parent);
			reply=findViewById(R.id.reply);
			boost=findViewById(R.id.boost);
			favorite=findViewById(R.id.favorite);
			share=findViewById(R.id.share);
			if(Build.VERSION.SDK_INT<Build.VERSION_CODES.N){
				UiUtils.fixCompoundDrawableTintOnAndroid6(reply, R.color.text_secondary);
				UiUtils.fixCompoundDrawableTintOnAndroid6(boost, R.color.text_secondary);
				UiUtils.fixCompoundDrawableTintOnAndroid6(favorite, R.color.text_secondary);
			}
		}

		@Override
		public void onBind(FooterStatusDisplayItem item){
			bindButton(reply, item.status.repliesCount);
			bindButton(boost, item.status.reblogsCount);
			bindButton(favorite, item.status.favouritesCount);
		}

		private void bindButton(TextView btn, int count){
			if(count>0){
				btn.setText(DecimalFormat.getIntegerInstance().format(count));
				btn.setCompoundDrawablePadding(V.dp(8));
			}else{
				btn.setText("");
				btn.setCompoundDrawablePadding(0);
			}
		}
	}
}
