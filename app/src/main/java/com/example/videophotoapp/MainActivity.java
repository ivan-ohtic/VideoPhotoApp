package com.example.videophotoapp;

import android.content.Context;
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
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private List<Resource> resourceQueue = new ArrayList<>();
    private int currentResourceIndex = 0;

    /**
     * Called when the activity is starting.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        playerView = findViewById(R.id.videoView);

        try {
            checkAndUnzipAssets();
            readEventsJson();
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
            showMedia(eventSchedule);

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
        // Assume we always want to show the first resource of the first playlist and first zone.
        // Check if there are playlists and zones available
        if (!eventSchedule.getPlaylists().isEmpty() && !eventSchedule.getPlaylists().get(0).getZones().isEmpty()) {
            Zone zone = eventSchedule.getPlaylists().get(0).getZones().get(0);
            configurePlayerView(zone.getX(), zone.getY(), zone.getWidth(), zone.getHeight());

            if (!zone.getResources().isEmpty()) {
                Resource resource = zone.getResources().get(0);
                String resourceName = resource.getName();

                // Check if the resource is a video or an image based on its file extension
                if (resourceName.endsWith(".mp4")) {
                    playVideo(resourceName);
                } else {
                    showImage(resourceName);
                }
            } else {
                Log.e("MainActivity", "No resources in the zone.");
            }
        } else {
            Log.e("MainActivity", "No playlists or zones in EventSchedule.");
        }
    }

    /**
     * Plays a video from the application's internal storage.
     * @param videoName The name of the video file to play.
     */
    private void playVideo(String videoName) {
        imageView.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);

        // Set the player to the PlayerView
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Create a MediaItem for the video
        MediaItem mediaItem = MediaItem.fromUri(getFilesDir() + "/" + videoName);

        // Prepare the player with the MediaItem and start playing
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    /**
     * Shows an image from the application's internal storage.
     * @param imageName The name of the image file to display.
     */
    private void showImage(String imageName) {
        imageView.setVisibility(View.VISIBLE);
        playerView.setVisibility(View.GONE);

        // Load the image file
        File imgFile = new File(getFilesDir(), imageName);
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            imageView.setImageBitmap(myBitmap);
        } else {
            Log.e("MainActivity", "Unable to find image: " + imageName);
        }
    }

    /**
     * Loads the next resource in the queue.
     */
    private void loadNextResource() {
        if (currentResourceIndex < resourceQueue.size()) {
            Resource resource = resourceQueue.get(currentResourceIndex);
            if (resource.getName().endsWith(".mp4")) {
                playVideo(resource.getName());
            } else {
                showImage(resource.getName());
            }

            // Schedule loading the next resource after the specified time
            handler.postDelayed(this::loadNextResource, resource.getDuration() * 1000); // Duration in milliseconds
            currentResourceIndex++;
        }
    }

    /**
     * Starts the sequence of displaying resources based on the event schedule.
     * @param eventSchedule The event schedule containing the resources.
     */
    private void startResourceSequence(EventSchedule eventSchedule) {
        // Add all resources to the queue. This might vary depending on your data structure.
        for (Playlist playlist : eventSchedule.getPlaylists()) {
            for (Zone zone : playlist.getZones()) {
                resourceQueue.addAll(zone.getResources());
            }
        }

        // Start with the first resource
        loadNextResource();
    }

    /**
     * Configures the layout of the PlayerView based on the provided dimensions and position.
     * @param x The X position for the PlayerView.
     * @param y The Y position for the PlayerView.
     * @param width The width of the PlayerView.
     * @param height The height of the PlayerView.
     */
    private void configurePlayerView(int x, int y, int width, int height) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                convertDpToPixel(width, this),
                convertDpToPixel(height, this));
        params.leftMargin = convertDpToPixel(x, this);
        params.topMargin = convertDpToPixel(y, this);

        playerView.setLayoutParams(params);
    }

    /**
     * Converts dp (density independent pixels) to pixel units based on screen density.
     * @param dp The size in density independent pixels.
     * @param context The context to get resources and device specific display metrics.
     * @return The size in pixels.
     */
    public static int convertDpToPixel(float dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
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
