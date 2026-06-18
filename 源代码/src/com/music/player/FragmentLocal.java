package com.music.player;

import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FragmentLocal extends Fragment {

    private ListView listSongs;
    private EditText etSearch;
    private TextView tvCount;
    private List<Map<String, String>> allSongs = new ArrayList<Map<String, String>>();
    private List<Map<String, String>> displaySongs = new ArrayList<Map<String, String>>();
    private boolean isDetached = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_local, container, false);
        listSongs = (ListView) view.findViewById(R.id.listSongs);
        etSearch = (EditText) view.findViewById(R.id.etSearch);
        tvCount = (TextView) view.findViewById(R.id.tvCount);
        TextView btnSearch = (TextView) view.findViewById(R.id.btnSearch);

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { filter(s.toString()); }
            public void afterTextChanged(android.text.Editable s) {}
        });
        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { filter(etSearch.getText().toString()); }
        });

        loadSongs();
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isDetached = true;
    }

    private void loadSongs() {
        if (getActivity() == null) return;
        allSongs.clear();
        try {
            String[] proj = {
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA
            };
            Cursor c = getActivity().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                proj, MediaStore.Audio.Media.IS_MUSIC + "!=0", null,
                MediaStore.Audio.Media.TITLE + " ASC");
            if (c != null) {
                while (c.moveToNext()) {
                    try {
                        long dur = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                        if (dur < 10000) continue;
                        Map<String, String> song = new HashMap<String, String>();
                        song.put("id", c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
                        song.put("title", c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)));
                        song.put("artist", c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));
                        song.put("album", c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)));
                        song.put("duration", MainActivity.formatDuration(dur));
                        song.put("path", c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
                        song.put("source", "local");
                        allSongs.add(song);
                    } catch (Exception e) {}
                }
                c.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
        if (!isDetached && tvCount != null) {
            tvCount.setText(allSongs.size() + " 首歌");
        }
        filter("");
    }

    private void filter(String q) {
        if (isDetached || getActivity() == null) return;
        displaySongs.clear();
        String lower = (q != null) ? q.toLowerCase() : "";
        for (Map<String, String> s : allSongs) {
            if (q == null || q.isEmpty() || s.get("title").toLowerCase().contains(lower)
                || s.get("artist").toLowerCase().contains(lower)) {
                displaySongs.add(s);
            }
        }
        SimpleAdapter adapter = new SimpleAdapter(getActivity(), displaySongs, R.layout.item_song,
            new String[]{"title", "artist", "duration"},
            new int[]{R.id.tvTitle, R.id.tvArtist, R.id.tvDuration});
        listSongs.setAdapter(adapter);
        tvCount.setText(displaySongs.size() + " 首歌");
        listSongs.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                if (!isDetached && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).playSongAt(new ArrayList<Map<String, String>>(displaySongs), pos);
                }
            }
        });
    }
}
