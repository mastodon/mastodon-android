package org.joinmastodon.android.fragments.onboarding;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.requests.instance.GetInstance;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.catalog.CatalogInstance;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.V;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

abstract class InstanceCatalogFragment extends BaseRecyclerFragment<CatalogInstance>{
	protected RecyclerView.Adapter adapter;
	protected MergeRecyclerAdapter mergeAdapter;
	protected CatalogInstance chosenInstance;
	protected Button nextButton;
	protected EditText searchEdit;
	protected Runnable searchDebouncer=this::onSearchChangedDebounced;
	protected String currentSearchQuery;
	protected String loadingInstanceDomain;
	protected HashMap<String, Instance> instancesCache=new HashMap<>();
	protected View buttonBar;
	protected List<CatalogInstance> filteredData=new ArrayList<>();
	protected GetInstance loadingInstanceRequest;
	protected Call loadingInstanceRedirectRequest;
	protected ProgressDialog instanceProgressDialog;
	protected HashMap<String, String> redirects=new HashMap<>();
	protected HashMap<String, String> redirectsInverse=new HashMap<>();
	protected boolean isSignup;
	protected CatalogInstance fakeInstance=new CatalogInstance();

	private static final double DUNBAR=Math.log(800);

	public InstanceCatalogFragment(int layout, int perPage){
		super(layout, perPage);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		isSignup=getArguments().getBoolean("signup");
	}

	protected abstract void proceedWithAuthOrSignup(Instance instance);

	protected boolean onSearchEnterPressed(TextView v, int actionId, KeyEvent event){
		if(event!=null && event.getAction()!=KeyEvent.ACTION_DOWN)
			return true;
		currentSearchQuery=searchEdit.getText().toString().toLowerCase().trim();
		updateFilteredList();
		searchEdit.removeCallbacks(searchDebouncer);
		Instance instance=instancesCache.get(normalizeInstanceDomain(currentSearchQuery));
		if(instance==null){
			showProgressDialog();
			loadInstanceInfo(currentSearchQuery, false);
		}else{
			proceedWithAuthOrSignup(instance);
		}
		return true;
	}

	protected void onSearchChangedDebounced(){
		currentSearchQuery=searchEdit.getText().toString().toLowerCase().trim();
		updateFilteredList();
		loadInstanceInfo(currentSearchQuery, false);
	}

	protected List<CatalogInstance> sortInstances(List<CatalogInstance> result){
		Map<Boolean, List<CatalogInstance>> byLang=result.stream().sorted(Comparator.comparingInt((CatalogInstance ci)->ci.lastWeekUsers).reversed()).collect(Collectors.groupingBy(ci->ci.approvalRequired));
		ArrayList<CatalogInstance> sortedList=new ArrayList<>();
		sortedList.addAll(byLang.getOrDefault(false, Collections.emptyList()));
		sortedList.addAll(byLang.getOrDefault(true, Collections.emptyList()));
		return sortedList;
	}

	protected abstract void updateFilteredList();

	protected void showProgressDialog(){
		instanceProgressDialog=new ProgressDialog(getActivity());
		instanceProgressDialog.setMessage(getString(R.string.loading_instance));
		instanceProgressDialog.setOnCancelListener(dialog->cancelLoadingInstanceInfo());
		instanceProgressDialog.show();
	}

	protected String normalizeInstanceDomain(String _domain){
		if(TextUtils.isEmpty(_domain))
			return null;
		if(_domain.contains(":")){
			try{
				_domain=Uri.parse(_domain).getAuthority();
			}catch(Exception ignore){
			}
			if(TextUtils.isEmpty(_domain))
				return null;
		}
		String domain;
		try{
			domain=IDN.toASCII(_domain);
		}catch(IllegalArgumentException x){
			return null;
		}
		if(redirects.containsKey(domain))
			return redirects.get(domain);
		return domain;
	}

	protected void loadInstanceInfo(String _domain, boolean isFromRedirect){
		if(TextUtils.isEmpty(_domain))
			return;
		String domain=normalizeInstanceDomain(_domain);
		Instance cachedInstance=instancesCache.get(domain);
		if(cachedInstance!=null){
			for(CatalogInstance ci : filteredData){
				if(ci.domain.equals(domain) && ci!=fakeInstance)
					return;
			}
			CatalogInstance ci=cachedInstance.toCatalogInstance();
			filteredData.add(0, ci);
			adapter.notifyItemInserted(0);
			return;
		}
		if(loadingInstanceDomain!=null){
			if(loadingInstanceDomain.equals(domain)){
				return;
			}else{
				cancelLoadingInstanceInfo();
			}
		}
		try{
			new URI("https://"+domain+"/api/v1/instance"); // Validate the host by trying to parse the URI
		}catch(URISyntaxException x){
			showInstanceInfoLoadError(domain, x);
			if(fakeInstance!=null){
				fakeInstance.description=getString(R.string.error);
				if(filteredData.size()>0 && filteredData.get(0)==fakeInstance){
					if(list.findViewHolderForAdapterPosition(1) instanceof BindableViewHolder<?> ivh){
						ivh.rebind();
					}
				}
			}
			return;
		}
		loadingInstanceDomain=domain;
		loadingInstanceRequest=new GetInstance();
		loadingInstanceRequest.setCallback(new Callback<>(){
			@Override
			public void onSuccess(Instance result){
				loadingInstanceRequest=null;
				loadingInstanceDomain=null;
				result.uri=domain; // needed for instances that use domain redirection
				instancesCache.put(domain, result);
				if(instanceProgressDialog!=null){
					instanceProgressDialog.dismiss();
					instanceProgressDialog=null;
					proceedWithAuthOrSignup(result);
				}
				if(Objects.equals(domain, currentSearchQuery) || Objects.equals(currentSearchQuery, redirects.get(domain)) || Objects.equals(currentSearchQuery, redirectsInverse.get(domain))){
					boolean found=false;
					for(CatalogInstance ci:filteredData){
						if(ci.domain.equals(domain) && ci!=fakeInstance){
							found=true;
							break;
						}
					}
					if(!found){
						CatalogInstance ci=result.toCatalogInstance();
						if(filteredData.size()==1 && filteredData.get(0)==fakeInstance){
							filteredData.set(0, ci);
							adapter.notifyItemChanged(0);
						}else{
							filteredData.add(0, ci);
							adapter.notifyItemInserted(0);
						}
					}
				}
			}

			@Override
			public void onError(ErrorResponse error){
				loadingInstanceRequest=null;
				if(!isFromRedirect && error instanceof MastodonErrorResponse me && me.httpStatus==404){
					fetchDomainFromHostMetaAndMaybeRetry(domain, error);
					return;
				}
				loadingInstanceDomain=null;
				showInstanceInfoLoadError(domain, error);
				if(fakeInstance!=null && getActivity()!=null){
					fakeInstance.description=getString(R.string.error);
					if(filteredData.size()>0 && filteredData.get(0)==fakeInstance){
						if(list.findViewHolderForAdapterPosition(1) instanceof BindableViewHolder<?> ivh){
							ivh.rebind();
						}
					}
				}
			}
		}).execNoAuth(domain);
	}

	private void cancelLoadingInstanceInfo(){
		if(loadingInstanceRequest!=null){
			loadingInstanceRequest.cancel();
			loadingInstanceRequest=null;
		}
		if(loadingInstanceRedirectRequest!=null){
			loadingInstanceRedirectRequest.cancel();
			loadingInstanceRedirectRequest=null;
		}
		loadingInstanceDomain=null;
		if(instanceProgressDialog!=null){
			instanceProgressDialog.dismiss();
			instanceProgressDialog=null;
		}
	}

	private void showInstanceInfoLoadError(String domain, Object error){
		if(instanceProgressDialog!=null){
			instanceProgressDialog.dismiss();
			instanceProgressDialog=null;
			String additionalInfo;
			if(error instanceof MastodonErrorResponse me){
				additionalInfo="\n\n"+me.error;
			}else if(error instanceof Throwable t){
				additionalInfo="\n\n"+t.getLocalizedMessage();
			}else{
				additionalInfo="";
			}
			new M3AlertDialogBuilder(getActivity())
					.setTitle(R.string.error)
					.setMessage(getString(R.string.not_a_mastodon_instance, domain)+additionalInfo)
					.setPositiveButton(R.string.ok, null)
					.show();
		}
	}

	private void fetchDomainFromHostMetaAndMaybeRetry(String domain, Object origError){
		String url="https://"+domain+"/.well-known/host-meta";
		Request req=new Request.Builder()
				.url(url)
				.build();
		loadingInstanceRedirectRequest=MastodonAPIController.getHttpClient().newCall(req);
		loadingInstanceRedirectRequest.enqueue(new okhttp3.Callback(){
			@Override
			public void onFailure(@NonNull Call call, @NonNull IOException e){
				loadingInstanceRedirectRequest=null;
				loadingInstanceDomain=null;
				Activity a=getActivity();
				if(a==null)
					return;
				a.runOnUiThread(()->showInstanceInfoLoadError(domain, e));
			}

			@Override
			public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException{
				loadingInstanceRedirectRequest=null;
				loadingInstanceDomain=null;
				Activity a=getActivity();
				if(a==null)
					return;
				try(response){
					if(!response.isSuccessful()){
						a.runOnUiThread(()->showInstanceInfoLoadError(domain, response.code()+" "+response.message()));
						return;
					}
					InputSource source=new InputSource(response.body().charStream());
					Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
					NodeList list=doc.getElementsByTagName("Link");
					for(int i=0; i<list.getLength(); i++){
						if(list.item(i) instanceof Element el){
							String template=el.getAttribute("template");
							if("lrdd".equals(el.getAttribute("rel")) && !TextUtils.isEmpty(template) && template.contains("{uri}")){
								Uri uri=Uri.parse(template.replace("{uri}", "qwe"));
								String redirectDomain=normalizeInstanceDomain(uri.getHost());
								redirects.put(domain, redirectDomain);
								redirectsInverse.put(redirectDomain, domain);
								a.runOnUiThread(()->loadInstanceInfo(redirectDomain, true));
								return;
							}
						}
					}
					a.runOnUiThread(()->showInstanceInfoLoadError(domain, origError));
				}catch(Exception x){
					a.runOnUiThread(()->showInstanceInfoLoadError(domain, x));
				}
			}
		});
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		super.onApplyWindowInsets(UiUtils.applyBottomInsetToFixedView(buttonBar, insets));
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		nextButton=view.findViewById(R.id.btn_next);
		nextButton.setOnClickListener(this::onNextClick);
		nextButton.setEnabled(chosenInstance!=null);
		buttonBar=view.findViewById(R.id.button_bar);
		setRefreshEnabled(false);
	}

	protected void onNextClick(View v){
		String domain=chosenInstance.domain;
		Instance instance=instancesCache.get(domain);
		if(instance!=null){
			proceedWithAuthOrSignup(instance);
		}else{
			showProgressDialog();
			if(!domain.equals(loadingInstanceDomain)){
				loadInstanceInfo(domain, false);
			}
		}
	}
}
