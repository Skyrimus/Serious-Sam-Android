package com.github.aarcangeli.serioussamandroid;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class NotificationHelper {
	private static final String PREFS_NAME = "app_prefs";
	private static final String KEY_LAST_NOTIFIED_VERSION = "last_notified_version_code";
	private static final String KEY_DEVICE_ID = "notification_device_id";
	private static final String CHANNEL_ID = "updates";
	private static final int NOTIFICATION_ID = 1001;
	private static final String TARGET_VERSION = "1.05.4";
	private static final String REPORT_URL = "http://skyrimus.ru:25711/notify";

	private NotificationHelper() {}

	public static void maybeShowUpdateNotification(Context context) {
		String versionName = getVersionName(context);
		if (versionName == null || !versionName.startsWith(TARGET_VERSION)) {
			return;
		}

		int versionCode = getVersionCode(context);
		if (versionCode <= 0) {
			return;
		}

		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		int lastNotified = prefs.getInt(KEY_LAST_NOTIFIED_VERSION, -1);
		if (lastNotified == versionCode) {
			return;
		}

		createChannelIfNeeded(context);
		showNotification(context);
		reportNotificationDelivered(context, versionCode, versionName);

		prefs.edit().putInt(KEY_LAST_NOTIFIED_VERSION, versionCode).apply();
	}

	private static void createChannelIfNeeded(Context context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return;
		}
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) {
			return;
		}
		NotificationChannel channel = new NotificationChannel(
				CHANNEL_ID,
				context.getString(R.string.notification_channel_name),
				NotificationManager.IMPORTANCE_DEFAULT
		);
		channel.setDescription(context.getString(R.string.notification_channel_desc));
		nm.createNotificationChannel(channel);
	}

	private static void showNotification(Context context) {
		Uri tgUri = Uri.parse("https://t.me/serious_sam_classic_vr");
		Intent openTg = new Intent(Intent.ACTION_VIEW, tgUri);
		openTg.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			piFlags |= PendingIntent.FLAG_IMMUTABLE;
		}
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openTg, piFlags);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(context.getString(R.string.notification_title))
				.setContentText(context.getString(R.string.notification_text))
				.setAutoCancel(true)
				.setContentIntent(pendingIntent)
				.setPriority(NotificationCompat.PRIORITY_DEFAULT);

		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (nm != null) {
			nm.notify(NOTIFICATION_ID, builder.build());
		}
	}

	private static int getVersionCode(Context context) {
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				return (int) info.getLongVersionCode();
			}
			return info.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			return -1;
		}
	}

	private static String getVersionName(Context context) {
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
			return info.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			return null;
		}
	}

	private static String getOrCreateDeviceId(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		String id = prefs.getString(KEY_DEVICE_ID, null);
		if (id != null && !id.isEmpty()) {
			return id;
		}
		id = UUID.randomUUID().toString();
		prefs.edit().putString(KEY_DEVICE_ID, id).apply();
		return id;
	}

	private static void reportNotificationDelivered(Context context, int versionCode, String versionName) {
		final Context appContext = context.getApplicationContext();
		final String deviceId = getOrCreateDeviceId(appContext);
		final long nowMs = System.currentTimeMillis();

		new Thread(() -> {
			HttpURLConnection conn = null;
			try {
				URL url = new URL(REPORT_URL);
				conn = (HttpURLConnection) url.openConnection();
				conn.setConnectTimeout(8000);
				conn.setReadTimeout(8000);
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

				JSONObject payload = new JSONObject();
				payload.put("device_id", deviceId);
				payload.put("version_code", versionCode);
				payload.put("version_name", versionName);
				payload.put("notified_at", nowMs);

				byte[] data = payload.toString().getBytes(StandardCharsets.UTF_8);
				conn.setFixedLengthStreamingMode(data.length);
				try (OutputStream os = conn.getOutputStream()) {
					os.write(data);
				}
				conn.getResponseCode();
			} catch (Exception ignored) {
			} finally {
				if (conn != null) {
					conn.disconnect();
				}
			}
		}, "notify-report").start();
	}
}
