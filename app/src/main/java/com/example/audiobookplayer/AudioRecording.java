package com.example.audiobookplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.Serializable;
import java.util.Objects;

public class AudioRecording implements Serializable {
    private String audioName;
    private int audioDuration;
    private int idColumn;
    private String data;

    AudioRecording(String audioName, int idColumn,int audioDuration){
        this.audioName = audioName;
        this.audioDuration = audioDuration;
        this.idColumn = idColumn;

        data = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, idColumn).toString();
    }


    public String getAudioName(){
        return audioName;
    }

    public int getAudioDuration(){
        return audioDuration;
    }

    public String getData(){
        return data;
    }

    public void setAudioName(String audioName){
        this.audioName = audioName;

    }

    public void setAudioDuration(int audioDuration){
        this.audioDuration = audioDuration;
    }

    public void setData(String data){
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioRecording that = (AudioRecording) o;
        return audioDuration == that.audioDuration &&
                audioName.equals(that.audioName);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = audioDuration;
        result = prime * result
                + ((audioName == null) ? 0 : audioName.hashCode());
        return result;
    }
}
