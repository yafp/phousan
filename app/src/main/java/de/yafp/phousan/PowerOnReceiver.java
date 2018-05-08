package de.yafp.phousan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PowerOnReceiver extends BroadcastReceiver {

    private static final String TAG = "phousan";

    @Override
    public void onReceive(Context context, Intent intent) {

        // SCREEN OFF
        //
        if (Intent.ACTION_SCREEN_OFF.equalsIgnoreCase(intent.getAction())) {
            Log.d(TAG, "ACTION_SCREEN_OFF");
        }


        // SCREEN ON
        //
        if (Intent.ACTION_SCREEN_ON.equalsIgnoreCase(intent.getAction())) {
            Log.d(TAG, "ACTION_SCREEN_ON");

            // do magic
            ((MainActivity) context).updateData();
        }
    }
}