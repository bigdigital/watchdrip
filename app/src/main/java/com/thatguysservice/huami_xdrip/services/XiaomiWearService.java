package com.thatguysservice.huami_xdrip.services;

import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG_FORCE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.INTENT_FUNCTION_KEY;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.services.broadcastservice.models.Settings;
import com.google.gson.Gson;
import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.UtilityModels.ForegroundServiceStarter;
import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.utils.framework.WakeLockTrampoline;
import com.thatguysservice.huami_xdrip.watch.miband.MiBand;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandService;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import test.invoke.sdk.XiaomiWatchHelper;

public class XiaomiWearService extends Service {

    protected String TAG = this.getClass().getSimpleName();
    private ForegroundServiceStarter foregroundServiceStarter;
    private XiaomiWatchHelper xiaomiWatchHelper;
    private long lastTime;
    private PendingIntent serviceIntent;

    private static final int SEND_DELAY = (int) (Constants.SECOND_IN_MS * 5);
    private String json;

    public static boolean shouldServiceRun() {
        return MiBandEntry.isXiaomiServiceEnabled();
    }

    public static void bgForce(String jsonString) {
        if (shouldServiceRun()) {
            Helper.startService(XiaomiWearService.class, INTENT_FUNCTION_KEY, CMD_UPDATE_BG, "json", jsonString);
        }
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
        switch (function) {
            case CMD_UPDATE_BG_FORCE:
                UserError.Log.d(TAG, "retry send last data");
                updateWearBg(json);
                break;

            case CMD_UPDATE_BG:
                json = intentIn.getStringExtra("json");
                cancelRetryTimer();
                setRetryTimer();
                updateWearBg(json);
                break;

            default:
                return;
        }
    }


    protected void setRetryTimer() {
        if (shouldServiceRun()) {
            serviceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.XIAOMI_SERVICE_RETRY_ID, CMD_UPDATE_BG_FORCE);
            Helper.wakeUpIntent(HuamiXdrip.getAppContext(), SEND_DELAY, serviceIntent);
        }
    }

    private void cancelRetryTimer() {
        Helper.cancelAlarm(HuamiXdrip.getAppContext(), serviceIntent);
    }

    private void updateWearBg(String jsonString) {
        if (!Helper.ratelimit("xiaomi-bg_force-limit", 2)) {
            return;
        }
        UserError.Log.d(TAG, "updateWearBg");
        xiaomiWatchHelper.launchApp("com.application.watch.watchdrip", obj -> {
            UserError.Log.d(TAG, "launchApp code: " + obj.getCode());
            if (obj.isSuccess()) {
                UserError.Log.d(TAG, "Init message send");
                Helper.threadSleep(1000);
                xiaomiWatchHelper.sendMessageToWear(jsonString, obj2 -> {
                    UserError.Log.d(TAG, "sendMessageToWear code: " + obj2.getCode());
                    if (obj2.isSuccess()) {
                        UserError.Log.d(TAG, "send -> " + obj.isSuccess());
                        cancelRetryTimer();
                    }
                });
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    protected void startInForeground() {
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), this);
        foregroundServiceStarter.start();
    }

    @Override
    public void onCreate() {
        UserError.Log.d(TAG, "starting service");

        startInForeground();

        xiaomiWatchHelper = XiaomiWatchHelper.getInstance(this);

        xiaomiWatchHelper.setReceiver((id, message) -> {
            try {
                Gson gson = new Gson();
                Map<String, String> map = gson.fromJson(new String(message, StandardCharsets.UTF_8), Map.class);
                String dataValue = map.get("data");
                lastTime = Long.parseLong(dataValue);
                UserError.Log.d(TAG, "got data: " + lastTime);
            } catch (Exception e) {
                UserError.Log.e(TAG, "parse error: " + e.getMessage());
            }
        });

        xiaomiWatchHelper.registerMessageReceiver();
        xiaomiWatchHelper.sendUpdateMessageToWear();

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        UserError.Log.d(TAG, "killing service");
        foregroundServiceStarter.stop();
        cancelRetryTimer();
        xiaomiWatchHelper.unRegisterWatchHelper();
        super.onDestroy();
    }
}
