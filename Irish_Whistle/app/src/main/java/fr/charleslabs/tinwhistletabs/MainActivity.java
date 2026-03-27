package fr.charleslabs.tinwhistletabs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;

import java.util.List;

import fr.charleslabs.tinwhistletabs.android.SheetsAdapter;
import fr.charleslabs.tinwhistletabs.dialogs.AppCreditsDialog;
import fr.charleslabs.tinwhistletabs.music.MusicDB;
import fr.charleslabs.tinwhistletabs.music.MusicSheet;

public class MainActivity extends AppCompatActivity  {
    public static final String EXTRA_SHEET= "fr.charleslabs.tinwhistletabs.SHEET";
    private SheetsAdapter adapter;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply bottom padding for navigation bar
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            // Apply top padding to toolbar for status bar
            toolbar.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        final ListView listView = findViewById(R.id.sheetsList) ;
        List<MusicSheet> listData = MusicDB.getInstance(this).musicDB;
        adapter = new SheetsAdapter(this, listData, findViewById(R.id.noResultsFoundView));
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id){
                MusicSheet a = (MusicSheet) parent.getItemAtPosition(position);
                Intent intent = new Intent(getApplicationContext(), TabActivity.class);
                intent.putExtra(EXTRA_SHEET, a);
                startActivity(intent);
            }
        });

        findViewById(R.id.mainActivity_contact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                            "mailto",getString(R.string.dev_email), null));
                    intent.putExtra(Intent.EXTRA_SUBJECT, "TinWhistle App Contact");
                    startActivity(intent);
                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), getString(R.string.error_contact,e.getMessage()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        final MenuItem searchItem = menu.findItem(R.id.mainAction_search);
        searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mainAction_about) {
            DialogFragment dialogFragment = new AppCreditsDialog();
            dialogFragment.show(getSupportFragmentManager(),"dialog");
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.refreshBookmarks();
        if (searchView != null)
            adapter.getFilter().filter(searchView.getQuery().toString());
    }
}
