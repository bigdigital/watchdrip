package com.thatguysservice.huami_xdrip.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.services.BroadcastService;

public class AutoStart extends BroadcastReceiver {

    private static final String TAG = "AutoStart";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        UserError.Log.ueh(TAG, "Device Rebooted - Auto Start: " + intent.getAction());
        BroadcastService.initialStartIfEnabled();
    }
}
