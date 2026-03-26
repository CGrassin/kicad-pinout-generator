package fr.charleslabs.tinwhistletabs.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import fr.charleslabs.tinwhistletabs.R;

public class AppCreditsDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        return new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.aboutDialog_title)
                .setIcon(R.drawable.logo)
                .setMessage(R.string.aboutDialog_message)
                .setCancelable(true)
                .setPositiveButton(getText(R.string.aboutDialog_okBtn), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }
                })
                .setNegativeButton(getText(R.string.aboutDialog_websiteBtn), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent= new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.charleslabs.fr"));
                        startActivity(intent);
                    }
                })
                .create();
    }
}