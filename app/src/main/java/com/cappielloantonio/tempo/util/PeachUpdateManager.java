package com.cappielloantonio.tempo.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import com.cappielloantonio.tempo.BuildConfig;
import com.cappielloantonio.tempo.repository.peach.PeachRepository;
import com.cappielloantonio.tempo.repository.peach.models.PeachBootstrapResponse;
import com.cappielloantonio.tempo.repository.peach.models.PeachUpdate;
import com.cappielloantonio.tempo.ui.dialog.PeachUpdateDialog;

import java.util.concurrent.atomic.AtomicBoolean;

public final class PeachUpdateManager {
    public static final String EXTRA_OPEN_UPDATE = "peach_open_update";

    private static final String PREFS = "peach_update_manager";
    private static final String KEY_LAST_PROMPT_CODE = "last_prompt_code";
    private static final String KEY_LAST_PROMPT_AT = "last_prompt_at";
    private static final long REPROMPT_DELAY_MS = 24L * 60L * 60L * 1000L;

    private static final AtomicBoolean CHECKING = new AtomicBoolean(false);

    private static volatile PeachUpdate pendingUpdate;
    private static volatile boolean pendingForceDisplay;

    private PeachUpdateManager() {
    }
    public static void check(
            @NonNull FragmentActivity activity,
            boolean forceDisplay
    ) {
        if (!"peach".equals(BuildConfig.FLAVOR)) {
            return;
        }

        if (!CHECKING.compareAndSet(false, true)) {
            return;
        }

        new PeachRepository().bootstrap(new PeachRepository.PeachCallback() {
            @Override
            public void onSuccess(PeachBootstrapResponse response) {
                CHECKING.set(false);

                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                PeachUpdate update = response != null
                        ? response.getUpdate()
                        : null;

                boolean appMatches = response != null
                        && response.getApplication() != null
                        && BuildConfig.APPLICATION_ID.equals(
                        response.getApplication().getAndroidApplicationId()
                );

                if (!appMatches
                        || update == null
                        || !update.isAvailable()
                        || update.getVersionCode() <= BuildConfig.VERSION_CODE) {
                    return;
                }

                if (!forceDisplay
                        && !shouldPrompt(activity, update)) {
                    return;
                }

                pendingUpdate = update;
                pendingForceDisplay = forceDisplay;

                showPendingIfPossible(activity);
            }

            @Override
            public void onError(int code, String message) {
                CHECKING.set(false);
            }
        });
    }
    public static void showPendingIfPossible(
            @NonNull FragmentActivity activity
    ) {
        PeachUpdate update = pendingUpdate;

        if (update == null
                || activity.isFinishing()
                || activity.isDestroyed()) {
            return;
        }

        if (!activity.getLifecycle()
                .getCurrentState()
                .isAtLeast(Lifecycle.State.RESUMED)) {
            return;
        }

        if (activity.getSupportFragmentManager().isStateSaved()) {
            return;
        }

        if (activity.getSupportFragmentManager()
                .findFragmentByTag("PeachUpdateDialog") != null) {
            pendingUpdate = null;
            pendingForceDisplay = false;
            return;
        }

        if (!pendingForceDisplay
                && !shouldPrompt(activity, update)) {
            pendingUpdate = null;
            return;
        }

        rememberPrompt(activity, update);

        pendingUpdate = null;
        pendingForceDisplay = false;

        PeachUpdateDialog.newInstance(update).show(
                activity.getSupportFragmentManager(),
                "PeachUpdateDialog"
        );
    }

    public static boolean consumeUpdateIntent(Activity activity) {
        if (activity == null || activity.getIntent() == null) {
            return false;
        }

        boolean openUpdate = activity.getIntent().getBooleanExtra(
                EXTRA_OPEN_UPDATE,
                false
        );

        if (openUpdate) {
            activity.getIntent().removeExtra(EXTRA_OPEN_UPDATE);
        }

        return openUpdate;
    }

    private static boolean shouldPrompt(
            Context context,
            PeachUpdate update
    ) {
        if (update.isMandatory()) {
            return true;
        }

        SharedPreferences prefs = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
        );

        int lastCode = prefs.getInt(KEY_LAST_PROMPT_CODE, -1);
        long lastAt = prefs.getLong(KEY_LAST_PROMPT_AT, 0L);

        if (lastCode != update.getVersionCode()) {
            return true;
        }

        return System.currentTimeMillis() - lastAt >= REPROMPT_DELAY_MS;
    }

    private static void rememberPrompt(
            Context context,
            PeachUpdate update
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_LAST_PROMPT_CODE, update.getVersionCode())
                .putLong(KEY_LAST_PROMPT_AT, System.currentTimeMillis())
                .apply();
    }
}
