package fr.charleslabs.tinwhistletabs.android;

import android.util.TypedValue;
import android.view.ScaleGestureDetector;
import android.widget.TextView;

import androidx.annotation.NonNull;

import fr.charleslabs.tinwhistletabs.utils.Utils;

public class TextViewScaleGestureDetector extends ScaleGestureDetector.SimpleOnScaleGestureListener{
    private static final float MIN_FONT_FACTOR = 0.75f, MAX_FONT_FACTOR = 2.0f;

    private final float minFontSize;
    private final float maxFontSize;
    private final TextView textView;

    public TextViewScaleGestureDetector(@NonNull TextView textView){
        minFontSize = textView.getTextSize()*MIN_FONT_FACTOR;
        maxFontSize = textView.getTextSize()*MAX_FONT_FACTOR;
        this.textView = textView;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float newFontSize = Utils.ensureRange(textView.getTextSize()*detector.getScaleFactor(),
                minFontSize,
                maxFontSize);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, newFontSize);
        return true;
    }
}