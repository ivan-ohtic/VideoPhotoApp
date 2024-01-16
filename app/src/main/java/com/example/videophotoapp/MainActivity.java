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
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.videophotoapp.R;
import com.example.videophotoapp.model.EventSchedule;
import com.example.videophotoapp.model.Playlist;
import com.example.videophotoapp.model.Resource;
import com.example.videophotoapp.model.Zone;
import com.example.videophotoapp.utils.ZipUtils;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String ZIP_FILE_NAME = "NSIGN_Prueba_Android.zip";
    private static final String EVENTS_JSON = "events.json";
    private ExoPlayer player;
    private RelativeLayout mainLayout;
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private Handler uiHandler = new Handler(Looper.getMainLooper());
    private EventSchedule eventSchedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        player = new ExoPlayer.Builder(this).build();
        mainLayout = findViewById(R.id.main_layout);

        try {
            loadEventSchedule();
            setupResources();
        } catch (IOException e) {
            Log.e("MainActivity", "Error en onCreate", e);
        }
    }

    /**
     * Carga el cronograma de eventos, descomprimiendo los recursos y leyendo el archivo JSON.
     */
    private void loadEventSchedule() throws IOException {
        checkAndUnzipAssets();
        readEventsJson();
    }

    /**
     * Descomprime los archivos y prepara el events.json para su uso.
     * @throws IOException Si ocurre un error de entrada/salida durante la descompresión.
     */
    private void checkAndUnzipAssets() throws IOException {
        File eventsJson = new File(getFilesDir(), EVENTS_JSON);

        if (eventsJson.exists()) {
            eventsJson.delete();
        }

        try (InputStream is = getAssets().open(ZIP_FILE_NAME)) {
            ZipUtils.unzip(is, getFilesDir());
        } catch (Exception e) {
            Log.e("MainActivity", "Error unzipping", e);
            throw e;
        }
    }

    /**
     * Lee el cronograma de eventos desde un archivo JSON y lo almacena en una variable.
     * @throws IOException Si ocurre un error de entrada/salida durante la lectura del archivo.
     */
    private void readEventsJson() throws IOException {
        File eventsJson = new File(getFilesDir(), EVENTS_JSON);
        Gson gson = new Gson();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(eventsJson))) {
            eventSchedule = gson.fromJson(reader, EventSchedule.class);
        } catch (Exception e) {
            Log.e("MainActivity", "Error reading events.json", e);
            throw e;
        }
    }

    /**
     * Configura los recursos para cada zona especificada en el cronograma de eventos.
     */
    private void setupResources() {
        // Ejemplo de cómo podrías implementar un control de duración para las Playlists.
        for (int i = 0; i < eventSchedule.getPlaylists().size(); i++) {
            final int index = i;
            Playlist playlist = eventSchedule.getPlaylists().get(index);
            uiHandler.postDelayed(() -> {

                for (Zone zone : playlist.getZones()) {
                    setupZone(zone);
                }
                if (index == eventSchedule.getPlaylists().size() - 1) {
                    // Si es la última playlist, vuelve a la primera.
                    setupResources(); // Esto reinicia el ciclo de Playlists.
                }
            }, playlist.getDuration() * 1000);
        }
    }

    /**
     * Configura una zona específica y prepara sus recursos para ser mostrados.
     *
     * @param zone Zona a ser configurada.
     */
    private void setupZone(Zone zone) {
        View zoneView = createZoneView(zone);
        mainLayout.addView(zoneView);

        for (Resource resource : zone.getResources()) {
            scheduleResourceDisplay(resource, zoneView);
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

        // Configurar dimensiones y posición de zoneLayout según los datos de Zone.
        // Utilizamos RelativeLayout.LayoutParams para posicionar la vista dentro del RelativeLayout.
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                convertDpToPixel(zone.getWidth()),
                convertDpToPixel(zone.getHeight())
        );
        params.leftMargin = convertDpToPixel(zone.getX());
        params.topMargin = convertDpToPixel(zone.getY());
        zoneLayout.setLayoutParams(params);

        return zoneLayout;
    }

    /**
     * Convierte un valor en dp (density-independent pixels) a píxeles.
     *
     * @param dp Valor en dp a ser convertido.
     * @return Valor convertido en píxeles.
     */
    private int convertDpToPixel(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Programa la visualización de un recurso específico en una zona.
     *
     * @param resource Recurso a ser mostrado.
     * @param zoneView Vista de la zona donde se mostrará el recurso.
     */
    private void scheduleResourceDisplay(Resource resource, View zoneView) {
        executorService.execute(() -> {
            if (resource.isVideo()) {
                prepareVideo(resource, zoneView);
            } else {
                prepareImage(resource, zoneView);
            }
        });
    }

    /**
     * Prepara un video para ser mostrado en una zona específica.
     *
     * @param resource Recurso de video.
     * @param zoneView Vista de la zona donde se reproducirá el video.
     */
    private void prepareVideo(Resource resource, View zoneView) {
        // Cargar y preparar video
        // En el hilo UI:
        uiHandler.post(() -> playVideo(resource, zoneView));
    }

    /**
     * Prepara una imagen para ser mostrada en una zona específica.
     *
     * @param resource Recurso de imagen.
     * @param zoneView Vista de la zona donde se mostrará la imagen.
     */
    private void prepareImage(Resource resource, View zoneView) {
        // Cargar imagen
        // En el hilo UI:
        uiHandler.post(() -> showImage(resource, zoneView));
    }

    /**
     * Configura y reproduce un video en una vista específica.
     *
     * @param resource Recurso de video.
     * @param zoneView Vista de la zona donde se reproducirá el video.
     */
    private void playVideo(Resource resource, View zoneView) {
        if (!(zoneView instanceof FrameLayout)) {
            Log.e("MainActivity", "zoneView no es FrameLayout");
            return;
        }

        PlayerView playerView = new PlayerView(this);
        playerView.setLayoutParams(new FrameLayout.LayoutParams(
                zoneView.getWidth(),
                zoneView.getHeight()));

        //((FrameLayout) zoneView).removeAllViews();
        ((FrameLayout) zoneView).addView(playerView);

        // Configurar y reproducir el video en playerView
        ExoPlayer videoPlayer = new ExoPlayer.Builder(this).build();
        MediaItem mediaItem = MediaItem.fromUri(getFilesDir() + "/" + resource.getName());
        videoPlayer.setMediaItem(mediaItem);
        videoPlayer.prepare();
        videoPlayer.play();

        playerView.setPlayer(videoPlayer);

        // Liberar el player al finalizar la reproducción
        uiHandler.postDelayed(() -> {
            videoPlayer.release();
            ((FrameLayout) zoneView).removeView(playerView);
        }, resource.getDuration() * 1000L);
    }

    /**
     * Muestra una imagen en una vista específica.
     *
     * @param resource Recurso de imagen.
     * @param zoneView Vista de la zona donde se mostrará la imagen.
     */
    private void showImage(Resource resource, View zoneView) {
        if (!(zoneView instanceof FrameLayout)) {
            Log.e("MainActivity", "zoneView no es FrameLayout");
            return;
        }

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        Bitmap bitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calculateInSampleSize(options, imageView.getWidth(), imageView.getHeight());
            bitmap = BitmapFactory.decodeFile(getFilesDir() + "/" + resource.getName(), options);
        } catch (Exception e) {
            Log.e("MainActivity", "Error al cargar la imagen", e);
        }

        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            //((FrameLayout) zoneView).removeAllViews();
            ((FrameLayout) zoneView).addView(imageView);

            // Eliminar el imageView y liberar el bitmap
            Bitmap finalBitmap = bitmap;
            uiHandler.postDelayed(() -> {
                ((FrameLayout) zoneView).removeView(imageView);
                finalBitmap.recycle();
            }, resource.getDuration() * 1000);
        } else {
            Log.e("MainActivity", "Bitmap es null, no se puede mostrar la imagen");
        }
    }


    /**
     * Calcula un factor de escala para reducir el tamaño de la imagen.
     *
     * @param options Opciones de BitmapFactory.
     * @param reqWidth Ancho requerido.
     * @param reqHeight Altura requerida.
     * @return Factor de escala.
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
        executorService.shutdown();
    }

}