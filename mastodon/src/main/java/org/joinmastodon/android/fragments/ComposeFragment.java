package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.graphics.Outline;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.twitter.twittertext.Regex;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;

import java.text.BreakIterator;
import java.util.UUID;
import java.util.regex.Pattern;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.ToolbarFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class ComposeFragment extends ToolbarFragment{

	private static final Pattern MENTION_PATTERN=Pattern.compile("(^|[^\\/\\w])@(([a-z0-9_]+)@[a-z0-9\\.\\-]+[a-z0-9]+)", Pattern.CASE_INSENSITIVE);

	private static final String VALID_URL_PATTERN_STRING =
					"(" +                                                            //  $1 total match
						"(" + Regex.URL_VALID_PRECEDING_CHARS + ")" +                        //  $2 Preceding character
						"(" +                                                          //  $3 URL
						"(https?://)" +                                             //  $4 Protocol (optional)
						"(" + Regex.URL_VALID_DOMAIN + ")" +                               //  $5 Domain(s)
						"(?::(" + Regex.URL_VALID_PORT_NUMBER + "))?" +                    //  $6 Port number (optional)
						"(/" +
						Regex.URL_VALID_PATH + "*+" +
						")?" +                                                       //  $7 URL Path and anchor
						"(\\?" + Regex.URL_VALID_URL_QUERY_CHARS + "*" +                   //  $8 Query String
						Regex.URL_VALID_URL_QUERY_ENDING_CHARS + ")?" +
						")" +
					")";
	private static final Pattern URL_PATTERN=Pattern.compile(VALID_URL_PATTERN_STRING, Pattern.CASE_INSENSITIVE);
	private final BreakIterator breakIterator=BreakIterator.getCharacterInstance();

	private TextView selfName, selfUsername;
	private ImageView selfAvatar;
	private Account self;
	private String instanceDomain;

	private EditText mainEditText;
	private TextView charCounter;
	private String accountID;
	private int charCount, charLimit;

	private MenuItem publishButton;

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
		accountID=getArguments().getString("account");
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		charLimit=session.tootCharLimit;
		if(charLimit==0)
			charLimit=500;
		self=session.self;
		instanceDomain=session.domain;
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_compose, container, false);
		mainEditText=view.findViewById(R.id.toot_text);
		charCounter=view.findViewById(R.id.char_counter);
		charCounter.setText(String.valueOf(charLimit));

		selfName=view.findViewById(R.id.name);
		selfUsername=view.findViewById(R.id.username);
		selfAvatar=view.findViewById(R.id.avatar);
		selfName.setText(self.displayName);
		selfUsername.setText('@'+self.username+'@'+instanceDomain);
		ViewImageLoader.load(selfAvatar, null, new UrlImageLoaderRequest(self.avatar));
		ViewOutlineProvider roundCornersOutline=new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), V.dp(12));
			}
		};
		selfAvatar.setOutlineProvider(roundCornersOutline);
		selfAvatar.setClipToOutline(true);

		return view;
	}

	@Override
	public void onResume(){
		super.onResume();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		InputMethodManager imm=getActivity().getSystemService(InputMethodManager.class);
		view.postDelayed(()->{
			mainEditText.requestFocus();
			imm.showSoftInput(mainEditText, 0);
		}, 100);

		mainEditText.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){

			}

			@Override
			public void afterTextChanged(Editable s){
				updateCharCounter(s);
			}
		});
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		publishButton=menu.add("TOOT!");
		publishButton.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		updatePublishButtonState();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		String text=mainEditText.getText().toString();
		CreateStatus.Request req=new CreateStatus.Request();
		req.status=text;
		String uuid=UUID.randomUUID().toString();
		new CreateStatus(req, uuid)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Status result){
						Nav.finish(ComposeFragment.this);
						E.post(new StatusCreatedEvent(result));
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.exec(accountID);
		return true;
	}

	private void updateCharCounter(CharSequence text){
		String countableText=MENTION_PATTERN.matcher(URL_PATTERN.matcher(text).replaceAll("$2xxxxxxxxxxxxxxxxxxxxxxx")).replaceAll("$1@$3");
		breakIterator.setText(countableText);
		charCount=0;
		while(breakIterator.next()!=BreakIterator.DONE){
			charCount++;
		}

		charCounter.setText(String.valueOf(charLimit-charCount));
		updatePublishButtonState();
	}

	private void updatePublishButtonState(){
		publishButton.setEnabled(charCount>0 && charCount<=charLimit);
	}
}
