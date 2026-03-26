package fr.charleslabs.tinwhistletabs.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

import fr.charleslabs.tinwhistletabs.R;
import fr.charleslabs.tinwhistletabs.music.MusicSettings;

public class KeyDialog extends DialogFragment {
    /**
     * Callback interface.
     */
    public interface KeyChangeCallback{
        void keyChangeCallback(String newKey);
    }

    // States
    private final String initialKey;
    private final KeyChangeCallback caller;
    private int selectedItem;

    public KeyDialog(String initialKey, KeyChangeCallback caller) {
        this.initialKey = initialKey;
        this.caller = caller;
        selectedItem = java.util.Arrays.asList(MusicSettings.WHISTLE_KEYS).indexOf(initialKey);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        return new AlertDialog.Builder(requireActivity())
                .setTitle(getText(R.string.keyDialog_title))
                //.setIcon(R.drawable.ic_note)
                //.setMessage(getResources().getString(R.string.tempoDialog_message, MusicSettings.MIN_TEMPO, MusicSettings.MAX_TEMPO))
                .setSingleChoiceItems(MusicSettings.WHISTLE_KEYS,
                        java.util.Arrays.asList(MusicSettings.WHISTLE_KEYS).indexOf(initialKey),
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        selectedItem = item;
                    }
                })
                .setCancelable(true)
                .setNegativeButton(getText(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }
                })
                .setPositiveButton(getText(R.string.dialog_set), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        caller.keyChangeCallback(MusicSettings.WHISTLE_KEYS[selectedItem]);
                    }
                })
                .create();
    }
}
