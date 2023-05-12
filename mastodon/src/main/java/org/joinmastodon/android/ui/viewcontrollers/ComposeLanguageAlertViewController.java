package org.joinmastodon.android.ui.viewcontrollers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextLanguage;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CheckableLinearLayout;
import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class ComposeLanguageAlertViewController{
	private Context context;
	private UsableRecyclerView list;
	private List<LocaleInfo> allLocales;
	private List<SpecialLocaleInfo> specialLocales=new ArrayList<>();
	private int selectedIndex=0;
	private Locale selectedLocale;

	public ComposeLanguageAlertViewController(Context context, String preferred, SelectedOption previouslySelected, String postText){
		this.context=context;

		allLocales=Arrays.stream(Locale.getAvailableLocales())
				.map(Locale::getLanguage)
				.distinct()
				.map(code->{
					Locale l=Locale.forLanguageTag(code);
					String name=l.getDisplayLanguage(Locale.getDefault());
					return new LocaleInfo(l, capitalizeLanguageName(name));
				})
				.sorted(Comparator.comparing(a->a.displayName))
				.collect(Collectors.toList());

		if(!TextUtils.isEmpty(preferred)){
			Locale l=Locale.forLanguageTag(preferred);
			SpecialLocaleInfo pref=new SpecialLocaleInfo();
			pref.locale=l;
			pref.displayName=capitalizeLanguageName(l.getDisplayLanguage(Locale.getDefault()));
			pref.title=context.getString(R.string.language_default);
			specialLocales.add(pref);
		}

		Locale def=Locale.forLanguageTag(Locale.getDefault().getLanguage());
		if(!def.getLanguage().equals(preferred)){
			SpecialLocaleInfo d=new SpecialLocaleInfo();
			d.locale=def;
			d.displayName=capitalizeLanguageName(def.getDisplayName());
			d.title=context.getString(R.string.language_system);
			specialLocales.add(d);
		}

		if(Build.VERSION.SDK_INT>=29 && !TextUtils.isEmpty(postText)){
			SpecialLocaleInfo detected=new SpecialLocaleInfo();
			detected.displayName=context.getString(R.string.language_detecting);
			detected.enabled=false;
			specialLocales.add(detected);
			detectLanguage(detected, postText);
		}

		if(previouslySelected!=null){
			if((previouslySelected.index<specialLocales.size() && Objects.equals(previouslySelected.locale, specialLocales.get(previouslySelected.index).locale)) ||
					(previouslySelected.index<specialLocales.size()+allLocales.size() && Objects.equals(previouslySelected.locale, allLocales.get(previouslySelected.index-specialLocales.size()).locale))){
				selectedIndex=previouslySelected.index;
				selectedLocale=previouslySelected.locale;
			}
		}else{
			selectedLocale=specialLocales.get(0).locale;
		}

		list=new UsableRecyclerView(context);
		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(new SpecialLanguagesAdapter());
		adapter.addAdapter(new AllLocalesAdapter());
		list.setAdapter(adapter);
		list.setLayoutManager(new LinearLayoutManager(context));

		list.addItemDecoration(new DividerItemDecoration(context, R.attr.colorM3OutlineVariant, 1, 16, 16, vh->vh.getAbsoluteAdapterPosition()==specialLocales.size()-1));
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			private Paint paint=new Paint();

			{
				paint.setColor(UiUtils.getThemeColor(context, R.attr.colorM3OutlineVariant));
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(V.dp(1));
			}

			@Override
			public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				if(parent.canScrollVertically(1)){
					float y=parent.getHeight()-paint.getStrokeWidth()/2f;
					c.drawLine(0, y, parent.getWidth(), y, paint);
				}
				if(parent.canScrollVertically(-1)){
					float y=paint.getStrokeWidth()/2f;
					c.drawLine(0, y, parent.getWidth(), y, paint);
				}
			}
		});

		if(previouslySelected!=null && selectedIndex>0){
			list.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
				@Override
				public boolean onPreDraw(){
					list.getViewTreeObserver().removeOnPreDrawListener(this);

					if(list.findViewHolderForAdapterPosition(selectedIndex)==null)
						list.scrollToPosition(selectedIndex);

					return true;
				}
			});
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.Q)
	private void detectLanguage(SpecialLocaleInfo info, String text){
		MastodonAPIController.runInBackground(()->{
			TextLanguage lang=context.getSystemService(TextClassificationManager.class).getTextClassifier().detectLanguage(new TextLanguage.Request.Builder(text).build());
			list.post(()->{
				SpecialLanguageViewHolder holder=(SpecialLanguageViewHolder) list.findViewHolderForAdapterPosition(specialLocales.indexOf(info));
				if(lang.getLocaleHypothesisCount()==0 || lang.getConfidenceScore(lang.getLocale(0))<0.75f){
					info.displayName=context.getString(R.string.language_cant_detect);
				}else{
					Locale locale=lang.getLocale(0).toLocale();
					info.locale=locale;
					info.displayName=capitalizeLanguageName(locale.getDisplayName(Locale.getDefault()));
					info.title=context.getString(R.string.language_detected);
					info.enabled=true;
					if(holder!=null)
						UiUtils.beginLayoutTransition(holder.view);
				}
				if(holder!=null)
					holder.rebind();
			});
		});
	}

	public View getView(){
		return list;
	}

	// Needed because in some languages (e.g. Slavic ones) these names returned by the system start with a lowercase letter
	private String capitalizeLanguageName(String name){
		return name.substring(0, 1).toUpperCase(Locale.getDefault())+name.substring(1);
	}

	public SelectedOption getSelectedOption(){
		return new SelectedOption(selectedIndex, selectedLocale);
	}

	private void selectItem(int index){
		if(index==selectedIndex)
			return;
		if(selectedIndex!=-1){
			RecyclerView.ViewHolder holder=list.findViewHolderForAdapterPosition(selectedIndex);
			if(holder!=null && holder.itemView instanceof Checkable checkable)
				checkable.setChecked(false);
		}
		RecyclerView.ViewHolder holder=list.findViewHolderForAdapterPosition(index);
		if(holder!=null && holder.itemView instanceof Checkable checkable)
			checkable.setChecked(true);
		selectedIndex=index;
	}

	private class AllLocalesAdapter extends RecyclerView.Adapter<SimpleLanguageViewHolder>{

		@NonNull
		@Override
		public SimpleLanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new SimpleLanguageViewHolder();
		}

		@Override
		public void onBindViewHolder(@NonNull SimpleLanguageViewHolder holder, int position){
			holder.bind(allLocales.get(position));
		}

		@Override
		public int getItemCount(){
			return allLocales.size();
		}

		@Override
		public int getItemViewType(int position){
			return 1;
		}
	}

	private class SimpleLanguageViewHolder extends BindableViewHolder<LocaleInfo> implements UsableRecyclerView.Clickable{
		private final CheckedTextView text;

		public SimpleLanguageViewHolder(){
			super(context, R.layout.item_alert_single_choice_1line, list);
			text=(CheckedTextView) itemView;
			text.setCompoundDrawablesRelativeWithIntrinsicBounds(new RadioButton(context).getButtonDrawable(), null, null, null);
		}

		@Override
		public void onBind(LocaleInfo item){
			text.setText(item.displayName);
			text.setChecked(selectedIndex==getAbsoluteAdapterPosition());
		}

		@Override
		public void onClick(){
			selectItem(getAbsoluteAdapterPosition());
			selectedLocale=item.locale;
		}
	}

	private class SpecialLanguagesAdapter extends RecyclerView.Adapter<SpecialLanguageViewHolder>{

		@NonNull
		@Override
		public SpecialLanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new SpecialLanguageViewHolder();
		}

		@Override
		public void onBindViewHolder(@NonNull SpecialLanguageViewHolder holder, int position){
			holder.bind(specialLocales.get(position));
		}

		@Override
		public int getItemCount(){
			return specialLocales.size();
		}

		@Override
		public int getItemViewType(int position){
			return 2;
		}
	}

	private class SpecialLanguageViewHolder extends BindableViewHolder<SpecialLocaleInfo> implements UsableRecyclerView.DisableableClickable{
		private final TextView text, title;
		private final CheckableLinearLayout view;

		public SpecialLanguageViewHolder(){
			super(context, R.layout.item_alert_single_choice_2lines, list);
			text=findViewById(R.id.text);
			title=findViewById(R.id.title);
			view=((CheckableLinearLayout) itemView);
			findViewById(R.id.radiobutton).setBackground(new RadioButton(context).getButtonDrawable());
		}

		@Override
		public void onBind(SpecialLocaleInfo item){
			text.setText(item.displayName);
			if(!TextUtils.isEmpty(item.title)){
				title.setVisibility(View.VISIBLE);
				title.setText(item.title);
			}else{
				title.setVisibility(View.GONE);
			}
			text.setEnabled(item.enabled);
			view.setEnabled(item.enabled);
			view.setChecked(selectedIndex==getAbsoluteAdapterPosition());
		}

		@Override
		public void onClick(){
			selectItem(getAbsoluteAdapterPosition());
			selectedLocale=item.locale;
		}

		@Override
		public boolean isEnabled(){
			return item.enabled;
		}
	}

	private static class LocaleInfo{
		public final Locale locale;
		public final String displayName;

		private LocaleInfo(Locale locale, String displayName){
			this.locale=locale;
			this.displayName=displayName;
		}
	}

	private static class SpecialLocaleInfo{
		public Locale locale;
		public String displayName;
		public String title;
		public boolean enabled=true;
	}

	@Parcel
	public static class SelectedOption{
		public int index;
		public Locale locale;

		public SelectedOption(){}

		public SelectedOption(int index, Locale locale){
			this.index=index;
			this.locale=locale;
		}
	}
}
