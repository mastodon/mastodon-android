package org.joinmastodon.android.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;

import org.joinmastodon.android.R;

public class ClearRecentSearchDialogFragment extends DialogFragment {
    public interface DialogListener {
        public void onDialogClearClicked(String id);
    }

    DialogListener listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (DialogListener) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement DialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        Bundle b = getArguments();
        String itemName = b.getString("itemName");
        String id = b.getString("itemId");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.clear_recent_search, itemName))
            .setPositiveButton(R.string.clear, (dialog, which) -> {
                listener.onDialogClearClicked(id);
            })
            .setNegativeButton(R.string.cancel, (dialog, which) -> {
                // Do nothing
            });
        return builder.create();
    }
}
