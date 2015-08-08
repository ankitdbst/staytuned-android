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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class FiltersDialogFragment extends DialogFragment {

    /* The activity that creates an instance of this dialog fragment must
         * implement this interface in order to receive event callbacks.
         * Each method passes the DialogFragment in case the host needs to query it. */
    public interface FiltersDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    FiltersDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (FiltersDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement FiltersDialogListener");
        }
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<Integer> mSelectedItems = new ArrayList<>();  // Where we track the selected items
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        Bundle args = getArguments();
        String category = args.getString(NavigationDrawerFragment.ITEM_CATEGORY);
        String language = args.getString(NavigationDrawerFragment.ITEM_LANGUAGE);

        String prefKey = getActivity().getPackageName() + "." + category + "_" + language;

        final SharedPreferences channelListPref = getActivity().getSharedPreferences(prefKey, 0);
        final SharedPreferences.Editor editor = channelListPref.edit();

        final List<String> channelList = new ArrayList<>();
        Map<String, ?> channelListMap = channelListPref.getAll();
        for (Map.Entry<String, ?> entry : channelListMap.entrySet()) {
            channelList.add(entry.getKey());
        }

        int idx = 0;
        final boolean[] channelsEnabled = new boolean[channelList.size()];
        for (Map.Entry<String, ?> entry : channelListMap.entrySet()) {
            if ((Boolean)entry.getValue()) {
                mSelectedItems.add(idx);
                channelsEnabled[idx] = true;
            }
            idx++;
        }

        final CharSequence[] channels = channelList.toArray(new CharSequence[channelList.size()]);

        // Set the dialog title
        builder.setTitle(R.string.channel_filter)
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setMultiChoiceItems(channels, channelsEnabled,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    mSelectedItems.add(which);
                                } else if (mSelectedItems.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    mSelectedItems.remove(Integer.valueOf(which));
                                }
                            }
                        })
                        // Set the action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the mSelectedItems results somewhere
                        // or return them to the component that opened the dialog
                        for (CharSequence channel : channels) {
                            editor.putBoolean((String)channel, false);
                        }

                        for (Integer idx : mSelectedItems) {
                            editor.putBoolean((String)channels[idx], true);
                        }

                        editor.apply();
                        mListener.onDialogPositiveClick(FiltersDialogFragment.this);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //
                    }
                });

        return builder.create();
    }
}
