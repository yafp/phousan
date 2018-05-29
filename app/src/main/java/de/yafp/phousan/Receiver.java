package de.yafp.phousan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

// SharedPreferences
//
// Today:
// - usage_last_date = timestamp, when the app last wrote to SharedPreferences
// - usage_today_count = todays usage count
//
// Yesterday:
// - usage_yesterday_count
//
// - usage_overall_max_count = overall max usage count per day
// - usage_overall_max_date = date when highscore was achieved
//
// - usage_overall_min_count = overall min usage count per day
// - usage_overall_min_date = date when lowscore was achieved


/**
 * Extends BroadcastReceiver: class for code that receives and handles broadcast intents.
 *
 * here:
 * Listens for
 * - ACTION_BOOT_COMPLETED
 * - ACTION_SCREEN_OFF
 * - ACTION SCREEN_ON
 * and realizes all the background logic of phousan which includes a lot of read and write actions after an event occured
 */
public class Receiver extends BroadcastReceiver {
    private static final String TAG = "phousan";
    private static final String PREFS_NAME = "phousan_settings";

    @Override
    public void onReceive(Context context, Intent intent) {

        // Boot completed
        //
        if (Intent.ACTION_BOOT_COMPLETED.equalsIgnoreCase(intent.getAction())) {
            Log.d(TAG, "onReceive - ACTION_BOOT_COMPLETED");

            Intent intent2 = new Intent(context,BackgroundService.class);
            context.startService(intent2);
        }

        // SCREEN OFF
        //
        if (Intent.ACTION_SCREEN_OFF.equalsIgnoreCase(intent.getAction())) {
            Log.d(TAG, "onReceive - ACTION_SCREEN_OFF");
        }

        // SCREEN ON
        //
        if (Intent.ACTION_SCREEN_ON.equalsIgnoreCase(intent.getAction())) {
            Log.d(TAG, "onReceive - ACTION_SCREEN_ON");

            boolean isItANewDay;
            isItANewDay = checkForDayChange(context);
            Log.d(TAG, "new day?: "+Boolean.toString(isItANewDay));

        }
    }



    /**
     * checks if the last update to sharedPreferences was today or somewhere in the past
     *
     * @param context context of current state of the application or object
     * @return true or false
     */
    private boolean checkForDayChange(Context context){
        Log.d(TAG, "F: checkForDayChange");

        // get todays date
        String curDate;
        curDate = getCurrentDate();

        // get last record from shared prefs
        String lastPrefsDate;
        lastPrefsDate = readPreferences(context, "usage_last_date");

        if(curDate.equals(lastPrefsDate)){
            // = same day

            // update usage count +1
            updateTodaysUsageCount(context);

            // update usage history per day
            updateUsagePerDayHistory(context, curDate);

            // get new  usage count
            String lastUsageCount = readPreferences(context, "usage_today_count");
            checkForNewHighScore(context, lastPrefsDate, lastUsageCount);

            return false;
        }
        else {
            // = new day

            // finish the previous day
            //
            String lastUsageCount = readPreferences(context, "usage_today_count");

            checkForNewHighScore(context, lastPrefsDate, lastUsageCount);
            checkForNewLowscore(context, lastPrefsDate, lastUsageCount);
            rememberYesterdayUsageCount(context, lastPrefsDate, lastUsageCount);

            // start new day
            //
            // write new current date
            writePreferences(context, "usage_last_date", curDate);

            // write new current count
            writePreferences(context, "usage_today_count", "1");

            return true;
        }
    }



    /**
     * Update the usage count of the current day (is assuming we do know it is today)
     *
     * @param context context of current state of the application or object
     */
    private void updateTodaysUsageCount(Context context){
        Log.d(TAG, "F: updateTodaysUsageCount");

        // read old value
        String oldValue;
        oldValue = readPreferences(context, "usage_today_count");

        // calculate new value
        int newValue;
        newValue = Integer.parseInt(oldValue) + 1;

        // write new value
        writePreferences(context, "usage_today_count", Integer.toString(newValue));

    }



    /**
     * remember the usage acount of the previous day during day-change
     *
     * @param context context of current state of the application or object
     * @param date the date of the previous day
     * @param usageCount the usage count of the previous day
     */
    private void rememberYesterdayUsageCount(Context context, String date, String usageCount){
        Log.d(TAG, "F: rememberYesterdayUsageCount");

        writePreferences(context,"usage_yesterday_count", usageCount);
        writePreferences(context, "usage_yesterday_date", date);
    }



    /**
     * Updates overall highscore if needed
     *
     * @param context context of current state of the application or object
     * @param date date of the usage data value
     * @param usageCount the usage count
     */
    private void checkForNewHighScore(Context context, String date, String usageCount){
        Log.d(TAG, "F: checkForNewHighScore");

        // get current highscore
        String curHighscore;
        curHighscore = readPreferences(context, "usage_overall_max_count");

        Log.d(TAG, "Highscore: "+curHighscore);

        if ((Integer.parseInt(curHighscore)) < (Integer.parseInt(usageCount))){
            Log.d(TAG, "Updated highscore");

            writePreferences(context, "usage_overall_max_count", usageCount);
            writePreferences(context, "usage_overall_max_date", date);

            curHighscore = readPreferences(context, "usage_overall_max_count");
            Log.d(TAG, "New highscore: "+curHighscore);
        }
    }



    /**
     * Updated overall lowscore if needed
     *
     * @param context context of current state of the application or object
     * @param date date when the new usage data occured
     * @param usageCount usage count for this particular date
     */
    private void checkForNewLowscore(Context context, String date, String usageCount){
        Log.d(TAG, "F: checkForNewLowscore");

        // get current lowscore
        String usage_min;
        usage_min = readPreferences(context, "usage_overall_min_count");

        // If it's a new lowscore - save it
        if(Integer.valueOf(usage_min).equals(0)){ // first low-score
            writePreferences(context,"usage_overall_min_count", usageCount);
            writePreferences(context,"usage_overall_min_date", date);
        }
        else {
            if(Integer.valueOf(usageCount) < Integer.valueOf(usage_min)){ // new lowscore
                writePreferences(context,"usage_overall_min_count", usageCount);
                writePreferences(context,"usage_overall_min_date", date);
            }
        }
    }



    /**
     * Returns todays timestamp
     *
     * @return timestamp as string (YYYYMMDD)
     */
    private String getCurrentDate(){
        Log.d(TAG, "F: getCurrentDate");

        SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMdd");
        Date myDate = new Date();
        String curDate = timeStampFormat.format(myDate);
        Log.d(TAG, curDate);

        return curDate;
    }



    /**
     * updates the usage per day history of the current day
     *
     * @param context context of current state of the application or object
     * @param date date of usage data value
     */
    private void updateUsagePerDayHistory(Context context, String date) {
        Log.v(TAG, "F: updateUsagePerDayHistory");

        // get current count
        String currentUsageCount = readPreferences(context, "usage_today_count");

        writePreferences(context, date, currentUsageCount);
    }



    /**
     * Read a SharedPreference key
     *
     * @param context context of current state of the application or object
     * @param key name of the shared preference key
     * @return returns the value of the key
     */
    private String readPreferences(Context context, String key) {
        Log.d(TAG, "F: readPreferences");

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String value = prefs.getString(key, "0");

        Log.d(TAG, "Key: "+key+", value is: "+value);

        return value;
    }



    /**
     * Write a shared preference key
     *
     * @param context context of current state of the application or object
     * @param key name of the shared preference key
     * @param newValue new value for the shared preference key
     */
    private void writePreferences(Context context, String key, String newValue){
        Log.d(TAG, "F: writePreferences");

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, newValue);
        editor.apply();

        Log.d(TAG, "Key: "+key+", new value is: "+newValue);
    }
}