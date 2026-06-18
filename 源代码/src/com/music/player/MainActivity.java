package com.music.player;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private LinearLayout navLocal, navOnline, navRadio, navMine;
    private TextView navLocalText, navOnlineText, navRadioText, navMineText;
    private TextView tvTitle;
    private int currentNav = -1;

    private LinearLayout miniPlayer;
    private TextView tvMiniTitle, tvMiniArtist, btnMiniPlay, btnMiniNext;

    public static MediaPlayer mediaPlayer;
    public static List<Map<String, String>> playQueue = new ArrayList<Map<String, String>>();
    public static int playIndex = -1;
    public static boolean isPrepared = false;
    public static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        instance = this;

        initMediaPlayer();
        initViews();
        setupNavigation();
        showFragment(0);
        requestPerms();
    }

    private void initMediaPlayer() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(3);
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    isPrepared = false;
                    Toast.makeText(MainActivity.this, "播放出错", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initViews() {
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        navLocal = (LinearLayout) findViewById(R.id.navLocal);
        navOnline = (LinearLayout) findViewById(R.id.navOnline);
        navRadio = (LinearLayout) findViewById(R.id.navRadio);
        navMine = (LinearLayout) findViewById(R.id.navMine);
        navLocalText = (TextView) findViewById(R.id.navLocalText);
        navOnlineText = (TextView) findViewById(R.id.navOnlineText);
        navRadioText = (TextView) findViewById(R.id.navRadioText);
        navMineText = (TextView) findViewById(R.id.navMineText);
        miniPlayer = (LinearLayout) findViewById(R.id.miniPlayer);
        tvMiniTitle = (TextView) findViewById(R.id.tvMiniTitle);
        tvMiniArtist = (TextView) findViewById(R.id.tvMiniArtist);
        btnMiniPlay = (TextView) findViewById(R.id.btnMiniPlay);
        btnMiniNext = (TextView) findViewById(R.id.btnMiniNext);

        btnMiniPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { togglePlay(); }
        });
        btnMiniNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { playNext(); }
        });
        miniPlayer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (playIndex >= 0 && playIndex < playQueue.size()) {
                    try {
                        startActivity(new Intent(MainActivity.this, PlayerActivity.class));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }

    private void setupNavigation() {
        navLocal.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showFragment(0); }
        });
        navOnline.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showFragment(1); }
        });
        navRadio.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showFragment(2); }
        });
        navMine.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showFragment(3); }
        });
    }

    private void showFragment(int index) {
        if (index == currentNav) return;
        currentNav = index;
        Fragment fragment;
        String title;
        switch (index) {
            case 1: fragment = new FragmentOnline(); title = "在线音乐"; break;
            case 2: fragment = new FragmentRadio(); title = "网络电台"; break;
            case 3: fragment = new FragmentMine(); title = "我的"; break;
            default: fragment = new FragmentLocal(); title = "本地音乐"; break;
        }
        tvTitle.setText(title);
        try {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.fragmentContainer, fragment);
            ft.commitAllowingStateLoss();
        } catch (Exception e) { e.printStackTrace(); }
        updateNavColors();
    }

    private void updateNavColors() {
        int active = 0xFF1DB954;
        int inactive = 0xFFB3B3B3;
        navLocalText.setTextColor(currentNav == 0 ? active : inactive);
        navOnlineText.setTextColor(currentNav == 1 ? active : inactive);
        navRadioText.setTextColor(currentNav == 2 ? active : inactive);
        navMineText.setTextColor(currentNav == 3 ? active : inactive);
    }

    public void showMiniPlayer(String title, String artist) {
        try {
            miniPlayer.setVisibility(View.VISIBLE);
            tvMiniTitle.setText(title != null ? title : "");
            tvMiniArtist.setText(artist != null ? artist : "");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void togglePlay() {
        try {
            if (mediaPlayer == null || !isPrepared) return;
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnMiniPlay.setText("\u25B6");
            } else {
                mediaPlayer.start();
                btnMiniPlay.setText("\u23F8");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void playNext() {
        if (playQueue.isEmpty()) return;
        playIndex++;
        if (playIndex >= playQueue.size()) playIndex = 0;
        playCurrent();
    }

    public void playPrev() {
        if (playQueue.isEmpty()) return;
        playIndex--;
        if (playIndex < 0) playIndex = playQueue.size() - 1;
        playCurrent();
    }

    public void playCurrent() {
        if (playIndex < 0 || playIndex >= playQueue.size()) return;
        try {
            final Map<String, String> song = playQueue.get(playIndex);
            if (mediaPlayer == null) initMediaPlayer();
            mediaPlayer.reset();
            isPrepared = false;

            String path = song.get("path");
            String url = song.get("url");
            if (path != null && !path.isEmpty()) {
                mediaPlayer.setDataSource(path);
            } else if (url != null && !url.isEmpty()) {
                mediaPlayer.setDataSource(url);
            } else {
                Toast.makeText(this, "无法播放", Toast.LENGTH_SHORT).show();
                return;
            }

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    isPrepared = true;
                    try {
                        mp.start();
                        btnMiniPlay.setText("\u23F8");
                        showMiniPlayer(song.get("title"), song.get("artist"));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    isPrepared = false;
                    playNext();
                }
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void playSongAt(List<Map<String, String>> songs, int index) {
        playQueue.clear();
        playQueue.addAll(songs);
        playIndex = index;
        playCurrent();
    }

    private void requestPerms() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            List<String> perms = new ArrayList<String>();
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    perms.add(Manifest.permission.READ_MEDIA_AUDIO);
                }
            }
            if (!perms.isEmpty()) {
                requestPermissions(perms.toArray(new String[0]), 100);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            instance = null;
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static String formatDuration(long ms) {
        long min = TimeUnit.MILLISECONDS.toMinutes(ms);
        long sec = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        return String.format("%02d:%02d", min, sec);
    }
}
