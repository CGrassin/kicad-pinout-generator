package fr.charleslabs.tinwhistletabs.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fr.charleslabs.tinwhistletabs.R;
import fr.charleslabs.tinwhistletabs.music.MusicDB;
import fr.charleslabs.tinwhistletabs.music.MusicSheet;
import fr.charleslabs.tinwhistletabs.utils.Utils;

public class SheetsAdapter extends BaseAdapter implements Filterable {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SHEET  = 1;

    private final List<MusicSheet> sheets;         // full unfiltered list
    private List<Object> displayList;              // headers (String) + sheets (MusicSheet)
    private final Context context;
    private final View noResult;

    public SheetsAdapter(Context context, List<MusicSheet> sheets, View noResult) {
        this.sheets = sheets;
        this.context = context;
        this.noResult = noResult;
        this.displayList = buildDisplayList(sheets, /*searchActive=*/false);
    }

    // Call this from MainActivity when returning from TabActivity,
    // so the bookmark section refreshes.
    public void refreshBookmarks() {
        displayList = buildDisplayList(sheets, /*searchActive=*/false);
        notifyDataSetChanged();
    }

    // --- Adapter overrides ---

    @Override
    public int getViewTypeCount() { return 2; }

    @Override
    public int getItemViewType(int position) {
        return displayList.get(position) instanceof String ? TYPE_HEADER : TYPE_SHEET;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) == TYPE_SHEET;
    }

    @Override
    public int getCount() { return displayList.size(); }

    @Override
    public Object getItem(int position) { return displayList.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (getItemViewType(position) == TYPE_HEADER) {
            if (convertView == null)
                convertView = LayoutInflater.from(context)
                        .inflate(R.layout.list_item_header, parent, false);
            ((TextView) convertView.findViewById(R.id.header_text))
                    .setText((String) displayList.get(position));
            return convertView;
        }

        // --- TYPE_SHEET ---
        if (convertView == null)
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_layout, parent, false);

        ComponentViewHolder viewHolder = (ComponentViewHolder) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new ComponentViewHolder();
            viewHolder.sheetName    = convertView.findViewById(R.id.mainActivity_SheetName);
            viewHolder.sheetDetails = convertView.findViewById(R.id.mainActivity_SheetDetails);
            viewHolder.sheetImage   = convertView.findViewById(R.id.mainActivity_sheetPicture);
            convertView.setTag(viewHolder);
        }

        final MusicSheet sheet = (MusicSheet) displayList.get(position);
        viewHolder.sheetName.setText(sheet.getTitle());
        viewHolder.sheetDetails.setText(context.getResources().getString(
                R.string.mainActivity_sheetDetails_string, sheet.getType(), sheet.getWhistle()));
        switch (sheet.getType()) {
            case "Reel":      viewHolder.sheetImage.setImageResource(R.drawable.reel);     break;
            case "Jig":       viewHolder.sheetImage.setImageResource(R.drawable.jig);      break;
            case "Slip Jig":  viewHolder.sheetImage.setImageResource(R.drawable.slipjig);  break;
            case "Slide":     viewHolder.sheetImage.setImageResource(R.drawable.slide);    break;
            case "Polka":     viewHolder.sheetImage.setImageResource(R.drawable.polka);    break;
            case "March":     viewHolder.sheetImage.setImageResource(R.drawable.march);    break;
            case "Hornpipe":  viewHolder.sheetImage.setImageResource(R.drawable.hornpipe); break;
            case "Song":      viewHolder.sheetImage.setImageResource(R.drawable.song);     break;
            case "Waltz":     viewHolder.sheetImage.setImageResource(R.drawable.waltz);    break;
            default:          viewHolder.sheetImage.setImageResource(R.drawable.misc);     break;
        }
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint == null || constraint.length() == 0) {
                    filterResults.values = buildDisplayList(sheets, /*searchActive=*/false);
                } else {
                    List<Object> results = new ArrayList<>();
                    String searchStr = Utils.normalize(constraint.toString());
                    for (MusicSheet sheet : sheets)
                        if (sheet.filter(searchStr)) results.add(sheet);
                    filterResults.values = results;
                }
                filterResults.count = ((List<?>) filterResults.values).size();
                return filterResults;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                displayList = (List<Object>) results.values;
                notifyDataSetChanged();
                if (noResult != null) {
                    boolean empty = displayList.isEmpty();
                    noResult.setVisibility(empty ? View.VISIBLE : View.GONE);
                }
            }
        };
    }


    // --- Helpers ---

    private List<Object> buildDisplayList(List<MusicSheet> allSheets, boolean searchActive) {
        List<Object> list = new ArrayList<>();
        if (searchActive) {
            list.addAll(allSheets);
            return list;
        }

        Set<String> bookmarkedIds = MusicDB.getInstance(context).bookmarks.getAll();

        // Bookmarks section
        List<MusicSheet> bookmarked = new ArrayList<>();
        for (MusicSheet sheet : allSheets)
            if (bookmarkedIds.contains(sheet.getFile())) bookmarked.add(sheet);

        if (!bookmarked.isEmpty()) {
            list.add(context.getString(R.string.header_bookmarks, bookmarked.size()));
            list.addAll(bookmarked);
        }

        // All tunes section
        list.add(context.getString(R.string.header_all_tunes, allSheets.size()));
        list.addAll(allSheets);

        return list;
    }

    // View Holder
    private static class ComponentViewHolder {
        TextView sheetName;
        TextView sheetDetails;
        ImageView sheetImage;
    }
}