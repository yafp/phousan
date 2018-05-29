package de.yafp.phousan;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;


/**
 * Extends Service: an application component representing either an application's desire to perform
 * a longer-running operation while not interacting with the user or to supply functionality
 * for other applications to use.
 *
 * here:
 * start and stop of the service and receiver
 */
public class BackgroundService extends Service{

    private static final String TAG = "phousan";
    static boolean isRegistered = false;
    private final Receiver Receiver = new Receiver();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);

        // register the receiver
        this.registerReceiver(Receiver, filter);
        isRegistered = true;

        // Display toast
        Toast.makeText(this, "Phousan service started.", Toast.LENGTH_LONG).show();
        Log.d(TAG, "F: onStartCommand");

        //we have some options for service
        //start sticky means service will be explicity started and stopped
        return START_STICKY;
    }



    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "F: onDestroy");

        if (isRegistered) {
            Log.d(TAG, "F: is registered, now lets stop it");

            // unregisterreceiver
            unregisterReceiver(Receiver);

            isRegistered = false;

            Toast.makeText(this, "Phousan service stopped", Toast.LENGTH_LONG).show();
        }
    }
}
