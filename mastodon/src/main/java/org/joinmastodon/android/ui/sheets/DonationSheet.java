package org.joinmastodon.android.ui.sheets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.joinmastodon.android.DonationFragmentActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.DonationWebViewFragment;
import org.joinmastodon.android.model.donations.DonationCampaign;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CurrencyAmountInput;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import me.grishka.appkit.utils.CustomViewHelper;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;

public class DonationSheet extends BottomSheet{
	private final DonationCampaign campaign;
	private final String accountID;
	private final Consumer<Intent> startCallback;
	private DonationFrequency frequency=DonationFrequency.MONTHLY;

	private View onceTab, monthlyTab, yearlyTab;
	private int currentTab;
	private CurrencyAmountInput amountField;
	private ToggleButton[] suggestedAmountButtons=new ToggleButton[6];
	private View button;
	private TextView buttonText;
	private Activity activity;

	public DonationSheet(@NonNull Activity activity, DonationCampaign campaign, String accountID, Consumer<Intent> startCallback){
		super(activity);
		this.campaign=campaign;
		this.accountID=accountID;
		this.activity=activity;
		this.startCallback=startCallback;
		Context context=activity;

		View content=context.getSystemService(LayoutInflater.class).inflate(R.layout.sheet_donation, null);
		setContentView(content);
		setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(context, R.attr.colorM3Surface),
				UiUtils.getThemeColor(context, R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());

		TextView text=findViewById(R.id.text);
		text.setText(campaign.donationMessage.trim());

		onceTab=findViewById(R.id.once);
		monthlyTab=findViewById(R.id.monthly);
		yearlyTab=findViewById(R.id.yearly);
		onceTab.setOnClickListener(this::onTabClick);
		monthlyTab.setOnClickListener(this::onTabClick);
		yearlyTab.setOnClickListener(this::onTabClick);

		if(campaign.amounts.yearly==null)
			yearlyTab.setVisibility(View.GONE);
		if(campaign.amounts.oneTime==null)
			onceTab.setVisibility(View.GONE);
		if(campaign.amounts.monthly==null){
			monthlyTab.setVisibility(View.GONE);
			if(campaign.amounts.oneTime!=null){
				onceTab.setSelected(true);
				currentTab=R.id.once;
				frequency=DonationFrequency.ONCE;
			}else if(campaign.amounts.yearly!=null){
				yearlyTab.setSelected(true);
				currentTab=R.id.yearly;
				frequency=DonationFrequency.YEARLY;
			}else{
				Toast.makeText(context, "Amounts object is empty", Toast.LENGTH_SHORT).show();
				dismiss();
				return;
			}
		}else{
			monthlyTab.setSelected(true);
			currentTab=R.id.monthly;
		}


		View tabBarItself=findViewById(R.id.tabbar_inner);
		tabBarItself.setOutlineProvider(OutlineProviders.roundedRect(20));
		tabBarItself.setClipToOutline(true);

		amountField=findViewById(R.id.amount);
		List<String> availableCurrencies=campaign.amounts.monthly.keySet().stream().sorted().collect(Collectors.toList());
		amountField.setCurrencies(availableCurrencies);
		try{
			amountField.setSelectedCurrency(campaign.defaultCurrency);
		}catch(IllegalArgumentException x){
			new M3AlertDialogBuilder(context)
					.setTitle(R.string.error)
					.setMessage("Default currency "+campaign.defaultCurrency+" not in list of available currencies "+availableCurrencies)
					.show();
			dismiss();
			return;
		}
		amountField.setChangeListener(new CurrencyAmountInput.ChangeListener(){
			@Override
			public void onCurrencyChanged(String code){
				updateSuggestedAmounts(code);
				button.setEnabled(amountField.getAmount()>=getMinimumChargeAmount(code));
				updateSuggestedButtonsState();
			}

			@Override
			public void onAmountChanged(long amount){
				button.setEnabled(amount>=getMinimumChargeAmount(amountField.getCurrency()));
				updateSuggestedButtonsState();
			}
		});
		button=findViewById(R.id.button);
		buttonText=findViewById(R.id.button_text);

		ViewGroup suggestedAmounts=findViewById(R.id.suggested_amounts);
		for(int i=0;i<suggestedAmountButtons.length;i++){
			ToggleButton btn=new ToggleButton(context);
			btn.setBackgroundResource(R.drawable.bg_filter_chip);
			btn.setTextAppearance(R.style.m3_label_large);
			btn.setTextColor(context.getResources().getColorStateList(R.color.filter_chip_text, context.getTheme()));
			btn.setMinWidth(V.dp(64));
			btn.setMinimumWidth(0);
			btn.setPadding(0, 0, 0, 0);
			btn.setStateListAnimator(null);
			btn.setTextOff(null);
			btn.setTextOn(null);
			btn.setOnClickListener(this::onSuggestedAmountClick);
			btn.setTag(i);
			btn.setSingleLine();
			suggestedAmountButtons[i]=btn;
			suggestedAmounts.addView(btn);
		}
		updateSuggestedAmounts(campaign.defaultCurrency);
		button.setEnabled(false);
		buttonText.setText(campaign.donationButtonText);
		button.setOnClickListener(v->openWebView());

		Arrays.stream(getCurrentSuggestedAmounts(campaign.defaultCurrency)).min().ifPresent(amountField::setAmount);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Window window=getWindow();
		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	}

	private void onTabClick(View v){
		if(v.getId()==currentTab)
			return;
		findViewById(currentTab).setSelected(false);
		v.setSelected(true);
		currentTab=v.getId();
		if(currentTab==R.id.once)
			frequency=DonationFrequency.ONCE;
		else if(currentTab==R.id.monthly)
			frequency=DonationFrequency.MONTHLY;
		else if(currentTab==R.id.yearly)
			frequency=DonationFrequency.YEARLY;
		updateSuggestedAmounts(amountField.getCurrency());
	}

	private long[] getCurrentSuggestedAmounts(String currency){
		long[] amounts=(switch(frequency){
			case ONCE -> campaign.amounts.oneTime;
			case MONTHLY -> campaign.amounts.monthly;
			case YEARLY -> campaign.amounts.yearly;
		}).get(currency);
		if(amounts==null){
			amounts=new long[0];
		}
		return amounts;
	}

	private void updateSuggestedAmounts(String currency){
		NumberFormat format=NumberFormat.getCurrencyInstance();
		try{
			format.setCurrency(Currency.getInstance(currency));
		}catch(IllegalArgumentException ignore){}
		int defaultFractionDigits=format.getMinimumFractionDigits();
		long[] amounts=getCurrentSuggestedAmounts(currency);
		for(int i=0;i<suggestedAmountButtons.length;i++){
			ToggleButton btn=suggestedAmountButtons[i];
			if(i>=amounts.length){
				btn.setVisibility(View.GONE);
				continue;
			}
			btn.setVisibility(View.VISIBLE);
			long amount=amounts[i];
			format.setMinimumFractionDigits(amount%100==0 ? 0 : defaultFractionDigits);
			btn.setText(format.format(amount/100.0));
		}
		updateSuggestedButtonsState();
	}

	private void onSuggestedAmountClick(View v){
		int index=(int) v.getTag();
		long[] amounts=getCurrentSuggestedAmounts(amountField.getCurrency());
		amountField.setAmount(amounts[index]);
	}

	private void updateSuggestedButtonsState(){
		long amount=amountField.getAmount();
		long[] amounts=getCurrentSuggestedAmounts(amountField.getCurrency());
		for(int i=0;i<Math.min(amounts.length, suggestedAmountButtons.length);i++){
			ToggleButton btn=suggestedAmountButtons[i];
			btn.setChecked(amounts[i]==amount);
		}
	}

	private void openWebView(){
		Uri.Builder builder=Uri.parse(campaign.donationUrl).buildUpon();
		builder.appendQueryParameter("locale", Locale.getDefault().toLanguageTag().replace('-', '_'))
				.appendQueryParameter("platform", "android")
				.appendQueryParameter("currency", amountField.getCurrency())
				.appendQueryParameter("amount", String.valueOf(amountField.getAmount()))
				.appendQueryParameter("source", "campaign")
				.appendQueryParameter("campaign_id", campaign.id)
				.appendQueryParameter("frequency", switch(frequency){
					case ONCE -> "one_time";
					case MONTHLY -> "monthly";
					case YEARLY -> "yearly";
				})
				.appendQueryParameter("success_callback_url", DonationWebViewFragment.SUCCESS_URL)
				.appendQueryParameter("cancel_callback_url", DonationWebViewFragment.CANCEL_URL)
				.appendQueryParameter("failure_callback_url", DonationWebViewFragment.FAILURE_URL);
		Bundle args=new Bundle();
		args.putString("url", builder.build().toString());
		args.putString("account", accountID);
		args.putString("campaignID", campaign.id);
		args.putString("successPostText", campaign.donationSuccessPost);
		args.putBoolean("_can_go_back", true);
		startCallback.accept(new Intent(activity, DonationFragmentActivity.class).putExtra("fragmentArgs", args));
	}

	private static long getMinimumChargeAmount(String currency){
		// https://docs.stripe.com/currencies#minimum-and-maximum-charge-amounts
		// values are in cents
		return switch(currency){
			case "USD" -> 50;
			case "AED" -> 2_00;
			case "AUD" -> 50;
			case "BGN" -> 1_00;
			case "BRL" -> 50;
			case "CAD" -> 50;
			case "CHF" -> 50;
			case "CZK" -> 15_00;
			case "DKK" -> 2_50;
			case "EUR" -> 50;
			case "GBP" -> 30;
			case "HKD" -> 4_00;
			case "HUF" -> 175_00;
			case "INR" -> 50;
			case "JPY" -> 50_00;
			case "MXN" -> 10_00;
			case "MYR" -> 2_00;
			case "NOK" -> 3_00;
			case "NZD" -> 50;
			case "PLN" -> 2_00;
			case "RON" -> 2_00;
			case "SEK" -> 3_00;
			case "SGD" -> 50;
			case "THB" -> 10_00;

			default -> 50;
		};
	}

	private enum DonationFrequency{
		ONCE,
		MONTHLY,
		YEARLY
	}

	public static class SuggestedAmountsLayout extends ViewGroup implements CustomViewHelper{
		private int visibleChildCount;
		private static final int H_GAP=24;
		private static final int V_GAP=8;
		private static final int ROW_HEIGHT=32;

		public SuggestedAmountsLayout(Context context){
			this(context, null);
		}

		public SuggestedAmountsLayout(Context context, AttributeSet attrs){
			this(context, attrs, 0);
		}

		public SuggestedAmountsLayout(Context context, AttributeSet attrs, int defStyle){
			super(context, attrs, defStyle);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
			visibleChildCount=0;
			for(int i=0;i<getChildCount();i++){
				View child=getChildAt(i);
				if(child.getVisibility()==GONE)
					continue;
				visibleChildCount++;
			}
			int width=MeasureSpec.getSize(widthMeasureSpec);
			setMeasuredDimension(width, visibleChildCount>4 ? dp(ROW_HEIGHT*2+V_GAP) : dp(ROW_HEIGHT));
			int buttonsPerRow=visibleChildCount>4 ? 3 : visibleChildCount;
			int buttonWidth=(width-dp(H_GAP)*(buttonsPerRow-1))/buttonsPerRow;
			for(int i=0;i<getChildCount();i++){
				View child=getChildAt(i);
				if(child.getVisibility()==GONE)
					continue;
				child.measure(buttonWidth | MeasureSpec.EXACTLY, dp(ROW_HEIGHT) | MeasureSpec.EXACTLY);
			}
		}

		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b){
			int width=r-l;
			int buttonsPerRow=visibleChildCount>4 ? 3 : visibleChildCount;
			int buttonWidth=(width-dp(H_GAP)*(buttonsPerRow-1))/buttonsPerRow;
			for(int i=0;i<getChildCount();i++){
				View child=getChildAt(i);
				if(child.getVisibility()==GONE)
					continue;
				int column=i%buttonsPerRow;
				int row=i/buttonsPerRow;
				int left=(buttonWidth+dp(H_GAP))*column;
				int top=dp(ROW_HEIGHT+V_GAP)*row;
				child.layout(left, top, left+buttonWidth, top+dp(ROW_HEIGHT));
			}
		}
	}
}
