package org.joinmastodon.android;

import android.app.Fragment;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import me.grishka.appkit.FragmentStackActivity;

public class ExternalShareActivity extends FragmentStackActivity{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState){
		UiUtils.setUserPreferredTheme(this);
		super.onCreate(savedInstanceState);
		if(savedInstanceState==null){
			List<AccountSession> sessions=AccountSessionManager.getInstance().getLoggedInAccounts();
			if(sessions.isEmpty()){
				Toast.makeText(this, R.string.err_not_logged_in, Toast.LENGTH_SHORT).show();
				finish();
			}else if(sessions.size()==1){
				openComposeFragment(sessions.get(0).getID());
			}else{
				getWindow().setBackgroundDrawable(new ColorDrawable(0xff000000));
				new M3AlertDialogBuilder(this)
						.setItems(sessions.stream().map(as->"@"+as.self.username+"@"+as.domain).toArray(String[]::new), (dialog, which)->{
							openComposeFragment(sessions.get(which).getID());
						})
						.setTitle(R.string.choose_account)
						.setOnCancelListener(dialog -> finish())
						.show();
			}
		}
	}

	private void openComposeFragment(String accountID){
		getWindow().setBackgroundDrawable(null);

		Intent intent=getIntent();
		String text=intent.getStringExtra(Intent.EXTRA_TEXT);
		String subject=intent.getStringExtra(Intent.EXTRA_SUBJECT);
		if(!TextUtils.isEmpty(subject)){
			if(!TextUtils.isEmpty(text))
				text=subject+"\n"+text;
			else
				text=subject;
		}
		List<Uri> mediaUris;
		if(Intent.ACTION_SEND.equals(intent.getAction())){
			Uri singleUri=intent.getParcelableExtra(Intent.EXTRA_STREAM);
			mediaUris=singleUri!=null ? Collections.singletonList(singleUri) : null;
		}else if(Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())){
			ClipData clipData=intent.getClipData();
			if(clipData!=null){
				mediaUris=new ArrayList<>(clipData.getItemCount());
				for(int i=0;i<clipData.getItemCount();i++){
					ClipData.Item item=clipData.getItemAt(i);
					mediaUris.add(item.getUri());
				}
			}else{
				mediaUris=null;
			}
		}else{
			Toast.makeText(this, "Unexpected intent action: "+intent.getAction(), Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		Bundle args=new Bundle();
		args.putString("account", accountID);
		if(!TextUtils.isEmpty(text))
			args.putString("prefilledText", text);
		if(mediaUris!=null && !mediaUris.isEmpty())
			args.putParcelableArrayList("mediaAttachments", toArrayList(mediaUris));
		Fragment fragment=new ComposeFragment();
		fragment.setArguments(args);
		showFragmentClearingBackStack(fragment);
	}

	private static <T> ArrayList<T> toArrayList(List<T> l){
		if(l instanceof ArrayList)
			return (ArrayList<T>) l;
		if(l==null)
			return null;
		return new ArrayList<>(l);
	}
}
