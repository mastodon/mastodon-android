package org.joinmastodon.android.fragments;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.notifications.GetNotificationRequests;
import org.joinmastodon.android.api.requests.notifications.RespondToNotificationRequest;
import org.joinmastodon.android.events.NotificationRequestRespondedEvent;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.NotificationRequest;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.Snackbar;
import org.parceler.Parcels;

import java.util.HashMap;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class NotificationRequestsFragment extends MastodonRecyclerFragment<NotificationRequest>{
	private String accountID;
	private String maxID;
	private HashMap<String, AccountViewModel> accountViewModels=new HashMap<>();
	private View endMark;
	private NotificationRequestsAdapter adapter;

	public NotificationRequestsFragment(){
		super(50);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		setTitle(R.string.filtered_notifications);
		loadData();
		E.register(this);
	}

	@Override
	public void onDestroy(){
		E.unregister(this);
		super.onDestroy();
	}

	@Override
	protected void doLoadData(int offset, int count){
		if(!refreshing && endMark!=null)
			endMark.setVisibility(View.GONE);
		currentRequest=new GetNotificationRequests(offset==0 ? null : maxID)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(HeaderPaginationList<NotificationRequest> result){
						if(data.isEmpty() || refreshing)
							accountViewModels.clear();
						maxID=result.getNextPageMaxID();
						for(NotificationRequest req:result){
							accountViewModels.put(req.account.id, new AccountViewModel(req.account, accountID, false, getActivity()));
						}
						onDataLoaded(result, !TextUtils.isEmpty(maxID));
						endMark.setVisibility(TextUtils.isEmpty(maxID) ? View.VISIBLE : View.GONE);
					}
				})
				.exec(accountID);
	}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		return adapter=new NotificationRequestsAdapter();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.setItemAnimator(new BetterItemAnimator());
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorM3OutlineVariant, 1, 0, 0, vh->vh instanceof NotificationRequestViewHolder).setDrawBelowLastItem(true));
	}

	@Override
	protected View onCreateFooterView(LayoutInflater inflater){
		View v=inflater.inflate(R.layout.load_more_with_end_mark, null);
		endMark=v.findViewById(R.id.end_mark);
		endMark.setVisibility(View.GONE);
		return v;
	}

	@Subscribe
	public void onNotificationRequestResponded(NotificationRequestRespondedEvent ev){
		if(adapter==null || !ev.accountID.equals(accountID))
			return;
		for(int i=0;i<data.size();i++){
			if(data.get(i).id.equals(ev.requestID)){
				data.remove(i);
				adapter.notifyItemRemoved(i);
				return;
			}
		}
		for(NotificationRequest nr:preloadedData){
			if(nr.id.equals(ev.requestID)){
				preloadedData.remove(nr);
				break;
			}
		}
	}

	private class NotificationRequestsAdapter extends UsableRecyclerView.Adapter<NotificationRequestViewHolder> implements ImageLoaderRecyclerAdapter{

		public NotificationRequestsAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public NotificationRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new NotificationRequestViewHolder();
		}

		@Override
		public int getItemCount(){
			return data.size();
		}

		@Override
		public void onBindViewHolder(NotificationRequestViewHolder holder, int position){
			holder.bind(data.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getImageCountForItem(int position){
			return Objects.requireNonNull(accountViewModels.get(data.get(position).account.id)).emojiHelper.getImageCount()+1;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			AccountViewModel model=Objects.requireNonNull(accountViewModels.get(data.get(position).account.id));
			return switch(image){
				case 0 -> model.avaRequest;
				default -> model.emojiHelper.getImageRequest(image-1);
			};
		}
	}

	private class NotificationRequestViewHolder extends BindableViewHolder<NotificationRequest> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable{
		private final TextView name, username, badge;
		private final ImageView ava;
		private final ImageButton allow, mute;

		public NotificationRequestViewHolder(){
			super(getActivity(), R.layout.item_notification_request, list);
			name=findViewById(R.id.name);
			username=findViewById(R.id.username);
			badge=findViewById(R.id.badge);
			ava=findViewById(R.id.ava);
			allow=findViewById(R.id.btn_allow);
			mute=findViewById(R.id.btn_mute);
			ava.setOutlineProvider(OutlineProviders.roundedRect(8));
			ava.setClipToOutline(true);
			allow.setOnClickListener(this::onAllowClick);
			mute.setOnClickListener(this::onMuteClick);
		}

		@SuppressLint("DefaultLocale")
		@Override
		public void onBind(NotificationRequest item){
			AccountViewModel model=Objects.requireNonNull(accountViewModels.get(item.account.id));
			name.setText(model.parsedName);
			username.setText(item.account.getDisplayUsername());
			badge.setText(item.notificationsCount>99 ? String.format("%d+", 99) : String.format("%d", item.notificationsCount));
		}

		@Override
		public void setImage(int index, Drawable image){
			if(index==0){
				if(image==null)
					 ava.setImageResource(R.drawable.image_placeholder);
				else
					 ava.setImageDrawable(image);
			}else{
				AccountViewModel model=Objects.requireNonNull(accountViewModels.get(item.account.id));
				model.emojiHelper.setImageDrawable(index-1, image);
				name.invalidate();
			}
		}

		@Override
		public void onClick(){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("targetAccount", Parcels.wrap(item.account));
			args.putString("requestID", item.id);
			Nav.go(getActivity(), AccountNotificationsListFragment.class, args);
		}

		private void onAllowClick(View v){
			acceptOrDecline(true);
		}

		private void onMuteClick(View v){
			acceptOrDecline(false);
		}

		private void acceptOrDecline(boolean accept){
			new RespondToNotificationRequest(item.id, accept)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Void result){
							int pos=data.indexOf(item);
							data.remove(pos);
							adapter.notifyItemRemoved(pos);
							new Snackbar.Builder(getActivity())
									.setText(getString(accept ? R.string.notifications_allowed : R.string.notifications_muted, item.account.displayName))
									.show();
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(getActivity());
						}
					})
					.wrapProgress(getActivity(), R.string.loading, false)
					.exec(accountID);
		}
	}
}
