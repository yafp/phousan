package de.yafp.phousan;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class MyPrefsBackupAgent extends BackupAgentHelper {
    // The name of the SharedPreferences file
    private static final String PREFS_NAME = "phousan_settings";

    // A key to uniquely identify the set of backup data
    static final String PREFS_BACKUP_KEY = "prefs";

    // Allocate a helper and add it to the backup agent
    @Override
    public void onCreate() {
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, PREFS_NAME);
        addHelper(PREFS_BACKUP_KEY, helper);
    }
}
