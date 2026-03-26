package fr.charleslabs.electrodb.utils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.TreeMap;

public class JSONutils {
    /**
     * Converts a JSONArray to a String Array.
     * @param array The JSONArray object to convert.
     * @return A String[].
     */
    public static String[] toStringArray(JSONArray array){
        if(array==null) return null;
        String[] arr = new String[array.length()];
        for(int i=0;i<arr.length;i++)
            arr[i]=array.optString(i);
        return arr;
    }

    public static TreeMap<String,String> toTreeMap(JSONArray array) throws JSONException {
        if(array==null) return null;
        TreeMap<String,String> map = new TreeMap<>();

        String pinKey;

        for(int i=0;i<array.length();i++){
            pinKey=array.getJSONObject(i).keys().next();
            map.put(pinKey,array.getJSONObject(i).getString(pinKey));
        }

        return map;
    }

    public static String fileToString(InputStream stream) throws IOException {
        final int bufferSize = 1024;
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(stream,"UTF-8");
        for (;;){
            int rsz = in.read(buffer,0,buffer.length);
            if (rsz <0)
                break;
            out.append(buffer,0,rsz);
        }
        return out.toString();
    }
}