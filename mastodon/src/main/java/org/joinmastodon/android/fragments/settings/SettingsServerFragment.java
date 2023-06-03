package org.joinmastodon.android.fragments.settings;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.tabs.TabLayout;
import org.joinmastodon.android.ui.tabs.TabLayoutMediator;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.NestedRecyclerScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.utils.V;

public class SettingsServerFragment extends AppKitFragment{
	private String accountID;
	private Instance instance;
	private TabLayout tabBar;
	private TabLayoutMediator tabLayoutMediator;
	private ViewPager2 pager;
	private FrameLayout[] tabViews;
	private View contentView;
	private WindowInsets childInsets;

	private SettingsServerAboutFragment aboutFragment;
	private SettingsServerRulesFragment rulesFragment;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		setTitle(AccountSessionManager.get(accountID).domain);

		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putBoolean("__is_tab", true);
		aboutFragment=new SettingsServerAboutFragment();
		aboutFragment.setArguments(args);
		rulesFragment=new SettingsServerRulesFragment();
		rulesFragment.setArguments(args);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_settings_server, container, false);

		TextView realTitle=view.findViewById(R.id.real_title);
		realTitle.setText(getTitle());
		realTitle.setSelected(true);

		pager=view.findViewById(R.id.pager);
		pager.setAdapter(new ServerPagerAdapter());

		FrameLayout sizeWrapper=new FrameLayout(getActivity()){
			@Override
			protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
				pager.getLayoutParams().height=MeasureSpec.getSize(heightMeasureSpec)-getPaddingTop()-getPaddingBottom()-V.dp(48);
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			}
		};

		tabViews=new FrameLayout[2];
		for(int i=0;i<tabViews.length;i++){
			FrameLayout tabView=new FrameLayout(getActivity());
			tabView.setId(switch(i){
				case 0 -> R.id.server_about;
				case 1 -> R.id.server_rules;
				default -> throw new IllegalStateException("Unexpected value: "+i);
			});
			tabView.setVisibility(View.GONE);
			sizeWrapper.addView(tabView); // needed so the fragment manager will have somewhere to restore the tab fragment
			tabViews[i]=tabView;
		}

		tabBar=view.findViewById(R.id.tabbar);
		tabBar.setTabTextColors(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant), UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));
		tabBar.setTabTextSize(V.dp(16));
		tabLayoutMediator=new TabLayoutMediator(tabBar, pager, (tab, position)->tab.setText(switch(position){
			case 0 -> R.string.about_server;
			case 1 -> R.string.server_rules;
			default -> throw new IllegalStateException("Unexpected value: "+position);
		}));
		tabLayoutMediator.attach();

		NestedRecyclerScrollView scrollView=view.findViewById(R.id.scroller);
		scrollView.setScrollableChildSupplier(()->switch(pager.getCurrentItem()){
			case 0 -> aboutFragment.scroller;
			case 1 -> rulesFragment.getList();
			default -> throw new IllegalStateException("Unexpected value: "+pager.getCurrentItem());
		});

		return contentView=view;
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		getToolbar().setTitle(null);
	}

	private Fragment getFragmentForPage(int page){
		return switch(page){
			case 0 -> aboutFragment;
			case 1 -> rulesFragment;
			default -> throw new IllegalStateException();
		};
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(contentView!=null){
			if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
				int insetBottom=insets.getSystemWindowInsetBottom();
				childInsets=insets.inset(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
				applyChildWindowInsets();
				insets=insets.inset(0, 0, 0, insetBottom);
			}
		}
		super.onApplyWindowInsets(insets);
	}

	private void applyChildWindowInsets(){
		if(aboutFragment!=null && aboutFragment.isAdded() && childInsets!=null){
			aboutFragment.onApplyWindowInsets(childInsets);
			rulesFragment.onApplyWindowInsets(childInsets);
		}
	}

	private class ServerPagerAdapter extends RecyclerView.Adapter<SimpleViewHolder>{
		@NonNull
		@Override
		public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			FrameLayout view=tabViews[viewType];
			((ViewGroup)view.getParent()).removeView(view);
			view.setVisibility(View.VISIBLE);
			view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			return new SimpleViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position){
			Fragment fragment=getFragmentForPage(position);
			if(!fragment.isAdded()){
				getChildFragmentManager().beginTransaction().add(holder.itemView.getId(), fragment).commit();
				holder.itemView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
						getChildFragmentManager().executePendingTransactions();
						if(fragment.isAdded()){
							holder.itemView.getViewTreeObserver().removeOnPreDrawListener(this);
							applyChildWindowInsets();
						}
						return true;
					}
				});
			}
		}

		@Override
		public int getItemCount(){
			return 2;
		}

		@Override
		public int getItemViewType(int position){
			return position;
		}
	}
}
