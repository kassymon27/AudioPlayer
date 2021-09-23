package com.example.audiobookplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.MediaStore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class StorageUtil {
    private SharedPreferences preferences;
    private Context context;
    private final String STORAGE = "com.example.audiobookplayer.STORAGE";

    StorageUtil(Context context) {
        this.context = context;
    }

    public void storeAudio(ArrayList<AudioRecording> audioList) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String stringAudioList = gson.toJson(audioList);
        editor.putString("audioList", stringAudioList);
        editor.apply();
    }

    public ArrayList<AudioRecording> loadAudio(){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        ArrayList<AudioRecording> audioList;
        String stringAudioList;

        SharedPreferences.Editor editor = preferences.edit();
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        stringAudioList = preferences.getString("audioList", null);
        Type type = new TypeToken<ArrayList<AudioRecording>>() {
        }.getType();
        audioList = gson.fromJson(stringAudioList, type);
        return audioList;
    }

    public void storeAudioIndex(int index){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();

        editor.putInt("audioListIndex", index);
        editor.apply();
    }

    public int loadAudioIndex(){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        return preferences.getInt("audioListIndex", -1);
    }

    public void cleanSharedPreferences(){
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();

        editor.clear();
        editor.commit();
    }


}