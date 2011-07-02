package org.androidappdev.wifiwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/* 
 * Code is largely based on
 * com.android.settings.widget.SettingsAppWidgetProvider
 * from Android 1.6 (git tag: android-1.6_r1)
 */
public class WifiAppWidgetProvider extends AppWidgetProvider {
	static final String TAG = "WifiAppWidgetProvider";

	private static final int BUTTON_WIFI = 0;

	private static final int STATE_DISABLED = 0;
	private static final int STATE_ENABLED = 1;
	private static final int STATE_INTERMEDIATE = 2;

	static final ComponentName THIS_APPWIDGET = new ComponentName(
			"org.androidappdev.wifiwidget",
			"org.androidappdev.wifiwidget.WifiAppWidgetProvider");

	private static int previousState;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		WifiAppWidgetProvider.previousState = getWifiState(context);
		// Update each requested appWidgetId
		RemoteViews view = buildUpdate(context, -1);

		for (int i = 0; i < appWidgetIds.length; i++) {
			appWidgetManager.updateAppWidget(appWidgetIds[i], view);
		}
	}

	/**
	 * Receives and processes a button pressed intent or state change.
	 * 
	 * @param context
	 * @param intent
	 *            Indicates the pressed button.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
			Uri data = intent.getData();
			int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
			if (buttonId == BUTTON_WIFI) {
				toggleWifi(context);
			}
		}

		// State changes fall through
		updateWidget(context);
	}

	/**
	 * Check if we're connected
	 * 
	 * @param context
	 * @return true if we're connected, false otherwise
	 */
	private static boolean isConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = null;
		if (cm != null) {
			networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		}
		return networkInfo == null ? false : networkInfo.isConnected();
	}

	/**
	 * Toggles the state of Wi-Fi
	 * 
	 * @param context
	 */
	private void toggleWifi(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		int wifiState = getWifiState(context);
		if (wifiState == STATE_ENABLED) {
			wifiManager.setWifiEnabled(false);
		} else if (wifiState == STATE_DISABLED) {
			wifiManager.setWifiEnabled(true);
		}
	}

	/**
	 * Gets the state of Wi-Fi
	 * 
	 * @param context
	 * @return STATE_ENABLED, STATE_DISABLED, or STATE_INTERMEDIATE
	 */
	private static int getWifiState(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		int wifiState = wifiManager.getWifiState();
		if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
			return STATE_DISABLED;
		} else if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
			return STATE_ENABLED;
		} else {
			return STATE_INTERMEDIATE;
		}
	}

	/**
	 * Updates the widget when something changes, or when a button is pushed.
	 * 
	 * @param context
	 */
	public static void updateWidget(Context context) {
		RemoteViews views = buildUpdate(context, -1);
		// Update specific list of appWidgetIds if given,
		// otherwise default to all
		final AppWidgetManager gm = AppWidgetManager.getInstance(context);
		gm.updateAppWidget(THIS_APPWIDGET, views);
	}

	/**
	 * Load image for given widget and build {@link RemoteViews} for it.
	 */
	static RemoteViews buildUpdate(Context context, int appWidgetId) {
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.main);
		views.setOnClickPendingIntent(R.id.btn_wifi, getLaunchPendingIntent(
				context, appWidgetId, BUTTON_WIFI));
		Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
		PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
		views.setOnClickPendingIntent(R.id.btn_ssid, pi);
		updateButtons(views, context);
		return views;
	}

	/**
	 * Creates PendingIntent to notify the widget of a button click.
	 * 
	 * @param context
	 * @param appWidgetId
	 * @return
	 */
	private static PendingIntent getLaunchPendingIntent(Context context,
			int appWidgetId, int buttonId) {
		Intent launchIntent = new Intent();
		launchIntent.setClass(context, WifiAppWidgetProvider.class);
		launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		launchIntent.setData(Uri.parse("custom:" + buttonId));
		PendingIntent pi = PendingIntent.getBroadcast(context, 0 /*
																 * no
																 * requestCode
																 */,
				launchIntent, 0 /* no flags */);
		return pi;
	}

	/**
	 * Updates the buttons based on the underlying states of wifi, etc.
	 * 
	 * @param views
	 *            The RemoteViews to update.
	 * @param context
	 */
	private static void updateButtons(RemoteViews views, Context context) {
		switch (getWifiState(context)) {
		case STATE_DISABLED:
			views.setImageViewResource(R.id.img_wifi,
					R.drawable.ic_appwidget_settings_wifi_off);
			views.setViewVisibility(R.id.btn_ssid, View.GONE);
			WifiAppWidgetProvider.previousState = STATE_DISABLED;
			break;
		case STATE_ENABLED:
			views.setImageViewResource(R.id.img_wifi,
					R.drawable.ic_appwidget_settings_wifi_on);
			if (isConnected(context)) {
				Log.d(TAG, "isConnected(context)");
				String ssid = getSSID(context);
				Log.d(TAG, "ssid = " + ssid);
				setSSID(views, ssid);
			} else {
				views.setTextViewText(R.id.ssid, context
						.getText(R.string.network_list));
			}
			views.setViewVisibility(R.id.btn_ssid, View.VISIBLE);
			WifiAppWidgetProvider.previousState = STATE_ENABLED;
			break;
		case STATE_INTERMEDIATE:
			if (WifiAppWidgetProvider.previousState == STATE_DISABLED) {
				views.setImageViewResource(R.id.img_wifi,
						R.drawable.ic_appwidget_settings_wifi_on);
			} else {
				views.setImageViewResource(R.id.img_wifi,
						R.drawable.ic_appwidget_settings_wifi_off);
			}
			break;
		}
	}

	/**
	 * SSID of the Wi-Fi network we're connected to
	 * 
	 * @param context
	 * @return the SSID of the Wi-Fi network we're connected to
	 */
	private static String getSSID(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		return wifiManager.getConnectionInfo().getSSID();
	}

	private static void setSSID(RemoteViews views, String ssid) {
		views.setTextViewText(R.id.ssid, ssid);
	}

}
