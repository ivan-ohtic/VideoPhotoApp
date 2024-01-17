package com.example.videophotoapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.videophotoapp.model.EventSchedule;
import com.example.videophotoapp.model.Playlist;
import com.example.videophotoapp.model.Resource;
import com.example.videophotoapp.model.Zone;
import com.example.videophotoapp.utils.EventsService;
import com.example.videophotoapp.utils.MediaUtils;
import com.example.videophotoapp.utils.ZipUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout mainLayout;
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private EventSchedule eventSchedule;

    private ZipUtils zipUtils = null;

    private MediaUtils mediaUtils = null;

    private EventsService eventsService = null;

    private int resourceViewIdCounter = 1;

    private int zoneIdCounter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("MainActivity", "onCreate called");

        zipUtils = new ZipUtils(this);
        mediaUtils = new MediaUtils();
        eventsService = new EventsService(this, zipUtils);
        setContentView(R.layout.activity_main);

        mainLayout = findViewById(R.id.main_layout);

        try {
            loadEventSchedule();
            setupResources(true);
        } catch (IOException e) {
            Log.e("MainActivity", "Error onCreate", e);
        }
    }

    /**
     * Carga el cronograma de eventos, descomprimiendo los recursos y leyendo el archivo JSON.
     */
    private void loadEventSchedule() throws IOException {
        eventsService.checkAndUnzipAssets();
        eventSchedule = eventsService.readEventsJson();
    }

    /**
     * Configura los recursos para cada zona especificada en el cronograma de eventos.
     */
    private void setupResources(boolean firstExecution) {
        executorService.execute(() -> {
            Thread currentThread = Thread.currentThread();
            long threadId = currentThread.getId();
            Log.d("MainActivity", "Thread started: " + threadId + ", firstExecution: " + firstExecution);

            long lastPlaylistDuration = 0;
            if (!firstExecution) {
                Playlist lastPlaylist = eventSchedule.getPlaylists().get(eventSchedule.getPlaylists().size() - 1);
                lastPlaylistDuration += lastPlaylist.getDuration() * 1000L; // Convierte a milisegundos
            }

            for (int i = 0; i < eventSchedule.getPlaylists().size(); i++) {
                final int index = i;
                Playlist playlist = eventSchedule.getPlaylists().get(index);

                long finalLastPlaylistDuration = lastPlaylistDuration;
                uiHandler.postDelayed(() -> {
                    Log.d("MainActivity", "Scheduled execution for playlist: " + (index + 1) + " delay (s): " + finalLastPlaylistDuration / 1000);

                    for (Zone zone : playlist.getZones()) {
                        setupZone(zone);
                    }

                    if (index == eventSchedule.getPlaylists().size() - 1) {
                        Log.d("MainActivity", "Repeating playlist sequence");
                        setupResources(false);
                    }
                }, lastPlaylistDuration);
                lastPlaylistDuration += playlist.getDuration() * 1000L;
            }
        });
    }

    /**
     * Configura una zona específica y prepara sus recursos para ser mostrados.
     *
     * @param zone Zona a ser configurada.
     */
    private void setupZone(Zone zone) {
        Log.d("MainActivity", "Zone setup: " + zone.getId());

        View zoneView = createZoneView(zone);
        mainLayout.addView(zoneView);

        // Ordenar los recursos por su campo 'order'.
        List<Resource> sortedResources = new ArrayList<>(zone.getResources());
        sortedResources.sort(Comparator.comparingInt(Resource::getOrder));

        long accumulatedDelay = 0;
        for (Resource resource : sortedResources) {
            scheduleResourceDisplay(resource, zoneView, accumulatedDelay);
            accumulatedDelay += resource.getDuration() * 1000L; // Acumula la duración en milisegundos.
        }
    }

    /**
     * Crea una vista para representar una zona específica, configurando su tamaño y posición.
     *
     * @param zone Zona para la cual se crea la vista.
     * @return La vista creada para la zona.
     */
    private View createZoneView(Zone zone) {
        FrameLayout zoneLayout = new FrameLayout(this);
        int zoneId = zoneIdCounter++;
        zoneLayout.setId(zoneId);

        // Configurar dimensiones y posición de zoneLayout según los datos de Zone.
        // Utilizamos RelativeLayout.LayoutParams para posicionar la vista dentro del RelativeLayout.
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                zone.getWidth(),
                zone.getHeight()
        );
        params.leftMargin = zone.getX();
        params.topMargin = zone.getY();
        zoneLayout.setLayoutParams(params);

        return zoneLayout;
    }

    /**
     * Programa la visualización de un recurso específico en una zona.
     *
     * @param resource Recurso a ser mostrado.
     * @param zoneView Vista de la zona donde se mostrará el recurso.
     */
    private void scheduleResourceDisplay(Resource resource, View zoneView, long delay) {
        Log.d("MainActivity", "schedule resource display: " + resource.getName() + ", delay: " + delay);
        uiHandler.postDelayed(() -> {
            // Cualquier vista existente en la zona se elimina antes de mostrar un nuevo recurso


            if (resource.isVideo()) {
                playVideo(resource, zoneView);
            } else {
                showImage(resource, zoneView);
            }
        }, delay);
    }

    /**
     * Configura y reproduce un video en una vista específica.
     *
     * @param resource Recurso de video.
     * @param zoneView Vista de la zona donde se reproducirá el video.
     */
    private void playVideo(Resource resource, View zoneView) {
        Log.d("MainActivity", "Playing video: " + resource.getName() + " in: " + zoneView.getId());
        if (!(zoneView instanceof FrameLayout)) {
            Log.e("MainActivity", "zoneView is not a FrameLayout");
            return;
        }

        PlayerView playerView = new PlayerView(this);
        int playerViewId = resourceViewIdCounter++;
        playerView.setId(playerViewId); // Asigna un ID único
        Log.d("MainActivity", "with playerview:"+playerViewId);
        playerView.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        ((FrameLayout) zoneView).addView(playerView);

        // Configurar y reproducir el video en playerView
        ExoPlayer videoPlayer = new ExoPlayer.Builder(this).build();
        MediaItem mediaItem = MediaItem.fromUri(getFilesDir() + "/" + resource.getName());

        videoPlayer.setMediaItem(mediaItem);
        videoPlayer.prepare();
        videoPlayer.play();

        playerView.setPlayer(videoPlayer);

        uiHandler.postDelayed(() -> {
            Log.d("MainActivity", "Video ended, releasing resources for: " + resource.getName());
            videoPlayer.release();
            ((FrameLayout) zoneView).removeView(playerView);
        }, (resource.getDuration()-1) * 1000L);


    }

    /**
     * Muestra una imagen en una vista específica.
     *
     * @param resource Recurso de imagen.
     * @param zoneView Vista de la zona donde se mostrará la imagen.
     */
    private void showImage(Resource resource, View zoneView) {
        Log.d("MainActivity", "Showing image: " + resource.getName() + " in: "+ zoneView.getId());
        if (!(zoneView instanceof FrameLayout)) {
            Log.e("MainActivity", "zoneView is not a FrameLayout");
            return;
        }

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        int playerViewId = resourceViewIdCounter++;
        imageView.setId(playerViewId);

        Bitmap bitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = mediaUtils.calculateInSampleSize(options, imageView.getWidth(), imageView.getHeight());
            bitmap = BitmapFactory.decodeFile(getFilesDir() + "/" + resource.getName(), options);
        } catch (Exception e) {
            Log.e("MainActivity", "Error loading image", e);
        }

        if (bitmap != null) {
            ((FrameLayout) zoneView).removeAllViews();
            imageView.setImageBitmap(bitmap);
            ((FrameLayout) zoneView).addView(imageView);

            // Eliminar el imageView y liberar el bitmap
            Bitmap finalBitmap = bitmap;
            uiHandler.postDelayed(() -> {
                Log.d("MainActivity", "Delete imageView" + resource.getName());
                ((FrameLayout) zoneView).removeView(imageView);
                finalBitmap.recycle();
            }, resource.getDuration() * 1000L);

        } else {
            Log.e("MainActivity", "Bitmap is null, can't show image");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        Log.d("MainActivity", "onDestroy called");
    }

}