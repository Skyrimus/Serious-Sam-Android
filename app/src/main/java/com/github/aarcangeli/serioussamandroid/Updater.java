package com.github.aarcangeli.serioussamandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Updater {
	private static final String TAG = "Updater";
	private static final String UPDATE_INFO_URL = "https://raw.githubusercontent.com/Skyrimus/Serious-Sam-Android/master/update.json";
	private static final int CONNECT_TIMEOUT_MS = 15000;
	private static final int READ_TIMEOUT_MS = 30000;

	public int latestVersionCode;
	private final WeakReference<Context> contextRef;
	private final int curVersionCode;

	public Updater(Context context, int curVer) {
		this.contextRef = new WeakReference<>(context);
		this.curVersionCode = curVer;
	}

	private Context getContext() {
		return contextRef.get();
	}

	private Context getAppContext() {
		Context context = getContext();
		return context != null ? context.getApplicationContext() : null;
	}

	private void logError(String message, Throwable error) {
		Log.w(TAG, message, error);
	}

	private void showToast(int resId) {
		Context context = getContext();
		if (context != null) {
			Toast.makeText(context, resId, Toast.LENGTH_LONG).show();
		}
	}

	private boolean canShowDialog() {
		Context context = getContext();
		if (!(context instanceof Activity)) {
			return false;
		}
		Activity activity = (Activity) context;
		if (activity.isFinishing()) {
			return false;
		}
		return Build.VERSION.SDK_INT < 17 || !activity.isDestroyed();
	}

	private String getUpdateUrl(JSONObject response) throws JSONException {
		if (BuildConfig.home.endsWith("TSE")) {
			return response.getString("url_tse");
		}
		return response.getString("url_tfe");
	}

	private int getVersionCode(JSONObject response) throws JSONException {
		Object value = response.get("versionCode");
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return Integer.parseInt(String.valueOf(value));
	}

	private File getDownloadDirectory(Context context) {
		File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
		if (dir == null) {
			dir = new File(context.getCacheDir(), "updates");
		}
		if (!dir.exists() && !dir.mkdirs()) {
			return null;
		}
		return dir;
	}

	private File getApkFile(Context context) {
		File dir = getDownloadDirectory(context);
		if (dir == null) {
			return null;
		}
		String fileName = context.getPackageName() + "-" + BuildConfig.VERSION_NAME + "-update.apk";
		return new File(dir, fileName);
	}

	private boolean canRequestPackageInstalls(Context context) {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.O
				|| context.getPackageManager().canRequestPackageInstalls();
	}

	private void openUnknownSourcesSettings(Context context) {
		try {
			Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
					Uri.parse("package:" + context.getPackageName()));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		} catch (Exception error) {
			logError("Failed to open unknown app sources settings", error);
		}
	}

	private void installDownloadedApk(File apkFile) {
		Context context = getAppContext();
		if (context == null) {
			return;
		}
		if (!apkFile.exists() || apkFile.length() <= 0) {
			showToast(R.string.updater_download_failed);
			return;
		}
		if (!canRequestPackageInstalls(context)) {
			showToast(R.string.updater_enable_unknown_sources);
			openUnknownSourcesSettings(context);
			return;
		}

		Uri uri;
		try {
			uri = FileProvider.getUriForFile(context,
					context.getPackageName() + ".provider",
					apkFile);
		} catch (IllegalArgumentException error) {
			logError("Failed to build FileProvider uri for update apk", error);
			showToast(R.string.updater_install_failed);
			return;
		}

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(uri, "application/vnd.android.package-archive");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

		PackageManager packageManager = context.getPackageManager();
		if (intent.resolveActivity(packageManager) == null) {
			showToast(R.string.updater_install_failed);
			return;
		}
		try {
			context.startActivity(intent);
		} catch (Exception error) {
			logError("Failed to launch package installer", error);
			showToast(R.string.updater_install_failed);
		}
	}

	private HttpURLConnection openConnection(String urlString) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
		connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(READ_TIMEOUT_MS);
		connection.setUseCaches(false);
		connection.setInstanceFollowRedirects(true);
		return connection;
	}

	private UpdateInfo fetchUpdateInfo() throws IOException, JSONException {
		HttpURLConnection connection = null;
		try {
			connection = openConnection(UPDATE_INFO_URL);
			connection.connect();
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new IOException("Unexpected HTTP " + connection.getResponseCode());
			}

			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
				StringBuilder buffer = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					buffer.append(line);
				}

				JSONObject response = new JSONObject(buffer.toString());
				UpdateInfo info = new UpdateInfo();
				info.updateUrl = getUpdateUrl(response);
				if (info.updateUrl == null || info.updateUrl.trim().isEmpty()) {
					throw new IOException("Update url is empty");
				}
				info.versionCode = getVersionCode(response);
				return info;
			}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private File downloadApk(String urlString) throws IOException {
		Context context = getAppContext();
		if (context == null) {
			throw new IOException("Context is gone");
		}

		File apkFile = getApkFile(context);
		if (apkFile == null) {
			throw new IOException("Cannot access update directory");
		}
		File tempFile = new File(apkFile.getAbsolutePath() + ".download");

		HttpURLConnection connection = null;
		try {
			connection = openConnection(urlString);
			connection.connect();
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				throw new IOException("Unexpected HTTP " + connection.getResponseCode());
			}

			int contentLength = connection.getContentLength();
			long total = 0;

			try (InputStream input = new BufferedInputStream(connection.getInputStream());
				 FileOutputStream output = new FileOutputStream(tempFile, false)) {
				byte[] data = new byte[8192];
				int count;
				while ((count = input.read(data)) != -1) {
					total += count;
					output.write(data, 0, count);
				}
				output.getFD().sync();
			}

			if (total <= 0) {
				throw new IOException("Downloaded file is empty");
			}
			if (contentLength > 0 && total < contentLength) {
				throw new IOException("Downloaded file is incomplete");
			}

			if (apkFile.exists() && !apkFile.delete()) {
				throw new IOException("Cannot replace previous apk");
			}
			if (!tempFile.renameTo(apkFile)) {
				throw new IOException("Cannot finalize downloaded apk");
			}
			return apkFile;
		} catch (IOException error) {
			if (tempFile.exists()) {
				//noinspection ResultOfMethodCallIgnored
				tempFile.delete();
			}
			throw error;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private void promptForUpdate(final UpdateInfo info) {
		if (!canShowDialog()) {
			return;
		}
		Context context = getContext();
		if (context == null) {
			return;
		}

		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(R.string.updater_title);
		dialog.setMessage(R.string.updater_message);
		dialog.setPositiveButton(R.string.updater_download, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int which) {
				new DownloadUpdate().execute(info.updateUrl);
			}
		});
		dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int which) {
				dialogInterface.dismiss();
			}
		});
		dialog.show();
	}

	private static final class UpdateInfo {
		int versionCode;
		String updateUrl;
	}

	public class DownloadUpdate extends AsyncTask<String, Integer, File> {
		private Exception error;

		@Override
		protected File doInBackground(String... urls) {
			if (urls == null || urls.length == 0 || urls[0] == null || urls[0].isEmpty()) {
				error = new IllegalArgumentException("Missing update url");
				return null;
			}
			try {
				return downloadApk(urls[0]);
			} catch (Exception exception) {
				error = exception;
				return null;
			}
		}

		@Override
		protected void onPostExecute(File apkFile) {
			super.onPostExecute(apkFile);
			if (apkFile == null) {
				logError("Update download failed", error);
				showToast(R.string.updater_download_failed);
				return;
			}
			installDownloadedApk(apkFile);
		}
	}

	public class ReadFileTask extends AsyncTask<Void, Void, UpdateInfo> {
		private Exception error;

		@Override
		protected UpdateInfo doInBackground(Void... params) {
			try {
				return fetchUpdateInfo();
			} catch (Exception exception) {
				error = exception;
				return null;
			}
		}

		@Override
		protected void onPostExecute(UpdateInfo info) {
			super.onPostExecute(info);
			if (info == null) {
				logError("Failed to fetch update info", error);
				return;
			}

			latestVersionCode = info.versionCode;
			if (latestVersionCode > curVersionCode) {
				promptForUpdate(info);
			}
		}
	}
}
