package com.android.app.tvbuff;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class FiltersDialogFragment extends DialogFragment {

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<Integer> mSelectedItems = new ArrayList<>();  // Where we track the selected items
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        Bundle args = getArguments();
        String category = args.getString(NavigationDrawerFragment.ITEM_CATEGORY);
        String language = args.getString(NavigationDrawerFragment.ITEM_LANGUAGE);

        String prefKey = getActivity().getPackageName() + "." + category + "_" + language;

        // Avoid network call, if we have the channel list already fetched from the previous time.
        final SharedPreferences channelListPref = getActivity().getSharedPreferences(prefKey, 0);
        SharedPreferences.Editor editor = channelListPref.edit();

        List<String> channelList = new ArrayList<>();
        Map<String, ?> channelListMap = channelListPref.getAll();
        for (Map.Entry<String, ?> entry : channelListMap.entrySet()) {
            channelList.add(entry.getKey());
        }

        int idx = 0;
        boolean[] channelsEnabled = new boolean[channelList.size()];
        for (Map.Entry<String, ?> entry : channelListMap.entrySet()) {
            if (entry.getValue().toString().equals("true")) {
                channelsEnabled[idx++] = true;
            }
        }

        CharSequence[] channels = channelList.toArray(new CharSequence[channelList.size()]);

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
                        //
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
