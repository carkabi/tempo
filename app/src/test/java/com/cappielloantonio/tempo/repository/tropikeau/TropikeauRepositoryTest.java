package com.cappielloantonio.tempo.repository.tropikeau;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequest;
import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequestResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TropikeauRepositoryTest {

    private MockWebServer mockWebServer;
    private TropikeauApiService apiService;
    private MusicRequest dummyRequest = new MusicRequest("https://open.spotify.com/track/123");

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        apiService = new Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(TropikeauApiService.class);
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void testAddMusicRequest_Success_202() throws IOException {
        String jsonResponse = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"message\": \"La demande a été ajoutée à la file.\",\n" +
                "  \"request\": {\n" +
                "    \"title\": \"Bohemian Rhapsody\",\n" +
                "    \"status\": \"queued\",\n" +
                "    \"queue_position\": 1\n" +
                "  }\n" +
                "}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(202)
                .setBody(jsonResponse));

        Response<MusicRequestResponse> response = apiService.addMusicRequest("user", "token", "salt", dummyRequest).execute();

        assertNotNull(response.body());
        assertEquals("success", response.body().getStatus());
        assertEquals("Bohemian Rhapsody", response.body().getRequest().getTitle());
        assertEquals(Integer.valueOf(1), response.body().getRequest().getQueuePosition());
    }

    @Test
    public void testAddMusicRequest_AlreadyQueued_200() throws IOException {
        String jsonResponse = "{\n" +
                "  \"status\": \"already_queued\",\n" +
                "  \"message\": \"Cette demande est déjà dans la file.\",\n" +
                "  \"request\": {\n" +
                "    \"title\": \"Pilé\",\n" +
                "    \"queue_position\": 5\n" +
                "  }\n" +
                "}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse));

        Response<MusicRequestResponse> response = apiService.addMusicRequest("user", "token", "salt", dummyRequest).execute();

        assertNotNull(response.body());
        assertEquals("already_queued", response.body().getStatus());
        assertEquals("Pilé", response.body().getRequest().getTitle());
    }

    @Test
    public void testAddMusicRequest_Unauthorized_401() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        Response<MusicRequestResponse> response = apiService.addMusicRequest("user", "token", "salt", dummyRequest).execute();

        assertEquals(401, response.code());
    }

    @Test
    public void testAddMusicRequest_Forbidden_403() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(403));

        Response<MusicRequestResponse> response = apiService.addMusicRequest("user", "token", "salt", dummyRequest).execute();

        assertEquals(403, response.code());
    }

    @Test
    public void testAddMusicRequest_Unprocessable_422() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(422));

        Response<MusicRequestResponse> response = apiService.addMusicRequest("user", "token", "salt", dummyRequest).execute();

        assertEquals(422, response.code());
    }
}