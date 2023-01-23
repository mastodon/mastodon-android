package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.joinmastodon.android.R;
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.tabs.TabLayout;
import org.joinmastodon.android.ui.tabs.TabLayoutMediator;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.utils.V;

public class NotificationsFragment extends MastodonToolbarFragment implements ScrollableToTop{

	private TabLayout tabLayout;
	private ViewPager2 pager;
	private FrameLayout[] tabViews;
	private TabLayoutMediator tabLayoutMediator;

	private NotificationsListFragment allNotificationsFragment, mentionsFragment;

	private String accountID;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);

		accountID=getArguments().getString("account");
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle(R.string.notifications);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		LinearLayout view=(LinearLayout) inflater.inflate(R.layout.fragment_notifications, container, false);

		tabLayout=view.findViewById(R.id.tabbar);
		pager=view.findViewById(R.id.pager);

		tabViews=new FrameLayout[2];
		for(int i=0;i<tabViews.length;i++){
			FrameLayout tabView=new FrameLayout(getActivity());
			tabView.setId(switch(i){
				case 0 -> R.id.notifications_all;
				case 1 -> R.id.notifications_mentions;
				default -> throw new IllegalStateException("Unexpected value: "+i);
			});
			tabView.setVisibility(View.GONE);
			view.addView(tabView); // needed so the fragment manager will have somewhere to restore the tab fragment
			tabViews[i]=tabView;
		}

		tabLayout.setTabTextSize(V.dp(16));
		tabLayout.setTabTextColors(UiUtils.getThemeColor(getActivity(), R.attr.colorTabInactive), UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {
				scrollToTop();
			}
		});

		pager.setOffscreenPageLimit(4);
		pager.setAdapter(new DiscoverPagerAdapter());
		pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
			@Override
			public void onPageSelected(int position){
				if(position==0)
					return;
				Fragment _page=getFragmentForPage(position);
				if(_page instanceof BaseRecyclerFragment<?> page){
					if(!page.loaded && !page.isDataLoading())
						page.loadData();
				}
			}
		});

		if(allNotificationsFragment==null){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putBoolean("__is_tab", true);

			allNotificationsFragment=new NotificationsListFragment();
			allNotificationsFragment.setArguments(args);

			args=new Bundle(args);
			args.putBoolean("onlyMentions", true);
			mentionsFragment=new NotificationsListFragment();
			mentionsFragment.setArguments(args);

			getChildFragmentManager().beginTransaction()
					.add(R.id.notifications_all, allNotificationsFragment)
					.add(R.id.notifications_mentions, mentionsFragment)
					.commit();
		}

		tabLayoutMediator=new TabLayoutMediator(tabLayout, pager, new TabLayoutMediator.TabConfigurationStrategy(){
			@Override
			public void onConfigureTab(@NonNull TabLayout.Tab tab, int position){
				tab.setText(switch(position){
					case 0 -> R.string.all_notifications;
					case 1 -> R.string.mentions;
					default -> throw new IllegalStateException("Unexpected value: "+position);
				});
				tab.view.textView.setAllCaps(true);
			}
		});
		tabLayoutMediator.attach();

		return view;
	}

	@Override
	public void scrollToTop(){
		getFragmentForPage(pager.getCurrentItem()).scrollToTop();
	}

	public void loadData(){
		if(allNotificationsFragment!=null && !allNotificationsFragment.loaded && !allNotificationsFragment.dataLoading)
			allNotificationsFragment.loadData();
	}

	@Override
	protected void updateToolbar(){
		super.updateToolbar();
		getToolbar().setOutlineProvider(null);
		getToolbar().setOnClickListener(v->scrollToTop());
	}

	private NotificationsListFragment getFragmentForPage(int page){
		return switch(page){
			case 0 -> allNotificationsFragment;
			case 1 -> mentionsFragment;
			default -> throw new IllegalStateException("Unexpected value: "+page);
		};
	}

	private class DiscoverPagerAdapter extends RecyclerView.Adapter<SimpleViewHolder>{
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
		public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position){}

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
