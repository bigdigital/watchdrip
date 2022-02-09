package com.thatguysservice.huami_xdrip.utils.jobs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.UserError;

public class CleanupWorker extends Worker {

    public static final String TAG = CleanupWorker.class.getSimpleName();

    public CleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        UserError.Log.d(TAG, "CleanupWorker started");
        final long startTime = Helper.tsl();
        UserError.cleanup();
        UserError.Log.uel(TAG, Helper.dateTimeText(Helper.tsl()) + " Job Ran - finished, duration: " + Helper.niceTimeScalar(Helper.msSince(startTime)));

        return Result.success();
    }
}
