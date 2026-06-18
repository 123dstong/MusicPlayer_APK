package com.music.player;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "musicpro.db";
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE favorites (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "song_id TEXT," +
            "title TEXT," +
            "artist TEXT," +
            "url TEXT," +
            "source TEXT," +
            "artwork TEXT," +
            "duration TEXT," +
            "UNIQUE(song_id, source))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS favorites");
        onCreate(db);
    }

    public boolean addFavorite(String songId, String title, String artist, String url, String source, String artwork, String duration) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("song_id", songId);
        cv.put("title", title);
        cv.put("artist", artist);
        cv.put("url", url);
        cv.put("source", source);
        cv.put("artwork", artwork);
        cv.put("duration", duration);
        long result = db.insertWithOnConflict("favorites", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return result != -1;
    }

    public boolean removeFavorite(String songId, String source) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete("favorites", "song_id=? AND source=?", new String[]{songId, source}) > 0;
    }

    public boolean isFavorite(String songId, String source) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("favorites", null, "song_id=? AND source=?",
            new String[]{songId, source}, null, null, null);
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    public List<Map<String, String>> getFavorites() {
        List<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("favorites", null, null, null, null, null, "id DESC");
        while (c.moveToNext()) {
            Map<String, String> song = new HashMap<>();
            song.put("id", c.getString(c.getColumnIndexOrThrow("song_id")));
            song.put("title", c.getString(c.getColumnIndexOrThrow("title")));
            song.put("artist", c.getString(c.getColumnIndexOrThrow("artist")));
            song.put("url", c.getString(c.getColumnIndexOrThrow("url")));
            song.put("source", c.getString(c.getColumnIndexOrThrow("source")));
            song.put("artwork", c.getString(c.getColumnIndexOrThrow("artwork")));
            song.put("duration", c.getString(c.getColumnIndexOrThrow("duration")));
            list.add(song);
        }
        c.close();
        return list;
    }

    public int getFavoriteCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM favorites", null);
        c.moveToFirst();
        int count = c.getInt(0);
        c.close();
        return count;
    }
}
