package com.github.aarcangeli.serioussamandroid;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import java.util.ArrayList;

public class SplashScreen extends Activity {
	public static final String TAG = "SeriousSamJava";
	private static final int REQ_STORAGE = 1001;
	private static final int REQ_NOTIF = 1002;
	private static final int REQ_MANAGE_STORAGE = 1003;
	private static final int SDK_TIRAMISU = 33;
	private static final String PERM_POST_NOTIF = "android.permission.POST_NOTIFICATIONS";
	private boolean started = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// speed up bootstrap if we have already storage permission
		//MainActivity.tryPremain(this);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash_layout);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!hasStoragePermission()) {
				requestStoragePermission();
				return;
			}
		}
		maybeRequestNotifications();

		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

		new Handler().postDelayed(new Runnable() {
			public void run() {
				startSeriousSam();
			}
		}, 1500);
	}

	private void startSeriousSam() {
		if (started) {
			return;
		}
		started = true;
		startActivity(new Intent(this, MainActivity.class));
		finish();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQ_STORAGE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				maybeRequestNotifications();
			} else {
				startSeriousSam();
			}
			return;
		}
		if (requestCode == REQ_NOTIF) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				NotificationHelper.maybeShowUpdateNotification(this);
			}
			startSeriousSam();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQ_MANAGE_STORAGE) {
			if (hasStoragePermission()) {
				maybeRequestNotifications();
			} else {
				startSeriousSam();
			}
		}
	}

	private void maybeRequestNotifications() {
		if (Build.VERSION.SDK_INT >= SDK_TIRAMISU) {
			if (checkSelfPermission(PERM_POST_NOTIF) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{PERM_POST_NOTIF}, REQ_NOTIF);
				return;
			}
		}
		NotificationHelper.maybeShowUpdateNotification(this);
		startSeriousSam();
	}

	private boolean hasStoragePermission() {
		if (Build.VERSION.SDK_INT >= 30) {
			return Environment.isExternalStorageManager();
		}
		return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}

	private void requestStoragePermission() {
		if (Build.VERSION.SDK_INT >= 30) {
			Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
			intent.setData(Uri.parse("package:" + getPackageName()));
			startActivityForResult(intent, REQ_MANAGE_STORAGE);
			return;
		}
		requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
	}

}
