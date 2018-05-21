package de.yafp.phousan;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
//import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
//import android.os.Build;
import android.os.Bundle;
//import android.provider.Settings.Secure;
//import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
//import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.provider.Settings;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;


// Preferences
//
// To store the current day values
// - usage_current_count
// - usage_current_date
//
// To store the min highscore
// - usage_min_count
// - usage_min_date
//
// To store the may highscore
// - usage_max_count
// - usage_max_date
//
// To store count of previous day
// - usage_yesterday_count
// - usage_yesterday_date
//
// To store all day history
// - date (example: 20180501)


public class MainActivity extends Activity {

    private static final String PREFS_NAME = "phousan_settings";
    private static final String TAG = "phousan";

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        setContentView(R.layout.activity_main);
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON); // main action
        filter.addAction(Intent.ACTION_SCREEN_OFF); // Not yet in use
        //filter.addAction(Intent.ACTION_DATE_CHANGED);  // not yet in use -> could be useful for day changeing routine
        PowerOnReceiver receiver = new PowerOnReceiver();
        registerReceiver(receiver, filter);

        // get android_id
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d(TAG, "Android ID: " + androidId);
        logFireBaseEvent("p_id_"+androidId);


        // -----------------------------------------------------------------------------------------
        // Developing - Testdata - START
        // -----------------------------------------------------------------------------------------
        // generate some
        devWritePseudoSharedPrefData();

        // read all SharedPref values
        devReadAllPrefValues();
        // -----------------------------------------------------------------------------------------
        // Developing - Testdata - STOP
        // -----------------------------------------------------------------------------------------

        // init App & UI
        initApp();

        // #2 - backup prefs to cloud
        requestBackup();

        // log to Firebase
        logFireBaseEvent("p_appLaunch");
    }




    public void requestBackup() {
        Log.d(TAG, "F: requestBackup");
        BackupManager bm = new BackupManager(this);
        bm.dataChanged();

        // log to Firebase
        logFireBaseEvent("p_requestBackup");
    }


    // FIXME:
    // tests for #3
    // additional ideas: https://stackoverflow.com/questions/5312334/how-to-handle-back-button-in-activity
    @Override
    public void onBackPressed() {
        Log.d(TAG, "F: onBackPressed");
        moveTaskToBack(true);

        // log to Firebase
        logFireBaseEvent("p_onBackPressed");
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
     * method to read all Shared Preferences
     * USED FOR TESTING & DEBUGGING
     */
    private void devReadAllPrefValues(){
        Log.d(TAG, "F: tempReadAllPrefValues");

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        for (String key : settings.getAll().keySet()) {
            Log.i(String.format("Shared Preference : %s - %s", PREFS_NAME, key), settings.getString(key, "error!"));
        }
    }



    /**
     * method to write some pseudo usage data
     * USED FOR TESTING & DEBUGGING
     */
    private void devWritePseudoSharedPrefData(){
        Log.d(TAG, "F: tempWritePseudoSharedPrefData");
        /*
        writeSetting("usage_min_count", "86");
        writeSetting("usage_min_date", "20150501");

        writeSetting("usage_max_count", "12");
        writeSetting("usage_max_date", "20160504");

        writeSetting("usage_yesterday_count", "10");
        writeSetting("usage_yesterday_date", "20180503");

        // Usage history:
        //
        writeSetting("20180508", "96");
        writeSetting("20180507", "67");
        writeSetting("20180506", "103");
        writeSetting("20180505", "102");
        writeSetting("20180504", "100");
        writeSetting("20180503", "99");
        writeSetting("20180502", "80");
        writeSetting("20180501", "70");
        writeSetting("20180430", "53");
        writeSetting("20180429", "72");
        */
    }



    /**
     * Delete all Shared Preferences and related values
     * displays toast for user
     */
    private void deleteAllSharedPreferences(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        Editor editor = settings.edit();
        editor.clear();
        editor.apply();

        // show toast after deletion
        displayToastMessage(getResources().getString(R.string.really_delete_all_prefs_confirm));

        // show notification
        displayNotification(getResources().getString(R.string.really_delete_all_prefs_dialog_title), getResources().getString(R.string.really_delete_all_prefs_confirm));

        resetUI();

        // log to Firebase
        logFireBaseEvent("p_deleteAllSharedPreferences");
    }



    /**
     * fetches all values from Shared Preferences and extracts only the daily usage counts.
     * puts all relevant data into an array, sort it and outputs it as dialog
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
        logFireBaseEvent("p_showUsageDataPerDay");
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
        logFireBaseEvent("p_recommendApp");
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

        // print all settings
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
        logFireBaseEvent("p_exportUsageData");
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.app_icon);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        builder.setMessage("\nPackage:\t\t" + info.packageName + "\nVersion:\t\t\t" + info.versionName + "\nBuild:\t\t\t\t" + info.versionCode);
        builder.create();
        builder.show();

        // log to Firebase
        logFireBaseEvent("p_openAbout");
    }



    /**
     * show the settings popup menu
     *
     * @param v the view
     */
    public void showPopup(final View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_example, popup.getMenu());

        // ad listener
        popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {

                Log.d(TAG, item.getTitle().toString());
                Log.d(TAG, Integer.toString(item.getItemId()));

                switch (item.getItemId()) {
                    case R.id.about:
                        try {
                            openAbout();
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                        return true;

                    case R.id.issues:
                        Uri uri = Uri.parse("https://github.com/yafp/phousan/issues"); // missing 'http://' will cause crashed
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        return true;

                    case R.id.history_show:
                        showUsageDataPerDay();
                        return true;

                    case R.id.history_export:
                        exportUsageData();
                        return true;

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

                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                        builder.setTitle(getResources().getString(R.string.really_delete_all_prefs_dialog_title));
                        builder.setMessage(getResources().getString(R.string.really_delete_all_prefs_dialog_text));
                        builder.setPositiveButton(getResources().getString(R.string.really_delete_all_prefs_dialog_yes), dialogClickListener);
                        builder.setNegativeButton(getResources().getString(R.string.really_delete_all_prefs_dialog__no), dialogClickListener);
                        builder.setIcon(R.mipmap.app_icon);
                        builder.show();
                        return true;

                    case R.id.recommend:
                        recommendApp();
                        return true;

                    default:
                        return true;
                        //return super.onOptionsItemSelected(item);
                }
            }
        });

        // show menu popup
        popup.show();

        // log to Firebase
        logFireBaseEvent("p_showPopup");
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
        logFireBaseEvent("p_displayToastMessage");
    }



    /**
     * used on day change to write a usage per day history
     *
     * @param date the date as string
     * @param usageCount the actual usage count as string
     */
    private void writeUsagePerDayHistory(String usageCount, String date) {
        Log.v(TAG, "F: writeUsagePerDayHistory");

        writeSetting(date, usageCount);

        // log to Firebase
        logFireBaseEvent("p_writeUsagePerDayHistory");
    }



    /**
     * init the application
     */
    private void initApp(){
        Log.d(TAG, "F: initApp");

        // get current date
        String curDate = getCurrentDate();

        // get date of last sharedPref activity
        String lastDate = readSetting("usage_current_date");

        // Read ScreenOn
        String currentScreenOnCount;
        currentScreenOnCount = readSetting("usage_current_count");

        if(lastDate.equals(curDate)){
            updateUI(currentScreenOnCount);
        }
        else
        {
            updateUI("1");
        }

        // add background to textviews
        //
        // today
        TextView ui_text_today;
        ui_text_today = findViewById(R.id.ui_text_today);
        ui_text_today.setTextColor(getResources().getColor(R.color.colorWhite));
        ui_text_today.setBackgroundResource(R.drawable.tags_rounded_corners);

        GradientDrawable drawable_today = (GradientDrawable) ui_text_today.getBackground();
        drawable_today.setCornerRadius(8);
        drawable_today.setColor(getResources().getColor(R.color.colorGray));

        // yesterday
        TextView ui_textYesterday;
        ui_textYesterday = findViewById(R.id.ui_textYesterday);
        ui_textYesterday.setTextColor(getResources().getColor(R.color.colorWhite));
        ui_textYesterday.setBackgroundResource(R.drawable.tags_rounded_corners);

        GradientDrawable drawable_yesterday = (GradientDrawable) ui_textYesterday.getBackground();
        drawable_yesterday.setCornerRadius(8);
        drawable_yesterday.setColor(getResources().getColor(R.color.colorGray));

        // overall
        TextView ui_textOverall;
        ui_textOverall = findViewById(R.id.ui_textOverall);
        ui_textOverall.setTextColor(getResources().getColor(R.color.colorWhite));
        ui_textOverall.setBackgroundResource(R.drawable.tags_rounded_corners);

        GradientDrawable drawable_overall = (GradientDrawable) ui_textOverall.getBackground();
        drawable_overall.setCornerRadius(8);
        drawable_overall.setColor(getResources().getColor(R.color.colorGray));

        // log to Firebase
        logFireBaseEvent("p_initApp");
    }



    /**
     * reads the last data from SharedPrefs and updates it
     * (including if day switches)
     */
    public void updateData(){
        Log.d(TAG, "F: updateData");

        // get current date
        String curDate = getCurrentDate();

        // get date of last sharedPref activity
        String lastDate = readSetting("usage_current_date");

        // Read ScreenOn
        String currentScreenOnCount;
        currentScreenOnCount = readSetting("usage_current_count");

        int newScreenOnCount;
        if(lastDate.equals(curDate)){
            Log.d(TAG, "lastDate == curDate");

            // Calculate new value: +1
            newScreenOnCount = Integer.parseInt(currentScreenOnCount);
            newScreenOnCount = newScreenOnCount +1;

            // check highscore max
            checkForNewHighscore(Integer.toString(newScreenOnCount), lastDate);

            // check lowscore
            //checkForNewLowscore(Integer.toString(newScreenOnCount), lastDate);

            // Update todays history
            writeUsagePerDayHistory(Integer.toString(newScreenOnCount), lastDate);
        }
        else {
            Log.d(TAG, "lastDate != curDate");

            // do some routines on each new day
            onNewDay(currentScreenOnCount, lastDate);

            // Start new day with the new date
            writeSetting("usage_current_date", curDate);
            newScreenOnCount = 1;
        }

        // Write new count value
        writeSetting("usage_current_count", Integer.toString(newScreenOnCount));

        // update the user interface
        updateUI(Integer.toString(newScreenOnCount));

        // log to Firebase
        logFireBaseEvent("p_updateData");
    }


    /**
     * fetches all usage data per day, calculates the average value and returns that value as a string
     */
    private String getUsageDataAverage(){
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
            averageUsageDataPerDay = getResources().getString(R.string.overall_n_a);
        }
        else {
            record_avg = record_sum / record_amount;
            averageUsageDataPerDay = Integer.toString(record_avg);
            Log.d(TAG, "Usage History Average: "+Integer.toString(record_avg));
        }

        // log to Firebase
        logFireBaseEvent("p_getUsageDataAverage");

        return averageUsageDataPerDay;
    }


    /**
     * starts different methods needed on a day change
     */
    private void onNewDay(String currentScreenOnCount, String lastDate){

        // Show last days usage count as notification if it is > 0
        if(!currentScreenOnCount.equals("0")){
            String title = getResources().getString(R.string.last_days_usage_count_title);
            String text = getResources().getString(R.string.last_days_usage_count_text);
            displayNotification(title, text + currentScreenOnCount);
        }

        // Check if new highscore
        checkForNewHighscore(currentScreenOnCount, lastDate);

        // Check for new lowscore
        checkForNewLowscore(currentScreenOnCount, lastDate);

        // Remember todays usage count as yesterday
        rememberYesterdayUsageCount(currentScreenOnCount, lastDate);

        // write usage per day log entry
        writeUsagePerDayHistory(currentScreenOnCount, lastDate);

        // log to Firebase
        logFireBaseEvent("p_onNewDay");
    }



    /**
     * on new day: check if last days usage count is highscore
     *
     * @param usageCount the count as string
     * @param lastDate the date as string
     */
    private void checkForNewHighscore(String usageCount, String lastDate){
        Log.d(TAG, "F: checkForNewHighscore");

        // get current highscore
        String usage_max;
        usage_max = readSetting("usage_max_count");

        // If it's a new highscore - save it
        if(Integer.valueOf(usageCount) > Integer.valueOf(usage_max)){
            writeSetting("usage_max_count", usageCount);
            writeSetting("usage_max_date", lastDate);
        }

        // log to Firebase
        logFireBaseEvent("p_checkForNewHighscore");
    }



    /**
     * on new day: check if last days usage count is lowscore
     *
     * @param usageCount the count as string
     * @param lastDate the date as string
     */
    private void checkForNewLowscore(String usageCount, String lastDate){
        Log.d(TAG, "F: checkForNewLowscore");

        // get current lowscore
        String usage_min;
        usage_min = readSetting("usage_min_count");

        // If it's a new lowscore - save it
        if(Integer.valueOf(usage_min).equals(0)){ // first low-score
            writeSetting("usage_min_count", usageCount);
            writeSetting("usage_min_date", lastDate);
        }
        else
        {
            if(Integer.valueOf(usageCount) < Integer.valueOf(usage_min)){ // new lowscore
                writeSetting("usage_min_count", usageCount);
                writeSetting("usage_min_date", lastDate);
            }
        }

        // log to Firebase
        logFireBaseEvent("p_checkForNewLowscore");
    }



    /**
     * on day change store days value as yesterdays count
     *
     * @param usageCount the count as string
     * @param lastDate the date as string
     */
    private void rememberYesterdayUsageCount(String usageCount, String lastDate){
        Log.d(TAG, "F: rememberYesterdayUsageCount");

        writeSetting("usage_yesterday_count", usageCount);
        writeSetting("usage_yesterday_date", lastDate);

        // log to Firebase
        logFireBaseEvent("p_rememberYesterdayUsageCount");
    }



    /**
     * generates the current date (YYYYMMDD) and returns it as string
     *
     * @return current date as string
     */
    private String getCurrentDate(){
        Log.d(TAG, "F: getCurrentDate");

        SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMdd");
        Date myDate = new Date();
        String curDate = timeStampFormat.format(myDate);
        Log.d(TAG, curDate);

        // log to Firebase
        logFireBaseEvent("p_getCurrentDate");

        return curDate;
    }



    /**
     * Read value from SharedPreferences
     * (default value is "0")
     *
     * @param key name of key to read
     * @return returns the key value
     */
    private String readSetting(String key) {
        Log.d(TAG, "F: readSetting");

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String value = settings.getString(key, "0");

        Log.d(TAG, "Key: "+key+", value is: "+value);

        // log to Firebase
        logFireBaseEvent("p_readSetting");

        return value;
    }



    /**
     * Write Settings to SharedPreferences
     *
     * @param key name of key to write
     * @param newValue value for key to write
     */
    private void writeSetting(String key, String newValue) {
        Log.d(TAG, "F: writeSetting");

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        Editor editor = settings.edit();
        editor.putString(key, newValue);
        //editor.commit();
        editor.apply();

        // log to Firebase
        logFireBaseEvent("p_writeSetting");

        Log.d(TAG, "Key: "+key+", new value is: "+newValue);
    }



    /**
     * display a notification
     *
     * @param title the title of the notification
     * @param message the message text
     */
    private void displayNotification(String title, String message) {
        Log.d(TAG, "F: displayNotification");

        // Prepare intent which is triggered if the notification is selected
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        // Build notification
        // Actions are just fake
        Notification noti = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.mipmap.app_icon)
                .setContentIntent(pIntent)
                .build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // hide the notification after its selected
        noti.flags |= Notification.FLAG_AUTO_CANCEL;

        Objects.requireNonNull(notificationManager).notify(0, noti);

        // log to Firebase
        logFireBaseEvent("p_displayNotification");
    }


    /**
     * resets the UI after having executed delete all Usage Data
     */
    private void resetUI(){

        // Update UI
        updateUI("0");

        // update min value in ui to default text
        TextView min;
        min = findViewById(R.id.ui_textMin);
        min.setText(R.string.min);

        // update min & max value in ui to default text
        TextView max;
        max = findViewById(R.id.ui_textMax);
        max.setText(R.string.max);

        // log to Firebase
        logFireBaseEvent("p_resetUI");
    }



    /**
     * Update the UI
     *
     * @param newValue new value for UI usage counter to display
     */
    private void updateUI(String newValue) {
        Log.d(TAG, "F: updateUI");

        // Current Counter
        //
        TextView currentCount;
        currentCount = findViewById(R.id.ui_currentCount);
        currentCount.setText(newValue);

        // yesterdays count
        //
        // get yesterday from settings
        String yesterday;
        yesterday = readSetting("usage_yesterday_count");
        //
        // update it if necessary
        if(!("0").equals(yesterday))   // if yesterday is not 0 - update it
        {
            TextView yesterdays_count;
            yesterdays_count = findViewById(R.id.ui_yesterdaysCount);
            yesterdays_count.setText(yesterday);

            // percentage diff from cur to yesterday
            //
            int yesterday_percent_value;
            yesterday_percent_value =  Integer.parseInt(yesterday) * 100 /  Integer.parseInt(newValue);
            yesterday_percent_value = yesterday_percent_value - 100;

            // update ui
            TextView yesterdays_percent;
            yesterdays_percent = findViewById(R.id.ui_yesterdaysPercent);
            yesterdays_percent.setText(Integer.toString(yesterday_percent_value)+"%");
        }

        // Overall: update average usage per day
        //
        String averageUsageDataPerDay;
        averageUsageDataPerDay = getUsageDataAverage();

        TextView ui_average;
        ui_average = findViewById(R.id.ui_avg);
        ui_average.setText(averageUsageDataPerDay);

        // Overall: Min value
        //
        String min_value;
        min_value = readSetting("usage_min_count");

        String min_date;
        min_date = readSetting("usage_min_date");

        Log.d(TAG, min_value);

        if(!("0").equals(min_value)) {
            TextView min;
            min = findViewById(R.id.ui_min);
            min.setText(min_value + " ("+min_date+")");
        }

        // Overall: Max value
        //
        String max_value;
        max_value = readSetting("usage_max_count");

        String max_date;
        max_date = readSetting("usage_max_date");

        Log.d(TAG, max_value);

        if(!("0").equals(max_value)) {
            TextView max;
            max = findViewById(R.id.ui_max);
            max.setText(max_value + " ("+max_date+")");
        }

        // log to Firebase
        logFireBaseEvent("p_updateUI");
    }
}