package com.thatguysservice.huami_xdrip.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.thatguysservice.huami_xdrip.BuildConfig;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.services.BroadcastService;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_PREFIX;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_XDRIP_APP_GOT_RESPONSE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_START;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG_FORCE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.INTENT_FUNCTION_KEY;

public class xDripReceiver extends BroadcastReceiver {
    protected String TAG = this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
           if (!BroadcastService.shouldServiceRun()) return;
            String function = intent.getStringExtra(INTENT_FUNCTION_KEY);
            String receiver = intent.getPackage();
            UserError.Log.e(TAG, String.format("got intent functionName: %s, receiver: %s", function, receiver));
            Helper.getWakeLock(TAG, 1000);
            if (function.equals(CMD_START)) {
                BroadcastService.handleCommand(CMD_UPDATE_BG_FORCE);
                return;
            }
            if (function.equals(CMD_UPDATE_BG_FORCE)) {
                BroadcastService.handleCommand(CMD_LOCAL_XDRIP_APP_GOT_RESPONSE);
            }
            if (!BuildConfig.APPLICATION_ID.equals(receiver)) return;
            if (function.startsWith(CMD_LOCAL_PREFIX))
                return; //do not allow to run local functions

            if (function.equals(BroadcastService.CMD_REPLY_MSG)) {
                String replyMsg = intent.getStringExtra(BroadcastService.INTENT_REPLY_MSG);
                UserError.Log.e(TAG, "replyMsg:" + replyMsg);
            }
            if (function.equals(BroadcastService.CMD_SNOOZE_ALERT)) {
                String replyMsg = intent.getStringExtra(BroadcastService.INTENT_REPLY_MSG);
                UserError.Log.e(TAG, "Snooze replyMsg:" + replyMsg);
            }
            Bundle extras = intent.getExtras();

            MiBandEntry.sendToService(function, extras);
        } catch (Exception e) {
            UserError.Log.e(TAG, "onReceive Error: " + e.toString());
        }
    }
}
