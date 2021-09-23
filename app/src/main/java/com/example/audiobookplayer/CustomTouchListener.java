package com.example.audiobookplayer;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CustomTouchListener implements RecyclerView.OnItemTouchListener {

    private OnItemTouchListener onItemTouchListener;
    private GestureDetector gestureDetector;

    CustomTouchListener(Context context, final OnItemTouchListener onItemTouchListener){
        this.onItemTouchListener = onItemTouchListener;
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });
    }


    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        if(gestureDetector.onTouchEvent(e)) {
            View child = rv.findChildViewUnder(e.getX(), e.getY());
            onItemTouchListener.onClick(child, rv.getChildLayoutPosition(child));
        }
        return false;

    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }
}
