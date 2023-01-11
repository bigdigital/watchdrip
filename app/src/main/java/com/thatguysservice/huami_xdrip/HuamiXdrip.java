package com.thatguysservice.huami_xdrip;

import android.content.Context;

import androidx.annotation.StringRes;
import androidx.multidex.MultiDexApplication;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.reactiveandroid.ReActiveAndroid;
import com.reactiveandroid.ReActiveConfig;
import com.reactiveandroid.internal.database.DatabaseConfig;
import com.thatguysservice.huami_xdrip.models.database.AppDatabase;
import com.thatguysservice.huami_xdrip.services.BroadcastService;
import com.thatguysservice.huami_xdrip.utils.jobs.CleanupWorker;

import java.util.concurrent.TimeUnit;

public class HuamiXdrip extends MultiDexApplication {
    private static Context context;

    public static Context getAppContext() {
        return HuamiXdrip.context;
    }

    public static String gs(@StringRes final int id) {
        return getAppContext().getString(id);
    }

    @Override
    public void onCreate() {
        HuamiXdrip.context = getApplicationContext();
        BroadcastService.initialStartIfEnabled();
        ReactiveAndroidInitialize();
        scheduleTask();
        super.onCreate();
    }

    private void ReactiveAndroidInitialize(){
        DatabaseConfig appDatabaseConfig = new DatabaseConfig.Builder(AppDatabase.class)
                .build();

        ReActiveAndroid.init(new ReActiveConfig.Builder(this)
                .addDatabaseConfigs(appDatabaseConfig)
                .build());
    }

    private void scheduleTask() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(false)
                .build();

        WorkRequest workRequest =
                new PeriodicWorkRequest.Builder(CleanupWorker.class, 24, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueue(workRequest);
    }


}
