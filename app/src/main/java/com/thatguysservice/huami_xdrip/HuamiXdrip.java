package com.thatguysservice.huami_xdrip;

import android.content.Context;

import androidx.annotation.StringRes;
import androidx.multidex.MultiDexApplication;

import com.activeandroid.ActiveAndroid;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

public class HuamiXdrip extends MultiDexApplication {
    private static Context context;

    @Override
    public void onCreate() {
        HuamiXdrip.context = getApplicationContext();
        MiBandEntry.initialStartIfEnabled();
        ActiveAndroid.initialize(this);
        super.onCreate();
    }

    public static Context getAppContext() {
        return HuamiXdrip.context;
    }

    public static String gs(@StringRes final int id) {
        return getAppContext().getString(id);
    }
}
