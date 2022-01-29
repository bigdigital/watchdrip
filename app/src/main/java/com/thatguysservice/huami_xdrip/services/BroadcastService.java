package com.thatguysservice.huami_xdrip.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models.WatchSettings;
import com.thatguysservice.huami_xdrip.BuildConfig;
import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.R;
import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.receivers.xDripReceiver;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;


public class BroadcastService extends Service {

    public static final String INTENT_FUNCTION_KEY = "FUNCTION";
    public static final String INTENT_PACKAGE_KEY = "PACKAGE";
    public static final String INTENT_REPLY_MSG = "REPLY_MSG";
    public static final String INTENT_SETTINGS = "SETTINGS";
    public static final String INTENT_ALERT_TYPE = "ALERT_TYPE";
    public static final String CMD_SET_SETTINGS = "set_settings";
    public static final String CMD_UPDATE_BG_FORCE = "update_bg_force";
    public static final String CMD_ALERT = "alarm";
    public static final String CMD_SNOOZE_ALERT = "snooze_alarm";
    public static final String CMD_ADD_STEPS = "add_steps";
    public static final String CMD_ADD_HR = "add_hrs";
    public static final String CMD_ADD_TREATMENT = "add_treatment";
    public static final String CMD_START = "start";
    public static final String CMD_UPDATE_BG = "update_bg";
    public static final String CMD_REPLY_MSG = "reply_msg";
    public static final String CMD_MESSAGE = "message";

    public static final String CMD_LOCAL_REFRESH = "local_refresh";
    public static final String CMD_LOCAL_AFTER_ALARM = "local_after_alarm";
    //send
    protected static final String ACTION_WATCH_COMMUNICATION_RECEIVER = "com.eveningoutpost.dexdrip.watch.wearintegration.WATCH_BROADCAST_RECEIVER";
    //listen
    protected static final String ACTION_WATCH_COMMUNICATION_SENDER = "com.eveningoutpost.dexdrip.watch.wearintegration.WATCH_BROADCAST_SENDER";
    protected String TAG = this.getClass().getSimpleName();
    private BroadcastReceiver newDataReceiver;

    public static boolean shouldServiceRun() {
        return MiBandEntry.isEnabled();
    }

    public static void initialStartIfEnabled() {
        if (shouldServiceRun()) {
            Inevitable.task("mb-full-initial-start", 500, new Runnable() {
                @Override
                public void run() {
                    bgForce();
                }
            });
        }
    }

    public static void bgForce() {
        Helper.startService(BroadcastService.class, INTENT_FUNCTION_KEY, CMD_UPDATE_BG_FORCE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setReceiversEnableState(boolean enable) {
        if (enable) {
            if (newDataReceiver == null) {
                newDataReceiver = new xDripReceiver();
                registerReceiver(newDataReceiver, new IntentFilter(ACTION_WATCH_COMMUNICATION_SENDER));
            }
        } else {
            if (newDataReceiver != null) {
                unregisterReceiver(newDataReceiver);
                newDataReceiver = null;
            }
        }
    }

    @Override
    public void onCreate() {
        UserError.Log.e(TAG, "starting service");

        if (shouldServiceRun()) {
            setReceiversEnableState(true);
        }
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        UserError.Log.e(TAG, "killing service");
        setReceiversEnableState(false);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = Helper.getWakeLock("Miband service", 60000);
        try {
            if (shouldServiceRun()) {
                if (intent != null) {
                    final String function = intent.getStringExtra(INTENT_FUNCTION_KEY);
                    if (function != null) {
                        handleCommand(function, intent);
                    } else {
                        // no specific function
                    }
                }
                return START_STICKY;
            } else {
                UserError.Log.d(TAG, "Service is NOT set be active - shutting down");
                stopSelf();
                return START_NOT_STICKY;
            }
        } finally {
            Helper.releaseWakeLock(wl);
        }
    }

    private void handleCommand(String function, Intent intentIn) {
        UserError.Log.d(TAG, "handleCommand function:" + function);

        Intent intent = new Intent(ACTION_WATCH_COMMUNICATION_RECEIVER);
        int value;
        switch (function) {
            case CMD_UPDATE_BG_FORCE:
                intent.putExtra(INTENT_SETTINGS, getSettings());
                break;
            case CMD_SNOOZE_ALERT:
                break;
            case CMD_ADD_STEPS:
                value = intentIn.getIntExtra("value", 0);
                intent.putExtra("timeStamp", Helper.tsl());
                intent.putExtra("value", value);
                break;
            case CMD_ADD_HR:
                value = intentIn.getIntExtra("value", 0);
                intent.putExtra("timeStamp", Helper.tsl());
                intent.putExtra("value", value);
                break;
            case CMD_REPLY_MSG:
                String replyMsg = intentIn.getStringExtra(INTENT_REPLY_MSG);
                UserError.Log.e(TAG, "replyMsg:" + replyMsg);
                break;
            default:
                return;
        }
        sendBroadcast(function, intent);
    }

    public WatchSettings getSettings() {
        WatchSettings settings = new WatchSettings();
        settings.setGraphStart(Helper.tsl() - Constants.HOUR_IN_MS * MiBandEntry.getGraphHours());
        settings.setGraphEnd(Helper.tsl() + Constants.MINUTE_IN_MS * 30);
        settings.setApkName(getString(R.string.app_name));
        settings.setDisplayGraph(true);
        return settings;
    }

    public void sendBroadcast(String functionName, Intent intent) {
        intent.putExtra(INTENT_FUNCTION_KEY, functionName);
        intent.putExtra(INTENT_PACKAGE_KEY, BuildConfig.APPLICATION_ID);
        UserError.Log.d(TAG, String.format("sendBroadcast functionName:%s", functionName));
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        HuamiXdrip.getAppContext().sendBroadcast(intent);
    }
}
