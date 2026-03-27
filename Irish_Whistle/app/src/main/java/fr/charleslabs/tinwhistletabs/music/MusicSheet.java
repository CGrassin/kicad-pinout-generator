package fr.charleslabs.tinwhistletabs.music;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

import fr.charleslabs.tinwhistletabs.utils.Utils;

public class MusicSheet implements Serializable {
    private final String title;
    private final String by;
    private final String file;
    private final String abc_file;
    private final String type;
    private final String license;
    private final String key;
    private final String whistle;

    MusicSheet(JSONObject jsonObject) throws JSONException {
        this.title    = jsonObject.getString("title");
        this.file     = jsonObject.getString("file");
        this.type     = jsonObject.getString("type");
        this.abc_file = jsonObject.optString("abc_file", null);
        this.by       = jsonObject.optString("by",       null);
        this.license  = jsonObject.optString("lic",      null);
        this.key      = jsonObject.optString("key",      MusicSettings.DEFAULT_KEY);
        this.whistle  = jsonObject.optString("w",        "D");
    }

    public void transposeKey(final List<MusicNote> notes, final String oldKey, final String newKey){
        if (!oldKey.equals(newKey)) {
            final int shift = MusicSettings.getShift(newKey) - MusicSettings.getShift(oldKey);
            transpose(notes, shift);
        }
    }

    private static void transpose(final List<MusicNote> notes, final int shift){
        if(shift != 0)
            for(MusicNote note : notes)
                note.transpose(shift);
    }

    public static String notesToTabs(final List<MusicNote> notes) {
        StringBuilder buffer = new StringBuilder();
        for (MusicNote note : notes) {
            buffer.append(note.toTab());
        }
        return buffer.toString();
    }

    public static float noteIndexToTime(final List<MusicNote> notes, final int noteIndex,
                                        final float tempoModifier){
        float time = 0;
        int i = 0, trueNotes = 0;
        while (trueNotes < noteIndex) {
            if (i >= notes.size()) return 0;
            if (!notes.get(i).isRest()) trueNotes++;
            time += notes.get(i).getLengthInS(tempoModifier);
            i++;
        }
        return time;
    }

    public boolean filter(final String search){
        return Utils.normalize(this.getTitle()).contains(search);
    }

    // Getters
    public String getAuthor()  { return by; }
    public String getTitle()   { return title; }
    public String getFile()    { return file; }
    public String getAbcFile() { return abc_file; }
    public String getKey()     { return key; }
    public String getType()    { return type; }
    public String getLicense() { return license; }
    public String getWhistle() { return whistle; }
}