package org.androidappdev.wifiwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.RemoteViews;

public class WifiAppWidgetProvider extends AppWidgetProvider {
	public static final String TAG = "WifiAppWidgetProvider";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
				R.layout.main);
		if (wifiManager.isWifiEnabled()) {
			remoteViews.setTextViewText(R.id.ssid, wifiManager
					.getConnectionInfo().getSSID());
		} else {
			remoteViews.setTextViewText(R.id.ssid, context
					.getText(R.string.disconnected));
		}
		ComponentName componentName = new ComponentName(context,
				WifiAppWidgetProvider.class);
		AppWidgetManager.getInstance(context).updateAppWidget(componentName,
				remoteViews);

	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
				R.layout.main);
		Log.d(TAG, intent.getAction());
		if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
			int wifiState = intent
					.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
			Log.d(TAG, new Integer(wifiState).toString());
			if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
				remoteViews.setTextViewText(R.id.ssid, context
						.getText(R.string.disconnected));
			} else if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
				WifiManager wifiManager = (WifiManager) context
						.getSystemService(Context.WIFI_SERVICE);
				remoteViews.setTextViewText(R.id.ssid, wifiManager
						.getConnectionInfo().getSSID());
			}
			ComponentName componentName = new ComponentName(context,
					WifiAppWidgetProvider.class);
			AppWidgetManager.getInstance(context).updateAppWidget(
					componentName, remoteViews);
		}
	}
}
