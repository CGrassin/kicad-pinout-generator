package fr.charleslabs.tinwhistletabs.utils;

import android.text.Layout;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class AndroidUtils {
    public static void clearSpans(@NonNull final Spannable editable) {
        final Object[] spans = editable.getSpans(0, editable.length(), Object.class);

        for (final Object span : spans) {
            if (span instanceof ForegroundColorSpan) {
                editable.removeSpan(span);
            }
        }
    }

    public static int getCharacterOffset(TextView textView, int x, int y) {
        x += textView.getScrollX() - textView.getTotalPaddingLeft();
        y += textView.getScrollY() - textView.getTotalPaddingTop();

        final Layout layout = textView.getLayout();

        final int lineCount = layout.getLineCount();
        if (lineCount == 0 || y < layout.getLineTop(0) || y >= layout.getLineBottom(lineCount - 1))
            return -1;

        final int line = layout.getLineForVertical(y);
        if (x < layout.getLineLeft(line) || x >= layout.getLineRight(line))
            return -1;

        int start = layout.getLineStart(line);
        int end = layout.getLineEnd(line);

        while (end > start + 1) {
            int middle = start + (end - start) / 2;

            if (x >= layout.getPrimaryHorizontal(middle)) {
                start = middle;
            }
            else {
                end = middle;
            }
        }

        return start;
    }


}
