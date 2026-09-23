package com.cappielloantonio.tempo.repository.tropikeau;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequestData;
import com.cappielloantonio.tempo.repository.tropikeau.models.MusicRequestResponse;

import org.junit.Test;

import java.lang.reflect.Field;

public class TropikeauLifecycleTest {

    @Test
    public void testStatusSequence() throws Exception {
        // Simuler le passage de queued -> processing -> completed
        MusicRequestData data = new MusicRequestData();
        setField(data, "status", "queued");
        assertEquals("queued", data.getStatus());

        setField(data, "status", "processing");
        setField(data, "progressStage", "Downloading...");
        assertEquals("processing", data.getStatus());
        assertEquals("Downloading...", data.getProgressStage());

        setField(data, "status", "completed");
        setField(data, "isTerminal", true);
        assertEquals("completed", data.getStatus());
        assertTrue(data.isTerminal());
    }

    @Test
    public void testFailedCase() throws Exception {
        MusicRequestData data = new MusicRequestData();
        setField(data, "status", "failed");
        setField(data, "errorMessage", "URL not supported");
        setField(data, "isTerminal", true);
        
        assertEquals("failed", data.getStatus());
        assertEquals("URL not supported", data.getErrorMessage());
        assertTrue(data.isTerminal());
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}