package fr.charleslabs.tinwhistletabs.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import fr.charleslabs.tinwhistletabs.R;
import fr.charleslabs.tinwhistletabs.music.MusicSheet;

public class SheetInfoDialog extends DialogFragment {
    private final String message;

    public SheetInfoDialog(final Context c, @NonNull MusicSheet sheet) {
        StringBuilder str = new StringBuilder();

        str.append(c.getResources().getString(R.string.sheetInfoDialog_sheet_title, sheet.getTitle()));
        str.append(c.getResources().getString(R.string.sheetInfoDialog_sheet_type, sheet.getType()));
        if (sheet.getAuthor() != null)
            str.append(c.getResources().getString(R.string.sheetInfoDialog_sheet_author, sheet.getAuthor()));
        if (sheet.getSheetAuthor() != null)
            str.append(c.getResources().getString(R.string.sheetInfoDialog_sheet_sheetAuthor, sheet.getSheetAuthor()));
        if (sheet.getLicense() != null)
            str.append(c.getResources().getString(R.string.sheetInfoDialog_sheet_license, sheet.getLicense()));

        this.message = str.toString();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        return new AlertDialog.Builder(requireActivity())
                .setTitle(getText(R.string.sheetInfoDialog_title))
                //.setIcon(R.drawable.ic_note)
                .setMessage(Html.fromHtml(message))
                .setNeutralButton(getText(R.string.sheetInfoDialog_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dismiss();
                            }
                        })
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        TextView messageTv = getDialog().findViewById(android.R.id.message);
        if(messageTv != null)
            messageTv.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
