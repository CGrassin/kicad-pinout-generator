package fr.charleslabs.tinwhistletabs.android;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

public class SingleTapTouchListener implements View.OnTouchListener {
    /**
     * Callback interface.
     */
    public interface SingleTapCallback{
        void singleTapCallback(SingleTapTouchListener origin, View v, MotionEvent event);
    }

    private static final int MAX_CLICK_DURATION = 200;
    private long startClickTime = 0;
    private final SingleTapCallback callback;

    public SingleTapTouchListener(SingleTapCallback singleTapCallback) {
        this.callback = singleTapCallback;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN && event.getPointerCount() == 1) {
            startClickTime = event.getEventTime();
        }
        else if (event.getAction() == MotionEvent.ACTION_UP && event.getPointerCount() == 1) {
            final long clickDuration = event.getEventTime() - startClickTime;
            if(clickDuration < MAX_CLICK_DURATION) {
                callback.singleTapCallback(this,v,event);
            }
        }
        return true;
    }
}
