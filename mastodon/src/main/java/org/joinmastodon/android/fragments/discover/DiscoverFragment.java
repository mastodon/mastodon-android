package org.joinmastodon.android.fragments.discover;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.QrCodeScanActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.ScrollableToTop;
import org.joinmastodon.android.googleservices.GmsClient;
import org.joinmastodon.android.googleservices.barcodescanner.Barcode;
import org.joinmastodon.android.googleservices.barcodescanner.BarcodeScanner;
import org.joinmastodon.android.model.SearchResult;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.tabs.TabLayout;
import org.joinmastodon.android.ui.tabs.TabLayoutMediator;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import me.grishka.appkit.Nav;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.utils.V;

public class DiscoverFragment extends AppKitFragment implements ScrollableToTop{
	private static final int QUERY_RESULT=937;
	private static final int SCAN_RESULT=456;

	private TabLayout tabLayout;
	private ViewPager2 pager;
	private FrameLayout[] tabViews;
	private TabLayoutMediator tabLayoutMediator;
	private boolean searchActive;
	private FrameLayout searchView;
	private ImageButton searchBack, searchScanQR;
	private TextView searchText;
	private View tabsDivider;

	private DiscoverPostsFragment postsFragment;
	private TrendingHashtagsFragment hashtagsFragment;
	private DiscoverNewsFragment newsFragment;
	private DiscoverAccountsFragment accountsFragment;
	private SearchFragment searchFragment;

	private String accountID;
	private String currentQuery;
	private Intent scannerIntent;
	private Runnable searchExitCallback=this::exitSearch;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);

		accountID=getArguments().getString("account");
		scannerIntent=GmsClient.isGooglePlayServicesAvailable(getActivity())
				? BarcodeScanner.createIntent(Barcode.FORMAT_QR_CODE, false, true)
				: new Intent(getActivity(), QrCodeScanActivity.class);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		LinearLayout view=(LinearLayout) inflater.inflate(R.layout.fragment_discover, container, false);

		tabLayout=view.findViewById(R.id.tabbar);
		pager=view.findViewById(R.id.pager);

		tabViews=new FrameLayout[4];
		for(int i=0;i<tabViews.length;i++){
			FrameLayout tabView=new FrameLayout(getActivity());
			tabView.setId(switch(i){
				case 0 -> R.id.discover_posts;
				case 1 -> R.id.discover_hashtags;
				case 2 -> R.id.discover_news;
				case 3 -> R.id.discover_users;
				default -> throw new IllegalStateException("Unexpected value: "+i);
			});
			tabView.setVisibility(View.GONE);
			view.addView(tabView); // needed so the fragment manager will have somewhere to restore the tab fragment
			tabViews[i]=tabView;
		}

		tabLayout.setTabTextColors(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant), UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));
		tabLayout.setTabTextSize(V.dp(14));

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

		if(postsFragment==null){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putBoolean("__is_tab", true);

			postsFragment=new DiscoverPostsFragment();
			postsFragment.setArguments(args);

			hashtagsFragment=new TrendingHashtagsFragment();
			hashtagsFragment.setArguments(args);

			newsFragment=new DiscoverNewsFragment();
			newsFragment.setArguments(args);

			accountsFragment=new DiscoverAccountsFragment();
			accountsFragment.setArguments(args);

			getChildFragmentManager().beginTransaction()
					.add(R.id.discover_posts, postsFragment)
					.add(R.id.discover_hashtags, hashtagsFragment)
					.add(R.id.discover_news, newsFragment)
					.add(R.id.discover_users, accountsFragment)
					.commit();
		}

		tabLayoutMediator=new TabLayoutMediator(tabLayout, pager, new TabLayoutMediator.TabConfigurationStrategy(){
			@Override
			public void onConfigureTab(@NonNull TabLayout.Tab tab, int position){
				tab.setText(switch(position){
					case 0 -> R.string.posts;
					case 1 -> R.string.hashtags;
					case 2 -> R.string.news;
					case 3 -> R.string.for_you;
					default -> throw new IllegalStateException("Unexpected value: "+position);
				});
			}
		});
		tabLayoutMediator.attach();
		tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
			@Override
			public void onTabSelected(TabLayout.Tab tab){}

			@Override
			public void onTabUnselected(TabLayout.Tab tab){}

			@Override
			public void onTabReselected(TabLayout.Tab tab){
				scrollToTop();
			}
		});

		searchView=view.findViewById(R.id.search_fragment);
		if(searchFragment==null){
			searchFragment=new SearchFragment();
			Bundle args=new Bundle();
			args.putString("account", accountID);
			searchFragment.setArguments(args);
			getChildFragmentManager().beginTransaction().add(R.id.search_fragment, searchFragment).commit();
		}

		searchBack=view.findViewById(R.id.search_back);
		searchText=view.findViewById(R.id.search_text);
		searchBack.setEnabled(searchActive);
		searchBack.setImportantForAccessibility(searchActive ? View.IMPORTANT_FOR_ACCESSIBILITY_YES : View.IMPORTANT_FOR_ACCESSIBILITY_NO);
		searchBack.setOnClickListener(v->exitSearch());
		if(searchActive){
			searchBack.setImageResource(me.grishka.appkit.R.drawable.ic_arrow_back);
			pager.setVisibility(View.GONE);
			tabLayout.setVisibility(View.GONE);
			searchView.setVisibility(View.VISIBLE);
		}
		searchScanQR=view.findViewById(R.id.search_scan_qr);
		searchScanQR.setOnClickListener(v->openQrScanner());

		View searchWrap=view.findViewById(R.id.search_wrap);
		searchWrap.setOutlineProvider(OutlineProviders.roundedRect(28));
		searchWrap.setClipToOutline(true);
		searchText.setOnClickListener(v->{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			if(!TextUtils.isEmpty(currentQuery)){
				args.putString("query", currentQuery);
			}
			Nav.goForResult(getActivity(), SearchQueryFragment.class, args, QUERY_RESULT, DiscoverFragment.this);
		});
		tabsDivider=view.findViewById(R.id.tabs_divider);

		return view;
	}

	@Override
	public void scrollToTop(){
		if(!searchActive){
			((ScrollableToTop)getFragmentForPage(pager.getCurrentItem())).scrollToTop();
		}else{
			searchFragment.scrollToTop();
		}
	}

	public void loadData(){
		if(postsFragment!=null && !postsFragment.loaded && !postsFragment.dataLoading)
			postsFragment.loadData();
	}

	private void enterSearch(){
		if(!searchActive){
			searchActive=true;
			pager.setVisibility(View.GONE);
			tabLayout.setVisibility(View.GONE);
			searchView.setVisibility(View.VISIBLE);
			searchBack.setImageResource(me.grishka.appkit.R.drawable.ic_arrow_back);
			searchBack.setEnabled(true);
			searchBack.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
			tabsDivider.setVisibility(View.GONE);
			addBackCallback(searchExitCallback);
		}
	}

	private void exitSearch(){
		if(!searchActive)
			return;
		searchActive=false;
		pager.setVisibility(View.VISIBLE);
		tabLayout.setVisibility(View.VISIBLE);
		searchView.setVisibility(View.GONE);
		searchText.setText(R.string.search_mastodon);
		searchBack.setImageResource(R.drawable.ic_search_24px);
		searchBack.setEnabled(false);
		searchBack.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
		tabsDivider.setVisibility(View.VISIBLE);
		currentQuery=null;
		removeBackCallback(searchExitCallback);
	}

	private Fragment getFragmentForPage(int page){
		return switch(page){
			case 0 -> postsFragment;
			case 1 -> hashtagsFragment;
			case 2 -> newsFragment;
			case 3 -> accountsFragment;
			default -> throw new IllegalStateException("Unexpected value: "+page);
		};
	}

	@Override
	public void onFragmentResult(int reqCode, boolean success, Bundle result){
		if(reqCode==QUERY_RESULT && success){
			enterSearch();
			currentQuery=result.getString("query");
			SearchResult.Type type;
			if(result.containsKey("filter")){
				type=SearchResult.Type.values()[result.getInt("filter")];
			}else{
				type=null;
			}
			searchFragment.setQuery(currentQuery, type);
			searchText.setText(currentQuery);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode==SCAN_RESULT && resultCode==Activity.RESULT_OK){
			String code=data.getStringExtra("barcode");
			if(BarcodeScanner.isValidResult(data)){
				Barcode barcode=BarcodeScanner.getResult(data);
				code=barcode.rawValue;
			}
			if(code!=null){
				if(code.startsWith("https:") || code.startsWith("http:")){
					((MainActivity)getActivity()).handleURL(Uri.parse(code), accountID);
				}else{
					Toast.makeText(getActivity(), R.string.link_not_supported, Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	private void openQrScanner(){
		if(scannerIntent.resolveActivity(getActivity().getPackageManager())!=null){
			startActivityForResult(scannerIntent, SCAN_RESULT);
		}else{
			BarcodeScanner.installScannerModule(getActivity(), ()->startActivityForResult(scannerIntent, SCAN_RESULT));
		}
	}

	private class DiscoverPagerAdapter extends RecyclerView.Adapter<SimpleViewHolder>{
		@NonNull
		@Override
		public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			FrameLayout view=new FrameLayout(parent.getContext());
			view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			return new SimpleViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position){
			FrameLayout view=tabViews[position];
			if(view.getParent() instanceof ViewGroup parent)
				parent.removeView(view);
			view.setVisibility(View.VISIBLE);
			((FrameLayout)holder.itemView).addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}

		@Override
		public int getItemCount(){
			return tabViews.length;
		}

		@Override
		public int getItemViewType(int position){
			return position;
		}
	}
}
