package com.pitchgauge.j9pr.pitchgauge;

import android.app.Dialog;
import android.content.Context;
import android.support.v7.app.AlertDialog;

public final class Utils {

    public static Dialog createSimpleOkErrorDialog(Context context, String title, String message) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(R.string.dialog_action_ok, null);
        return alertDialog.create();
    }


}
