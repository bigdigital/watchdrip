package com.thatguysservice.huami_xdrip.services;

import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG_FORCE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.INTENT_FUNCTION_KEY;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.services.broadcastservice.models.Settings;
import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.UtilityModels.ForegroundServiceStarter;
import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.utils.framework.WakeLockTrampoline;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandService;

import test.invoke.sdk.XiaomiWatchHelper;

public class XiaomiWearService extends Service {

    protected String TAG = this.getClass().getSimpleName();
    private ForegroundServiceStarter foregroundServiceStarter;
    private XiaomiWatchHelper xiaomiWatchHelper;
    private long lastTime;

    public static boolean shouldServiceRun() {
        return MiBandEntry.isXiaomiServiceEnabled();
    }

    public static void bgForce(String jsonString) {
        if (shouldServiceRun()) {
            Helper.startService(XiaomiWearService.class, INTENT_FUNCTION_KEY, CMD_UPDATE_BG_FORCE, "json", jsonString);
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
        int value;
        switch (function) {
            case CMD_UPDATE_BG_FORCE:
                String json = intentIn.getStringExtra("json");
                updateWearBg(json);
                if (!Helper.ratelimit("miband-bg_force-limit", 5)) {
                    return;
                }
                break;

            default:
                return;
        }
    }


    private void updateWearBg(String jsonString) {
        xiaomiWatchHelper.launchApp("com.application.watch.watchdrip", obj -> {
            if (obj.isSuccess()) {
                UserError.Log.e(TAG, "Init message send");
                Helper.threadSleep(1000);
                xiaomiWatchHelper.sendMessageToWear(jsonString, obj2 -> {
                    if (obj2.isSuccess()) {
                        UserError.Log.e(TAG, "send -> " + obj.isSuccess());
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
        UserError.Log.e(TAG, "starting service");

        startInForeground();

        xiaomiWatchHelper = XiaomiWatchHelper.getInstance(this);

        xiaomiWatchHelper.setReceiver((id, message) -> {
            try {
                lastTime = Long.parseLong(message.toString());
            } catch (Exception e) {
            }
        });

        xiaomiWatchHelper.registerMessageReceiver();

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        UserError.Log.e(TAG, "killing service");
        foregroundServiceStarter.stop();
        xiaomiWatchHelper.unRegisterWatchHelper();
        super.onDestroy();
    }
}
