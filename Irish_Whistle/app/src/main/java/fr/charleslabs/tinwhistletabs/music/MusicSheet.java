package fr.charleslabs.tinwhistletabs.music;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

public class MusicSheet implements Serializable {
    // Constants
    /* final static public int DIFFICULTY_EASY = 0,
            DIFFICULTY_MEDIUM = 1,
            DIFFICULTY_HARD = 2;*/

    // Allocated at construction
    private final String title;
    private final String author;
    private final String file;
    private final String type;
    private final String sheet_author;
    private final String license ;
    private final String key;

    private final String whistle;

    private String abc;
    //public int difficulty = DIFFICULTY_MEDIUM;

    MusicSheet(JSONObject jsonObject) throws JSONException {
        // Mandatory
        this.title = jsonObject.getString("title");
        this.file = jsonObject.getString("file");
        this.type = jsonObject.getString("type");
        if (jsonObject.has("abc")) this.abc = jsonObject.getString("abc");

        // Optional
        //if (jsonObject.has("difficulty")) this.difficulty = jsonObject.getInt("difficulty");
        if (jsonObject.has("author")) this.author = jsonObject.getString("author");
        else this.author = null;
        if (jsonObject.has("sheet_author")) this.sheet_author = jsonObject.getString("sheet_author");
        else this.sheet_author = null;
        if (jsonObject.has("license")) this.license = jsonObject.getString("license");
        else this.license = null;
        if (jsonObject.has("key")) this.key = jsonObject.getString("key");
        else this.key = MusicSettings.DEFAULT_KEY;
        if (jsonObject.has("whistle")) this.whistle = jsonObject.getString("whistle");
        else this.whistle = "D";
        //if (jsonObject.has("tempo")) this.tempo = jsonObject.getInt("tempo");
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
            if (i >= notes.size())
                return 0;
            if(!notes.get(i).isRest())
                trueNotes++;
            time += notes.get(i).getLengthInS(tempoModifier);
            i ++;
        }
        return time;
    }

    // Filter
    public boolean filter(final String search){
        return this.getTitle().toLowerCase().contains(search);
    }

    // Getter and setters
    public String getAuthor() {return author;}
    public String getTitle() {return title;}
    public String getFile() {return file;}
    public String getKey() {return key;}
    public String getType() {return type;}
    public String getSheetAuthor() {return sheet_author;}
    public String getLicense() {return license;}
    public String getABC() {return abc;}
    //public int getDifficulty() {return difficulty;}
    public String getWhistle() {
        return whistle;
    }
}
