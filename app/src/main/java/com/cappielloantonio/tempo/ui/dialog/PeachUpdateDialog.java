package com.cappielloantonio.tempo.ui.dialog;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;
import androidx.media3.common.util.UnstableApi;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.repository.peach.models.PeachUpdate;
import com.cappielloantonio.tempo.util.PeachUpdateDownloader;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;

@UnstableApi
public class PeachUpdateDialog extends DialogFragment {

    private static final String STATE_PENDING_APK_PATH = "pending_apk_path";
    private static final String STATE_HAS_ATTEMPTED_INSTALL = "has_attempted_install";

    private final PeachUpdate update;
    private String pendingApkPath;
    private boolean hasAttemptedInstall = false;

    public PeachUpdateDialog(PeachUpdate update) {
        this.update = update;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingApkPath != null) {
            outState.putString(STATE_PENDING_APK_PATH, pendingApkPath);
        }
        outState.putBoolean(STATE_HAS_ATTEMPTED_INSTALL, hasAttemptedInstall);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_peach_update, null);

        if (savedInstanceState != null) {
            pendingApkPath = savedInstanceState.getString(STATE_PENDING_APK_PATH, null);
            hasAttemptedInstall = savedInstanceState.getBoolean(STATE_HAS_ATTEMPTED_INSTALL, false);
        }

        // If pendingApkPath is not set, check if expected APK exists on disk from a previous run
        if (pendingApkPath == null && update != null && getContext() != null) {
            File updatesDir = new File(requireContext().getFilesDir(), "peach_updates");
            File expectedApk = new File(updatesDir, "peach-update-" + update.getVersionCode() + ".apk");
            if (expectedApk.exists()) {
                if (PeachUpdateDownloader.verifyDownloadedApk(requireContext(), expectedApk, update)) {
                    pendingApkPath = expectedApk.getAbsolutePath();
                } else {
                    if (expectedApk.exists()) expectedApk.delete();
                }
            }
        }

        TextView titleView = view.findViewById(R.id.update_title);
        TextView versionView = view.findViewById(R.id.update_version);
        TextView changelogView = view.findViewById(R.id.update_changelog);
        View progressGroup = view.findViewById(R.id.update_progress_group);
        TextView progressLabel = view.findViewById(R.id.update_progress_label);
        LinearProgressIndicator progressBar = view.findViewById(R.id.update_progress_bar);
        Button laterButton = view.findViewById(R.id.update_later_button);
        Button downloadButton = view.findViewById(R.id.update_download_button);

        if (update != null) {
            if (titleView != null) titleView.setText(update.getTitle());
            if (versionView != null) versionView.setText("Version " + update.getVersionName());
            if (changelogView != null && update.getChangelog() != null) {
                changelogView.setText(HtmlCompat.fromHtml(update.getChangelog(), HtmlCompat.FROM_HTML_MODE_COMPACT));
            }
        }

        boolean isMandatory = update != null && update.isMandatory();

        if (laterButton != null) {
            laterButton.setVisibility(isMandatory ? View.GONE : View.VISIBLE);
            laterButton.setOnClickListener(v -> {
                clearPendingApk();
                dismiss();
            });
        }

        if (pendingApkPath != null && downloadButton != null) {
            downloadButton.setText("Installer la mise à jour");
        }

        if (downloadButton != null) {
            downloadButton.setOnClickListener(v -> {
                if (pendingApkPath != null) {
                    File apkFile = new File(pendingApkPath);
                    if (PeachUpdateDownloader.verifyDownloadedApk(requireContext(), apkFile, update)) {
                        hasAttemptedInstall = true;
                        PeachUpdateDownloader.installApk(requireContext(), apkFile);
                    } else {
                        clearPendingApk();
                        Toast.makeText(getContext(), "Fichier APK corrompu ou supprimé. Veuillez télécharge à nouveau.", Toast.LENGTH_LONG).show();
                        downloadButton.setText("Mettre à jour");
                    }
                } else {
                    startDownload(downloadButton, laterButton, progressGroup, progressLabel, progressBar);
                }
            });
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
                .setView(view)
                .setCancelable(!isMandatory);

        AlertDialog dialog = builder.create();
        if (isMandatory) {
            dialog.setCanceledOnTouchOutside(false);
        }

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pendingApkPath != null && getContext() != null) {
            File apkFile = new File(pendingApkPath);
            if (!PeachUpdateDownloader.verifyDownloadedApk(requireContext(), apkFile, update)) {
                clearPendingApk();
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                boolean canInstall = requireContext().getPackageManager().canRequestPackageInstalls();
                if (canInstall && !hasAttemptedInstall) {
                    hasAttemptedInstall = true;
                    boolean installed = PeachUpdateDownloader.installApk(requireContext(), apkFile);
                    if (installed && (update == null || !update.isMandatory())) {
                        dismiss();
                    }
                }
            } else if (!hasAttemptedInstall) {
                hasAttemptedInstall = true;
                boolean installed = PeachUpdateDownloader.installApk(requireContext(), apkFile);
                if (installed && (update == null || !update.isMandatory())) {
                    dismiss();
                }
            }
        }
    }

    private void clearPendingApk() {
        if (pendingApkPath != null) {
            File file = new File(pendingApkPath);
            if (file.exists()) {
                file.delete();
            }
            pendingApkPath = null;
        }
        hasAttemptedInstall = false;
    }

    private void startDownload(Button downloadBtn, Button laterBtn, View progressGroup, TextView progressLabel, LinearProgressIndicator progressBar) {
        if (downloadBtn != null) downloadBtn.setVisibility(View.GONE);
        if (laterBtn != null) laterBtn.setVisibility(View.GONE);
        if (progressGroup != null) progressGroup.setVisibility(View.VISIBLE);
        if (progressLabel != null) progressLabel.setText("Téléchargement de la mise à jour...");
        if (progressBar != null) {
            progressBar.setIndeterminate(true);
        }

        PeachUpdateDownloader.downloadAndVerify(requireContext(), update, new PeachUpdateDownloader.DownloadCallback() {
            @Override
            public void onProgress(int progress, long bytesRead, long totalBytes) {
                if (progressBar != null) {
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(progress);
                }
                if (progressLabel != null) {
                    progressLabel.setText("Téléchargement : " + progress + "%");
                }
            }

            @Override
            public void onSuccess(File apkFile) {
                pendingApkPath = apkFile.getAbsolutePath();
                hasAttemptedInstall = true;

                if (progressGroup != null) progressGroup.setVisibility(View.GONE);
                if (downloadBtn != null) {
                    downloadBtn.setText("Installer la mise à jour");
                    downloadBtn.setVisibility(View.VISIBLE);
                }

                boolean installed = PeachUpdateDownloader.installApk(requireContext(), apkFile);
                if (!installed && update != null && !update.isMandatory()) {
                    Toast.makeText(getContext(), "Veuillez autoriser l'installation pour continuer.", Toast.LENGTH_LONG).show();
                } else if (installed && (update == null || !update.isMandatory())) {
                    dismiss();
                }
            }

            @Override
            public void onError(String message) {
                clearPendingApk();
                if (progressGroup != null) progressGroup.setVisibility(View.GONE);
                if (downloadBtn != null) {
                    downloadBtn.setText("Réessayer");
                    downloadBtn.setVisibility(View.VISIBLE);
                }
                if (laterBtn != null && (update == null || !update.isMandatory())) {
                    laterBtn.setVisibility(View.VISIBLE);
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
