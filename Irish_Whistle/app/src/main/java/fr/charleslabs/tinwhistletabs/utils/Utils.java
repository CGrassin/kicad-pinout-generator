package fr.charleslabs.tinwhistletabs.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class Utils {

    public static float ensureRange(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }

    public static String fileToString(InputStream stream) throws IOException {
        final int bufferSize = 1024;
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        Reader in = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            in = new InputStreamReader(stream, StandardCharsets.UTF_8);
        } else {
            in = new InputStreamReader(stream, "UTF-8");
        }
        for (;;){
            int rsz = in.read(buffer,0,buffer.length);
            if (rsz <0)
                break;
            out.append(buffer,0,rsz);
        }
        return out.toString();
    }
}
