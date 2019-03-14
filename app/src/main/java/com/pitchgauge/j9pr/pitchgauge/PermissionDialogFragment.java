package com.pitchgauge.j9pr.pitchgauge;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;



public class PermissionDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private int msgId = R.string.rationale_message_storage;


    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        Bundle args = getArguments();
        if (args != null) {
            msgId = args.getInt("MSG");
        }

        return new AlertDialog.Builder(getContext()).setTitle(R.string.rationale_title)
                .setMessage(msgId)
                .setPositiveButton(R.string.rationale_request, this)
                .setNegativeButton(R.string.rationale_cancel, null).create();
    }

    @Override
    public void onClick(final DialogInterface dialogInterface, final int i) {
        final PermissionDialogListener parent = (PermissionDialogListener) getParentFragment();
        if(parent == null) {
            if (getActivity() instanceof DeviceListActivity) {
                ((DeviceListActivity) getActivity()).onRequestPermission();
            } else if (getActivity() instanceof DeviceListActivity) {
                ((DeviceListActivity) getActivity()).onRequestPermission();
            }
        } else {
            parent.onRequestPermission();
        }
    }

    public interface PermissionDialogListener {
        void onRequestPermission();
    }
}

