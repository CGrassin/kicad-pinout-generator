package fr.charleslabs.tinwhistletabs;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;

import java.util.List;

import fr.charleslabs.tinwhistletabs.android.SingleTapTouchListener;
import fr.charleslabs.tinwhistletabs.android.TextViewScaleGestureDetector;
import fr.charleslabs.tinwhistletabs.dialogs.KeyDialog;
import fr.charleslabs.tinwhistletabs.dialogs.SheetInfoDialog;
import fr.charleslabs.tinwhistletabs.dialogs.TempoDialog;
import fr.charleslabs.tinwhistletabs.music.MusicDB;
import fr.charleslabs.tinwhistletabs.music.MusicNote;
import fr.charleslabs.tinwhistletabs.music.MusicPlayer;
import fr.charleslabs.tinwhistletabs.music.MusicSettings;
import fr.charleslabs.tinwhistletabs.music.MusicSheet;
import fr.charleslabs.tinwhistletabs.utils.AndroidUtils;

public class TabActivity extends AppCompatActivity implements TempoDialog.TempoChangeCallback,
        KeyDialog.KeyChangeCallback, SingleTapTouchListener.SingleTapCallback {
    public static final String EXTRA_ABC= "fr.charleslabs.tinwhistletabs.ABC";
    public static final String EXTRA_SHEET_TITLE= "fr.charleslabs.tinwhistletabs.SHEET_TITLE";
    public static final float START_DELAY_AMOUNT = 1.5f; // s
    private static final int COUNTDOWN_STEPS = 3;
    private static final int SCROLL_DURATION = 750; // ms

    // States
    private  boolean isPlaying = false;
    private MusicSheet sheet = null;
    private int tempo = MusicSettings.DEFAULT_TEMPO;
    private final Handler musicHandler = new Handler();
    private List<MusicNote> notes;
    private int scroll_value = -1;

    // UI elements
    private ScrollView scrollView;
    private Spannable span = null;
    private  TextView tab = null;
    private  TextView countdownOverlay = null;
    private Menu menu;

    // Zoom
    private ScaleGestureDetector mScaleDetector;

    // Cursor
    private int cursorPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tabRootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            toolbar.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                stop();
                finish();
            }
        });

        // Get data
        final Intent intent = getIntent();
        if (!intent.hasExtra(MainActivity.EXTRA_SHEET)) finish();
        sheet = (MusicSheet)intent.getSerializableExtra(MainActivity.EXTRA_SHEET);

        // Set action bar title
        try{
            ActionBar actionBar = this.getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(sheet.getTitle());
        }catch (Exception ignored){}

        // Sheet UI
        tab = findViewById(R.id.TabActivity_tab);
        countdownOverlay = findViewById(R.id.TabActivity_countdown);
        try {
            notes = MusicDB.getNotes(this, sheet.getFile());
        } catch (Exception e) {
            finish();
        }
        scrollView = findViewById(R.id.TabActivity_tabScrollPane);
        tab.setText(MusicSheet.notesToTabs(notes), TextView.BufferType.SPANNABLE);
        span = (Spannable)tab.getText();
        this.sheet.transposeKey(notes, sheet.getKey(), MusicSettings.currentKey);

        // Media buttons
        findViewById(R.id.TabActivity_btnPlayPause).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                playPause();
            }
        });
        findViewById(R.id.TabActivity_btnStop).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stop();
            }
        });
        findViewById(R.id.TabActivity_btnInfo).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stop();
                Intent intent = new Intent(getApplicationContext(), SheetActivity.class);
                intent.putExtra(EXTRA_ABC, sheet.getAbcFile());
                intent.putExtra(EXTRA_SHEET_TITLE, sheet.getTitle());
                startActivity(intent);
            }
        });
        this.setTune();

        // Scale gesture
        mScaleDetector = new ScaleGestureDetector(this, new TextViewScaleGestureDetector(tab));
        // Tap on TextView
        tab.setOnTouchListener(new SingleTapTouchListener(this));
        // Bookmarks
        updateBookmarkUI();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.tabAction_tempo) {
            final DialogFragment tempoDialog = new TempoDialog(tempo, MusicSettings.isStartDelayed, this);
            tempoDialog.show(getSupportFragmentManager(), "dialog");
        } else if (id == R.id.tabAction_key) {
            final DialogFragment keyDialog = new KeyDialog(MusicSettings.currentKey, this);
            keyDialog.show(getSupportFragmentManager(), "dialog");
        } else if (id == R.id.tabAction_more) {
            final SheetInfoDialog sheetInfoDialog = new SheetInfoDialog(getApplicationContext(), sheet);
            sheetInfoDialog.show(getSupportFragmentManager(), "dialog");
        } else if (id == android.R.id.home) {
            this.stop();
            finish();
        } else if (id == R.id.tabAction_bookmark) {
            MusicDB.getInstance(this).bookmarks.toggle(sheet.getFile());
            updateBookmarkUI();
        }
        return true;
    }

    private void updateBookmarkUI() {
        if (menu == null) return;
        MenuItem bookmarkItem = menu.findItem(R.id.tabAction_bookmark);
        if (MusicDB.getInstance(this).bookmarks.isBookmarked(sheet.getFile())) {
            bookmarkItem.setIcon(R.drawable.ic_star);
            bookmarkItem.setTitle(R.string.TabActivity_remove_bookmark);
        } else {
            bookmarkItem.setIcon(R.drawable.ic_star_border);
            bookmarkItem.setTitle(R.string.TabActivity_add_bookmark);
        }
    }

    private void setTune() {
        // Disable buttons while loading
        findViewById(R.id.TabActivity_btnPlayPause).setEnabled(false);
        findViewById(R.id.TabActivity_btnStop).setEnabled(false);

        new Thread(() -> {
            try {
                final byte[] track = MusicPlayer.genMusic(notes, (float) tempo / 100f);
                runOnUiThread(() -> {
                    MusicPlayer.getInstance().setAudioTrack(track);
                    findViewById(R.id.TabActivity_btnPlayPause).setEnabled(true);
                    findViewById(R.id.TabActivity_btnStop).setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(TabActivity.this,
                            getString(R.string.error_tune_generation, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                    // buttons stay disabled
                });
            }
        }).start();
    }
    private void playPause(){
        if(!isPlaying){
            isPlaying = true;
            if (!MusicSettings.isStartDelayed)
                play();
            else{
                drawCursor(true);
                countdown(COUNTDOWN_STEPS-1);
            }
        } else {
            musicHandler.removeCallbacksAndMessages(null);
            MusicPlayer.getInstance().pause();
            countdownOverlay.setVisibility(View.GONE);
            isPlaying = false;
        }
    }

    @SuppressLint("SetTextI18n")
    private void countdown(final int stepsLeft){
        countdownOverlay.setVisibility(View.VISIBLE);
        countdownOverlay.setText(Integer.toString(stepsLeft+1));
        musicHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(stepsLeft <= 0){
                    countdownOverlay.setVisibility(View.GONE);
                    play();
                }
                else
                    countdown(stepsLeft - 1);
            }
        }, (long)((START_DELAY_AMOUNT * 1000)/COUNTDOWN_STEPS));
    }

    private void play(){
        scroll_value = -1; // invalidate scroll value
        MusicPlayer.getInstance().move(MusicSheet.noteIndexToTime(notes, cursorPos,(float)tempo/100f));
        moveCursor(musicHandler, cursorPos);
        MusicPlayer.getInstance().play();
    }

    private void stop(){
        countdownOverlay.setVisibility(View.GONE);
        cursorPos = 0;
        scroll_value = -1; // invalidate scroll value
        musicHandler.removeCallbacksAndMessages(null);
        AndroidUtils.clearSpans(span);
        MusicPlayer.getInstance().stop();
        isPlaying = false;
    }

    private void moveCursor(final Handler handler, final int index){
        if (index >= notes.size()){
            stop();
            return;
        }

        // Move cursor
        if(!notes.get(index).isRest()) {
            drawCursor(true);
            cursorPos++;
        }
        // Wait for next note
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index < notes.size() - 1)
                    moveCursor(handler,index+1);
                else
                    stop();
            }
        }, (long)(notes.get(index).getLengthInMS((float)tempo/100f)));
    }

    private void drawCursor(final boolean scroll){
        try {
                span.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)),
                        cursorPos, cursorPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                final Layout layout = tab.getLayout();
                final int current_scroll = layout.getLineTop(layout.getLineForOffset(cursorPos));
                if (scroll && current_scroll != scroll_value) {
                    scroll_value = current_scroll;
                    try {
                        ObjectAnimator.ofInt(scrollView, "scrollY",current_scroll)
                                .setDuration(SCROLL_DURATION).start();
                    } catch (Exception e) {
                        scrollView.scrollTo(0, current_scroll);
                    }
                }
        } catch(Exception ignored){}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tab_menu, menu);
        updateBookmarkUI(); // <-- move the call here, menu is ready now
        return true;
    }

    // Settings callbacks
    @Override
    public void tempoChangeCallback(int newTempo, boolean isDelayApplied) {
        if(newTempo != tempo) {
            this.stop();
            tempo = newTempo;
            this.setTune();
        }
        MusicSettings.isStartDelayed = isDelayApplied;
    }
    @Override
    public void keyChangeCallback(String newKey) {
        if(!newKey.equals(MusicSettings.currentKey)) {
            this.stop();
            this.sheet.transposeKey(notes, MusicSettings.currentKey, newKey);
            MusicSettings.currentKey = newKey;
            this.setTune();
        }
    }

    // Scale tab on pinch
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        super.dispatchTouchEvent(event);
        mScaleDetector.onTouchEvent(event);
        return true;
    }

    // Single tap in tab
    @Override
    public void singleTapCallback(SingleTapTouchListener origin, View v, MotionEvent event) {
        final int index = AndroidUtils.getCharacterOffset((TextView) v,(int) event.getX(),(int) event.getY());
        if (index<0) return;
        this.stop();
        cursorPos = index;
        AndroidUtils.clearSpans(span);
        drawCursor(false);
        MusicPlayer.getInstance().move(MusicSheet.noteIndexToTime(notes, cursorPos,(float)tempo/100f));
    }

}
