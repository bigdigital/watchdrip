package com.thatguysservice.huami_xdrip.utils.jobs;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.UserError;

public class DailyJob extends Worker {

    public static final String TAG = DailyJob.class.getSimpleName();

    public DailyJob(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final long startTime = Helper.tsl();
        work();
        UserError.Log.uel(TAG, Helper.dateTimeText(Helper.tsl()) + " Job Ran - finished, duration: " + Helper.niceTimeScalar(Helper.msSince(startTime)));

        return Result.success();
    }

    private void work() {
        final PowerManager.WakeLock wl = Helper.getWakeLock("DailyIntentWork", 120000);
        try {
            if (Helper.pratelimit("daily-intent-service", 60000)) {
                Log.i(TAG, "DailyIntentService onHandleIntent Starting");
                Long start = Helper.tsl();
                try {
                    UserError.cleanup();
                } catch (Exception e) {
                    Log.e(TAG, "DailyIntentService exception on UserError ", e);
                }
               /* try {
                    checkForAnUpdate(xdrip.getAppContext());
                } catch (Exception e) {
                    Log.e(TAG, "DailyIntentService exception on checkForAnUpdate ", e);
                } */

            } else {
                // Log.e(TAG, "DailyIntentService exceeding rate limit");
            }
        } finally {
            Helper.releaseWakeLock(wl);
        }
    }
}
