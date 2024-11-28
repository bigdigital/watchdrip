package com.thatguysservice.huami_xdrip.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.services.broadcastservice.models.Settings;
import com.thatguysservice.huami_xdrip.BuildConfig;
import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.R;
import com.thatguysservice.huami_xdrip.UtilityModels.ForegroundServiceStarter;
import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.utils.framework.WakeLockTrampoline;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandService;

public class BroadcastService {

    public static final String INTENT_FUNCTION_KEY = "FUNCTION";
    public static final String INTENT_PACKAGE_KEY = "PACKAGE";
    public static final String INTENT_REPLY_MSG = "REPLY_MSG";
    public static final String INTENT_REPLY_CODE = "REPLY_CODE";
    public static final String INTENT_SETTINGS = "SETTINGS";
    public static final String INTENT_ALERT_TYPE = "ALERT_TYPE";
    public static final String INTENT_STAT_HOURS = "STAT_HOURS";

    public static final String CMD_SET_SETTINGS = "set_settings";
    public static final String CMD_UPDATE_BG_FORCE = "update_bg_force";
    public static final String CMD_ALERT = "alarm";
    public static final String CMD_CANCEL_ALERT = "cancel_alarm";
    public static final String CMD_SNOOZE_ALERT = "snooze_alarm";
    public static final String CMD_ADD_STEPS = "add_steps";
    public static final String CMD_ADD_HR = "add_hrs";
    public static final String CMD_ADD_TREATMENT = "add_treatment";
    public static final String CMD_START = "start";
    public static final String CMD_UPDATE_BG = "update_bg";
    public static final String CMD_REPLY_MSG = "reply_msg";
    public static final String CMD_MESSAGE = "message";
    public static final String CMD_STAT_INFO = "stat_info";

    public static final String INTENT_REPLY_CODE_OK = "OK";
    public static final String INTENT_REPLY_CODE_ERROR = "ERROR";
    public static final String INTENT_REPLY_CODE_PACKAGE_ERROR = "ERROR_NO_PACKAGE";
    public static final String INTENT_REPLY_CODE_NOT_REGISTERED = "NOT_REGISTERED";

    public static final String CMD_LOCAL_PREFIX = "local_";
    public static final String CMD_LOCAL_REFRESH = CMD_LOCAL_PREFIX + "refresh";
    public static final String CMD_LOCAL_AFTER_MISSING_ALARM = CMD_LOCAL_PREFIX + "after_alarm";
    public static final String CMD_LOCAL_BG_FORCE_REMOTE = CMD_LOCAL_PREFIX + "bg_force";
    public static final String CMD_LOCAL_UPDATE_BG_AS_NOTIFICATION = CMD_LOCAL_PREFIX + "update_bg_as_notification";
    public static final String CMD_LOCAL_XDRIP_APP_NO_RESPONSE = CMD_LOCAL_PREFIX + "xdrip_app_no_responce";
    public static final String CMD_LOCAL_XDRIP_APP_GOT_RESPONSE = CMD_LOCAL_PREFIX + "xdrip_app_got_responce";
    public static final String CMD_LOCAL_WATCHDOG = CMD_LOCAL_PREFIX + "watchdog";

    private static final int XDRIP_APP_RESPONCE_DELAY = (int) (Constants.SECOND_IN_MS * 10);

    //send
    protected static final String ACTION_WATCH_COMMUNICATION_RECEIVER = "com.eveningoutpost.dexdrip.watch.wearintegration.BROADCAST_SERVICE_RECEIVER";
    //listen
    protected static final String ACTION_WATCH_COMMUNICATION_SENDER = "com.eveningoutpost.dexdrip.watch.wearintegration.BROADCAST_SERVICE_SENDER";
    protected static String TAG = "BroadcastService";

    private static PendingIntent xdripResponseIntend;
    private ForegroundServiceStarter foregroundServiceStarter;

    public static boolean shouldServiceRun() {
        return MiBandEntry.isEnabled();
    }

    public static void initialStartIfEnabled() {
        bgForce();
    }

    public static void bgForce() {
        if (shouldServiceRun()) {
            handleCommand(CMD_UPDATE_BG_FORCE);
        }
    }

    public static void handleCommand(String function) {
        handleCommand(function,null);
    }

    public static void handleCommand(String function, Intent intentIn) {
        Intent intent = new Intent(ACTION_WATCH_COMMUNICATION_RECEIVER);
        int value;
        switch (function) {
            case CMD_LOCAL_XDRIP_APP_GOT_RESPONSE:
                Helper.cancelAlarm(HuamiXdrip.getAppContext(), xdripResponseIntend);
                break;
            case CMD_UPDATE_BG_FORCE:
                if (!Helper.ratelimit("miband-bg_force-limit", 5)) {
                    return;
                }
                xdripResponseIntend = WakeLockTrampoline.getPendingIntent(MiBandService.class, Constants.MIBAND_SERVICE_XDRIP_NO_RESPONCE_ID, CMD_LOCAL_XDRIP_APP_NO_RESPONSE);
                Helper.wakeUpIntent(HuamiXdrip.getAppContext(), XDRIP_APP_RESPONCE_DELAY, xdripResponseIntend);
                Settings settings = getSettings();
                intent.putExtra(INTENT_SETTINGS, settings);
                break;
            case CMD_SNOOZE_ALERT:
                intent.putExtra(INTENT_ALERT_TYPE, intentIn.getStringExtra(INTENT_ALERT_TYPE));
                break;
            case CMD_ADD_STEPS:
            case CMD_ADD_HR:
                value = intentIn.getIntExtra("value", 0);
                intent.putExtra("timeStamp", Helper.tsl());
                intent.putExtra("value", value);
                break;
            case CMD_STAT_INFO:
                intent.putExtra(INTENT_STAT_HOURS, intentIn.getIntExtra(INTENT_STAT_HOURS, 24));
                break;
            case CMD_ADD_TREATMENT:
                intent.putExtra("timeStamp", Helper.tsl());
                intent.putExtras(intentIn);
                break;
            default:
                return;
        }
        sendBroadcast(function, intent);
    }

    private static Settings getSettings() {
        Settings settings = new Settings();
        settings.setGraphStart(Constants.HOUR_IN_MS * MiBandEntry.getGraphHours());
        settings.setGraphEnd(Constants.MINUTE_IN_MS * 30);
        settings.setApkName(HuamiXdrip.getAppContext().getString(R.string.app_name));
        settings.setDisplayGraph(true);
        return settings;
    }

    private static void sendBroadcast(String functionName, Intent intent) {
        intent.putExtra(INTENT_FUNCTION_KEY, functionName);
        intent.putExtra(INTENT_PACKAGE_KEY, BuildConfig.APPLICATION_ID);
        UserError.Log.d(TAG, String.format("sendBroadcast functionName:%s", functionName));
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        HuamiXdrip.getAppContext().sendBroadcast(intent);
    }
}
