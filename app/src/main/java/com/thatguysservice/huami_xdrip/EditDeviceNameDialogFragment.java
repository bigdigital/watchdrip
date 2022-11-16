package com.thatguysservice.huami_xdrip;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class EditDeviceNameDialogFragment extends DialogFragment {

    private OnDialogResultListener listener;
    private EditText mEditText;

    public static EditDeviceNameDialogFragment newInstance(String name) {
        EditDeviceNameDialogFragment frag = new EditDeviceNameDialogFragment();
        Bundle args = new Bundle();
        args.putString("name", name);
        frag.setArguments(args);
        return frag;
    }

    // Assign the listener implementing events interface that will receive the events
    public void setDialogResultListener(OnDialogResultListener listener) {
        this.listener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_edit_device_name_fragment, null);

        mEditText = view.findViewById(R.id.text_device_name);
        // Fetch arguments from bundle and set title
        String deviceName = getArguments().getString("name", "");
        mEditText.setText(deviceName);
        mEditText.setSingleLine(true);
        // Show soft keyboard automatically and request focus to field
        mEditText.requestFocus();

        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    callDialogResult();
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        alertDialogBuilder.setTitle(getString(R.string.enter_device_name));
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callDialogResult();
            }
        });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }

        });
        AlertDialog dialog = alertDialogBuilder.create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    private void callDialogResult() {
        if (listener != null) {
            listener.onDialogResult(mEditText.getText().toString());
        }
    }

    public interface OnDialogResultListener {
        public void onDialogResult(String result);
    }

}
