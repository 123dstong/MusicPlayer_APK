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

public class FragmentRadio extends Fragment {

    private static final String RADIO_API = "https://de1.api.radio-browser.info/json";
    private ListView listRadios;
    private EditText etSearch;
    private LinearLayout countryContainer;
    private LinearLayout nowPlaying;
    private TextView tvNowPlaying, btnRadioPlay;
    private List<Map<String, String>> displayRadios = new ArrayList<Map<String, String>>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isDetached = false;

    private String[] countries = {"全部", "中国", "美国", "日本", "韩国", "英国", "德国", "法国"};
    private String[] countryCodes = {"", "CN", "US", "JP", "KR", "GB", "DE", "FR"};
    private int selectedCountryIndex = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_radio, container, false);
        listRadios = (ListView) view.findViewById(R.id.listRadios);
        etSearch = (EditText) view.findViewById(R.id.etSearch);
        countryContainer = (LinearLayout) view.findViewById(R.id.countryContainer);
        nowPlaying = (LinearLayout) view.findViewById(R.id.nowPlaying);
        tvNowPlaying = (TextView) view.findViewById(R.id.tvNowPlaying);
        btnRadioPlay = (TextView) view.findViewById(R.id.btnRadioPlay);
        TextView btnSearch = (TextView) view.findViewById(R.id.btnSearch);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { search(etSearch.getText().toString()); }
        });
        btnRadioPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!isDetached && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).togglePlay();
                }
            }
        });

        setupCountries();
        loadPopular();
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isDetached = true;
    }

    private void setupCountries() {
        countryContainer.removeAllViews();
        for (int i = 0; i < countries.length; i++) {
            final int idx = i;
            TextView tv = new TextView(getActivity());
            tv.setText(countries[i]);
            tv.setTextSize(13);
            tv.setPadding(24, 8, 24, 8);
            tv.setTextColor(i == selectedCountryIndex ? 0xFF1DB954 : 0xFFB3B3B3);
            tv.setBackgroundColor(i == selectedCountryIndex ? 0xFF282828 : 0xFF1E1E1E);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 8, 0);
            tv.setLayoutParams(lp);
            tv.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    selectedCountryIndex = idx;
                    setupCountries();
                    if (etSearch.getText().length() > 0) {
                        search(etSearch.getText().toString());
                    } else {
                        loadPopular();
                    }
                }
            });
            countryContainer.addView(tv);
        }
    }

    private void loadPopular() {
        executor.execute(new Runnable() {
            public void run() {
                try {
                    String cc = countryCodes[selectedCountryIndex];
                    String urlStr;
                    if (cc.isEmpty()) {
                        urlStr = RADIO_API + "/stations/topvote?limit=50";
                    } else {
                        urlStr = RADIO_API + "/stations/bycountrycodeexact/" + cc + "?order=votes&reverse=true&limit=50";
                    }
                    String json = httpGet(urlStr);
                    if (json != null) parseRadios(json);
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
        if (query == null || query.isEmpty()) { loadPopular(); return; }
        final String q = query;
        executor.execute(new Runnable() {
            public void run() {
                try {
                    String urlStr = RADIO_API + "/stations/byname/" + URLEncoder.encode(q, "UTF-8") + "?limit=50";
                    String json = httpGet(urlStr);
                    if (json != null) parseRadios(json);
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

    private void parseRadios(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            final List<Map<String, String>> radios = new ArrayList<Map<String, String>>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject station = arr.getJSONObject(i);
                Map<String, String> radio = new HashMap<String, String>();
                radio.put("name", station.optString("name", "Unknown"));
                radio.put("country", station.optString("country", ""));
                radio.put("language", station.optString("language", ""));
                radio.put("tags", station.optString("tags", ""));
                radio.put("url", station.optString("url_resolved", station.optString("url", "")));
                radios.add(radio);
            }
            postSafe(new Runnable() {
                public void run() { updateList(radios); }
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

    private void updateList(List<Map<String, String>> radios) {
        if (isDetached || getActivity() == null) return;
        displayRadios.clear();
        displayRadios.addAll(radios);
        SimpleAdapter adapter = new SimpleAdapter(getActivity(), displayRadios, R.layout.item_radio,
            new String[]{"name", "country", "language"},
            new int[]{R.id.tvName, R.id.tvInfo});
        listRadios.setAdapter(adapter);
        listRadios.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                playRadio(pos);
            }
        });
    }

    private void playRadio(int pos) {
        if (pos < 0 || pos >= displayRadios.size()) return;
        if (isDetached || getActivity() == null) return;
        final Map<String, String> radio = displayRadios.get(pos);
        String url = radio.get("url");
        if (url == null || url.isEmpty()) {
            Toast.makeText(getActivity(), "电台地址无效", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            final MainActivity main = (MainActivity) getActivity();
            if (main.mediaPlayer == null) return;
            main.mediaPlayer.reset();
            main.isPrepared = false;
            main.mediaPlayer.setDataSource(url);
            main.mediaPlayer.setOnPreparedListener(new android.media.MediaPlayer.OnPreparedListener() {
                public void onPrepared(android.media.MediaPlayer mp) {
                    try {
                        mp.start();
                        main.isPrepared = true;
                        main.showMiniPlayer(radio.get("name"), radio.get("country"));
                        if (!isDetached) {
                            nowPlaying.setVisibility(View.VISIBLE);
                            tvNowPlaying.setText("正在播放: " + radio.get("name"));
                            btnRadioPlay.setText("\u23F8");
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            });
            main.mediaPlayer.setOnCompletionListener(new android.media.MediaPlayer.OnCompletionListener() {
                public void onCompletion(android.media.MediaPlayer mp) {
                    main.isPrepared = false;
                    if (!isDetached) {
                        btnRadioPlay.setText("\u25B6");
                        nowPlaying.setVisibility(View.GONE);
                    }
                }
            });
            main.mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Toast.makeText(getActivity(), "播放失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
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
