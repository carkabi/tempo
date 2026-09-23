package com.cappielloantonio.tempo.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import com.cappielloantonio.tempo.BuildConfig;
import com.cappielloantonio.tempo.repository.peach.models.PeachUpdate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PeachUpdateDownloader {

    public interface DownloadCallback {
        void onProgress(int progress, long bytesRead, long totalBytes);
        void onSuccess(File apkFile);
        void onError(String message);
    }

    public static boolean isValidTropikeauUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        try {
            Uri uri = Uri.parse(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && "tropikeau.fr".equalsIgnoreCase(uri.getHost());
        } catch (Exception e) {
            return false;
        }
    }

    public static String calculateSha256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(file);
            byte[] byteArray = new byte[8192];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            fis.close();
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean verifyDownloadedApk(Context context, File apkFile, PeachUpdate update) {
        if (apkFile == null || !apkFile.exists() || update == null) {
            return false;
        }

        // 1. Size check
        if (update.getSizeBytes() != null && update.getSizeBytes() > 0) {
            if (apkFile.length() != update.getSizeBytes()) {
                apkFile.delete();
                return false;
            }
        }

        // 2. SHA-256 check
        if (update.getSha256() != null && !update.getSha256().isEmpty()) {
            String calculatedHash = calculateSha256(apkFile);
            if (calculatedHash == null || !calculatedHash.equalsIgnoreCase(update.getSha256())) {
                apkFile.delete();
                return false;
            }
        }

        // 3. Package name check via PackageManager
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
            if (pkgInfo == null || !BuildConfig.APPLICATION_ID.equals(pkgInfo.packageName)) {
                apkFile.delete();
                return false;
            }
        } catch (Exception e) {
            apkFile.delete();
            return false;
        }

        return true;
    }

    public static void downloadAndVerify(Context context, PeachUpdate update, DownloadCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        if (update == null || update.getDownloadUrl() == null) {
            callback.onError("Mise à jour invalide.");
            return;
        }

        if (!isValidTropikeauUrl(update.getDownloadUrl())) {
            callback.onError("URL de téléchargement non sécurisée ou non autorisée.");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            File updatesDir = new File(context.getFilesDir(), "peach_updates");
            if (!updatesDir.exists()) {
                updatesDir.mkdirs();
            }

            File apkFile = new File(updatesDir, "peach-update-" + update.getVersionCode() + ".apk");
            if (apkFile.exists()) {
                apkFile.delete();
            }

            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(update.getDownloadUrl())
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    mainHandler.post(() -> callback.onError("Erreur serveur lors du téléchargement (code " + response.code() + ")."));
                    return;
                }

                ResponseBody body = response.body();
                long contentLength = body.contentLength();
                if (update.getSizeBytes() != null && update.getSizeBytes() > 0) {
                    contentLength = update.getSizeBytes();
                }

                InputStream is = body.byteStream();
                OutputStream os = new FileOutputStream(apkFile);

                byte[] buffer = new byte[8192];
                long totalRead = 0;
                int read;
                int lastProgress = -1;

                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                    totalRead += read;

                    if (contentLength > 0) {
                        int progress = (int) ((totalRead * 100) / contentLength);
                        if (progress != lastProgress) {
                            lastProgress = progress;
                            long currentRead = totalRead;
                            long total = contentLength;
                            mainHandler.post(() -> callback.onProgress(progress, currentRead, total));
                        }
                    }
                }

                os.flush();
                os.close();
                is.close();

                if (!verifyDownloadedApk(context, apkFile, update)) {
                    mainHandler.post(() -> callback.onError("Contrôle de sécurité de l'APK échoué. Le fichier a été supprimé."));
                    return;
                }

                mainHandler.post(() -> callback.onSuccess(apkFile));

            } catch (Exception e) {
                if (apkFile.exists()) {
                    apkFile.delete();
                }
                mainHandler.post(() -> callback.onError("Échec du téléchargement : " + e.getMessage()));
            }
        });
    }

    public static boolean installApk(Context context, File apkFile) {
        if (apkFile == null || !apkFile.exists()) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                settingsIntent.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(settingsIntent);
                return false;
            }
        }

        try {
            Uri apkUri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    apkFile
            );

            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(installIntent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
