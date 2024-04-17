package org.joinmastodon.android.ui.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.style.ReplacementSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.grishka.appkit.utils.CustomViewHelper;

public class CurrencyAmountInput extends LinearLayout implements CustomViewHelper{
	private ActualEditText edit;
	private Button currencyBtn;
	private List<CurrencyInfo> currencies;
	private CurrencyInfo currentCurrency;
	private boolean spanAdded;
	private CurrencySymbolSpan symbolSpan;
	private boolean symbolBeforeAmount;
	private ChangeListener changeListener;
	private long lastAmount=0;
	private NumberFormat numberFormat=NumberFormat.getNumberInstance();
	private boolean allowSymbolToBeDeleted;

	public CurrencyAmountInput(Context context){
		this(context, null);
	}

	public CurrencyAmountInput(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}

	public CurrencyAmountInput(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		setForeground(getResources().getDrawable(R.drawable.fg_currency_input, context.getTheme()));
		setAddStatesFromChildren(true);

		if(!isInEditMode())
			setOutlineProvider(OutlineProviders.roundedRect(8));
		setClipToOutline(true);

		currencyBtn=new Button(context);
		currencyBtn.setTextAppearance(R.style.m3_label_large);
		currencyBtn.setSingleLine();
		currencyBtn.setTextColor(UiUtils.getThemeColor(context, R.attr.colorM3OnSurfaceVariant));
		int pad=dp(12);
		currencyBtn.setPadding(pad, 0, pad, 0);
		currencyBtn.setBackgroundColor(UiUtils.getThemeColor(context, R.attr.colorM3SurfaceVariant));
		currencyBtn.setMinimumWidth(0);
		currencyBtn.setMinWidth(0);
		currencyBtn.setOnClickListener(v->showCurrencySelector());
		currencyBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_unfold_more_wght600_15pt_8x20px, 0, 0, 0);
		currencyBtn.setCompoundDrawableTintList(currencyBtn.getTextColors());
		currencyBtn.setCompoundDrawablePadding(dp(4));
		addView(currencyBtn, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

		edit=new ActualEditText(context);
		edit.setBackgroundColor(UiUtils.getThemeColor(context, R.attr.colorM3Surface));
		pad=dp(16);
		edit.setPadding(pad, 0, pad, 0);
		edit.setSingleLine();
		edit.setTextAppearance(R.style.m3_title_large);
		edit.setTextColor(UiUtils.getThemeColor(context, R.attr.colorM3OnSurface));
		edit.setGravity(Gravity.END |Gravity.CENTER_VERTICAL);
		edit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		InputFilter[] filters=edit.getText().getFilters();
		for(int i=0;i<filters.length;i++){
			if(filters[i] instanceof DigitsKeyListener){
				filters[i]=new FormattingFriendlyDigitsKeyListener();
				edit.getText().setFilters(filters);
				break;
			}
		}
		addView(edit, new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
		symbolSpan=new CurrencySymbolSpan(edit.getPaint());

		NumberFormat format=NumberFormat.getInstance();
		String one=format.format(1);
		format=NumberFormat.getCurrencyInstance();
		format.setCurrency(Currency.getInstance("USD"));
		symbolBeforeAmount=format.format(1).indexOf(one)>0;

		edit.addTextChangedListener(new TextWatcher(){
			private boolean ignore;

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){

			}

			@Override
			public void afterTextChanged(Editable e){
				if(ignore)
					return;
				ignore=true;
				if(e.length()>0 && !spanAdded){
					SpannableString ss=new SpannableString(" ");
					ss.setSpan(symbolSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					if(symbolBeforeAmount)
						e.insert(0, ss);
					else
						e.append(ss);
					spanAdded=true;
				}else if(spanAdded && e.length()<=1){
					spanAdded=false;
					if(e.length()>0){
						allowSymbolToBeDeleted=true;
						e.clear();
						allowSymbolToBeDeleted=false;
					}
				}
				ignore=false;

				updateAmount();
			}
		});
	}

	public void setCurrencies(List<String> currencies){
		this.currencies=currencies.stream().map(CurrencyInfo::new).collect(Collectors.toList());
	}

	public void setSelectedCurrency(String code){
		CurrencyInfo info=null;
		for(CurrencyInfo c:currencies){
			if(c.code.equals(code)){
				info=c;
				break;
			}
		}
		if(info==null)
			throw new IllegalArgumentException();
		setCurrency(info);
	}

	private void setCurrency(CurrencyInfo info){
		currencyBtn.setText(info.code);
		currentCurrency=info;
		edit.invalidate();
		if(changeListener!=null)
			changeListener.onCurrencyChanged(info.code);
	}

	private void showCurrencySelector(){
		ArrayAdapter<CurrencyInfo> adapter=new ArrayAdapter<>(getContext(), R.layout.item_alert_single_choice_2lines_but_different, R.id.text, currencies){
			@Override
			public boolean hasStableIds(){
				return true;
			}

			@NonNull
			@Override
			public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
				View view=super.getView(position, convertView, parent);
				TextView subtitle=view.findViewById(R.id.subtitle);
				CurrencyInfo item=getItem(position);
				if(item.jCurrency==null || item.jCurrency.getDisplayName().equals(item.code)){
					subtitle.setVisibility(View.GONE);
				}else{
					subtitle.setVisibility(View.VISIBLE);
					subtitle.setText(item.jCurrency.getDisplayName());
				}
				return view;
			}
		};
		new M3AlertDialogBuilder(getContext())
				.setTitle(R.string.currency)
				.setSingleChoiceItems(adapter, currencies.indexOf(currentCurrency), (dlg, item)->{
					setCurrency(currencies.get(item));
					dlg.dismiss();
				})
				.show();
	}

	public void setChangeListener(ChangeListener changeListener){
		this.changeListener=changeListener;
	}

	private void updateAmount(){
		long newAmount;
		try{
			Number n=numberFormat.parse(edit.getText().toString().trim());
			if(n instanceof Long l){
				newAmount=l*100L;
			}else if(n instanceof Double d){
				newAmount=(long)(d*100);
			}else{
				newAmount=0;
			}
		}catch(ParseException x){
			newAmount=0;
		}
		if(newAmount!=lastAmount){
			lastAmount=newAmount;
			if(changeListener!=null)
				changeListener.onAmountChanged(lastAmount);
		}
	}

	public long getAmount(){
		return lastAmount;
	}

	public String getCurrency(){
		return currentCurrency.code;
	}

	@SuppressLint("DefaultLocale")
	public void setAmount(long amount){
		String value;
		if(amount%100==0)
			value=String.valueOf(amount/100);
		else
			value=String.format("%.2f", amount/100.0);
		int start=spanAdded ? 1 : 0;
		edit.getText().replace(start, edit.length(), value);
	}

	private class ActualEditText extends EditText{
		public ActualEditText(Context context){
			super(context);
			setClipToPadding(false);
		}

		@Override
		protected void onSelectionChanged(int selStart, int selEnd){
			super.onSelectionChanged(selStart, selEnd);
			// Adjust the selection to prevent the symbol span being selected
			if(spanAdded){
				int newSelStart=symbolBeforeAmount ? Math.max(selStart, 1) : Math.min(selStart, length()-1);
				int newSelEnd=symbolBeforeAmount ? Math.max(selEnd, 1) : Math.min(selEnd, length()-1);
				if(newSelStart!=selStart || newSelEnd!=selEnd){
					setSelection(newSelStart, newSelEnd);
				}
			}
		}
	}

	private static class CurrencyInfo{
		public String code;
		public String symbol;
		public Currency jCurrency;

		public CurrencyInfo(String code){
			this.code=code;
			try{
				jCurrency=Currency.getInstance(code);
				symbol=jCurrency.getSymbol();
			}catch(IllegalArgumentException x){
				symbol=code;
			}
		}

		@NonNull
		@Override
		public String toString(){
			return code;
		}
	}

	private class CurrencySymbolSpan extends ReplacementSpan{
		private Paint paint;
		public CurrencySymbolSpan(Paint paint){
			this.paint=new Paint(paint);
			this.paint.setTextSize(paint.getTextSize()*0.66f);
		}

		@Override
		public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm){
			return Math.round(this.paint.measureText(currentCurrency.symbol))+dp(2);
		}

		@Override
		public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint){
			this.paint.setColor(paint.getColor());
			this.paint.setAlpha(77);
			if(!symbolBeforeAmount)
				x+=dp(2);
			canvas.drawText(currentCurrency.symbol, x, top+dp(1.5f)-this.paint.ascent(), this.paint);
		}
	}

	private class FormattingFriendlyDigitsKeyListener extends DigitsKeyListener{
		public FormattingFriendlyDigitsKeyListener(){
			super(false, true);
		}

		@Override
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend){
			// Allow the currency symbol to be inserted (always done as a separate insertion operation)
			if(source instanceof Spannable s && s.getSpans(start, end, CurrencySymbolSpan.class).length>0){
				return source;
			}
			// Don't allow the currency symbol to be deleted
			if(!allowSymbolToBeDeleted && end-start<dend-dstart && dest.getSpans(dstart, dend, CurrencySymbolSpan.class).length>0){
				return dest.subSequence(dstart, dend);
			}
			return super.filter(source, start, end, dest, dstart, dend);
		}
	}

	public interface ChangeListener{
		void onCurrencyChanged(String code);
		void onAmountChanged(long amount);
	}
}
