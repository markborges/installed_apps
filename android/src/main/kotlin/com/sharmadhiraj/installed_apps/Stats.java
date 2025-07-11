//Original Source: https://github.com/cph-cachet/flutter-plugins/blob/master/packages/app_usage/android/src/main/kotlin/dk/cachet/app_usage/Stats.java
package com.sharmadhiraj.installed_apps;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.util.Log;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * Created by User on 3/2/15.
 */
public class Stats {
    private static final String TAG = Stats.class.getSimpleName();

    /** Check if permission for usage statistics is required,
     * by fetching usage statistics since the beginning of time
     */
    @SuppressWarnings("ResourceType")
    public static boolean checkIfStatsAreAvailable(Context context) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService("usagestats");
        long now  = Calendar.getInstance().getTimeInMillis();

        // Check if any usage stats are available from the beginning of time until now
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, 0, now);

        // Return whether or not stats are available
        return stats.size() > 0;
    }
}