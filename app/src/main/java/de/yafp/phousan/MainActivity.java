package de.yafp.phousan;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

/**
 * Main class which is used to realize the app itself with its user-interface.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "phousan";
    private static final String PREFS_NAME = "phousan_settings";
    //private static Context context;

    private FirebaseAnalytics mFirebaseAnalytics;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, 0);

        Log.d(TAG, "onCreate - UI Process launched");

        // customize action bar
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.app_icon);
        getSupportActionBar().setTitle(" " + getResources().getString(R.string.app_name)); // Icon + Space + String
        getSupportActionBar().setSubtitle(" " + getResources().getString(R.string.app_name_long));

        // get android_id
        @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d(TAG, "Android ID: " + androidId);
        logFireBaseEvent("p_id_"+androidId);

        // init the ui
        initApp();

        // Check if service is running, otherwise start it
        checkService();

        // Trigger backup
        requestBackup();

        // log to Firebase
        logFireBaseEvent("p___appStarted");
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "F: onCreateOptionsMenu");

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // log to Firebase
        logFireBaseEvent("p___onCreateOptionsMenu");

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "F: onOptionsItemSelected");

        // log to Firebase
        logFireBaseEvent("p___onOptionsItemSelected");

        switch (item.getItemId()) {

            // Menu: About
            case R.id.about:
                try {
                    openAbout();
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                break;

            // Menu: Issues
            case R.id.issues:
                // log to Firebase
                logFireBaseEvent("p___issues");

                Uri uri_issues = Uri.parse("https://github.com/yafp/phousan/issues"); // missing 'http://' will cause crashed
                Intent intent = new Intent(Intent.ACTION_VIEW, uri_issues);
                startActivity(intent);
                return true;

            // Menu: GooglePlay
            case R.id.googleplay:
                // log to Firebase
                logFireBaseEvent("p___googlePlay");

                Uri uri_googlePlay = Uri.parse("https://play.google.com/store/apps/details?id=de.yafp.phousan"); // missing 'http://' will cause crashed
                Intent intent3 = new Intent(Intent.ACTION_VIEW, uri_googlePlay);
                startActivity(intent3);
                return true;

            // Menu: show history
            case R.id.history_show:
                showUsageDataPerDay();
                return true;

            // Menu: export history
            case R.id.history_export:
                exportUsageData();
                return true;

            // Menu: delete usage data
            case R.id.usage_data_delete:
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                deleteAllSharedPreferences();
                                break;

                            default:
                        }
                    }
                };

                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle(getResources().getString(R.string.really_delete_all_prefs_dialog_title));
                builder.setMessage(getResources().getString(R.string.really_delete_all_prefs_dialog_text));
                builder.setPositiveButton(getResources().getString(R.string.really_delete_all_prefs_dialog_yes), dialogClickListener);
                builder.setNegativeButton(getResources().getString(R.string.really_delete_all_prefs_dialog__no), dialogClickListener);
                builder.setIcon(R.mipmap.app_icon);
                builder.show();
                return true;

            // Menu: Recommend
            case R.id.recommend:
                recommendApp();
                return true;

            // Menu: service start
            case R.id.service_start:
                startService(new Intent(this, BackgroundService.class));
                Log.d(TAG, "onClick - User wants to start the service");

                // log to Firebase
                logFireBaseEvent("p___serviceStart");

                return true;

            // Menu: service stop
            case R.id.service_stop:
                stopService(new Intent(this, BackgroundService.class));
                Log.d(TAG, "onClick - User wants to stop the service");

                // log to Firebase
                logFireBaseEvent("p___serviceStop");
                return true;

            default:
                break;
        }
        return true;
    }


    /**
     * generates a firebase log event from a given input string
     *
     * @param message the message text to be logged
     */
    private void logFireBaseEvent(String message) {
        Log.d(TAG, "F: logFireBaseEvent");

        Bundle params = new Bundle();
        params.putString(message, "1");
        mFirebaseAnalytics.logEvent(message, params);
    }


    /**
     *
     */
    private void requestBackup() {
        Log.d(TAG, "F: requestBackup");
        BackupManager bm = new BackupManager(this);
        bm.dataChanged();

        // log to Firebase
        logFireBaseEvent("p___requestBackup");
    }


    /**
     * init the application
     */
    private void initApp(){
        Log.d(TAG, "F: initApp");

        // add background to some textviews
        //
        int colorWhite = ContextCompat.getColor(MainActivity.this, R.color.colorWhite);
        int colorGray = ContextCompat.getColor(MainActivity.this, R.color.colorGray);

        // today
        TextView ui_text_today;
        ui_text_today = findViewById(R.id.ui_currentUsageCountText);
        ui_text_today.setTextColor(colorWhite);
        ui_text_today.setBackgroundResource(R.drawable.tags_rounded_corners);

        GradientDrawable drawable_today = (GradientDrawable) ui_text_today.getBackground();
        drawable_today.setCornerRadius(8);
        drawable_today.setColor(colorGray);

        // yesterday/previous
        TextView ui_textYesterday;
        ui_textYesterday = findViewById(R.id.ui_yesterdaysUsageCountText);
        ui_textYesterday.setTextColor(colorWhite);
        ui_textYesterday.setBackgroundResource(R.drawable.tags_rounded_corners);

        GradientDrawable drawable_yesterday = (GradientDrawable) ui_textYesterday.getBackground();
        drawable_yesterday.setCornerRadius(8);
        drawable_yesterday.setColor(colorGray);

        // overall
        TextView ui_textOverall;
        ui_textOverall = findViewById(R.id.ui_overallUsageCountText);
        ui_textOverall.setTextColor(colorWhite);
        ui_textOverall.setBackgroundResource(R.drawable.tags_rounded_corners);

        GradientDrawable drawable_overall = (GradientDrawable) ui_textOverall.getBackground();
        drawable_overall.setCornerRadius(8);
        drawable_overall.setColor(colorGray);

        // log to Firebase
        logFireBaseEvent("p___initApp");
    }



    /**
     * User opens app about dialog
     */
    private void openAbout() throws PackageManager.NameNotFoundException {
        Log.d(TAG, "F: openAbout");

        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

        // Get package informations
        PackageManager manager = this.getPackageManager();
        PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);

        // create dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.app_icon);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        builder.setMessage("\nPackage:\t\t" + info.packageName + "\nVersion:\t\t\t" + info.versionName + "\nBuild:\t\t\t\t" + info.versionCode);
        builder.create();
        builder.show();

        // log to Firebase
        logFireBaseEvent("p___openAbout");
    }


    /**
     * User wants to export usage data
     * Generates usage data text which gets forwarded to an app of choice
     */
    private void exportUsageData() {
        Log.d(TAG, "F: exportUsageData");

        StringBuilder outputUsageHistory;
        outputUsageHistory = new StringBuilder();
        ArrayList<String> al= new ArrayList<>(); // stores the relevant prefs for later sorting

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        int record_amount; // counter

        record_amount = 0;

        // fetch all usage-data preference strings
        for (String key : settings.getAll().keySet()) {
            if(key.startsWith("20")) { // collect only usage-history data
                Log.i(String.format("Shared Preference : %s - %s", PREFS_NAME, key), settings.getString(key, "error!"));

                // add to array
                al.add(key+";"+settings.getString(key, "error!")+";\n");

                record_amount = record_amount +1; // counter
            }
            else {
                Log.d(TAG, "Ignoring key, as it is not part of the daily usage history");
            }
        }

        // calc average
        if(record_amount == 0)
        {
            outputUsageHistory.append(getResources().getString(R.string.show_usage_history_per_day_empty_history));
        }

        // sort & reverse array-data
        Collections.sort(al); // sort
        Collections.reverse(al); //

        // build output string
        for (int i = 0; i < al.size(); i++) {
            outputUsageHistory.append(al.get(i));
        }

        // forward information to external app
        //
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

        // Add data to the intent, the receiving app will decide what to do with it.
        share.putExtra(Intent.EXTRA_SUBJECT, R.string.usage_data_export_title);
        share.putExtra(Intent.EXTRA_TEXT, outputUsageHistory.toString());

        startActivity(Intent.createChooser(share, getResources().getString(R.string.usage_data_export_title)));

        // log to Firebase
        logFireBaseEvent("p___exportUsageData");
    }



    /**
     * Delete all Shared Preferences and related values
     * displays toast for user
     */
    private void deleteAllSharedPreferences(){
        Log.d(TAG, "F: deleteAllSharedPreferences");

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.apply();

        // show toast after deletion
        displayToastMessage(getResources().getString(R.string.really_delete_all_prefs_confirm));

        // show notification
        displayNotification(getResources().getString(R.string.really_delete_all_prefs_dialog_title), getResources().getString(R.string.really_delete_all_prefs_confirm));

        resetUI();

        // log to Firebase
        logFireBaseEvent("p___deleteAllSharedPreferences");
    }


    /**
     * User wants to recommend tihs app to others
     * Generates a recommend text with a link to the play store which gets forwarded to an app of choice
     */
    private void recommendApp() {
        Log.d(TAG, "F: recommendApp");

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

        // Add data to the intent, the receiving app will decide what to do with it.
        share.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.recommend_text));
        share.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=de.yafp.phousan");

        startActivity(Intent.createChooser(share, "Share link"));

        // log to Firebase
        logFireBaseEvent("p___recommendApp");
    }



    /**
     * Displays a given string as a toast message to the user
     *
     * @param message the message to be displayed
     */
    private void displayToastMessage(String message) {
        Log.v(TAG, "F: displayToastMessage");

        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);       // for center vertical
        toast.show();

        // log to Firebase
        logFireBaseEvent("p___displayToastMessage");
    }



    /**
     * display a notification
     *
     * @param title the title of the notification
     * @param message the message text
     */
    private void displayNotification(String title, String message) {
        Log.d(TAG, "F: displayNotification");

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "M_CH_ID");

        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.notification_icon)
                //.setTicker("")
                //.setPriority(Notification.PRIORITY_MAX) // this is deprecated in API 26 but you can still use for below 26. check below update for 26 API
                .setContentTitle(title)
                .setContentText(message);

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.notify(1, notificationBuilder.build());

        // log to Firebase
        logFireBaseEvent("p___displayNotification");
    }


    /**
     * resets the UI after having executed delete all Usage Data
     */
    private void resetUI(){
        Log.d(TAG, "F: resetUI");


        // reset current count
        TextView cur;
        cur = findViewById(R.id.ui_currentUsageCount);
        cur.setText(R.string.zero);

        // reset previous/yesterdays count
        TextView previous;
        previous = findViewById(R.id.ui_yesterdaysUsageCount);
        previous.setText(R.string.zero);

        // update min value in ui to default text
        TextView min;
        min = findViewById(R.id.ui_overall_lowscore_count);
        min.setText(R.string.zero);

        // update min value date
        TextView min_date;
        min_date = findViewById(R.id.ui_overall_lowscore_date);
        min_date.setText(R.string.empty);

        // update average value in ui to default text
        TextView avg;
        avg = findViewById(R.id.ui_overall_average_count);
        avg.setText(R.string.zero);

        // update max value in ui to default text
        TextView max;
        max = findViewById(R.id.ui_overall_highscore_count);
        max.setText(R.string.zero);

        // update min value date
        TextView max_date;
        max_date = findViewById(R.id.ui_overall_highscore_date);
        max_date.setText(R.string.empty);

        // log to Firebase
        logFireBaseEvent("p___resetUI");
    }



    /**
     * checks if the service is running, otherwise restarts it
     * triggers UI update
     */
    private void checkService(){
        Log.d(TAG, "F: checkService");

        // Check if service is running, otherwise start it
        if(BackgroundService.isRegistered){
            // service is running
            Log.d(TAG, "Service is running");
        }
        else {
            // service is not yet running
            Log.d(TAG, "Service is NOT running");

            // Start the service
            startService(new Intent(this, BackgroundService.class));
        }

        updateUI();

        // log to Firebase
        logFireBaseEvent("p___checkService");
    }




    /**
     * prevent closing the app on back button press
     */
    @Override
    public void onBackPressed() {
        Log.d(TAG, "F: onBackPressed");
        moveTaskToBack(true);

        // log to Firebase
        logFireBaseEvent("p___onBackPressed");
    }



    /**
     * on click handling
     */
    @Override
    public void onClick(View view) {
        Log.d(TAG, "F: onClick");
        /*
        if (view == buttonStart) {
            //starting service
            startService(new Intent(this, BackgroundService.class));
            Log.d(TAG, "onClick - User wants to start the service");

        } else if (view == buttonStop) {
            //stopping service
            stopService(new Intent(this, BackgroundService.class));
            Log.d(TAG, "onClick - User wants to stop the service");
        }
        */

        // log to Firebase
        logFireBaseEvent("p___onClick");
    }


    /**
     * on resume of the activity
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "F: onResume");

        checkService();

        // log to Firebase
        logFireBaseEvent("p___onResume");
    }



    /**
     * on pause of the activity
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "F: onPause");

        checkService();

        // log to Firebase
        logFireBaseEvent("p___onPause");
    }



    /**
     * Opens a popup dialog which shows all use per day data
     * data is fetched via SharedPreferences
     */
    private void showUsageDataPerDay() {
        Log.d(TAG, "F: showUsageDataPerDay");

        StringBuilder outputUsageHistory;
        outputUsageHistory = new StringBuilder();
        ArrayList<String> al= new ArrayList<>(); // stores the relevant prefs for later sorting

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // print all settings
        for (String key : settings.getAll().keySet()) {
            if(key.startsWith("20")) { // collect only usage-history data
                Log.i(String.format("Shared Preference : %s - %s", PREFS_NAME, key), settings.getString(key, "error!"));

                // add to array
                al.add(key+":\t\t"+settings.getString(key, "error!")+"\n");
            }
            else {
                Log.d(TAG, "Ignoring key, as it is not part of the daily usage history");
            }
        }

        // sort & reverse array-data
        Collections.sort(al); // sort
        Collections.reverse(al); //

        // build output string
        for (int i = 0; i < al.size(); i++) {
            outputUsageHistory.append(al.get(i));
        }

        // show as dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title_show_usage_history_per_day);
        builder.setMessage(outputUsageHistory.toString());
        builder.setIcon(R.drawable.app_icon);
        AlertDialog alert = builder.create();
        alert.show();

        // log to Firebase
        logFireBaseEvent("p___showUsageDataPerDay");
    }



    /**
     * fetches all usage data per day,
     * calculates the average value
     * and returns that value as a string
     *
     * @return the average usage count per day (overall)
     */
    private String getUsageDataAverage(){
        Log.d(TAG, "F: getUsageDataAverage");

        String averageUsageDataPerDay;

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        int record_amount; // counter
        int record_sum; // usage sum
        int record_avg; // calculated usage average

        record_amount = 0;
        record_sum = 0;

        // print all settings
        for (String key : settings.getAll().keySet()) {
            if(key.startsWith("20")) { // collect only usage-history data
                Log.i(String.format("Shared Preference : %s - %s", PREFS_NAME, key), settings.getString(key, "error!"));

                record_amount = record_amount +1; // counter
                record_sum = record_sum + Integer.parseInt(settings.getString(key, "error!"));
            }
            else {
                Log.d(TAG, "Ignoring key, as it is not part of the daily usage history");
            }
        }

        // calc average
        if(record_amount == 0) {
            //averageUsageDataPerDay = getResources().getString(R.string.overall_n_a);
            averageUsageDataPerDay = "0";
        }
        else {
            record_avg = record_sum / record_amount;
            averageUsageDataPerDay = Integer.toString(record_avg);
            Log.d(TAG, "Usage History Average: "+Integer.toString(record_avg));
        }

        // log to Firebase
        logFireBaseEvent("p___getUsageDataAverage");

        return averageUsageDataPerDay;
    }



    /**
     * Update the User Interface (UI) with all SharedPreference values
     */
    private void updateUI(){
        Log.d(TAG, "F: updateUI");

        // -----------------------------------------------------------------------------------------
        // get current count
        // -----------------------------------------------------------------------------------------
        String currentUsageCount;
        currentUsageCount = readPreferences("usage_today_count");

        // update UI
        TextView ui_currentUsageCount;
        ui_currentUsageCount = findViewById(R.id.ui_currentUsageCount);
        ui_currentUsageCount.setText(currentUsageCount);


        // -----------------------------------------------------------------------------------------
        // get yesterdays count
        // -----------------------------------------------------------------------------------------
        String yesterdaysUsageCount;
        yesterdaysUsageCount = readPreferences("usage_yesterday_count");

        // update UI
        TextView ui_yesterdaysUsageCount;
        ui_yesterdaysUsageCount = findViewById(R.id.ui_yesterdaysUsageCount);
        ui_yesterdaysUsageCount.setText(yesterdaysUsageCount);


        // -----------------------------------------------------------------------------------------
        // get lowscore value
        // -----------------------------------------------------------------------------------------
        String usageOverallMinCount;
        usageOverallMinCount = readPreferences("usage_overall_min_count");

        // update UI
        TextView ui_overall_lowscore_count;
        ui_overall_lowscore_count = findViewById(R.id.ui_overall_lowscore_count);
        ui_overall_lowscore_count.setText(usageOverallMinCount);


        // -----------------------------------------------------------------------------------------
        // get lowscore date
        // -----------------------------------------------------------------------------------------
        String usageOverallMinDate;
        usageOverallMinDate = readPreferences("usage_overall_min_date");

        // update UI
        TextView ui_overall_lowscore_date;
        ui_overall_lowscore_date = findViewById(R.id.ui_overall_lowscore_date);
        ui_overall_lowscore_date.setText(usageOverallMinDate);


        // -----------------------------------------------------------------------------------------
        // get highscore count
        // -----------------------------------------------------------------------------------------
        String usageOverallMaxCount;
        usageOverallMaxCount = readPreferences("usage_overall_max_count");

        // update UI
        TextView ui_overall_highscore_count;
        ui_overall_highscore_count = findViewById(R.id.ui_overall_highscore_count);
        ui_overall_highscore_count.setText(usageOverallMaxCount);


        // -----------------------------------------------------------------------------------------
        // get highscore date
        // -----------------------------------------------------------------------------------------
        String usageOverallMaxDate;
        usageOverallMaxDate = readPreferences("usage_overall_max_date");

        // update UI
        TextView ui_overall_highscore_date;
        ui_overall_highscore_date = findViewById(R.id.ui_overall_highscore_date);
        ui_overall_highscore_date.setText(usageOverallMaxDate);


        // -----------------------------------------------------------------------------------------
        // Average Usage per day
        // -----------------------------------------------------------------------------------------
        String averageUsageDataPerDay;
        averageUsageDataPerDay = getUsageDataAverage();

        // update ui
        TextView ui_overall_average_count;
        ui_overall_average_count = findViewById(R.id.ui_overall_average_count);
        ui_overall_average_count.setText(averageUsageDataPerDay);

        // log to Firebase
        logFireBaseEvent("p___updateUI");
    }


    /**
     * Read single setting from SharedPreferences
     *
     * @param key name of the shared preference
     * @return contains the related value as string
     */
    private String readPreferences(String key){
        Log.d(TAG, "F: readPreferences");

        String value = prefs.getString(key, "0");
        Log.d(TAG, "Key: "+key+", value is: "+value);

        // log to Firebase
        logFireBaseEvent("p___readPreferences");

        return value;
    }
}
