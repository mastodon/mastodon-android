package org.joinmastodon.android.fragments.profile;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.joinmastodon.android.ui.utils.HideableSingleViewRecyclerAdapter;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.LinkedTextView;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import me.grishka.appkit.fragments.WindowInsetsAwareFragment;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class ProfileAboutFragment extends Fragment implements WindowInsetsAwareFragment{
	public UsableRecyclerView list;
	public LinkedTextView bio;
	private List<AccountField> fields=Collections.emptyList();
	private AboutAdapter adapter;
	private ListImageLoaderWrapper imgLoader;
	private HideableSingleViewRecyclerAdapter bioAdapter;
	private MergeRecyclerAdapter mergeAdapter=new MergeRecyclerAdapter();
	private CharSequence parsedBio;

	public void setFields(List<AccountField> fields){
		this.fields=fields;
		if(adapter!=null)
			adapter.notifyDataSetChanged();
	}

	public void setBio(CharSequence parsedBio){
		this.parsedBio=parsedBio;
		if(list!=null)
			updateBio();
	}

	private void updateBio(){
		if(TextUtils.isEmpty(parsedBio)){
			bioAdapter.setVisible(false);
		}else{
			bioAdapter.setVisible(true);
			bio.setText(parsedBio);
		}
		UiUtils.loadCustomEmojiInTextView(bio);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		list=new UsableRecyclerView(getActivity());
		list.setId(R.id.list);
		list.setItemAnimator(new BetterItemAnimator());
		list.setDrawSelectorOnTop(true);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorM3Outline, 0.5f, 0, 0));
		imgLoader=new ListImageLoaderWrapper(getActivity(), list, list, null);

		adapter=new AboutAdapter();
		bio=new LinkedTextView(getActivity());
		bio.setTextSize(15);
		bio.setLineSpacing(0, 1.4f);
		bio.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurface));
		bio.setPadding(0, 0, 0, V.dp(4));
		bio.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		bioAdapter=new HideableSingleViewRecyclerAdapter(bio);
		mergeAdapter.addAdapter(bioAdapter);
		mergeAdapter.addAdapter(adapter);
		list.setAdapter(mergeAdapter);

		UiUtils.setAllPaddings(list, 16);
		list.setClipToPadding(false);

		updateBio();

		return list;
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
			list.setPadding(0, V.dp(16), 0, V.dp(12)+insets.getSystemWindowInsetBottom());
		}
	}

	@Override
	public boolean wantsLightStatusBar(){
		return false;
	}

	@Override
	public boolean wantsLightNavigationBar(){
		return false;
	}

	private class AboutAdapter extends UsableRecyclerView.Adapter<AboutViewHolder> implements ImageLoaderRecyclerAdapter{
		public AboutAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public AboutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return switch(viewType){
				case 0 -> new AboutViewHolder();
				default -> throw new IllegalStateException("Unexpected value: "+viewType);
			};
		}

		@Override
		public void onBindViewHolder(AboutViewHolder holder, int position){
			if(position<fields.size()){
				holder.bind(fields.get(position));
			}else{
				holder.bind(null);
			}
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			return fields.size();
		}

		@Override
		public int getItemViewType(int position){
			return 0;
		}

		@Override
		public int getImageCountForItem(int position){
			return fields.get(position).emojiRequests==null ? 0 : fields.get(position).emojiRequests.size();
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return fields.get(position).emojiRequests.get(image);
		}
	}

	private class AboutViewHolder extends BindableViewHolder<AccountField> implements ImageLoaderViewHolder{
		private final TextView title;
		private final LinkedTextView value;
		private final ImageView verifiedIcon;

		public AboutViewHolder(){
			super(getActivity(), R.layout.item_profile_about, list);
			title=findViewById(R.id.title);
			value=findViewById(R.id.value);
			verifiedIcon=findViewById(R.id.verified_icon);
		}

		@Override
		public void onBind(AccountField item){
			title.setText(item.parsedName);
			value.setText(item.parsedValue);
			verifiedIcon.setVisibility(item.verifiedAt!=null ? View.VISIBLE : View.INVISIBLE);
			itemView.setBackgroundColor(item.verifiedAt!=null ? (UiUtils.isDarkTheme() ? 0xff032E15 : 0xffF0FDF4) : 0);
		}

		@Override
		public void setImage(int index, Drawable image){
			CustomEmojiSpan span=index>=item.nameEmojis.length ? item.valueEmojis[index-item.nameEmojis.length] : item.nameEmojis[index];
			span.setDrawable(image);
			title.invalidate();
			value.invalidate();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}
	}
}
