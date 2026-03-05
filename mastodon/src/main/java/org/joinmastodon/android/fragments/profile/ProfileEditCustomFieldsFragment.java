package org.joinmastodon.android.fragments.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentials;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.SelfAccountUpdatedEvent;
import org.joinmastodon.android.fragments.MastodonRecyclerFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.Snackbar;
import org.joinmastodon.android.ui.sheets.M3BottomSheet;
import org.joinmastodon.android.ui.text.LinkSpan;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.LinkedTextView;
import org.parceler.Parcels;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;

public class ProfileEditCustomFieldsFragment extends MastodonRecyclerFragment<AccountField>{
	private FieldsAdapter adapter;
	private String accountID;
	private ItemTouchHelper dragHelper=new ItemTouchHelper(new ReorderCallback());
	private Button addButton;
	private boolean needReorder;
	private boolean ignoreUpdatedEvent;

	public ProfileEditCustomFieldsFragment(){
		super(1);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRefreshEnabled(false);
		setTitle(R.string.edit_profile_custom_fields);
		accountID=getArguments().getString("account");
		ArrayList<AccountField> fields=new ArrayList<>();
		for(Parcelable p:Objects.requireNonNull(getArguments().getParcelableArrayList("fields"))){
			fields.add(Parcels.unwrap(p));
		}
		onDataLoaded(fields);
		E.register(this);
	}

	@Override
	public void onDestroy(){
		E.unregister(this);
		super.onDestroy();
	}

	@Subscribe
	public void onAccountUpdated(SelfAccountUpdatedEvent ev){
		if(ev.accountID().equals(accountID) && !ignoreUpdatedEvent){
			data.clear();
			data.addAll(ev.account().fields);
			adapter.notifyDataSetChanged();
			updateAddButtonVisibility();
		}
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		adapter=new FieldsAdapter();

		View footer=getActivity().getLayoutInflater().inflate(R.layout.footer_profile_edit_fields, list, false);
		addButton=footer.findViewById(R.id.add);
		LinkedTextView helpText=footer.findViewById(R.id.help_text);
		SpannableString text=new SpannableString(getResources().getText(R.string.edit_profile_custom_fields_help));
		StyleSpan[] spans=text.getSpans(0, text.length(), StyleSpan.class);
		if(spans.length>0){
			int start=text.getSpanStart(spans[0]);
			int end=text.getSpanEnd(spans[0]);
			text.removeSpan(spans[0]);
			LinkSpan link=new LinkSpan("", span->showLinkVerificationSheet(), LinkSpan.Type.CUSTOM, accountID, null, null);
			text.setSpan(link, start, end, 0);
		}
		helpText.setText(text);
		addButton.setOnClickListener(v->startFieldEditFragment(-1));
		updateAddButtonVisibility();

		MergeRecyclerAdapter mergeAdapter=new MergeRecyclerAdapter();
		mergeAdapter.addAdapter(adapter);
		mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(footer));
		return mergeAdapter;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		dragHelper.attachToRecyclerView(list);
	}

	private void updateAddButtonVisibility(){
		addButton.setVisibility(data.size()<4 ? View.VISIBLE : View.GONE);
	}

	private void showLinkVerificationSheet(){
		BottomSheet sheet=new M3BottomSheet(getActivity());
		sheet.setContentView(getActivity().getLayoutInflater().inflate(R.layout.sheet_link_verification_help, null));
		int i=1;
		for(int id:new int[]{R.id.number1, R.id.number2, R.id.number3}){
			TextView v=sheet.findViewById(id);
			v.setText(String.format("%d", i));
			i++;
		}
		TextView codeView=sheet.findViewById(R.id.code);
		codeView.setOutlineProvider(OutlineProviders.roundedRect(8));
		Account self=AccountSessionManager.get(accountID).self;
		String code=String.format("<a rel=\"me\" href=\"%s\">Mastodon</a>", self.url);
		codeView.setText(code);
		sheet.findViewById(R.id.copy_btn).setOnClickListener(v->{
			getActivity().getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, code));
			UiUtils.maybeShowTextCopiedToast(getActivity());
		});
		sheet.show();
	}

	private void startFieldEditFragment(int editIndex){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelableArrayList("fields", (ArrayList<? extends Parcelable>) data.stream().map(Parcels::wrap).collect(Collectors.toCollection(ArrayList::new)));
		if(editIndex!=-1)
			args.putInt("fieldIndex", editIndex);
		Nav.go(getActivity(), ProfileEditFieldFragment.class, args);
	}

	private void saveReorderedFields(){
		new UpdateAccountCredentials(null, null, (File)null, null, data)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						ignoreUpdatedEvent=true;
						AccountSessionManager.getInstance().updateAccountInfo(accountID, result);
						ignoreUpdatedEvent=false;
					}

					@Override
					public void onError(ErrorResponse error){
						if(getActivity()==null || !(error instanceof MastodonErrorResponse me))
							return;
						Snackbar[] sb={null};
						sb[0]=new Snackbar.Builder(getActivity())
								.setText(me.getErrorMessage())
								.setAction(R.string.retry, ()->{
									saveReorderedFields();
									sb[0].dismiss();
								})
								.setPersistent()
								.create();
						sb[0].show();
					}
				})
				.exec(accountID);
	}

	private class FieldsAdapter extends RecyclerView.Adapter<FieldViewHolder>{
		@NonNull
		@Override
		public FieldViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new FieldViewHolder();
		}

		@Override
		public void onBindViewHolder(@NonNull FieldViewHolder holder, int position){
			holder.bind(data.get(position));
		}

		@Override
		public int getItemCount(){
			return data.size();
		}
	}

	private class FieldViewHolder extends BindableViewHolder<AccountField>{
		private final TextView title, value;
		private final ImageView dragger, editBtn;

		public FieldViewHolder(){
			super(getActivity(), R.layout.item_profile_edit_field, list);
			title=findViewById(R.id.title);
			value=findViewById(R.id.value);
			dragger=findViewById(R.id.dragger_thingy);
			editBtn=findViewById(R.id.edit);
			dragger.setOnLongClickListener(v->{
				dragHelper.startDrag(this);
				return true;
			});
			editBtn.setOnClickListener(v->startFieldEditFragment(getLayoutPosition()));
		}

		@Override
		public void onBind(AccountField item){
			title.setText(item.name);
			value.setText(item.value);
		}
	}

	private class ReorderCallback extends ItemTouchHelper.SimpleCallback{
		public ReorderCallback(){
			super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
		}

		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target){
			if(!(target instanceof FieldViewHolder))
				return false;
			int fromPosition=viewHolder.getAbsoluteAdapterPosition();
			int toPosition=target.getAbsoluteAdapterPosition();
			if(fromPosition<toPosition){
				for(int i=fromPosition;i<toPosition;i++){
					Collections.swap(data, i, i+1);
				}
			}else{
				for(int i=fromPosition;i>toPosition;i--){
					Collections.swap(data, i, i-1);
				}
			}
			adapter.notifyItemMoved(fromPosition, toPosition);
			((BindableViewHolder<?>) viewHolder).rebind();
			((BindableViewHolder<?>) target).rebind();
			needReorder=true;
			return true;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction){}

		@Override
		public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState){
			super.onSelectedChanged(viewHolder, actionState);
			if(actionState==ItemTouchHelper.ACTION_STATE_DRAG){
				viewHolder.itemView.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Surface));
				viewHolder.itemView.setTag(me.grishka.appkit.R.id.item_touch_helper_previous_elevation, viewHolder.itemView.getElevation()); // prevents the default behavior of changing elevation in onDraw()
				viewHolder.itemView.animate().translationZ(V.dp(1)).setDuration(200).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
			}
		}

		@Override
		public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder){
			super.clearView(recyclerView, viewHolder);
			viewHolder.itemView.setHasTransientState(true);
			viewHolder.itemView.animate().translationZ(0).setDuration(100).setInterpolator(CubicBezierInterpolator.DEFAULT).withEndAction(()->{
				viewHolder.itemView.setHasTransientState(false);
				viewHolder.itemView.setBackground(null);
			}).start();
			if(needReorder){
				needReorder=false;
				saveReorderedFields();
			}
		}

		@Override
		public boolean isLongPressDragEnabled(){
			return false;
		}
	}
}
