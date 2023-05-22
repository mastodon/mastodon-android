package org.joinmastodon.android.fragments.onboarding;

import android.app.ProgressDialog;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.accounts.GetFollowSuggestions;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.fragments.HomeFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.model.FollowSuggestion;
import org.joinmastodon.android.model.ParsedAccount;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.ProgressBarButton;
import org.joinmastodon.android.utils.ElevationOnScrollListener;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;
import me.grishka.appkit.views.UsableRecyclerView;

public class OnboardingFollowSuggestionsFragment extends BaseRecyclerFragment<ParsedAccount>{
	private String accountID;
	private Map<String, Relationship> relationships=Collections.emptyMap();
	private GetAccountRelationships relationshipsRequest;
	private View buttonBar;
	private ElevationOnScrollListener onScrollListener;
	private int numRunningFollowRequests=0;

	public OnboardingFollowSuggestionsFragment(){
		super(R.layout.fragment_onboarding_follow_suggestions, 40);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setTitle(R.string.popular_on_mastodon);
		accountID=getArguments().getString("account");
		loadData();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		buttonBar=view.findViewById(R.id.button_bar);
		list.addOnScrollListener(onScrollListener=new ElevationOnScrollListener((FragmentRootLinearLayout) view, buttonBar, getToolbar()));

		view.findViewById(R.id.btn_next).setOnClickListener(UiUtils.rateLimitedClickListener(this::onFollowAllClick));
		view.findViewById(R.id.btn_skip).setOnClickListener(UiUtils.rateLimitedClickListener(v->proceed()));
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		if(onScrollListener!=null){
			onScrollListener.setViews(buttonBar, getToolbar());
		}
	}

	@Override
	protected void doLoadData(int offset, int count){
		new GetFollowSuggestions(40)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<FollowSuggestion> result){
						onDataLoaded(result.stream().map(fs->new ParsedAccount(fs.account, accountID)).collect(Collectors.toList()), false);
						loadRelationships();
					}
				})
				.exec(accountID);
	}

	private void loadRelationships(){
		relationships=Collections.emptyMap();
		relationshipsRequest=new GetAccountRelationships(data.stream().map(fs->fs.account.id).collect(Collectors.toList()));
		relationshipsRequest.setCallback(new Callback<>(){
			@Override
			public void onSuccess(List<Relationship> result){
				relationshipsRequest=null;
				relationships=result.stream().collect(Collectors.toMap(rel->rel.id, Function.identity()));
				if(list==null)
					return;
				for(int i=0;i<list.getChildCount();i++){
					RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
					if(holder instanceof SuggestionViewHolder svh)
						svh.rebind();
				}
			}

			@Override
			public void onError(ErrorResponse error){
				relationshipsRequest=null;
			}
		}).exec(accountID);
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		super.onApplyWindowInsets(UiUtils.applyBottomInsetToFixedView(buttonBar, insets));
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		return new SuggestionsAdapter();
	}

	private void onFollowAllClick(View v){
		if(!loaded || relationships.isEmpty())
			return;
		if(data.isEmpty()){
			proceed();
			return;
		}
		ArrayList<String> accountIdsToFollow=new ArrayList<>();
		for(ParsedAccount acc:data){
			Relationship rel=relationships.get(acc.account.id);
			if(rel==null)
				continue;
			if(rel.canFollow())
				accountIdsToFollow.add(acc.account.id);
		}

		final ProgressDialog progress=new ProgressDialog(getActivity());
		progress.setIndeterminate(false);
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setMax(accountIdsToFollow.size());
		progress.setCancelable(false);
		progress.setMessage(getString(R.string.sending_follows));
		progress.show();

		for(int i=0;i<Math.min(accountIdsToFollow.size(), 5);i++){ // Send up to 5 requests in parallel
			followNextAccount(accountIdsToFollow, progress);
		}
	}

	private void followNextAccount(ArrayList<String> accountIdsToFollow, ProgressDialog progress){
		if(accountIdsToFollow.isEmpty()){
			if(numRunningFollowRequests==0){
				progress.dismiss();
				proceed();
			}
			return;
		}
		numRunningFollowRequests++;
		String id=accountIdsToFollow.remove(0);
		new SetAccountFollowed(id, true, true)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Relationship result){
						relationships.put(id, result);
						for(int i=0;i<list.getChildCount();i++){
							if(list.getChildViewHolder(list.getChildAt(i)) instanceof SuggestionViewHolder svh && svh.getItem().account.id.equals(id)){
								svh.rebind();
								break;
							}
						}
						numRunningFollowRequests--;
						progress.setProgress(progress.getMax()-accountIdsToFollow.size()-numRunningFollowRequests);
						followNextAccount(accountIdsToFollow, progress);
					}

					@Override
					public void onError(ErrorResponse error){
						numRunningFollowRequests--;
						progress.setProgress(progress.getMax()-accountIdsToFollow.size()-numRunningFollowRequests);
						followNextAccount(accountIdsToFollow, progress);
					}
				})
				.exec(accountID);
	}

	private void proceed(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), OnboardingProfileSetupFragment.class, args);
	}

	private class SuggestionsAdapter extends UsableRecyclerView.Adapter<SuggestionViewHolder> implements ImageLoaderRecyclerAdapter{

		public SuggestionsAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new SuggestionViewHolder();
		}

		@Override
		public int getItemCount(){
			return data.size();
		}

		@Override
		public void onBindViewHolder(SuggestionViewHolder holder, int position){
			holder.bind(data.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getImageCountForItem(int position){
			return data.get(position).emojiHelper.getImageCount()+1;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			ParsedAccount account=data.get(position);
			if(image==0)
				return account.avatarRequest;
			return account.emojiHelper.getImageRequest(image-1);
		}
	}

	private class SuggestionViewHolder extends BindableViewHolder<ParsedAccount> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable{
		private final TextView name, username, bio;
		private final ImageView avatar;
		private final ProgressBarButton actionButton;
		private final ProgressBar actionProgress;
		private final View actionWrap;

		private Relationship relationship;

		public SuggestionViewHolder(){
			super(getActivity(), R.layout.item_user_row_m3, list);
			name=findViewById(R.id.name);
			username=findViewById(R.id.username);
			bio=findViewById(R.id.bio);
			avatar=findViewById(R.id.avatar);
			actionButton=findViewById(R.id.action_btn);
			actionProgress=findViewById(R.id.action_progress);
			actionWrap=findViewById(R.id.action_btn_wrap);

			avatar.setOutlineProvider(OutlineProviders.roundedRect(10));
			avatar.setClipToOutline(true);
			actionButton.setOnClickListener(UiUtils.rateLimitedClickListener(this::onActionButtonClick));
		}

		@Override
		public void onBind(ParsedAccount item){
			name.setText(item.parsedName);
			username.setText(item.account.getDisplayUsername());
			if(TextUtils.isEmpty(item.parsedBio)){
				bio.setVisibility(View.GONE);
			}else{
				bio.setVisibility(View.VISIBLE);
				bio.setText(item.parsedBio);
			}

			relationship=relationships.get(item.account.id);
			if(relationship==null){
				actionWrap.setVisibility(View.GONE);
			}else{
				actionWrap.setVisibility(View.VISIBLE);
				UiUtils.setRelationshipToActionButtonM3(relationship, actionButton);
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			if(index==0){
				avatar.setImageDrawable(image);
			}else{
				item.emojiHelper.setImageDrawable(index-1, image);
				name.invalidate();
				bio.invalidate();
			}
			if(image instanceof Animatable a && !a.isRunning())
				a.start();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}

		@Override
		public void onClick(){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("profileAccount", Parcels.wrap(item.account));
			Nav.go(getActivity(), ProfileFragment.class, args);
		}

		private void onActionButtonClick(View v){
			itemView.setHasTransientState(true);
			UiUtils.performAccountAction(getActivity(), item.account, accountID, relationship, actionButton, this::setActionProgressVisible, rel->{
				itemView.setHasTransientState(false);
				relationships.put(item.account.id, rel);
				rebind();
			});
		}

		private void setActionProgressVisible(boolean visible){
			if(visible)
				actionProgress.setIndeterminateTintList(actionButton.getTextColors());
			actionButton.setTextVisible(!visible);
			actionProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
			actionButton.setClickable(!visible);
		}
	}
}
