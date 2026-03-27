package fr.charleslabs.tinwhistletabs.music;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BookmarkManager {

    private static final String PREFS_NAME = "bookmarks";
    private static final String KEY_IDS    = "bookmarked_ids";

    private final SharedPreferences prefs;

    public BookmarkManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void add(String tuneId) {
        Set<String> current = getMutableSet();
        current.add(tuneId);
        save(current);
    }

    public void remove(String tuneId) {
        Set<String> current = getMutableSet();
        current.remove(tuneId);
        save(current);
    }

    public void toggle(String tuneId) {
        if (isBookmarked(tuneId)) remove(tuneId);
        else add(tuneId);
    }

    public boolean isBookmarked(String tuneId) {
        return getAll().contains(tuneId);
    }

    public Set<String> getAll() {
        return Collections.unmodifiableSet(
                prefs.getStringSet(KEY_IDS, new HashSet<>())
        );
    }

    // --- private helpers ---

    private Set<String> getMutableSet() {
        return new HashSet<>(prefs.getStringSet(KEY_IDS, new HashSet<>()));
    }

    private void save(Set<String> ids) {
        prefs.edit().putStringSet(KEY_IDS, ids).apply();
    }
}