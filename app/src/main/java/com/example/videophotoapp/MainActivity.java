package com.example.videophotoapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.graphics.BitmapFactory;
import android.widget.RelativeLayout;

import com.example.videophotoapp.model.Playlist;
import com.example.videophotoapp.model.Resource;
import com.example.videophotoapp.model.Zone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.videophotoapp.utils.MediaUtils;
import com.example.videophotoapp.utils.ZipUtils;
import com.google.gson.Gson;
import com.example.videophotoapp.model.EventSchedule;

/**
 * Main activity class that handles the display of media resources.
 */
public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private PlayerView playerView;
    private ExoPlayer player;

    private static final String ZIP_FILE_NAME = "NSIGN_Prueba_Android.zip";
    private static final String EVENTS_JSON = "events.json";

    private Handler handler = new Handler();

    Map<Zone,List<Resource>> zoneResourceMap = new LinkedHashMap<>();

    private ExecutorService executorService = Executors.newSingleThreadExecutor();


    /**
     * Called when the activity is starting.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            checkAndUnzipAssets();
            readEventsJson();

            setContentView(R.layout.activity_main);

            imageView = findViewById(R.id.imageView);
            playerView = findViewById(R.id.videoView);

            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);

            player.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(PlaybackException error) {
                    Log.e("MainActivity", "Player error: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e("MainActivity", "Error in onCreate", e);
        }
    }

    /**
     * Unzips the assets into the application's internal storage.
     * @throws IOException If an input or output exception occurred.
     */
    private void checkAndUnzipAssets() throws IOException {
        File eventsJson = new File(getFilesDir(), EVENTS_JSON);

        // If the events.json file exists, delete it
        if (eventsJson.exists()) {
            eventsJson.delete();
        }

        // Unzip the ZIP file
        try (InputStream is = getAssets().open(ZIP_FILE_NAME)) {
            ZipUtils.unzip(is, getFilesDir());
        } catch (Exception e) {
            Log.e("MainActivity", "Error unzipping", e);
            throw e;
        }
    }

    /**
     * Reads the JSON data from the internal storage and processes it.
     * @throws IOException If an input or output exception occurred.
     */
    private void readEventsJson() throws IOException {
        File eventsJson = new File(getFilesDir(), EVENTS_JSON);
        Gson gson = new Gson();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(eventsJson))) {
            EventSchedule eventSchedule = gson.fromJson(reader, EventSchedule.class);

            // Show the first media from the EventSchedule
            //showMedia(eventSchedule);

            // Start the resource sequence
            startResourceSequence(eventSchedule);
        } catch (Exception e) {
            Log.e("MainActivity", "Error reading events.json", e);
            throw e;
        }
    }

    /**
     * Displays media (image or video) based on the information in the event schedule.
     * @param eventSchedule The event schedule object containing media information.
     */
    private void showMedia(EventSchedule eventSchedule) {
            for (Playlist playlist : eventSchedule.getPlaylists()) {
                for (Zone zone : playlist.getZones()) {
                    for (Resource resource : zone.getResources()) {
                        executorService.execute(() -> {
                            String resourceName = resource.getName();
                            if (resourceName.endsWith(".mp4")) {
                                handler.post(() -> {
                                    PlayerView videoView = createAndConfigurePlayerView(zone);
                                    playVideo(resourceName, videoView, resource.getDuration() * 1000L);
                                });
                            } else {
                                handler.post(() -> {
                                    //ImageView imageView = createAndConfigureImageView(zone);
                                    showImage(resourceName, imageView, resource.getDuration() * 1000L);
                                });
                            }
                            try {
                                Thread.sleep(resource.getDuration() * 1000L);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                    }
                }
            }

    }

    /**
     * Starts the sequence of displaying resources based on the event schedule.
     * @param eventSchedule The event schedule containing the resources.
     */
    private void startResourceSequence(EventSchedule eventSchedule) {

        for (Playlist playlist : eventSchedule.getPlaylists()) {
            for (Zone zone : playlist.getZones()) {
                List<Resource> resources = new ArrayList<>(zone.getResources());
                zoneResourceMap.put(zone, resources);
            }
        }

        // Iniciar visualización para la primera zona
        if (!zoneResourceMap.isEmpty()) {
            displayZoneResources(zoneResourceMap.keySet().iterator().next());
        }
    }

    private void displayZoneResources(Zone zone) {
        List<Resource> resources = zoneResourceMap.get(zone);
        for (Resource resource : resources) {
            executorService.execute(() -> {
                if (resource.getName().endsWith(".mp4")) {
                    handler.post(() -> {
                        PlayerView videoView = createAndConfigurePlayerView(zone);
                        playVideo(resource.getName(), videoView, resource.getDuration() * 1000L);
                    });
                } else {
                    handler.post(() -> {
                        //ImageView imageView = createAndConfigureImageView(zone);
                        showImage(resource.getName(), imageView, resource.getDuration() * 1000L);
                    });
                }
                try {
                    Thread.sleep(resource.getDuration() * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // Añade código aquí para manejar la finalización del recurso si es necesario.
            });
        }
    }

    private void playVideo(String videoName, PlayerView videoView, long duration) {
        videoView.setVisibility(View.VISIBLE);
        MediaUtils.playVideo(videoName, videoView, player);

        handler.postDelayed(() -> videoView.setVisibility(View.GONE), duration);
    }

    private void showImage(String imageName, ImageView imageView, long duration) {
        imageView.setVisibility(View.VISIBLE);
        File imgFile = new File(getFilesDir(), imageName);
        if (imgFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            /*runOnUiThread(() -> {
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
            });*/
            imageView.setImageBitmap(bitmap);
        }

        //handler.postDelayed(() -> imageView.setVisibility(View.GONE), duration);
    }

    private PlayerView createAndConfigurePlayerView(Zone zone) {
        PlayerView playerView = new PlayerView(this);
        MediaUtils.configurePlayerView(zone.getX(), zone.getY(), zone.getWidth(), zone.getHeight(), playerView, this);
        // Agregar playerView al layout principal
        ((RelativeLayout) findViewById(R.id.main_layout)).addView(playerView);
        return playerView;
    }

    private ImageView createAndConfigureImageView(Zone zone) {
        ImageView imageView = new ImageView(this);
        MediaUtils.configureImageView(zone.getX(), zone.getY(), zone.getWidth(), zone.getHeight(), imageView, this);
        // Agregar imageView al layout principal
        ((RelativeLayout) findViewById(R.id.main_layout)).addView(imageView);
        return imageView;
    }

    /**
     * Releases the resources used by the player when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }

}
