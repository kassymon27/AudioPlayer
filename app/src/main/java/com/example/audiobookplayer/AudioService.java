package com.example.audiobookplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.SeekBar;

import androidx.annotation.Nullable;

import com.example.audiobookplayer.db.AudioBookContract;
import com.example.audiobookplayer.db.AudioBookDBHelper;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;

import static java.security.AccessController.getContext;

public class AudioService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener, AudioManager.OnAudioFocusChangeListener{
    IBinder iBinder = new MyBinder();
    MediaPlayer mediaPlayer;
    AudioManager audioManager;

    int resumePosition = 0;
    AudioRecording activeAudio;
    ArrayList<AudioRecording> audioList;
    int audioIndex = -1;
    private  boolean isPlaying = false;
    AudioBookDBHelper helper;
    SQLiteDatabase readableDB;
    SQLiteDatabase writablleDB;


    @Override
    public void onCreate() {
        super.onCreate();
        register_playNewAudio();
        register_seekBarChanged();
        helper = new AudioBookDBHelper(getApplicationContext());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        openWritableDB();
        Log.d("From onDestroy","we are in onDestroy");
        removeAudioFocus();
        helper.close();
        unregisterReceiver(playAnotherAudio);
        unregisterReceiver(seekBarChanged);
        new StorageUtil(getApplicationContext()).cleanSharedPreferences();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        return super.onStartCommand(intent, flags, startId);
    }

    public void initMediaPlayer(){
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.setDataSource(getApplication(), Uri.parse(activeAudio.getData()));
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }

        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }

        openReadableDB();
        mediaPlayer.prepareAsync();
    }

    private BroadcastReceiver playAnotherAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                StorageUtil storageUtil = new StorageUtil(getApplicationContext());
                int prevAudioIndex = audioIndex;
                audioIndex = storageUtil.loadAudioIndex();
                audioList = storageUtil.loadAudio();


                if (audioIndex != -1 && audioIndex < audioList.size()) {

                    if(prevAudioIndex == audioIndex && mediaPlayer.isPlaying()){
                        Log.d("from Playing points", "point one");
                        pauseMedia();
                    }else if(prevAudioIndex == audioIndex && !mediaPlayer.isPlaying()){
                        Log.d(" from Playing points", "point two");
                        resumeMedia();
                    }else {
                        stopMedia();
                        activeAudio = audioList.get(audioIndex);
                        Log.d(" from Playing points", "point three");
                        if(mediaPlayer != null){
                             mediaPlayer.reset();
                            Log.d(" from Playing points", "point four");
                        }

                        initMediaPlayer();
                    }


                } else {
                    stopSelf();
                }
            }catch (NullPointerException e){
                stopSelf();

            }


        }
    };

    private BroadcastReceiver seekBarChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            resumePosition = intent.getIntExtra("New_seekbar_position", 0);
            int seekBarsindex = intent.getIntExtra("SeekBar's index", 0);
            Log.d(" from seekBarChange", seekBarsindex + " "  + activeAudio.getAudioName());
            if(mediaPlayer != null && seekBarsindex == audioIndex) {
                mediaPlayer.seekTo(resumePosition);
            }

            if(isPlaying){
                playMedia();
            }
        }
    };


    private void playMedia(){
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
            isPlaying = true;
            startSeekBar();
        }
    }

    private void stopMedia(){
        if(mediaPlayer == null){
            return;
        }
        if(mediaPlayer.isPlaying()){
            resumePosition = mediaPlayer.getCurrentPosition();
            openWritableDB();
            mediaPlayer.stop();
            isPlaying = false;
        }
    }

    private void pauseMedia(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
            openWritableDB();
            isPlaying = false;
        }
    }

    private void resumeMedia(){
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
            isPlaying = true;
            startSeekBar();
        }
    }

    private void startSeekBar(){
        Intent intent = new Intent();
        intent.setAction(MainActivity.Broadcast_SEEKBAR_START);
        sendBroadcast(intent);
    }



    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        resumePosition = 0;
        stopMedia();
        stopSelf();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        switch (what){
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("Media Player Error","MEDIA_ERROR_UNKNOWN" + extra);
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("Media Player Error","MEDIA_ERROR_SERVER_DIED" + extra);
        }
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        playMedia();

    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }
    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }




    public class MyBinder extends Binder {
        AudioService getService(){
            return AudioService.this;

        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    public int getCurrentPostion() {
        if (mediaPlayer != null)
        {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;


    }

    public boolean getPlaybackStatus(){
        return isPlaying;
    }

    private void openReadableDB(){
        if(activeAudio != null){
            readableDB = helper.getReadableDatabase();
            String selection = AudioBookContract.ResumePositions.SONG_NAME + " = ?";
            String[] selectionArgs  = {activeAudio.getAudioName()};
            Log.d("AudioName", activeAudio.getAudioName());
            String sortOrder = AudioBookContract.ResumePositions.SONG_NAME + " DESC";
            String[] projection = { AudioBookContract.ResumePositions.RESUME_POSITION };


            Cursor cursor = readableDB.query(AudioBookContract.ResumePositions.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            if(!cursor.moveToNext()){
                Log.d("From Cursor", "we are creating a new row");
                resumePosition = 0;
                AudioBookDBHelper myHelper = new AudioBookDBHelper(getApplicationContext());
                SQLiteDatabase myDb = myHelper.getWritableDatabase();
                ContentValues contentValues = new ContentValues();
                contentValues.put(AudioBookContract.ResumePositions.SONG_NAME, activeAudio.getAudioName());
                contentValues.put(AudioBookContract.ResumePositions.RESUME_POSITION, 0);

                myDb.insert(AudioBookContract.ResumePositions.TABLE_NAME, null, contentValues);
                myHelper.close();
            }else {
                resumePosition = cursor.getInt(cursor.getColumnIndexOrThrow(AudioBookContract.ResumePositions.RESUME_POSITION));
                Log.d("From Cursor", "We are starting from recorded position " + String.valueOf(resumePosition) + " " + activeAudio.getAudioName());
            }
            cursor.close();
        }



    }

    private void openWritableDB(){
        if(activeAudio != null) {

            writablleDB = helper.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            Log.d("From WB", "We are saving a resume position " + resumePosition + " " + activeAudio.getAudioName());
            contentValues.put(AudioBookContract.ResumePositions.RESUME_POSITION, resumePosition);

            String selection = AudioBookContract.ResumePositions.SONG_NAME + " LIKE ?";
            String[] selectionArgs = {activeAudio.getAudioName()};
            writablleDB.update(AudioBookContract.ResumePositions.TABLE_NAME, contentValues, selection, selectionArgs);

        }
    }


    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playAnotherAudio, filter);
    }

    private void register_seekBarChanged() {
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_SEEK_BAR_CHANGED);
        registerReceiver(seekBarChanged, filter);
    }

}
