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
import androidx.media3.common.MediaItem;
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
        setContentView(R.layout.activity_main);
        player = new ExoPlayer.Builder(this).build();

        try {
            checkAndUnzipAssets();
            readEventsJson();
            startResourceSequence(new Gson().fromJson(new InputStreamReader(new FileInputStream(new File(getFilesDir(), EVENTS_JSON))), EventSchedule.class));
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
                        playVideo(resource.getName(), zone, resource.getDuration() * 1000L);
                    });
                } else {
                    handler.post(() -> {
                        showImage(resource.getName(), zone, resource.getDuration() * 1000L);
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


    private void playVideo(String videoName, Zone zone, long duration) {
        PlayerView videoView = createAndConfigurePlayerView(zone);

        MediaItem mediaItem = MediaItem.fromUri(String.valueOf(new File(getFilesDir(), videoName).toURI()));

        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        videoView.setPlayer(player);
        handler.postDelayed(() -> ((RelativeLayout) findViewById(R.id.main_layout)).removeView(videoView), duration);
    }

    private void showImage(String imageName, Zone zone, long duration) {
        ImageView imageView = createAndConfigureImageView(zone);
        loadImageIntoView(imageName, imageView);

        handler.postDelayed(() -> ((RelativeLayout) findViewById(R.id.main_layout)).removeView(imageView), duration);
    }

    private void loadImageIntoView(String imageName, ImageView imageView) {
        File imgFile = new File(getFilesDir(), imageName);
        if (imgFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            imageView.setImageBitmap(bitmap);
        }
    }
    private PlayerView createAndConfigurePlayerView(Zone zone) {
        PlayerView playerView = new PlayerView(this);
        MediaUtils.configurePlayerView(zone.getX(), zone.getY(), zone.getWidth(), zone.getHeight(), playerView, this);
        RelativeLayout layout = findViewById(R.id.main_layout);
        layout.addView(playerView);
        return playerView;
    }

    private ImageView createAndConfigureImageView(Zone zone) {
        ImageView imageView = new ImageView(this);
        MediaUtils.configureImageView(zone.getX(), zone.getY(), zone.getWidth(), zone.getHeight(), imageView, this);
        RelativeLayout layout = findViewById(R.id.main_layout);
        layout.addView(imageView);
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
