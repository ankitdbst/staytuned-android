package com.android.app.tvbuff;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class SettingsDialogFragment extends DialogFragment {

    public static final Integer[] duration = {5, 15, 60, 180};
    public static final String SETTINGS_PREF_FILE = "settings_pref";
    public static final String REMINDER_INTERVAL = "reminder_interval";

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SharedPreferences settingsPref = getActivity()
                .getSharedPreferences(SETTINGS_PREF_FILE, 0);
        final SharedPreferences.Editor editor = settingsPref.edit();

        final Integer d = (int) settingsPref.getLong(REMINDER_INTERVAL, 15*60*1000)/(1000*60);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.reminder_filter)
            .setSingleChoiceItems(R.array.duration_array, Arrays.asList(duration).indexOf(d),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        editor.putLong(REMINDER_INTERVAL, duration[which] * 60 * 1000);
                    }
                })
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    editor.apply();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    editor.putLong(REMINDER_INTERVAL, d * 60 * 1000);
                    editor.apply();
                }
            });
        return builder.create();
    }
}
