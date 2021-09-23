package com.example.audiobookplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.audiobookplayer.db.AudioBookContract;
import com.example.audiobookplayer.db.AudioBookDBHelper;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity implements Runnable{
    AudioBookRecyclerViewAdapter adapter;
    RecyclerView recyclerView;
    AudioService audioService;
    boolean serviceBound = false;

    ArrayList<AudioRecording> audioList;
    private static final int REQUEST_PERMISSION = 0;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.audiobookplayer.AudioService.PlayAnotherAudio";
    public static final String Broadcast_SEEK_BAR_CHANGED = "com.example.audiobookplayer.AudioService.SeekBarChanged";
    public static final String Broadcast_PROGRESS_BAR_UPDATED = "com.example.audiobookplayer.AudioService.PROGRESSBarUPDATED";
    public static final String Broadcast_SEEKBAR_START = "com.example.audiobookplayer.AudioService.SeekBarStart";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            int hasWritePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int hasReadPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

            List<String> permissions = new ArrayList<>();
            if (hasWritePermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
//              preferencesUtility.setString("storage", "true");
            }

            if (hasReadPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);


            } else {
//              preferencesUtility.setString("storage", "true");
            }

            if (!permissions.isEmpty()) {
//              requestPermissions(permissions.toArray(new String[permissions.size()]), REQUEST_CODE_SOME_FEATURES_PERMISSIONS);

                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE},
                        REQUEST_PERMISSION);
            }

        }
        loadAudio();
        initRecyclerView();
        _startSeekbarListener();



    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, AudioService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serviceBound = false;
        unregisterReceiver(seekBarStart);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION: {
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {


                        System.out.println("Permissions --> " + "Permission Granted: " + permissions[i]);


                    } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        System.out.println("Permissions --> " + "Permission Denied: " + permissions[i]);

                    }
                }
            }
            break;
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }

    }

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioService.MyBinder binder = (AudioService.MyBinder) service;
            audioService = binder.getService();

            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            serviceBound = false;
        }
    };

    public void initRecyclerView(){
        recyclerView = findViewById(R.id.book_audio_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        if(audioList != null) {
            adapter = new AudioBookRecyclerViewAdapter(this, audioList);
        }

        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);


    }


    public void loadAudio(){
        Set<String> mySet = new HashSet();
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection  = MediaStore.Audio.Media.IS_MUSIC + "!= 0";

        Cursor cursor = contentResolver.query(uri, null, selection, null, null);

        if(cursor != null && cursor.getCount() > 0){
            audioList = new ArrayList<>();
            while(cursor.moveToNext()){
                String audioName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                int duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                int idColumn = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                if(!mySet.contains(audioName)){
                    audioList.add(new AudioRecording(audioName, idColumn, duration));
                    mySet.add(audioName);
                }

            }
        }
        cursor.close();
    }

    @Override
    public void run() {
        if(audioService != null) {
            int currentPosition;
            int total = 0;
            int position = new StorageUtil(this).loadAudioIndex();
            AudioBookRecyclerViewAdapter.AudioBookViewHolder viewHolder = (AudioBookRecyclerViewAdapter.AudioBookViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
            total = viewHolder.duration;
            while (audioService.getCurrentPostion() < total && audioService.getPlaybackStatus()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                currentPosition = audioService.getCurrentPostion();
                if(viewHolder != null) {

                    viewHolder.seekBar.setProgress(currentPosition);
                }
                viewHolder = (AudioBookRecyclerViewAdapter.AudioBookViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
            }
            if(viewHolder != null && audioService.getCurrentPostion() == viewHolder.duration){
                viewHolder.seekBar.setProgress(0);

            }

        }
        else{
            Log.d("From main", "its null");
        }
    }

    BroadcastReceiver seekBarStart = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Thread thread = new Thread(MainActivity.this);
            thread.start();
        }
    };

    private void _startSeekbarListener(){
        IntentFilter intentFilter = new IntentFilter(Broadcast_SEEKBAR_START);
        registerReceiver(seekBarStart, intentFilter);
    }
}