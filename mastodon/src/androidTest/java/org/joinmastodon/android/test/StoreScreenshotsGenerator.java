package org.joinmastodon.android.test;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.instance.GetInstance;
import org.joinmastodon.android.api.requests.statuses.GetStatusByID;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.fragments.onboarding.InstanceRulesFragment;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.parceler.Parcels;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.screenshot.ScreenCapture;
import androidx.test.runner.screenshot.Screenshot;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import okio.BufferedSink;
import okio.Okio;
import okio.Sink;
import okio.Source;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class StoreScreenshotsGenerator{
	private static final String PHOTO_FILE="IMG_1010.jpg";
	private static final long LOAD_WAIT_TIMEOUT=20_000;

	@Rule
	public ActivityScenarioRule<MainActivity> activityScenarioRule=new ActivityScenarioRule<>(MainActivity.class);

	@Test
	public void takeScreenshots() throws Exception{
		File photo=new File(MastodonApp.context.getCacheDir(), PHOTO_FILE);
		try(Source source=Okio.source(InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(PHOTO_FILE)); BufferedSink sink=Okio.buffer(Okio.sink(photo))){
			sink.writeAll(source);
			sink.flush();
		}

		GlobalUserPreferences.theme=GlobalUserPreferences.ThemePreference.LIGHT;
		Bundle args=InstrumentationRegistry.getArguments();
		InstrumentationRegistry.getInstrumentation().setInTouchMode(true);

		AccountSession session=AccountSessionManager.getInstance().getAccount(AccountSessionManager.getInstance().getLastActiveAccountID());
		MastodonApp.context.deleteDatabase(session.getID()+".db");

		onView(isRoot()).perform(waitId(R.id.more, LOAD_WAIT_TIMEOUT));
		Thread.sleep(500);
		takeScreenshot("HomeTimeline");

		GlobalUserPreferences.theme=GlobalUserPreferences.ThemePreference.DARK;
		activityScenarioRule.getScenario().recreate();

		onView(isRoot()).perform(waitId(R.id.more, LOAD_WAIT_TIMEOUT));
		Thread.sleep(500);
		takeScreenshot("HomeTimeline_Dark");

		GlobalUserPreferences.theme=GlobalUserPreferences.ThemePreference.LIGHT;
		activityScenarioRule.getScenario().recreate();

		activityScenarioRule.getScenario().onActivity(activity->UiUtils.openProfileByID(activity, session.getID(), args.getString("profileAccountID")));
		Thread.sleep(500);
		onView(isRoot()).perform(waitId(R.id.avatar_border, LOAD_WAIT_TIMEOUT)); // wait for profile to load
		onView(isRoot()).perform(waitId(R.id.more, LOAD_WAIT_TIMEOUT)); // wait for timeline to load
		Thread.sleep(500);
		takeScreenshot("Profile");

		Status[] _status={null};
		CyclicBarrier barrier=new CyclicBarrier(2);
		new GetStatusByID(args.getString("threadPostID"))
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Status result){
						_status[0]=result;
						try{
							barrier.await();
						}catch(Exception ignore){}
					}

					@Override
					public void onError(ErrorResponse error){
						try{
							barrier.await();
						}catch(Exception ignore){}
					}
				})
				.exec(session.getID());
		barrier.await();
		Assert.assertNotNull(_status[0]);

		ThreadFragment[] _fragment={null};
		activityScenarioRule.getScenario().onActivity(activity->{
			activity.getSystemService(InputMethodManager.class).hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
			Bundle threadArgs=new Bundle();
			threadArgs.putParcelable("status", Parcels.wrap(_status[0]));
			threadArgs.putString("account", session.getID());
			threadArgs.putBoolean("_can_go_back", true);
			ThreadFragment fragment=new ThreadFragment();
			fragment.setArguments(threadArgs);
			activity.showFragment(fragment);
			_fragment[0]=fragment;
		});
		while(!_fragment[0].loaded){
			Thread.sleep(50);
		}
		Thread.sleep(300);
		takeScreenshot("Thread");

		Instance[] _instance={null};
		new GetInstance()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Instance result){
						_instance[0]=result;
						try{
							barrier.await();
						}catch(Exception ignore){}
					}

					@Override
					public void onError(ErrorResponse error){
						try{
							barrier.await();
						}catch(Exception ignore){}
					}
				})
				.execNoAuth("mastodon.social");
		barrier.await();
		Assert.assertNotNull(_instance[0]);

		activityScenarioRule.getScenario().onActivity(activity->{
			Bundle rulesArgs=new Bundle();
			rulesArgs.putParcelable("instance", Parcels.wrap(_instance[0]));
			InstanceRulesFragment fragment=new InstanceRulesFragment();
			fragment.setArguments(rulesArgs);
			activity.showFragment(fragment);
		});

		Thread.sleep(500);
		takeScreenshot("InstanceRules");

		activityScenarioRule.getScenario().onActivity(activity->{
			activity.onBackPressed();
			Bundle composeArgs=new Bundle();
			composeArgs.putString("account", session.getID());
			ComposeFragment fragment=new ComposeFragment();
			fragment.setArguments(composeArgs);
			activity.showFragment(fragment);
			fragment.addFakeMediaAttachment(Uri.fromFile(photo), "Pantheon");
		});
		onView(withId(R.id.toot_text)).perform(typeText("This is a picture I took the last time I visited #Athens, Greece. What a beautiful place!"));
		InstrumentationRegistry.getInstrumentation().setInTouchMode(true);
		takeScreenshot("Compose");
		GlobalUserPreferences.theme=GlobalUserPreferences.ThemePreference.DARK;
		activityScenarioRule.getScenario().recreate();
		Thread.sleep(500);
		takeScreenshot("Compose_Dark");
		GlobalUserPreferences.theme=GlobalUserPreferences.ThemePreference.LIGHT;
		activityScenarioRule.getScenario().recreate();
	}

	private void takeScreenshot(String name) throws IOException{
		Screenshot.capture().setName(name).setFormat(Bitmap.CompressFormat.PNG).process();
	}

	/**
	 * Perform action of waiting for a specific view id.
	 * @param viewId The id of the view to wait for.
	 * @param millis The timeout of until when to wait for.
	 */
	public static ViewAction waitId(final int viewId, final long millis) {
		return new ViewAction() {
			@Override
			public Matcher<View> getConstraints() {
				return isRoot();
			}

			@Override
			public String getDescription() {
				return "wait for a specific view with id <" + viewId + "> during " + millis + " millis.";
			}

			@Override
			public void perform(final UiController uiController, final View view) {
				uiController.loopMainThreadUntilIdle();
				final long startTime = System.currentTimeMillis();
				final long endTime = startTime + millis;
				final Matcher<View> viewMatcher=CoreMatchers.allOf(withId(viewId), withEffectiveVisibility(Visibility.VISIBLE));

				do {
					for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
						// found view with required ID
						if (viewMatcher.matches(child)) {
							return;
						}
					}

					uiController.loopMainThreadForAtLeast(50);
				}
				while (System.currentTimeMillis() < endTime);

				// timeout happens
				throw new PerformException.Builder()
						.withActionDescription(this.getDescription())
						.withViewDescription(HumanReadables.describe(view))
						.withCause(new TimeoutException())
						.build();
			}
		};
	}

}
