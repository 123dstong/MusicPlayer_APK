package com.music.player;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FragmentOnline extends Fragment {

    private static final String AUDIUS_BASE = "https://api.audius.co";
    private ListView listSongs;
    private EditText etSearch;
    private LinearLayout categoryContainer;
    private List<Map<String, String>> displaySongs = new ArrayList<Map<String, String>>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isDetached = false;

    private String[] categories = {"全部", "流行", "摇滚", "电子", "嘻哈", "古典", "爵士", "民谣"};
    private String selectedCategory = "全部";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_online, container, false);
        listSongs = (ListView) view.findViewById(R.id.listSongs);
        etSearch = (EditText) view.findViewById(R.id.etSearch);
        categoryContainer = (LinearLayout) view.findViewById(R.id.categoryContainer);
        TextView btnSearch = (TextView) view.findViewById(R.id.btnSearch);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { search(etSearch.getText().toString()); }
        });

        setupCategories();
        loadTrending();
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isDetached = true;
    }

    private void setupCategories() {
        categoryContainer.removeAllViews();
        for (final String cat : categories) {
            TextView tv = new TextView(getActivity());
            tv.setText(cat);
            tv.setTextSize(13);
            tv.setPadding(24, 8, 24, 8);
            tv.setTextColor(cat.equals(selectedCategory) ? 0xFF1DB954 : 0xFFB3B3B3);
            tv.setBackgroundColor(cat.equals(selectedCategory) ? 0xFF282828 : 0xFF1E1E1E);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 8, 0);
            tv.setLayoutParams(lp);
            tv.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    selectedCategory = cat;
                    setupCategories();
                    if (etSearch.getText().length() > 0) {
                        search(etSearch.getText().toString());
                    } else {
                        loadTrending();
                    }
                }
            });
            categoryContainer.addView(tv);
        }
    }

    private void loadTrending() {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    String tagParam = "";
                    if (!"全部".equals(selectedCategory)) {
                        tagParam = "&tags=" + URLEncoder.encode(selectedCategory, "UTF-8");
                    }
                    String urlStr = AUDIUS_BASE + "/v1/tracks/search?query=music&sort=play_count&limit=30&filtered=true" + tagParam;
                    String json = httpGet(urlStr);
                    if (json != null) parseTracks(json);
                } catch (Exception e) {
                    postSafe(new Runnable() {
                        public void run() {
                            if (!isDetached && getActivity() != null) {
                                Toast.makeText(getActivity(), "加载失败，请检查网络", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }

    private void search(String query) {
        if (query == null || query.isEmpty()) { loadTrending(); return; }
        final String q = query;
        executor.execute(new Runnable() {
            public void run() {
                try {
                    String urlStr = AUDIUS_BASE + "/v1/tracks/search?query="
                        + URLEncoder.encode(q, "UTF-8") + "&limit=30&filtered=true";
                    String json = httpGet(urlStr);
                    if (json != null) parseTracks(json);
                } catch (Exception e) {
                    postSafe(new Runnable() {
                        public void run() {
                            if (!isDetached && getActivity() != null) {
                                Toast.makeText(getActivity(), "搜索失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }

    private void parseTracks(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray data = root.optJSONArray("data");
            if (data == null) {
                postSafe(new Runnable() {
                    public void run() {
                        if (!isDetached && getActivity() != null) {
                            Toast.makeText(getActivity(), "暂无结果", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                return;
            }
            final List<Map<String, String>> songs = new ArrayList<Map<String, String>>();
            for (int i = 0; i < data.length(); i++) {
                JSONObject track = data.getJSONObject(i);
                Map<String, String> song = new HashMap<String, String>();
                song.put("id", track.optString("id", ""));
                song.put("title", track.optString("title", "Unknown"));
                String artist = "Unknown";
                try {
                    if (track.has("user")) {
                        artist = track.getJSONObject("user").optString("name", "Unknown");
                    }
                } catch (Exception e) {}
                song.put("artist", artist);
                song.put("duration", MainActivity.formatDuration(track.optLong("duration", 0) * 1000));
                song.put("source", "audius");
                String trackId = track.optString("id", "");
                song.put("url", AUDIUS_BASE + "/v1/tracks/" + trackId + "/stream?app_name=musicpro");
                songs.add(song);
            }
            postSafe(new Runnable() {
                public void run() { updateList(songs); }
            });
        } catch (Exception e) {
            postSafe(new Runnable() {
                public void run() {
                    if (!isDetached && getActivity() != null) {
                        Toast.makeText(getActivity(), "解析失败", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void updateList(List<Map<String, String>> songs) {
        if (isDetached || getActivity() == null) return;
        displaySongs.clear();
        displaySongs.addAll(songs);
        SimpleAdapter adapter = new SimpleAdapter(getActivity(), displaySongs, R.layout.item_song,
            new String[]{"title", "artist", "duration"},
            new int[]{R.id.tvTitle, R.id.tvArtist, R.id.tvDuration});
        listSongs.setAdapter(adapter);
        listSongs.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                if (!isDetached && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).playSongAt(new ArrayList<Map<String, String>>(displaySongs), pos);
                }
            }
        });
    }

    private void postSafe(Runnable r) {
        if (!isDetached) mainHandler.post(r);
    }

    private String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "MusicPro/1.0");
            if (conn.getResponseCode() != 200) return null;
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) { sb.append(line); }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try { if (reader != null) reader.close(); } catch (Exception e) {}
            try { if (conn != null) conn.disconnect(); } catch (Exception e) {}
        }
    }
}
