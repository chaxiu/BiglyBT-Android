package com.vuze.android.remote.activity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.*;
import android.widget.SearchView;

import com.vuze.android.remote.AndroidUtils;
import com.vuze.android.remote.R;
import com.vuze.android.remote.VuzeEasyTracker;

/**
 * TODO: handle torrent download better
 * TODO: give view enough width (zoom out)
 */
@SuppressLint("SetJavaScriptEnabled")
public class MetaSearch
	extends FragmentActivity
{
	private WebView myWebView;

	private SearchView mSearchView;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setupHoneyComb();
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			setupIceCream();
		}

		Intent intent = getIntent();
		if (AndroidUtils.DEBUG) {
			System.out.println("metasearch intent = " + intent);
			System.out.println("Type:" + intent.getType() + ";"
					+ intent.getDataString());
		}

		setContentView(R.layout.activity_metasearch);

		myWebView = (WebView) findViewById(R.id.searchwebview);

		myWebView.clearCache(true);

		myWebView.setWebChromeClient(new WebChromeClient() {
			public void onConsoleMessage(String message, int lineNumber,
					String sourceID) {
				// Just in case FROYO and above call this for backwards compat reasons
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
					AndroidUtils.handleConsoleMessageFroyo(MetaSearch.this, message,
							sourceID, lineNumber);
				}
			}

			@TargetApi(Build.VERSION_CODES.FROYO)
			public boolean onConsoleMessage(ConsoleMessage cm) {
				AndroidUtils.handleConsoleMessageFroyo(MetaSearch.this, cm.message(),
						cm.sourceId(), cm.lineNumber());
				return true;
			}
		});

		myWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				try {
					Uri uri = Uri.parse(url);

					String host = uri.getHost().toLowerCase(Locale.getDefault());
					String path = uri.getPath().toLowerCase(Locale.getDefault());
					if (host.endsWith("vuze.com") && !path.contains(".torrent")) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
						startActivity(intent);
					} else {
						Intent myIntent = new Intent();
						myIntent.setClass(getApplicationContext(), EmbeddedWebRemote.class);
						myIntent.setAction(Intent.ACTION_VIEW);
						myIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						myIntent.setData(uri);

						startActivity(myIntent);
						finish();
					}
				} catch (Throwable t) {
					if (AndroidUtils.DEBUG) {
						t.printStackTrace();
					}
					VuzeEasyTracker.getInstance(MetaSearch.this).logError(
							MetaSearch.this, t);
				}
				return true;
			}
		});

		WebSettings webSettings = myWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setLightTouchEnabled(true);
		webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
		webSettings.setDomStorageEnabled(true);
		webSettings.setBuiltInZoomControls(true);
		webSettings.setUseWideViewPort(false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setupHoneyComb(webSettings);
		}

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			try {
				doMySearch(query);
			} catch (UnsupportedEncodingException e) {
				if (AndroidUtils.DEBUG) {
					e.printStackTrace();
				}
				VuzeEasyTracker.getInstance(this).logError(this, e);
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		VuzeEasyTracker.getInstance(this).activityStart(this);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupHoneyComb(WebSettings webSettings) {
		webSettings.setDisplayZoomControls(true);
	}

	private void doMySearch(String query)
			throws UnsupportedEncodingException {
		String strURL = "http://search.vuze.com/xsearch/?q="
				+ URLEncoder.encode(query, "utf-8")
				+ "&xdmv=no&source=android&mode=plus";

		Bundle appData = getIntent().getBundleExtra(SearchManager.APP_DATA);
		if (appData != null) {
			System.out.println("hasAppData=" + appData.toString());
			String searchSource = appData.getString("com.vuze.android.remote.searchsource");
			String ac = appData.getString("com.vuze.android.remote.ac");
			System.out.println("ss=" + searchSource + ";ac=" + ac);
			if (searchSource != null && ac != null) {
				strURL += "&search_source=" + URLEncoder.encode(searchSource, "utf-8")
						+ "&ac=" + URLEncoder.encode(ac, "utf-8");
			} else {
				strURL += "&search_source=web";
			}
		} else {
			strURL += "&search_source=web";
		}

		System.out.println("URL = " + strURL);

		myWebView.loadUrl(strURL);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setupIceCream() {
		getActionBar().setHomeButtonEnabled(true);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupHoneyComb() {
		// needed because one of our test machines won't listen to <item name="android:windowActionBar">true</item>
		requestWindowFeature(Window.FEATURE_ACTION_BAR);

		// enable ActionBar app icon to behave as action to toggle nav drawer
		ActionBar actionBar = getActionBar();
		if (actionBar == null) {
			System.err.println("actionBar is null");
			return;
		}

		// enable ActionBar app icon to behave as action to toggle nav drawer
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		pauseUI();
	}

	private void pauseUI() {
		if (!AndroidUtils.areWebViewsPaused() && myWebView != null) {
			//			myWebView.pauseTimers();
			//AndroidUtils.setWebViewsPaused(true);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		resumeUI();
	}

	private void resumeUI() {
		if (AndroidUtils.areWebViewsPaused() && myWebView != null) {
			//			myWebView.resumeTimers();
			//AndroidUtils.setWebViewsPaused(false);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		System.out.println("STOP MS");
		VuzeEasyTracker.getInstance(this).activityStop(this);
	}

	@Override
	protected void onDestroy() {
		// Ensure webview gets destroyed (reports are it doesn't!)
		// http://www.anddev.org/other-coding-problems-f5/webviewcorethread-problem-t10234.html
		myWebView.stopLoading();
		//		try {
		//			((ViewGroup) myWebView.getParent()).removeView(myWebView);
		//		} catch (Exception e) {
		//		}
		//		myWebView.destroy();

		myWebView.loadUrl("about:blank");

		super.onDestroy();
		System.out.println("onDestroy MS");
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				//goHome();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	private void goHome() {
		// This ID represents the Home or Up button. In the case of this
		// activity, the Up button is shown. Use NavUtils to allow users
		// to navigate up one level in the application structure. For
		// more details, see the Navigation pattern on Android Design:
		//
		// http://developer.android.com/design/patterns/navigation.html#up-vs-back
		//
		// Opens parent with FLAG_ACTIVITY_CLEAR_TOP
		// this doesn't work because we don't pass ac information..
		//NavUtils.navigateUpFromSameTask(this);
		//return true;

		/*
		Intent upIntent = NavUtils.getParentActivityIntent(this);
		System.out.println("upIntent = " + upIntent.toString());
		System.out.println("shouldUpRecreate? " + NavUtils.shouldUpRecreateTask(this, upIntent));
		if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
		    // This activity is NOT part of this app's task, so create a new task
		    // when navigating up, with a synthesized back stack.
		    TaskStackBuilder.create(this)
		            // Add all of this activity's parents to the back stack
		            .addNextIntentWithParentStack(upIntent)
		            // Navigate up to the closest parent
		            .startActivities();
		} else {
		    // This activity is part of this app's task, so simply
		    // navigate up to the logical parent activity.
		// Opens parent with FLAG_ACTIVITY_CLEAR_TOP
		// this doesn't work because we don't pass ac information..
		    NavUtils.navigateUpTo(this, upIntent);
		}
	}
	*/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.menu_search, menu);

		//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		//			MenuItem searchItem = menu.findItem(R.id.action_search);
		//			setupSearchView(searchItem);
		//		}

		return super.onCreateOptionsMenu(menu);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupSearchView(MenuItem searchItem) {
		mSearchView = (SearchView) searchItem.getActionView();

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		mSearchView.setIconifiedByDefault(true);

		//		searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	}
}
