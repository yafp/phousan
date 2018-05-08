package de.yafp.phousan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.text.SimpleDateFormat;
import java.util.Date;

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
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        PowerOnReceiver receiver = new PowerOnReceiver();
        registerReceiver(receiver, filter);

        // read all SharedPref values
        tempReadAllPrefValues();

        // update data & UI
        updateData();

        // Click listener for image view: image_recommend
        ImageView imgRecommend = findViewById(R.id.image_recommend);
        imgRecommend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageRecommendClicked();
            }
        });

        // Click listener for image view: image_settings
        ImageView imgSettings = findViewById(R.id.image_settings);
        imgSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageSettingsClicked();
            }
        });

        // Click listener for image view: image_history
        ImageView imgHistory = findViewById(R.id.image_history);
        imgHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUsageDataPerDay();
            }
        });

        logFireBaseEvent("p_app_Launch");
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
     * for debugging during development
     */
    private void tempReadAllPrefValues(){
        Log.d(TAG, "F: tempReadAllPrefValues");

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        for (String key : settings.getAll().keySet()) {
            Log.i(String.format("Shared Preference : %s - %s", PREFS_NAME, key),
                    settings.getString(key, "error!"));
        }
    }



    /**
     * method to write some pseudo usage data
     * for debugging during development
     */
    private void tempWritePseudoSharedPrefData(){
        Log.d(TAG, "F: tempWritePseudoSharedPrefData");

        //WriteSetting("usage_min_count", "6");
        //WriteSetting("usage_min_date", "20150501");

        //WriteSetting("usage_max_count", "12");
        //WriteSetting("usage_max_date", "20160504");

        //WriteSetting("usage_yesterday_count", "10");
        //WriteSetting("usage_yesterday_date", "20180503");

        // Usage history:
        //WriteSetting("20180508", "96");
        //WriteSetting("20180507", "67");
        //WriteSetting("20180506", "103");
        //WriteSetting("20180505", "1");
    }



    /**
     * fetches all values from Shared Preferences and extracts only the daily usage counts
     */
    private void showUsageDataPerDay() {
        Log.d(TAG, "F: showUsageDataPerDay");

        StringBuilder outputUsageHistory;
        outputUsageHistory = new StringBuilder();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // print all settings
        for (String key : settings.getAll().keySet()) {

            if(key.startsWith("20")) {
                Log.i(String.format("Shared Preference : %s - %s", PREFS_NAME, key), settings.getString(key, "error!"));
                outputUsageHistory.append(key).append(" ").append(settings.getString(key, "error!")).append("\n");
            }
            else {
                Log.d(TAG, "Ignoring key, as it is not part of the daily usage history");
            }
        }

        // show as dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Usage History Per Day").setMessage(outputUsageHistory.toString());

        AlertDialog alert = builder.create();
        alert.show();

        logFireBaseEvent("p_show_usage_history");
    }



    /**
     * User can recommend the app to others
     */
    private void imageRecommendClicked() {
        Log.d(TAG, "F: imageRecommendClicked");

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

        // Add data to the intent, the receiving app will decide what to do with it.
        share.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.recommend_text));
        share.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=de.yafp.phousan");

        startActivity(Intent.createChooser(share, "Share link"));

        logFireBaseEvent("p_recommend");
    }



    /**
     * User opens app settings - Dummy so far
     */
    private void imageSettingsClicked() {
        Log.d(TAG, "F: imageSettingsClicked");
        displayToastMessage(getResources().getString(R.string.settings_text)); // show error as toast

        logFireBaseEvent("p_settings");
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

        logFireBaseEvent("p_display_toast");
    }



    /**
     * used on day change to write a usage per day history
     *
     * @param date the date as string
     * @param usageCount the actual usage count as string
     */
    private void onNewDayWriteUsagePerDayHistory(String date, String usageCount) {
        Log.v(TAG, "F: onNewDayWriteUsagePerDayHistory");

        WriteSetting(date, usageCount);

        logFireBaseEvent("p_write_day_history");
    }


    /**
     * reads the last data from SharedPrefs and updates it
     * (including if day switches)
     */
    public void updateData(){
        Log.d(TAG, "F: updateData");

        // get current date
        String curDate = GetCurrentDate();

        // get date of last sharedPref activity
        String lastDate = ReadSetting("usage_current_date");

        // Read ScreenOn
        String currentScreenOnCount;
        currentScreenOnCount = ReadSetting("usage_current_count");

        int newScreenOnCount;
        if(lastDate.equals(curDate)){
            Log.d(TAG, "lastDate == curDate");

            // Calculate new value: +1
            newScreenOnCount = Integer.parseInt(currentScreenOnCount);
            newScreenOnCount = newScreenOnCount +1;

            // check highscore max
            onNewDayCheckForUsageMax(Integer.toString(newScreenOnCount), lastDate);
        }
        else {
            Log.d(TAG, "lastDate != curDate");

            // new day
            //
            onNewDayCheckForUsageMax(currentScreenOnCount, lastDate); // check if last days value is highscore
            onNewDayCheckForUsageMin(currentScreenOnCount, lastDate); // check if last days value is lowscore
            onNewDayStoreYesterdaysValue(currentScreenOnCount, lastDate);

            onNewDayWriteUsagePerDayHistory(currentScreenOnCount, lastDate); // write usage per day history

            // Start new day with the new date
            WriteSetting("usage_current_date", curDate);
            newScreenOnCount = 1;
        }

        // Write new count value
        WriteSetting("usage_current_count", Integer.toString(newScreenOnCount));

        updateUI(Integer.toString(newScreenOnCount));

        logFireBaseEvent("p_update_data");
    }



    /**
     * on new day: check if last days usage count is highscore
     *
     * @param usageCount the count as string
     * @param lastDate the date as string
     */
    private void onNewDayCheckForUsageMax(String usageCount, String lastDate){
        Log.d(TAG, "F: newDateCheckForUsageMax");

        // get current highscore
        String usage_max;
        usage_max = ReadSetting("usage_max_count");

        // If it's a new highscore - save it
        if(Integer.valueOf(usageCount) > Integer.valueOf(usage_max)){
            WriteSetting("usage_max_count", usageCount);
            WriteSetting("usage_max_date", lastDate);

            logFireBaseEvent("p_new_highscore");
        }
    }



    /**
     * on new day: check if last days usage count is lowscore
     *
     * @param usageCount the count as string
     * @param lastDate the date as string
     */
    private void onNewDayCheckForUsageMin(String usageCount, String lastDate){
        Log.d(TAG, "F: newDateCheckForUsageMin");

        // get current highscore
        String usage_min;
        usage_min = ReadSetting("usage_min_count");

        // If it's a new lowscore - save it
        if(Integer.valueOf(usage_min).equals(0)){ // first low-score
            WriteSetting("usage_min_count", usageCount);
            WriteSetting("usage_min_date", lastDate);
        }
        else
        {
            if(Integer.valueOf(usageCount) < Integer.valueOf(usage_min)){ // new lowscore
                WriteSetting("usage_min_count", usageCount);
                WriteSetting("usage_min_date", lastDate);

                logFireBaseEvent("p_new_lowscore");
            }
        }
    }



    /**
     * on day change store days value as yesterdays count
     *
     * @param usageCount the count as string
     * @param lastDate the date as string
     */
    private void onNewDayStoreYesterdaysValue(String usageCount, String lastDate){
        Log.d(TAG, "F: onNewDayStoreYesterdaysValue");

        WriteSetting("usage_yesterday_count", usageCount);
        WriteSetting("usage_yesterday_date", lastDate);

        logFireBaseEvent("p_new_day");
    }



    /**
     * generates the current date (YYYYMMDD) and returns it as string
     *
     * @return current date as string
     */
    private String GetCurrentDate(){
        Log.d(TAG, "F: GetCurrentDate");

        SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMdd");
        Date myDate = new Date();
        String curDate = timeStampFormat.format(myDate);
        Log.d(TAG, curDate);

        logFireBaseEvent("p_get_data");

        return curDate;
    }



    /**
     * Read value from SharedPreferences
     * (default value is "0")
     *
     * @param key name of key to read
     * @return returns the key value
     */
    private String ReadSetting(String key) {
        Log.d(TAG, "F: ReadSetting");

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String value = settings.getString(key, "0");

        Log.d(TAG, "Key: "+key+", value is: "+value);

        logFireBaseEvent("p_read");

        return value;
    }



    /**
     * Write Settings to SharedPreferences
     *
     * @param key name of key to write
     * @param newValue value for key to write
     */
    private void WriteSetting(String key, String newValue) {
        Log.d(TAG, "F: WriteSetting");

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        Editor editor = settings.edit();
        editor.putString(key, newValue);
        //editor.commit();
        editor.apply();

        logFireBaseEvent("p_write");

        Log.d(TAG, "Key: "+key+", new value is: "+newValue);
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
        currentCount = findViewById(R.id.current_count);
        currentCount.setText(newValue);

        // yesterdays count
        String yesterday;
        yesterday = ReadSetting("usage_yesterday_count");
        TextView yesterdays_count;
        yesterdays_count = findViewById(R.id.yesterdays_count);
        yesterdays_count.setText(yesterday);

        // Historic Min value
        //
        String min_value;
        min_value = ReadSetting("usage_min_count");

        String min_date;
        min_date = ReadSetting("usage_min_date");

        Log.d(TAG, min_value);

        if(!min_value.equals("0")) {
            TextView min;
            min = findViewById(R.id.min);
            min.setText(min_value + "\n("+min_date+")");
        }

        // Historic Max value
        //
        String max_value;
        max_value = ReadSetting("usage_max_count");

        String max_date;
        max_date = ReadSetting("usage_max_date");

        Log.d(TAG, max_value);

        if(!max_value.equals("0")) {
            TextView max;
            max = findViewById(R.id.max);
            max.setText(max_value + "\n("+max_date+")");
        }

        logFireBaseEvent("p_update_ui");
    }
}