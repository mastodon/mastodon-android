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

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.MastodonRecyclerFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.sheets.M3BottomSheet;
import org.joinmastodon.android.ui.text.LinkSpan;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.LinkedTextView;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
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
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		adapter=new FieldsAdapter();

		View footer=getLayoutInflater().inflate(R.layout.footer_profile_edit_fields, list, false);
		Button addButton=footer.findViewById(R.id.add);
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
		addButton.setOnClickListener(v->{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), ProfileEditFieldFragment.class, args);
		});

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

	private void showLinkVerificationSheet(){
		BottomSheet sheet=new M3BottomSheet(getActivity());
		sheet.setContentView(getLayoutInflater().inflate(R.layout.sheet_link_verification_help, null));
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
			editBtn.setOnClickListener(v->{
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putString("label", item.name);
				args.putString("value", item.value);
				Nav.go(getActivity(), ProfileEditFieldFragment.class, args);
			});
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
		}

		@Override
		public boolean isLongPressDragEnabled(){
			return false;
		}
	}
}
