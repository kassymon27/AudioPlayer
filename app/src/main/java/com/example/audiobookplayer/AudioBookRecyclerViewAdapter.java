package com.example.audiobookplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.audiobookplayer.db.AudioBookContract;
import com.example.audiobookplayer.db.AudioBookDBHelper;

import java.util.ArrayList;

import static java.security.AccessController.getContext;


public class AudioBookRecyclerViewAdapter extends RecyclerView.Adapter<AudioBookRecyclerViewAdapter.AudioBookViewHolder> {
    int numberOfItems;
    ArrayList<AudioRecording> audioList;
    Context context;
    AudioBookDBHelper helper;

    AudioBookRecyclerViewAdapter(Context context, ArrayList<AudioRecording> audioRecordings) {
        this.context = context;
        this.numberOfItems = audioRecordings.size();
        audioList = audioRecordings;
        helper = new AudioBookDBHelper(context);

    }

    @NonNull
    @Override
    public AudioBookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int idOfLayout = R.layout.audio_file_layout;
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(idOfLayout, parent, false);
        AudioBookViewHolder audioBookViewHolder = new AudioBookViewHolder(view);
        return audioBookViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AudioBookViewHolder holder, int position) {
        holder.onBind(position);
    }

    @Override
    public int getItemCount() {
        return numberOfItems;
    }


    class AudioBookViewHolder extends RecyclerView.ViewHolder implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {
        TextView songName;
        SeekBar seekBar;
        Button button;
        int duration;
        int audioPosition;

        AudioBookViewHolder(View view) {
            super(view);
            songName = view.findViewById(R.id.textView);
            seekBar = view.findViewById(R.id.seekBar2);
            button = view.findViewById(R.id.button);


            seekBar.setOnSeekBarChangeListener(this);
            button.setOnClickListener(this);
        }

        private void onBind(int position) {
            audioPosition = position;
            songName.setText(audioList.get(position).getAudioName());
            duration = audioList.get(position).getAudioDuration();
            seekBar.setMax(duration);


        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Intent intent = new Intent();
            intent.setAction(MainActivity.Broadcast_SEEK_BAR_CHANGED);
            intent.putExtra("New_seekbar_position", seekBar.getProgress());
            intent.putExtra("SeekBar's index", audioPosition);
            context.sendBroadcast(intent);

        }

        @Override
        public void onClick(View v) {
            playAudio(audioPosition);

        }

        public void playAudio(int position) {
            StorageUtil storageUtil = new StorageUtil(context);
            storageUtil.storeAudioIndex(position);
            storageUtil.storeAudio(audioList);

            Intent intent = new Intent();
            intent.setAction(MainActivity.Broadcast_PLAY_NEW_AUDIO);
            context.sendBroadcast(intent);

        }


    }
}


