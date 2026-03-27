package fr.charleslabs.tinwhistletabs.music;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

import fr.charleslabs.tinwhistletabs.utils.Utils;

public class MusicSheet implements Serializable {
    // Allocated at construction
    private final String title;
    private final String author;
    private final String file;
    private final String abc_file;   // filename of the ABC file, replaces inline abc
    private final String type;
    private final String sheet_author;
    private final String license;
    private final String key;
    private final String whistle;

    MusicSheet(JSONObject jsonObject) throws JSONException {
        // Mandatory
        this.title = jsonObject.getString("title");
        this.file  = jsonObject.getString("file");
        this.type  = jsonObject.getString("type");

        // abc_file replaces abc — fall back to null if old-format entry
        this.abc_file = jsonObject.has("abc_file") ? jsonObject.getString("abc_file") : null;

        // Optional
        this.author       = jsonObject.optString("author",       null);
        this.sheet_author = jsonObject.optString("sheet_author", null);
        this.license      = jsonObject.optString("license",      null);
        this.key          = jsonObject.optString("key",          MusicSettings.DEFAULT_KEY);
        this.whistle      = jsonObject.optString("whistle",      "D");
    }

    // Lazy-load ABC from its dedicated file
    public String getABC(Context context) {
        if (abc_file == null) return null;
        try {
            return MusicDB.openRessource(context, abc_file);
        } catch (Exception e) {
            return null;
        }
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
        return this.getTitle().toLowerCase().contains(search);
    }

    // Getters
    public String getAuthor()      { return author; }
    public String getTitle()       { return title; }
    public String getFile()        { return file; }
    public String getAbcFile()     { return abc_file; }
    public String getKey()         { return key; }
    public String getType()        { return type; }
    public String getSheetAuthor() { return sheet_author; }
    public String getLicense()     { return license; }
    public String getWhistle()     { return whistle; }
}