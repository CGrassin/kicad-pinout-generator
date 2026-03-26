package fr.charleslabs.tinwhistletabs.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import fr.charleslabs.tinwhistletabs.R;
import fr.charleslabs.tinwhistletabs.TabActivity;
import fr.charleslabs.tinwhistletabs.music.MusicSettings;

public class TempoDialog extends DialogFragment {
    /**
     * Callback interface.
     */
    public interface TempoChangeCallback{
        void tempoChangeCallback(int newTempo, boolean isDelayApplied);
    }

    // States
    private final int initialTempo;
    private EditText tempoText;
    private final boolean initialCBState;
    private CheckBox checkbox;
    private final TempoChangeCallback caller;

    public TempoDialog(int initialTempo, boolean initialCBState, TempoChangeCallback caller) {
        super();
        this.initialTempo = initialTempo;
        this.initialCBState = initialCBState;
        this.caller = caller;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        final Dialog dialog =  new AlertDialog.Builder(requireActivity())
                .setTitle(getText(R.string.tempoDialog_title))
                //.setIcon(R.drawable.ic_timer_black)
                .setMessage(getResources().getString(R.string.tempoDialog_message,
                        MusicSettings.MIN_TEMPO, MusicSettings.MAX_TEMPO))
                .setView(R.layout.dialog_tempo_layout)
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
                        try{
                            // Get and cast
                            int newTempo = Integer.parseInt(tempoText.getText().toString());
                            // Bounds
                            if(newTempo > MusicSettings.MAX_TEMPO){
                                newTempo = MusicSettings.MAX_TEMPO;
                                Toast.makeText(getContext(),
                                        getResources().getString(R.string.tempoDialog_error_max,
                                                MusicSettings.MAX_TEMPO),Toast.LENGTH_LONG).show();
                            }
                            else if(newTempo < MusicSettings.MIN_TEMPO) {
                                newTempo = MusicSettings.MIN_TEMPO;
                                Toast.makeText(getContext(),
                                        getResources().getString(R.string.tempoDialog_error_min,
                                                MusicSettings.MIN_TEMPO),Toast.LENGTH_LONG).show();
                            }

                            // Callback
                            caller.tempoChangeCallback(newTempo, checkbox.isChecked());
                        } catch (Exception e){
                            // Non-numeric?
                            Toast.makeText(getContext(),
                                    getResources().getString(R.string.tempoDialog_error,
                                            e.getMessage()),Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .create();
        try{
            dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }catch (Exception ignored){}
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        tempoText = getDialog().findViewById(R.id.tempoDialog_bpm);
        tempoText.setText(Integer.toString(initialTempo));
        tempoText.requestFocus(); // open kb

        checkbox = getDialog().findViewById(R.id.tempoDialog_checkbox);
        checkbox.setChecked(this.initialCBState);
        checkbox.setText(getResources().getString(R.string.tempoDialog_checkbox,
                TabActivity.START_DELAY_AMOUNT));
    }
}
