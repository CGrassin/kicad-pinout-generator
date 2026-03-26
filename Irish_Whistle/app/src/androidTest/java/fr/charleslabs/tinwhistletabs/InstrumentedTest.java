package fr.charleslabs.tinwhistletabs;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import fr.charleslabs.tinwhistletabs.music.MusicDB;
import fr.charleslabs.tinwhistletabs.music.MusicNote;
import fr.charleslabs.tinwhistletabs.music.MusicSheet;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class InstrumentedTest {
    private List<String> halfHoled = Arrays.asList("i","j","h","n","I","J","H","N","M");

    @Test
    public void checkSheets() throws IOException, JSONException {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Test proper sheets read
        List<MusicSheet> sheets = MusicDB.getInstance(appContext).musicDB;
        for (MusicSheet sheet : sheets){
            sheet.readNotes(appContext);

            for(MusicNote note:sheet.notes) {
                assertNotEquals(sheet.getTitle()+" contains invalid notes",note.toTab(),"?");
                if (note.toTab().equals("?")){
                    Log.e("INVALID NOTE",sheet.getTitle());
                    assertTrue(false);
                }

                if (halfHoled.contains(note.toTab())){
                    Log.w("HALF-HOLED",sheet.getTitle() + " contains half-holed notes.");
                    assertTrue(false);
                }

            }
        }

        assertEquals("fr.charleslabs.tinwhistletabs", appContext.getPackageName());
    }
}
