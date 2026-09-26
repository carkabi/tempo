package com.cappielloantonio.tempo.util;

import android.os.Build;
import android.util.Log;

import com.cappielloantonio.tempo.App;
import com.cappielloantonio.tempo.repository.peach.models.PeachPackageRadio;
import com.cappielloantonio.tempo.repository.peach.models.PeachRadioStation;
import com.cappielloantonio.tempo.repository.peach.models.RadioManifestResponse;
import com.cappielloantonio.tempo.repository.peach.models.RadioPackageResponse;
import com.cappielloantonio.tempo.repository.peach.models.RadioProgramItem;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PeachRadioCache {

    private static final String TAG = "PEACH_RADIO";

    private static final String PREF_PACKAGE_ID = "peach_radio_package_id";
    private static final String PREF_SERVER_OFFSET_MS = "peach_radio_server_offset_ms";
    private static final String PREF_LAST_SYNC_TS = "peach_radio_last_sync_ts";
    private static final String PREF_STATIONS_JSON = "peach_radio_stations_json";
    private static final String PREF_PACKAGE_JSON = "peach_radio_package_json";

    // Legacy keys for migration
    private static final String PREF_LEGACY_CURRENT_GRID = "peach_radio_current_grid_json";
    private static final String PREF_LEGACY_NEXT_GRID = "peach_radio_next_grid_json";

    private static final Gson gson = new Gson();

    static {
        migrateCorruptedLegacyCache();
    }

    public static synchronized void migrateCorruptedLegacyCache() {
        try {
            String legacyCurrentGrid = App.getInstance().getPreferences().getString(PREF_LEGACY_CURRENT_GRID, null);
            String packageJson = App.getInstance().getPreferences().getString(PREF_PACKAGE_JSON, null);

            if ("null".equals(legacyCurrentGrid) || packageJson == null || packageJson.isEmpty() || "null".equals(packageJson)) {
                Log.w(TAG, "Legacy corrupted cache detected (current_grid is null), clearing package cache keys...");
                App.getInstance().getPreferences().edit()
                        .remove(PREF_PACKAGE_ID)
                        .remove(PREF_PACKAGE_JSON)
                        .remove(PREF_LEGACY_CURRENT_GRID)
                        .remove(PREF_LEGACY_NEXT_GRID)
                        .apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during legacy cache migration: " + e.getMessage());
        }
    }

    public static synchronized void saveManifest(RadioManifestResponse manifest) {
        if (manifest == null) return;

        long serverTimeMs = RadioProgramItem.parseTimeMs(manifest.getServerTime());
        if (serverTimeMs <= 0) {
            serverTimeMs = System.currentTimeMillis();
        }
        long offsetMs = serverTimeMs - System.currentTimeMillis();

        App.getInstance().getPreferences().edit()
                .putLong(PREF_SERVER_OFFSET_MS, offsetMs)
                .putLong(PREF_LAST_SYNC_TS, System.currentTimeMillis())
                .putString(PREF_STATIONS_JSON, gson.toJson(manifest.getRadios()))
                .apply();

        if (manifest.getPackageId() != null) {
            App.getInstance().getPreferences().edit()
                    .putString(PREF_PACKAGE_ID, manifest.getPackageId())
                    .apply();
        }
    }

    public static synchronized void savePackage(RadioPackageResponse pkg) {
        if (pkg == null || pkg.getRadios() == null || pkg.getRadios().isEmpty()) return;

        long serverTimeMs = RadioProgramItem.parseTimeMs(pkg.getServerTime());
        if (serverTimeMs > 0) {
            long offsetMs = serverTimeMs - System.currentTimeMillis();
            App.getInstance().getPreferences().edit().putLong(PREF_SERVER_OFFSET_MS, offsetMs).apply();
        }

        // Set radioSlug on tracks
        for (PeachPackageRadio radio : pkg.getRadios()) {
            String slug = radio.getSlug();
            if (radio.getCurrentSchedule() != null && radio.getCurrentSchedule().getTracks() != null) {
                for (RadioProgramItem track : radio.getCurrentSchedule().getTracks()) {
                    track.setRadioSlug(slug);
                }
            }
            if (radio.getNextSchedule() != null && radio.getNextSchedule().getTracks() != null) {
                for (RadioProgramItem track : radio.getNextSchedule().getTracks()) {
                    track.setRadioSlug(slug);
                }
            }
        }

        App.getInstance().getPreferences().edit()
                .putString(PREF_PACKAGE_ID, pkg.getPackageId())
                .putLong(PREF_LAST_SYNC_TS, System.currentTimeMillis())
                .putString(PREF_PACKAGE_JSON, gson.toJson(pkg))
                .remove(PREF_LEGACY_CURRENT_GRID)
                .remove(PREF_LEGACY_NEXT_GRID)
                .apply();

        Log.d(TAG, "RadioPackage successfully saved in cache with " + pkg.getRadios().size() + " radios.");
    }

    public static String getPackageId() {
        return App.getInstance().getPreferences().getString(PREF_PACKAGE_ID, null);
    }

    public static RadioPackageResponse getCachedPackage() {
        String json = App.getInstance().getPreferences().getString(PREF_PACKAGE_JSON, null);
        if (json == null || json.isEmpty() || "null".equals(json)) return null;
        try {
            return gson.fromJson(json, RadioPackageResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static PeachPackageRadio getPackageRadio(String radioSlug) {
        if (radioSlug == null) return null;
        RadioPackageResponse pkg = getCachedPackage();
        if (pkg == null || pkg.getRadios() == null) return null;
        for (PeachPackageRadio radio : pkg.getRadios()) {
            if (radioSlug.equalsIgnoreCase(radio.getSlug())) {
                return radio;
            }
        }
        return null;
    }

    public static long getServerOffsetMs() {
        return App.getInstance().getPreferences().getLong(PREF_SERVER_OFFSET_MS, 0L);
    }

    public static long getRadioNow() {
        return System.currentTimeMillis() + getServerOffsetMs();
    }

    public static List<PeachRadioStation> getRadioStations() {
        String json = App.getInstance().getPreferences().getString(PREF_STATIONS_JSON, null);
        if (json == null || json.isEmpty()) {
            return getDefaultStations();
        }
        try {
            RadioManifestResponse manifest = gson.fromJson("{\"radios\":" + json + "}", RadioManifestResponse.class);
            return (manifest != null && manifest.getRadios() != null && !manifest.getRadios().isEmpty()) ? manifest.getRadios() : getDefaultStations();
        } catch (Exception e) {
            return getDefaultStations();
        }
    }

    public static List<RadioProgramItem> getActiveTracks(String radioSlug, long radioNow) {
        List<RadioProgramItem> active = new ArrayList<>();
        if (radioSlug == null) return active;

        PeachPackageRadio radio = getPackageRadio(radioSlug);
        if (radio == null) return active;

        List<RadioProgramItem> allTracks = new ArrayList<>();
        if (radio.getCurrentSchedule() != null && radio.getCurrentSchedule().getTracks() != null) {
            allTracks.addAll(radio.getCurrentSchedule().getTracks());
        }
        if (radio.getNextSchedule() != null && radio.getNextSchedule().getTracks() != null) {
            allTracks.addAll(radio.getNextSchedule().getTracks());
        }

        if (allTracks.isEmpty()) return active;

        // Sort all tracks by startsAtMs ascending
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            allTracks.sort((t1, t2) -> Long.compare(t1.getStartsAtMs(), t2.getStartsAtMs()));
        } else {
            Collections.sort(allTracks, (t1, t2) -> Long.compare(t1.getStartsAtMs(), t2.getStartsAtMs()));
        }

        // Search for tracks where starts_at <= radioNow < ends_at
        for (RadioProgramItem track : allTracks) {
            long start = track.getStartsAtMs();
            long end = track.getEndsAtMs();
            if (start <= radioNow && radioNow < end) {
                active.add(track);
            }
        }

        // Fallback ONLY for small schedule gaps (<= 5000 ms)
        if (active.isEmpty()) {
            RadioProgramItem previous = null;
            RadioProgramItem next = null;

            for (RadioProgramItem track : allTracks) {
                long start = track.getStartsAtMs();
                if (start <= radioNow) {
                    if (previous == null || start > previous.getStartsAtMs()) {
                        previous = track;
                    }
                } else if (next == null || start < next.getStartsAtMs()) {
                    next = track;
                }
            }

            if (previous != null) {
                Log.d(TAG, "Gap fallback check: previous pos=" + previous.getPosition() + ", title=" + previous.getTitle() + ", endsAt=" + previous.getEndsAtMs() + ", radioNow=" + radioNow + ", gap=" + (radioNow - previous.getEndsAtMs()) + " ms");
            }
            if (next != null) {
                Log.d(TAG, "Gap fallback check: next pos=" + next.getPosition() + ", title=" + next.getTitle() + ", startsAt=" + next.getStartsAtMs() + ", radioNow=" + radioNow);
            }

            if (previous != null && (radioNow - previous.getEndsAtMs()) <= 5000L) {
                Log.d(TAG, "Tolerating small gap <= 5000 ms, adding previous track pos=" + previous.getPosition());
                active.add(previous);
            } else {
                Log.w(TAG, "No valid active track found and gap > 5000 ms. Returning empty activeTracks (NO fallback to position 1).");
            }
        }

        return active;
    }

    public static RadioProgramItem findActiveProgramItem(String radioSlug) {
        List<RadioProgramItem> active = getActiveTracks(radioSlug, getRadioNow());
        return !active.isEmpty() ? active.get(0) : null;
    }

    public static RadioProgramItem findNextProgramItem(String radioSlug, RadioProgramItem currentActive) {
        if (radioSlug == null) return null;
        long radioNow = getRadioNow();

        PeachPackageRadio radio = getPackageRadio(radioSlug);
        if (radio == null) return null;

        List<RadioProgramItem> candidates = new ArrayList<>();
        if (radio.getCurrentSchedule() != null && radio.getCurrentSchedule().getTracks() != null) {
            candidates.addAll(radio.getCurrentSchedule().getTracks());
        }
        if (radio.getNextSchedule() != null && radio.getNextSchedule().getTracks() != null) {
            candidates.addAll(radio.getNextSchedule().getTracks());
        }

        RadioProgramItem bestNext = null;
        long minDiff = Long.MAX_VALUE;

        for (RadioProgramItem item : candidates) {
            long start = item.getStartsAtMs();
            if (currentActive != null && item.getNavidromeTrackId() != null && item.getNavidromeTrackId().equals(currentActive.getNavidromeTrackId()) && start == currentActive.getStartsAtMs()) {
                continue;
            }
            if (start >= radioNow) {
                long diff = start - radioNow;
                if (diff < minDiff) {
                    minDiff = diff;
                    bestNext = item;
                }
            }
        }
        return bestNext;
    }

    public static List<RadioProgramItem> getPreviousProgramItems(
            String radioSlug,
            int count
    ) {
        List<RadioProgramItem> result = new ArrayList<>();
        List<RadioProgramItem> timeline = getTimeline(radioSlug);
        long now = getRadioNow();

        for (int i = timeline.size() - 1; i >= 0; i--) {
            RadioProgramItem item = timeline.get(i);

            if (item.getEndsAtMs() <= now) {
                result.add(item);

                if (result.size() >= count) {
                    break;
                }
            }
        }

        Collections.reverse(result);
        return result;
    }

    public static List<RadioProgramItem> getUpcomingProgramItems(
            String radioSlug,
            int count
    ) {
        List<RadioProgramItem> result = new ArrayList<>();
        List<RadioProgramItem> timeline = getTimeline(radioSlug);
        long now = getRadioNow();

        for (RadioProgramItem item : timeline) {
            if (item.getStartsAtMs() > now) {
                result.add(item);

                if (result.size() >= count) {
                    break;
                }
            }
        }

        return result;
    }

    private static List<RadioProgramItem> getTimeline(
            String radioSlug
    ) {
        List<RadioProgramItem> timeline = new ArrayList<>();
        PeachPackageRadio radio = getPackageRadio(radioSlug);

        if (radio == null) {
            return timeline;
        }

        if (radio.getCurrentSchedule() != null
                && radio.getCurrentSchedule().getTracks() != null) {
            timeline.addAll(
                    radio.getCurrentSchedule().getTracks()
            );
        }

        if (radio.getNextSchedule() != null
                && radio.getNextSchedule().getTracks() != null) {
            timeline.addAll(
                    radio.getNextSchedule().getTracks()
            );
        }

        timeline.sort(
                (a, b) -> Long.compare(
                        a.getStartsAtMs(),
                        b.getStartsAtMs()
                )
        );

        return timeline;
    }

    public static long calculateStreamPositionMs(RadioProgramItem item) {
        if (item == null) return 0L;
        long radioNow = getRadioNow();
        long startMs = item.getStartsAtMs();
        long elapsedSinceStart = Math.max(0L, radioNow - startMs);
        return elapsedSinceStart + item.getPlayFromMs();
    }

    public static List<PeachRadioStation> getDefaultStations() {
        List<PeachRadioStation> defaults = new ArrayList<>();
        defaults.add(new PeachRadioStation("1", "house", "House", "Sélection House & Electro", null));
        defaults.add(new PeachRadioStation("2", "rnb", "R&B", "Groot R&B et Soul", null));
        defaults.add(new PeachRadioStation("3", "hits", "Hits", "Les plus grands hits du moment", null));
        defaults.add(new PeachRadioStation("4", "chill", "Chill", "Musique détente et ambiance", null));
        defaults.add(new PeachRadioStation("5", "party", "Party", "Ambiance festive", null));
        defaults.add(new PeachRadioStation("6", "retro", "80 / 90", "Le meilleur des années 80 et 90", null));
        return defaults;
    }
}
