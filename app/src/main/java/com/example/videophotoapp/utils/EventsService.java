package com.example.videophotoapp.utils;

import android.content.Context;
import android.util.Log;

import com.example.videophotoapp.model.EventSchedule;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class EventsService {

    private static final String ZIP_FILE_NAME = "NSIGN_Prueba_Android.zip";
    private static final String EVENTS_JSON = "events.json";

    private final Context context;
    private final ZipUtils zipUtils;

    public EventsService(Context context, ZipUtils zipUtils) {
        this.context = context;
        this.zipUtils = zipUtils;
    }

    /**
     * Descomprime los archivos y prepara el events.json para su uso.
     * @throws IOException Si ocurre un error de entrada/salida durante la descompresión.
     */
    public void checkAndUnzipAssets() throws IOException {
        File eventsJson = new File(context.getFilesDir(), EVENTS_JSON);

        // Borra el eventsJson en caso de que exista para actualizarlo en caso de tener cambios
        if (eventsJson.exists()) {
            eventsJson.delete();
        }

        try (InputStream is = context.getAssets().open(ZIP_FILE_NAME)) {
            Log.d("EventsService", "unzipping...");
            zipUtils.unzip(is);
        } catch (Exception e) {
            Log.e("EventsService", "Error unzipping", e);
            throw e;
        }
    }

    /**
     * Lee el cronograma de eventos desde un archivo JSON y lo almacena en una variable.
     * @throws IOException Si ocurre un error de entrada/salida durante la lectura del archivo.
     */
    public EventSchedule readEventsJson() throws IOException {
        File eventsJson = new File(context.getFilesDir(), EVENTS_JSON);
        Gson gson = new Gson();
        EventSchedule eventSchedule = null;
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(eventsJson))) {
            eventSchedule = gson.fromJson(reader, EventSchedule.class);
        } catch (Exception e) {
            Log.e("MainActivity", "Error reading events.json", e);
            throw e;
        }
        return eventSchedule;
    }
}
