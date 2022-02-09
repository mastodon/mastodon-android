package org.joinmastodon.android.fragments;

import android.app.Fragment;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.ui.views.LinkedTextView;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class ProfileAboutFragment extends Fragment{
	public UsableRecyclerView list;
	private List<AccountField> fields=Collections.emptyList();
	private AboutAdapter adapter;
	private Paint dividerPaint=new Paint();

	public void setFields(List<AccountField> fields){
		this.fields=fields;
		if(adapter!=null)
			adapter.notifyDataSetChanged();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		list=new UsableRecyclerView(getActivity());
		list.setId(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		list.setAdapter(adapter=new AboutAdapter());
		int pad=V.dp(16);
		list.setPadding(pad, pad, pad, pad);
		list.setClipToPadding(false);
		dividerPaint.setStyle(Paint.Style.STROKE);
		dividerPaint.setStrokeWidth(V.dp(1));
		dividerPaint.setColor(getResources().getColor(R.color.gray_200));
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			@Override
			public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				for(int i=0;i<parent.getChildCount();i++){
					View item=parent.getChildAt(i);
					int pos=parent.getChildAdapterPosition(item);
					if(pos<fields.size()-1){
						c.drawLine(item.getLeft(), item.getBottom(), item.getRight(), item.getBottom(), dividerPaint);
					}
				}
			}
		});
		return list;
	}

	private class AboutAdapter extends UsableRecyclerView.Adapter<AboutViewHolder>{
		public AboutAdapter(){
			super(null);
		}

		@NonNull
		@Override
		public AboutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new AboutViewHolder();
		}

		@Override
		public void onBindViewHolder(AboutViewHolder holder, int position){
			holder.bind(fields.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
			return fields.size();
		}
	}

	private class AboutViewHolder extends BindableViewHolder<AccountField>{
		private TextView title;
		private LinkedTextView value;
		private ShapeDrawable background=new ShapeDrawable();

		public AboutViewHolder(){
			super(getActivity(), R.layout.item_profile_about, list);
			title=findViewById(R.id.title);
			value=findViewById(R.id.value);
			background.getPaint().setColor(getResources().getColor(R.color.gray_50));
			itemView.setBackground(background);
		}

		@Override
		public void onBind(AccountField item){
			title.setText(item.name);
			value.setText(item.parsedValue);
			boolean first=getAbsoluteAdapterPosition()==0, last=getAbsoluteAdapterPosition()==fields.size()-1;
			float radius=V.dp(10);
			float[] rad=new float[8];
			if(first)
				rad[0]=rad[1]=rad[2]=rad[3]=radius;
			if(last)
				rad[4]=rad[5]=rad[6]=rad[7]=radius;
			background.setShape(new RoundRectShape(rad, null, null));
			itemView.invalidateOutline();
		}
	}
}
