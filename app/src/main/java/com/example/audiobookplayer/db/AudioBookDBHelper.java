package com.example.audiobookplayer.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class AudioBookDBHelper extends SQLiteOpenHelper {

    final static public String DATA_BASE_NAME = "AudioBookDB.db";
    final static public int DATA_BASE_VERSION = 2;

    public AudioBookDBHelper(@Nullable Context context) {
        super(context, DATA_BASE_NAME, null, DATA_BASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_ENTRIES);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("form helper", "here we are");
        db.execSQL(SQL_DELETE_ENTRIES);
        // Создаём новую таблицу
        onCreate(db);
    }


    private final static String SQL_CREATE_ENTRIES = "CREATE TABLE " + AudioBookContract.ResumePositions.TABLE_NAME + "("
            + AudioBookContract.ResumePositions._ID + " INTEGER PRIMARY KEY, "
            + AudioBookContract.ResumePositions.SONG_NAME + " TEXT, "
            + AudioBookContract.ResumePositions.RESUME_POSITION + " TEXT" + ")";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + AudioBookContract.ResumePositions.TABLE_NAME;

}