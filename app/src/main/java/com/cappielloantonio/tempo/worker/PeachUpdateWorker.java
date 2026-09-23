package com.cappielloantonio.tempo.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.cappielloantonio.tempo.BuildConfig;
import com.cappielloantonio.tempo.helper.PeachNotificationHelper;
import com.cappielloantonio.tempo.repository.peach.PeachRepository;
import com.cappielloantonio.tempo.repository.peach.models.PeachBootstrapResponse;
import com.cappielloantonio.tempo.repository.peach.models.PeachNotice;
import com.cappielloantonio.tempo.repository.peach.models.RadioManifestResponse;
import com.cappielloantonio.tempo.repository.peach.models.RadioPackageResponse;
import com.cappielloantonio.tempo.repository.tropikeau.TropikeauRepository;
import com.cappielloantonio.tempo.util.PeachRadioCache;
import com.cappielloantonio.tempo.util.Preferences;

import java.io.IOException;

@UnstableApi
public class PeachUpdateWorker extends Worker {

    public PeachUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!"peach".equals(BuildConfig.FLAVOR)) {
            return Result.success();
        }

        syncPeachRadiosSilently();

        try {
            PeachRepository repository = new PeachRepository();
            PeachBootstrapResponse response = repository.bootstrapSync();

            if (response != null) {
                processResponse(response);
            }
            return Result.success();

        } catch (IOException e) {
            // Temporary network error -> Retry
            return Result.retry();
        } catch (Exception e) {
            // Non-recoverable error -> Success (don't retry endlessly)
            return Result.success();
        }
    }

    private void syncPeachRadiosSilently() {
        try {
            TropikeauRepository tropikeauRepo = new TropikeauRepository();
            RadioManifestResponse manifest = tropikeauRepo.getRadioManifestSync();
            if (manifest != null) {
                String oldPkgId = PeachRadioCache.getPackageId();
                PeachRadioCache.saveManifest(manifest);

                String newPkgId = manifest.getPackageId();
                if (newPkgId != null && !newPkgId.equals(oldPkgId)) {
                    RadioPackageResponse pkg = tropikeauRepo.getRadioPackageSync();
                    if (pkg != null) {
                        PeachRadioCache.savePackage(pkg);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void processResponse(PeachBootstrapResponse response) {
        if (!Preferences.isPeachNotificationEnabled()) return;

        if (response.getApplication() != null && BuildConfig.APPLICATION_ID.equals(response.getApplication().getAndroidApplicationId())) {
            if (response.getUpdate() != null && response.getUpdate().isAvailable()) {
                if (response.getUpdate().getVersionCode() > BuildConfig.VERSION_CODE) {
                    String updateKey = "peach-update-" + response.getUpdate().getVersionCode();
                    PeachNotificationHelper.postNotification(
                            getApplicationContext(),
                            PeachNotificationHelper.CHANNEL_UPDATES,
                            100,
                            "Mise à jour de Peach disponible",
                            "La version " + response.getUpdate().getVersionName() + " est disponible.",
                            null,
                            updateKey
                    );
                }
            }
        }

        if (response.getNotices() != null) {
            for (PeachNotice notice : response.getNotices()) {
                if (notice.isSendNotification() && notice.getNotificationKey() != null) {
                    PeachNotificationHelper.postNotification(
                            getApplicationContext(),
                            PeachNotificationHelper.CHANNEL_ALERTS,
                            notice.getId(),
                            notice.getTitle(),
                            notice.getMessage(),
                            notice.getUrl(),
                            notice.getNotificationKey()
                    );
                }
            }
        }
    }
}
