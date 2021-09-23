package com.example.audiobookplayer.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import androidx.annotation.Nullable;

public final class AudioBookContract {
    private AudioBookContract(){ };

    public static class ResumePositions implements BaseColumns{
        public static final String TABLE_NAME = "songResume";
        public static final String _ID = BaseColumns._ID;
        public static final String RESUME_POSITION = "resumePosition";
        public static final String SONG_NAME = "songName";
    }

}

