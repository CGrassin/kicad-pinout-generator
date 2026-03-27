package fr.charleslabs.tinwhistletabs;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

import fr.charleslabs.tinwhistletabs.music.MusicDB;
import fr.charleslabs.tinwhistletabs.utils.Utils;

public class SheetActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sheet);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sheetRootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            toolbar.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        // Fetch intent
        final Intent intent = getIntent();
        if (!intent.hasExtra(TabActivity.EXTRA_ABC) ||
                !intent.hasExtra(TabActivity.EXTRA_SHEET_TITLE))
            finish();


        final String abc_content;
        try {
            abc_content = MusicDB.openResource(this, (String)intent.getSerializableExtra(TabActivity.EXTRA_ABC));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final String abc = Utils.escape(abc_content);

        final String title = (String)intent.getSerializableExtra(TabActivity.EXTRA_SHEET_TITLE);

        // Set action bar title
        try{
            ActionBar actionBar = this.getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.sheetActivity_title,title));
        }catch (Exception ignored){}

        // Render page
        final WebView myWebView = findViewById(R.id.sheetActivity_sheet);
        myWebView.loadUrl("file:///android_asset/sheet.html");
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView web, String url) {
                web.loadUrl("javascript:" + "(function(){ABCJS.renderAbc(\"paper\", \"" + abc +
                        "\", {responsive:\"resize\"});})()");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return true;
    }



}
