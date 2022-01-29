package com.thatguysservice.huami_xdrip.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.thatguysservice.huami_xdrip.BuildConfig;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.services.BroadcastService;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_AFTER_ALARM;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_REFRESH;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_START;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG_FORCE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.INTENT_FUNCTION_KEY;

public class xDripReceiver extends BroadcastReceiver {
    protected String TAG = this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String function = intent.getStringExtra(INTENT_FUNCTION_KEY);
            String receiver = intent.getPackage();
            UserError.Log.e(TAG, "got intent");
            UserError.Log.d(TAG, String.format("functionName: %s, receiver: %s", function, receiver));
            Helper.getWakeLock(TAG, 1000);
            if (function.equals(CMD_START)) {
                Helper.startService(BroadcastService.class, INTENT_FUNCTION_KEY, CMD_UPDATE_BG_FORCE);
                return;
            }
            if (!BuildConfig.APPLICATION_ID.equals(receiver)) return;
            if (function.equals(CMD_LOCAL_REFRESH) || function.equals(CMD_LOCAL_AFTER_ALARM))
                return; //do not allow to run local functions
            Bundle extras = intent.getExtras();
            MiBandEntry.sendToService(function, extras);
        } catch (Exception e) {
            UserError.Log.e(TAG, "onReceive Error: " + e.toString());
        }
    }
}
