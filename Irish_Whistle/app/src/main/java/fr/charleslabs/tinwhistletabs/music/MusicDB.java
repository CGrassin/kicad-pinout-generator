package fr.charleslabs.tinwhistletabs.music;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.charleslabs.tinwhistletabs.R;
import fr.charleslabs.tinwhistletabs.utils.Utils;

public class MusicDB {
    public List<MusicSheet> musicDB = new ArrayList<>();
    public BookmarkManager bookmarks;

    //Singleton
    private static MusicDB instance;
    public static synchronized MusicDB getInstance(Context c){
        if(instance == null){
            instance = new MusicDB(c.getApplicationContext());
        }
        return instance;
    }

    private MusicDB(Context c){
        try {
            String fileContent =  Utils.fileToString(c.getResources().openRawResource(R.raw.db));

            JSONArray jsonSheets = new JSONArray(fileContent);

            for (int i=0; i < jsonSheets.length(); i++) {
                musicDB.add(new MusicSheet(jsonSheets.getJSONObject(i)));
            }

            // Get bookmarks
            bookmarks = new BookmarkManager(c);

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static String openRessource(Context c, String filename) throws IOException {
        int resId = c.getResources().getIdentifier(filename, "raw", c.getPackageName());
        if (resId == 0)
            throw new IOException("Resource not found: " + filename);
        return Utils.fileToString(c.getResources().openRawResource(resId));
    }

    public static List<MusicNote> getNotes(Context c, String filename) throws IOException {
        List<MusicNote> notes = new ArrayList<>();
        String fileContent = openRessource(c,filename);
        String[] notesArray = fileContent.split(",");
        for (String note : notesArray){
            final String[] split = note.split("/");
            if (split.length >= 2) {
                notes.add(new MusicNote(Integer.parseInt(split[0]), Integer.parseInt(split[1])));
            }
        }
        return notes;
    }
}
