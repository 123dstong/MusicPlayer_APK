package com.music.player;

import android.app.Fragment;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.List;
import java.util.Map;

public class FragmentMine extends Fragment {

    private TextView tvFavCount, tvCacheSize;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);
        tvFavCount = (TextView) view.findViewById(R.id.tvFavCount);
        tvCacheSize = (TextView) view.findViewById(R.id.tvCacheSize);
        LinearLayout menuFavorites = (LinearLayout) view.findViewById(R.id.menuFavorites);
        LinearLayout menuHistory = (LinearLayout) view.findViewById(R.id.menuHistory);
        LinearLayout menuCache = (LinearLayout) view.findViewById(R.id.menuCache);
        LinearLayout menuAbout = (LinearLayout) view.findViewById(R.id.menuAbout);

        menuFavorites.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showFavorites(); }
        });
        menuHistory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getActivity(), "播放历史: 首页播放记录", Toast.LENGTH_SHORT).show();
            }
        });
        menuCache.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { manageCache(); }
        });
        menuAbout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showAbout(); }
        });

        updateInfo();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateInfo();
    }

    private void updateInfo() {
        DatabaseHelper db = new DatabaseHelper(getActivity());
        int favCount = db.getFavoriteCount();
        tvFavCount.setText(favCount + " 首");
        db.close();
        long cacheSize = getCacheSize();
        tvCacheSize.setText(formatSize(cacheSize));
    }

    private void showFavorites() {
        DatabaseHelper db = new DatabaseHelper(getActivity());
        final List<Map<String, String>> favs = db.getFavorites();
        db.close();
        if (favs.isEmpty()) {
            Toast.makeText(getActivity(), "暂无收藏", Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < favs.size(); i++) {
            Map<String, String> song = favs.get(i);
            sb.append((i + 1)).append(". ").append(song.get("title"))
              .append(" - ").append(song.get("artist")).append("\n");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("我的收藏 (" + favs.size() + " 首)");
        builder.setMessage(sb.toString());
        builder.setPositiveButton("播放全部", new AlertDialog.OnClickListener() {
            public void onClick(android.content.DialogInterface d, int w) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).playSongAt(favs, 0);
                }
            }
        });
        builder.setNegativeButton("关闭", null);
        builder.show();
    }

    private void manageCache() {
        long size = getCacheSize();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("缓存管理");
        builder.setMessage("当前缓存: " + formatSize(size));
        builder.setPositiveButton("清除缓存", new AlertDialog.OnClickListener() {
            public void onClick(android.content.DialogInterface d, int w) {
                clearCache();
                updateInfo();
                Toast.makeText(getActivity(), "缓存已清除", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("关闭", null);
        builder.show();
    }

    private void showAbout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("关于 MusicPro");
        builder.setMessage("版本: 1.0\n\n功能:\n- 本地音乐播放\n- 在线音乐 (Audius)\n- 网络电台 (RadioBrowser)\n- 收藏管理\n\n所有内容均来自合法免费API");
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    private long getCacheSize() {
        return getDirSize(getActivity().getCacheDir());
    }

    private long getDirSize(File dir) {
        long size = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) size += f.length();
                    else size += getDirSize(f);
                }
            }
        }
        return size;
    }

    private void clearCache() {
        deleteDir(getActivity().getCacheDir());
    }

    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) { deleteDir(f); }
            }
        }
        return dir.delete();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
