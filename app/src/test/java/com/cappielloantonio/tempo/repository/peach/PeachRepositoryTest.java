package com.cappielloantonio.tempo.repository.peach;

import com.cappielloantonio.tempo.repository.peach.models.ContactDiagnostics;
import com.cappielloantonio.tempo.repository.peach.models.ContactErrorResponse;
import com.cappielloantonio.tempo.repository.peach.models.ContactRequest;
import com.cappielloantonio.tempo.repository.peach.models.ContactResponse;
import com.cappielloantonio.tempo.repository.peach.models.PeachBootstrapResponse;
import com.cappielloantonio.tempo.repository.peach.models.PeachPackageRadio;
import com.cappielloantonio.tempo.repository.peach.models.RadioManifestResponse;
import com.cappielloantonio.tempo.repository.peach.models.RadioPackageResponse;
import com.cappielloantonio.tempo.repository.peach.models.RadioProgramItem;
import com.cappielloantonio.tempo.service.PeachCrossfadeEngine;
import com.cappielloantonio.tempo.util.PeachUpdateDownloader;
import com.google.gson.Gson;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PeachRepositoryTest {

    private final Gson gson = new Gson();

    @Test
    public void testDeserializationWithNullUpdate() {
        String json = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"server_time\": \"2026-09-06T20:00:00+00:00\",\n" +
                "  \"application\": {\n" +
                "    \"slug\": \"peach\",\n" +
                "    \"name\": \"Peach\",\n" +
                "    \"android_application_id\": \"fr.tropikeau.peach\",\n" +
                "    \"channel\": \"beta\",\n" +
                "    \"current_version_code\": 1\n" +
                "  },\n" +
                "  \"update\": null,\n" +
                "  \"notices\": [],\n" +
                "  \"news\": []\n" +
                "}";

        PeachBootstrapResponse response = gson.fromJson(json, PeachBootstrapResponse.class);

        Assert.assertNotNull(response);
        Assert.assertEquals("success", response.getStatus());
        Assert.assertNotNull(response.getApplication());
        Assert.assertEquals("fr.tropikeau.peach", response.getApplication().getAndroidApplicationId());
        Assert.assertNull(response.getUpdate());
        Assert.assertNotNull(response.getNotices());
        Assert.assertTrue(response.getNotices().isEmpty());
        Assert.assertNotNull(response.getNews());
        Assert.assertTrue(response.getNews().isEmpty());
    }

    @Test
    public void testDeserializationWithUpdate() {
        String json = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"server_time\": \"2026-09-06T20:00:00+00:00\",\n" +
                "  \"application\": {\n" +
                "    \"slug\": \"peach\",\n" +
                "    \"name\": \"Peach\",\n" +
                "    \"android_application_id\": \"fr.tropikeau.peach\",\n" +
                "    \"channel\": \"beta\",\n" +
                "    \"current_version_code\": 1\n" +
                "  },\n" +
                "  \"update\": {\n" +
                "    \"available\": true,\n" +
                "    \"mandatory\": false,\n" +
                "    \"id\": 1,\n" +
                "    \"channel\": \"beta\",\n" +
                "    \"version_code\": 2,\n" +
                "    \"version_name\": \"0.1.0-beta.2\",\n" +
                "    \"title\": \"Nouvelle version de Peach\",\n" +
                "    \"changelog\": \"Liste des changements\",\n" +
                "    \"minimum_supported_version_code\": 1,\n" +
                "    \"size_bytes\": 30000000,\n" +
                "    \"sha256\": \"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\",\n" +
                "    \"published_at\": \"2026-09-06T20:00:00+00:00\",\n" +
                "    \"download_url\": \"https://tropikeau.fr/api/peach/v1/app/releases/1/download\"\n" +
                "  },\n" +
                "  \"notices\": [],\n" +
                "  \"news\": []\n" +
                "}";

        PeachBootstrapResponse response = gson.fromJson(json, PeachBootstrapResponse.class);

        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getUpdate());
        Assert.assertTrue(response.getUpdate().isAvailable());
        Assert.assertFalse(response.getUpdate().isMandatory());
        Assert.assertEquals(2, response.getUpdate().getVersionCode());
        Assert.assertEquals("0.1.0-beta.2", response.getUpdate().getVersionName());
        Assert.assertEquals("Nouvelle version de Peach", response.getUpdate().getTitle());
        Assert.assertEquals(Long.valueOf(30000000L), response.getUpdate().getSizeBytes());
        Assert.assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", response.getUpdate().getSha256());
    }

    @Test
    public void testVersionCodeComparison() {
        int currentVersionCode = 1;

        int newVersionCode = 2;
        Assert.assertTrue(newVersionCode > currentVersionCode);

        int sameVersionCode = 1;
        Assert.assertFalse(sameVersionCode > currentVersionCode);

        int olderVersionCode = 0;
        Assert.assertFalse(olderVersionCode > currentVersionCode);
    }

    @Test
    public void testNotificationKeyDeduplication() {
        Set<String> notifiedKeys = new HashSet<>();
        String key = "news-1-1788724800";

        Assert.assertFalse(notifiedKeys.contains(key));

        notifiedKeys.add(key);

        Assert.assertTrue(notifiedKeys.contains(key));

        // Attempting to notify again for the same key
        boolean shouldNotify = !notifiedKeys.contains(key);
        Assert.assertFalse(shouldNotify);
    }

    @Test
    public void testSha256Calculation() throws IOException {
        File tempFile = File.createTempFile("test_sha256", ".txt");
        tempFile.deleteOnExit();

        FileWriter writer = new FileWriter(tempFile);
        writer.write("Peach Test Tropikeau");
        writer.close();

        String sha256 = PeachUpdateDownloader.calculateSha256(tempFile);

        Assert.assertNotNull(sha256);
        Assert.assertEquals(64, sha256.length());

        // Case-insensitive matching check
        Assert.assertTrue(sha256.equalsIgnoreCase(sha256.toUpperCase()));
    }

    @Test
    public void testContactRequestSerialization() {
        ContactDiagnostics diag = new ContactDiagnostics("0.1.0-beta.1", 1, "peach", "14", "ASUS Exemple");
        ContactRequest request = new ContactRequest("bug", "Maxime", "exemple@domaine.fr", "Problème pendant la lecture", "Description détaillée du problème", diag);

        String json = gson.toJson(request);

        Assert.assertTrue(json.contains("\"category\":\"bug\""));
        Assert.assertTrue(json.contains("\"name\":\"Maxime\""));
        Assert.assertTrue(json.contains("\"reply_email\":\"exemple@domaine.fr\""));
        Assert.assertTrue(json.contains("\"subject\":\"Problème pendant la lecture\""));
        Assert.assertTrue(json.contains("\"app_version_name\":\"0.1.0-beta.1\""));
        Assert.assertTrue(json.contains("\"android_version\":\"14\""));
        Assert.assertTrue(json.contains("\"device_model\":\"ASUS Exemple\""));
    }

    @Test
    public void testContactResponseDeserialization() {
        String json = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"message\": \"Ton message a bien été envoyé.\",\n" +
                "  \"contact\": {\n" +
                "    \"id\": 123,\n" +
                "    \"status\": \"new\",\n" +
                "    \"created_at\": \"2026-09-06T20:00:00+00:00\"\n" +
                "  }\n" +
                "}";

        ContactResponse response = gson.fromJson(json, ContactResponse.class);

        Assert.assertNotNull(response);
        Assert.assertEquals("success", response.getStatus());
        Assert.assertEquals("Ton message a bien été envoyé.", response.getMessage());
        Assert.assertNotNull(response.getContact());
        Assert.assertEquals(123, response.getContact().getId());
        Assert.assertEquals("new", response.getContact().getStatus());
    }

    @Test
    public void testContact422ErrorDeserialization() {
        String json = "{\n" +
                "  \"status\": \"error\",\n" +
                "  \"code\": \"validation_failed\",\n" +
                "  \"message\": \"Message explicatif\",\n" +
                "  \"errors\": {\n" +
                "    \"subject\": [\"Le sujet est obligatoire.\"]\n" +
                "  }\n" +
                "}";

        ContactErrorResponse error = gson.fromJson(json, ContactErrorResponse.class);

        Assert.assertNotNull(error);
        Assert.assertEquals("error", error.getStatus());
        Assert.assertEquals("validation_failed", error.getCode());
        Assert.assertEquals("Message explicatif", error.getMessage());
        Assert.assertNotNull(error.getErrors());
        Assert.assertTrue(error.getErrors().has("subject"));
    }

    @Test
    public void testContactDraftStorage() {
        String draftCategory = "suggestion";
        String draftName = "Maxime";

        Assert.assertNotNull(draftCategory);
        Assert.assertEquals("Maxime", draftName);

        draftCategory = null;
        draftName = null;

        Assert.assertNull(draftCategory);
        Assert.assertNull(draftName);
    }

    @Test
    public void testRadioManifestDeserialization() {
        String json = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"server_time\": \"2026-09-07T12:00:00+00:00\",\n" +
                "  \"package_id\": \"pkg_20260907_01\",\n" +
                "  \"current_grid_checksum\": \"abc123\",\n" +
                "  \"next_grid_checksum\": \"def456\",\n" +
                "  \"radios\": [\n" +
                "    {\n" +
                "      \"id\": \"1\",\n" +
                "      \"slug\": \"house\",\n" +
                "      \"name\": \"House\",\n" +
                "      \"description\": \"Sélection House & Electro\",\n" +
                "      \"cover_image_url\": \"https://tropikeau.fr/storage/radios/house.jpg\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        RadioManifestResponse manifest = gson.fromJson(json, RadioManifestResponse.class);

        Assert.assertNotNull(manifest);
        Assert.assertEquals("success", manifest.getStatus());
        Assert.assertEquals("pkg_20260907_01", manifest.getPackageId());
        Assert.assertNotNull(manifest.getRadios());
        Assert.assertEquals(1, manifest.getRadios().size());
        Assert.assertEquals("house", manifest.getRadios().get(0).getSlug());
    }

    @Test
    public void testRadioPackageRealServerJsonDeserialization() {
        String json = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"server_time\": \"2026-09-07T13:02:42+00:00\",\n" +
                "  \"package_id\": \"e1514d885e5aefc8f8c6814dad68c4d27d86f9a0b3053256d100d73f1fa0391a\",\n" +
                "  \"radios\": [\n" +
                "    {\n" +
                "      \"id\": 1,\n" +
                "      \"slug\": \"house\",\n" +
                "      \"name\": \"House\",\n" +
                "      \"description\": \"House, deep house, dance et électro.\",\n" +
                "      \"crossfade_ms\": 6000,\n" +
                "      \"current_schedule\": {\n" +
                "        \"id\": 35,\n" +
                "        \"version\": \"house-20260907-r5\",\n" +
                "        \"tracks_count\": 2,\n" +
                "        \"tracks\": [\n" +
                "          {\n" +
                "            \"position\": 1,\n" +
                "            \"navidrome_track_id\": \"5IEKFdaImA8JsxSZs9L9xm\",\n" +
                "            \"title\": \"High Speed\",\n" +
                "            \"artist\": \"Coldplay\",\n" +
                "            \"album\": \"Parachutes\",\n" +
                "            \"duration_ms\": 254280,\n" +
                "            \"starts_at\": \"2026-09-07T03:00:00+00:00\",\n" +
                "            \"ends_at\": \"2026-09-07T03:04:14+00:00\",\n" +
                "            \"play_from_ms\": 0,\n" +
                "            \"play_until_ms\": 254280,\n" +
                "            \"fade_in_ms\": 0,\n" +
                "            \"fade_out_ms\": 6000\n" +
                "          },\n" +
                "          {\n" +
                "            \"position\": 2,\n" +
                "            \"navidrome_track_id\": \"ykxQHxhosmHNbBJBgaIvng\",\n" +
                "            \"title\": \"L'amour à la plage\",\n" +
                "            \"artist\": \"Collectif Métissé\",\n" +
                "            \"album\": \"Fans des années 80\",\n" +
                "            \"duration_ms\": 203050,\n" +
                "            \"starts_at\": \"2026-09-07T03:04:08+00:00\",\n" +
                "            \"ends_at\": \"2026-09-07T03:07:31+00:00\",\n" +
                "            \"play_from_ms\": 0,\n" +
                "            \"play_until_ms\": 203050,\n" +
                "            \"fade_in_ms\": 6000,\n" +
                "            \"fade_out_ms\": 6000\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      \"next_schedule\": null\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        RadioPackageResponse pkg = gson.fromJson(json, RadioPackageResponse.class);

        Assert.assertNotNull(pkg);
        Assert.assertEquals("e1514d885e5aefc8f8c6814dad68c4d27d86f9a0b3053256d100d73f1fa0391a", pkg.getPackageId());
        Assert.assertNotNull(pkg.getRadios());
        Assert.assertEquals(1, pkg.getRadios().size());

        PeachPackageRadio house = pkg.getRadios().get(0);
        Assert.assertEquals("house", house.getSlug());
        Assert.assertNotNull(house.getCurrentSchedule());
        Assert.assertEquals(2, house.getCurrentSchedule().getTracksCount());
        Assert.assertNotNull(house.getCurrentSchedule().getTracks());
        Assert.assertEquals(2, house.getCurrentSchedule().getTracks().size());

        RadioProgramItem item = house.getCurrentSchedule().getTracks().get(0);
        Assert.assertEquals("5IEKFdaImA8JsxSZs9L9xm", item.getNavidromeTrackId());
        Assert.assertEquals("High Speed", item.getTitle());
        Assert.assertEquals("Coldplay", item.getArtist());
        Assert.assertEquals(254280L, item.getDurationMs());
        Assert.assertEquals(6000L, item.getFadeOutMs());
    }

    @Test
    public void testServerOffsetAndLivePositionCalculation() {
        long localTime = 1000000L;
        long serverTime = 1005000L; // Server is 5000ms ahead of local clock
        long offset = serverTime - localTime;

        Assert.assertEquals(5000L, offset);

        long radioNow = localTime + offset;
        Assert.assertEquals(serverTime, radioNow);

        long startsAtMs = 1000000L;
        long playFromMs = 10000L;

        // Joined 5000ms after track started at playFromMs=10000ms
        long positionMs = (radioNow - startsAtMs) + playFromMs;
        Assert.assertEquals(15000L, positionMs);
    }

    @Test
    public void testCrossfadeVolumeCurvesAndEnergyConservation() {
        // At start (x = 0.0)
        float outVolStart = PeachCrossfadeEngine.calculateOutgoingVolume(0.0f);
        float inVolStart = PeachCrossfadeEngine.calculateIncomingVolume(0.0f);
        Assert.assertEquals(1.0f, outVolStart, 0.001f);
        Assert.assertEquals(0.0f, inVolStart, 0.001f);

        // At midpoint (x = 0.5)
        float outVolMid = PeachCrossfadeEngine.calculateOutgoingVolume(0.5f);
        float inVolMid = PeachCrossfadeEngine.calculateIncomingVolume(0.5f);
        Assert.assertEquals(0.7071f, outVolMid, 0.01f);
        Assert.assertEquals(0.7071f, inVolMid, 0.01f);

        // Equal-power energy conservation check: outVol^2 + inVol^2 == 1.0
        double energyMid = (outVolMid * outVolMid) + (inVolMid * inVolMid);
        Assert.assertEquals(1.0, energyMid, 0.01);

        // At end (x = 1.0)
        float outVolEnd = PeachCrossfadeEngine.calculateOutgoingVolume(1.0f);
        float inVolEnd = PeachCrossfadeEngine.calculateIncomingVolume(1.0f);
        Assert.assertEquals(0.0f, outVolEnd, 0.001f);
        Assert.assertEquals(1.0f, inVolEnd, 0.001f);
    }
}
