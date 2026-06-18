package com.music.player;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Map;

public class PlayerActivity extends Activity {

    private TextView tvTitle, tvArtist, tvCurrent, tvTotal;
    private TextView btnPlay, btnPrev, btnNext, btnBack, btnFav;
    private SeekBar seekBar;
    private Handler handler = new Handler();
    private boolean isSeeking = false;
    private boolean isRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_player);

        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvArtist = (TextView) findViewById(R.id.tvArtist);
        tvCurrent = (TextView) findViewById(R.id.tvCurrent);
        tvTotal = (TextView) findViewById(R.id.tvTotal);
        btnPlay = (TextView) findViewById(R.id.btnPlay);
        btnPrev = (TextView) findViewById(R.id.btnPrev);
        btnNext = (TextView) findViewById(R.id.btnNext);
        btnBack = (TextView) findViewById(R.id.btnBack);
        btnFav = (TextView) findViewById(R.id.btnFav);
        seekBar = (SeekBar) findViewById(R.id.seekBar);

        btnBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        btnPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (MainActivity.instance != null) {
                    MainActivity.instance.togglePlay();
                    updatePlayBtn();
                }
            }
        });
        btnPrev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (MainActivity.instance != null) {
                    MainActivity.instance.playPrev();
                    updateUI();
                }
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (MainActivity.instance != null) {
                    MainActivity.instance.playNext();
                    updateUI();
                }
            }
        });
        btnFav.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { toggleFavorite(); }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && MainActivity.mediaPlayer != null && MainActivity.isPrepared) {
                    try { MainActivity.mediaPlayer.seekTo(progress); } catch (Exception e) {}
                }
            }
            public void onStartTrackingTouch(SeekBar sb) { isSeeking = true; }
            public void onStopTrackingTouch(SeekBar sb) { isSeeking = false; }
        });

        updateUI();
        startUpdating();
    }

    private void updatePlayBtn() {
        try {
            if (MainActivity.mediaPlayer != null && MainActivity.isPrepared) {
                btnPlay.setText(MainActivity.mediaPlayer.isPlaying() ? "\u23F8" : "\u25B6");
            }
        } catch (Exception e) {}
    }

    private void updateUI() {
        try {
            if (MainActivity.playIndex >= 0 && MainActivity.playIndex < MainActivity.playQueue.size()) {
                Map<String, String> song = MainActivity.playQueue.get(MainActivity.playIndex);
                tvTitle.setText(song.get("title") != null ? song.get("title") : "");
                tvArtist.setText(song.get("artist") != null ? song.get("artist") : "");
                updateFavButton(song);
            }
            if (MainActivity.mediaPlayer != null && MainActivity.isPrepared) {
                btnPlay.setText(MainActivity.mediaPlayer.isPlaying() ? "\u23F8" : "\u25B6");
                int total = MainActivity.mediaPlayer.getDuration();
                int current = MainActivity.mediaPlayer.getCurrentPosition();
                seekBar.setMax(total);
                if (!isSeeking) seekBar.setProgress(current);
                tvCurrent.setText(MainActivity.formatDuration(current));
                tvTotal.setText(MainActivity.formatDuration(total));
            } else {
                btnPlay.setText("\u25B6");
            }
        } catch (Exception e) {}
    }

    private void startUpdating() {
        handler.postDelayed(new Runnable() {
            public void run() {
                if (isRunning) {
                    updateUI();
                    handler.postDelayed(this, 500);
                }
            }
        }, 500);
    }

    private void toggleFavorite() {
        try {
            if (MainActivity.playIndex < 0 || MainActivity.playIndex >= MainActivity.playQueue.size()) return;
            Map<String, String> song = MainActivity.playQueue.get(MainActivity.playIndex);
            DatabaseHelper db = new DatabaseHelper(this);
            String songId = song.get("id");
            String source = song.get("source");
            if (songId == null || songId.isEmpty()) songId = song.get("title");
            if (source == null) source = "local";

            if (db.isFavorite(songId, source)) {
                db.removeFavorite(songId, source);
                Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show();
            } else {
                db.addFavorite(songId, song.get("title"), song.get("artist"),
                    song.get("url"), source, song.get("artwork"), song.get("duration"));
                Toast.makeText(this, "已收藏", Toast.LENGTH_SHORT).show();
            }
            db.close();
            updateFavButton(song);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateFavButton(Map<String, String> song) {
        try {
            DatabaseHelper db = new DatabaseHelper(this);
            String songId = song.get("id");
            String source = song.get("source");
            if (songId == null || songId.isEmpty()) songId = song.get("title");
            if (source == null) source = "local";
            boolean isFav = db.isFavorite(songId, source);
            db.close();
            btnFav.setText(isFav ? "\u2764" : "\u2661");
            btnFav.setTextColor(isFav ? 0xFFFF6B6B : 0xFFB3B3B3);
        } catch (Exception e) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        isRunning = true;
        updateUI();
        startUpdating();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
    }
}
