package com.braulio.app14_musicalocal;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.content.ContentUris;
import android.view.View;
import android.widget.*;
import androidx.documentfile.provider.DocumentFile;
import java.util.*;

public class MainActivity extends Activity {
    Spinner carpetas;
    ListView lista;
    TextView titulo, tiempo;
    SeekBar progreso;
    Button reproducir;
    ArrayList<Uri> uris = new ArrayList<>();
    ArrayList<String> nombres = new ArrayList<>();
    MediaPlayer player;
    int actual = -1;
    Uri carpetaPersonalizada;
    Handler handler = new Handler();
    boolean actualizando = false;
    String[] opciones = {"Interno / Download", "Interno / Music", "Interno / Documents", "Interno / Podcasts", "Elegir otra carpeta..."};

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        carpetas = findViewById(R.id.spinnerCarpetas);
        lista = findViewById(R.id.listaCanciones);
        titulo = findViewById(R.id.txtCancion);
        tiempo = findViewById(R.id.txtTiempo);
        progreso = findViewById(R.id.seekProgreso);
        reproducir = findViewById(R.id.btnPlay);
        carpetas.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, opciones));
        carpetas.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 4) { if (carpetaPersonalizada != null) cargarDocumentos(carpetaPersonalizada); else abrirSelector(); }
                else cargarMediaStore(new String[]{"Download", "Music", "Documents", "Podcasts"}[position]);
            }
        });
        lista.setOnItemClickListener((parent, view, position, id) -> tocar(position));
        reproducir.setOnClickListener(v -> {
            if (player == null) { if (!uris.isEmpty()) tocar(0); return; }
            if (player.isPlaying()) player.pause(); else player.start();
            actualizarBoton();
        });
        findViewById(R.id.btnAnterior).setOnClickListener(v -> cambiar(-1));
        findViewById(R.id.btnSiguiente).setOnClickListener(v -> cambiar(1));
        progreso.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar s) { actualizando = true; }
            public void onStopTrackingTouch(SeekBar s) {
                if (player != null) player.seekTo(s.getProgress());
                actualizando = false;
            }
            public void onProgressChanged(SeekBar s, int value, boolean user) {}
        });
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.READ_MEDIA_AUDIO") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.READ_MEDIA_AUDIO"}, 22);
        } else if (android.os.Build.VERSION.SDK_INT < 33 && checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 22);
        }
        handler.post(new Runnable() {
            public void run() {
                if (player != null) {
                    try {
                        if (!actualizando) progreso.setProgress(player.getCurrentPosition());
                        tiempo.setText(formato(player.getCurrentPosition()) + " / " + formato(player.getDuration()));
                    } catch (IllegalStateException ignored) {}
                }
                handler.postDelayed(this, 500);
            }
        });
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 22 && results.length > 0 && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) cargarMediaStore("Download");
        else if (requestCode == 22) Toast.makeText(this, "Autoriza acceso a audio o elige otra carpeta", Toast.LENGTH_LONG).show();
    }
    void cargarMediaStore(String carpeta) {
        limpiar();
        String[] cols = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.RELATIVE_PATH};
        String filtro = MediaStore.Audio.Media.RELATIVE_PATH + " LIKE ?";
        try (Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols, filtro, new String[]{carpeta + "/%"}, MediaStore.Audio.Media.DISPLAY_NAME + " ASC")) {
            if (c != null) while (c.moveToNext()) {
                long id = c.getLong(0);
                uris.add(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id));
                nombres.add(c.getString(1));
            }
        } catch (Exception e) { Toast.makeText(this, "No se pudo leer la música: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
        mostrar();
    }
    void abrirSelector() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, 100);
    }
    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req == 100 && result == RESULT_OK && data != null) {
            carpetaPersonalizada = data.getData();
            try { getContentResolver().takePersistableUriPermission(carpetaPersonalizada, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (Exception ignored) {}
            cargarDocumentos(carpetaPersonalizada);
        } else if (req == 100) carpetas.setSelection(0);
    }
    void cargarDocumentos(Uri uri) {
        limpiar();
        DocumentFile dir = DocumentFile.fromTreeUri(this, uri);
        if (dir != null) for (DocumentFile f : dir.listFiles()) {
            String nombre = f.getName();
            if (f.isFile() && nombre != null && nombre.toLowerCase(Locale.ROOT).matches(".*\\.(mp3|wav|m4a|aac|ogg|flac)$")) {
                uris.add(f.getUri()); nombres.add(nombre);
            }
        }
        mostrar();
    }
    void mostrar() {
        lista.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombres));
        ((TextView)findViewById(R.id.txtCantidad)).setText(nombres.size() + " canciones");
        if (nombres.isEmpty()) titulo.setText("No se encontraron canciones");
    }
    void limpiar() {
        if (player != null) { player.release(); player = null; }
        actual = -1; uris.clear(); nombres.clear();
        titulo.setText("Selecciona una canción");
        progreso.setProgress(0); progreso.setMax(0); tiempo.setText("0:00 / 0:00");
        actualizarBoton();
    }
    void tocar(int indice) {
        if (indice < 0 || indice >= uris.size()) return;
        if (player != null) { player.release(); player = null; }
        actual = indice;
        try {
            player = new MediaPlayer();
            player.setDataSource(this, uris.get(indice));
            player.setOnPreparedListener(mp -> { progreso.setMax(mp.getDuration()); mp.start(); actualizarBoton(); });
            player.setOnCompletionListener(mp -> cambiar(1));
            player.setOnErrorListener((mp, what, extra) -> { Toast.makeText(this, "Error al reproducir", Toast.LENGTH_SHORT).show(); return true; });
            player.prepareAsync();
            titulo.setText(nombres.get(indice));
        } catch (Exception e) { Toast.makeText(this, "No se puede abrir el audio", Toast.LENGTH_SHORT).show(); }
    }
    void cambiar(int paso) {
        if (uris.isEmpty()) return;
        tocar(actual < 0 ? 0 : (actual + paso + uris.size()) % uris.size());
    }
    void actualizarBoton() { reproducir.setText(player != null && player.isPlaying() ? "⏸" : "▶"); }
    String formato(int ms) { int s = Math.max(0, ms / 1000); return (s / 60) + ":" + String.format(Locale.getDefault(), "%02d", s % 60); }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); if (player != null) player.release(); super.onDestroy(); }
}
