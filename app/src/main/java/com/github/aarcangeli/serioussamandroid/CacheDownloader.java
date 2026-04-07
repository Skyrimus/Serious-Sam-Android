package com.github.aarcangeli.serioussamandroid;

import static com.github.aarcangeli.serioussamandroid.MainActivity.TAG;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.BaseDownloadTask;
import java.io.File;
import java.lang.ref.WeakReference;

public class CacheDownloader {
    public interface DownloadCallback {
        void onDownloadComplete(Boolean downloaded);
    }

    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private static final String GRO_TSE = "SE1_00.gro";
    private static final String GRO_TFE = "1_00c.gro";
    private static final String ZIP_TSE = "hgfjgfjfjtse.zip";
    private static final String ZIP_TFE = "sadfasfsafastfe.zip";
    private final Context appContext;
    private final WeakReference<Activity> activityRef;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static volatile boolean downloaderInitialized = false;

    public CacheDownloader(Activity activity) {
        this.appContext = activity.getApplicationContext();
        this.activityRef = new WeakReference<>(activity);
    }

    public AlertDialog.Builder getDialogProgressBar(String title, String message, Boolean cancelable) {

        Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) {
            return null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(cancelable);
        progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        progressBar.setLayoutParams(lp);
        builder.setView(progressBar);
        return builder;
    }

    @NonNull
    private static File getCheckFile() {
        String gro = getGroName();
        return new File(Environment.getExternalStorageDirectory(), BuildConfig.home + "/" + gro).getAbsoluteFile();
    }

    private static String getZipName() {
        return BuildConfig.home.endsWith("TFE") ? ZIP_TFE : ZIP_TSE;
    }

    private static String getGroName() {
        return BuildConfig.home.endsWith("TFE") ? GRO_TFE : GRO_TSE;
    }

    private static File getZipFile() {
        return new File(Environment.getExternalStorageDirectory(), BuildConfig.home + ".zip").getAbsoluteFile();
    }
    // Проверка папки, если папки нет вызов функции showDownloadDialog
    public void checkFolderAndDownloadFile(final DownloadCallback callback) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.wtf(TAG, "External storage not mounted");
            mainHandler.post(() -> Toast.makeText(appContext, "Ошибка: внешнее хранилище недоступно", Toast.LENGTH_LONG).show());
            callback.onDownloadComplete(false);
            return;
        }
        File fil = getCheckFile();
        if (!fil.exists()) {
            Log.wtf(TAG, "Gro not found, starting download");
            showDownloadDialog(callback);
        } else {
            Log.wtf(TAG, "Gro found, starting game!");
            callback.onDownloadComplete(false);
        }
    }
    // Диалоговое окно спрашивающее качаем или нет, если да - вызов функции downloadFile
    private void showDownloadDialog(final DownloadCallback callback) {
        Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) {
            callback.onDownloadComplete(false);
            return;
        }
        String gro = getGroName();
        new AlertDialog.Builder(activity)
                .setTitle("Файл " + gro + " не найден")
                .setMessage("Скачать и установить кэш " + BuildConfig.home + "?")
                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadFile(callback);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        callback.onDownloadComplete(false);
                    }
                })
                .show();
    }

    private void updateProgress(int progress) {
        if (progressBar != null) {
            progressBar.setProgress(progress);
        }
    }

    private void downloadFile(final DownloadCallback callback) {
        AlertDialog.Builder builder = getDialogProgressBar("Загрузка игровых данных", "Начинается скачивание", false);
        if (builder == null) {
            callback.onDownloadComplete(false);
            return;
        }
        progressDialog = builder.create();
        progressDialog.show();
        ensureDownloaderInitialized();
        new Thread(new Runnable() {
            public void run() {
                String url = "https://github.com/slowpoke1337one/zadacha44/releases/download/v1.0.0/" + getZipName();
                File zipFile = getZipFile();
                FileDownloader.getImpl().create(url)
                        .setPath(zipFile.getAbsolutePath())
                        .setListener(new FileDownloadListener() {
                            @Override
                            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                                Log.wtf(TAG, "Preparing to download " + getZipName());
                            }

                            @Override
                            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                                final int progress;
                                if (totalBytes > 0) {
                                    progress = (int) ((soFarBytes * 100L) / totalBytes);
                                } else {
                                    progress = -1;
                                }
                                mainHandler.post(() -> {
                                    if (progressDialog != null) {
                                        if (progress >= 0) {
                                            progressDialog.setMessage("Скачивание кэша: " + progress + "%");
                                            updateProgress(progress);
                                        } else {
                                            progressDialog.setMessage("Скачивание кэша...");
                                        }
                                    }
                                });
                            }

                            @Override
                            protected void completed(BaseDownloadTask task) {
                                Log.wtf(TAG, "Download completed!");
                                mainHandler.post(() -> {
                                    Toast.makeText(appContext, "Загрузка успешно завершена", Toast.LENGTH_LONG).show();
                                    if (progressDialog != null) {
                                        progressDialog.setMessage("Загрузка успешно завершена");
                                        progressDialog.dismiss();
                                    }
                                    callback.onDownloadComplete(true);
                                });
                            }


                            @Override
                            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                                //mozhet byt kogda nibut budet used
                            }

                            @Override
                            protected void error(BaseDownloadTask task, final Throwable e) {
                                e.printStackTrace();
                                Log.wtf(TAG, "Error: " + e.getMessage());
                                mainHandler.post(() -> {
                                    if (progressDialog != null) {
                                        progressDialog.setMessage(e.getMessage());
                                        progressDialog.dismiss();
                                    }
                                    Toast.makeText(appContext, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    callback.onDownloadComplete(false);
                                });
                            }

                            @Override
                            protected void warn(BaseDownloadTask task) {
                                //mozhet byt kogda nibut budet used
                            }
                        }).start();
            }
        }).start();
    }

    private void ensureDownloaderInitialized() {
        if (downloaderInitialized) {
            return;
        }
        synchronized (CacheDownloader.class) {
            if (!downloaderInitialized) {
                FileDownloader.setup(appContext);
                downloaderInitialized = true;
            }
        }
    }
}
