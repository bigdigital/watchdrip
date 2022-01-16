package com.thatguysservice.huami_xdrip.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.thatguysservice.huami_xdrip.BuildConfig;
import com.thatguysservice.huami_xdrip.models.JoH;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.services.BroadcastService;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

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
            JoH.getWakeLock(TAG, 1000);
            if (function.equals(CMD_START)) {
                JoH.startService(BroadcastService.class, INTENT_FUNCTION_KEY, CMD_UPDATE_BG_FORCE);
                return;
            }
            if (!BuildConfig.APPLICATION_ID.equals(receiver)) return;
            Bundle extras = intent.getExtras();
            MiBandEntry.sendToService(function, extras);
        } catch (Exception e) {
            UserError.Log.e(TAG, "onReceive Error: " + e.toString());
        }
    }
}
