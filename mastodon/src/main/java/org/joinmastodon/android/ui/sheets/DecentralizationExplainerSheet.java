package org.joinmastodon.android.ui.sheets;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.Snackbar;
import org.joinmastodon.android.ui.text.LinkSpan;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.RippleAnimationTextView;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import androidx.annotation.NonNull;
import me.grishka.appkit.views.BottomSheet;

public class DecentralizationExplainerSheet extends BottomSheet{
	private final String handleStr;

	public DecentralizationExplainerSheet(@NonNull Context context, String accountID, Account account){
		super(context);
		View content=context.getSystemService(LayoutInflater.class).inflate(R.layout.sheet_decentralization_info, null);
		setContentView(content);
		setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(context, R.attr.colorM3Surface),
				UiUtils.getThemeColor(context, R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());

		TextView handleTitle=findViewById(R.id.handle_title);
		RippleAnimationTextView handle=findViewById(R.id.handle);
		TextView usernameExplanation=findViewById(R.id.username_text);
		TextView serverExplanation=findViewById(R.id.server_text);
		TextView handleExplanation=findViewById(R.id.handle_explanation);
		findViewById(R.id.btn_cancel).setOnClickListener(v->dismiss());

		String domain=account.getDomain();
		if(TextUtils.isEmpty(domain))
			domain=AccountSessionManager.get(accountID).domain;
		handleStr="@"+account.username+"@"+domain;
		boolean isSelf=AccountSessionManager.getInstance().isSelf(accountID, account);

		handleTitle.setText(isSelf ? R.string.handle_title_own : R.string.handle_title);
		handle.setText(handleStr);
		usernameExplanation.setText(isSelf ? R.string.handle_username_explanation_own : R.string.handle_username_explanation);
		serverExplanation.setText(isSelf ? R.string.handle_server_explanation_own : R.string.handle_server_explanation);

		String explanation=context.getString(isSelf ? R.string.handle_explanation_own : R.string.handle_explanation);
		SpannableStringBuilder ssb=new SpannableStringBuilder();
		Jsoup.parseBodyFragment(explanation).body().traverse(new NodeVisitor(){
			private int spanStart;
			@Override
			public void head(Node node, int depth){
				if(node instanceof TextNode tn){
					ssb.append(tn.text());
				}else if(node instanceof Element){
					spanStart=ssb.length();
				}
			}

			@Override
			public void tail(Node node, int depth){
				if(node instanceof Element){
					ssb.setSpan(new LinkSpan("", DecentralizationExplainerSheet.this::showActivityPubAlert, LinkSpan.Type.CUSTOM, null, null, null), spanStart, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
		});
		handleExplanation.setText(ssb);

		findViewById(R.id.handle_wrap).setOnClickListener(v->{
			context.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, handleStr));
			if(UiUtils.needShowClipboardToast()){
				new Snackbar.Builder(context)
						.setText(R.string.handle_copied)
						.show();
			}
		});
		String _domain=domain;
		findViewById(R.id.username_row).setOnClickListener(v->handle.animate(1, account.username.length()+1));
		findViewById(R.id.server_row).setOnClickListener(v->handle.animate(handleStr.length()-_domain.length(), handleStr.length()));
	}

	private void showActivityPubAlert(LinkSpan s){
		new M3AlertDialogBuilder(getContext())
				.setTitle(R.string.what_is_activitypub_title)
				.setMessage(R.string.what_is_activitypub)
				.setPositiveButton(R.string.ok, null)
				.show();
	}
}
