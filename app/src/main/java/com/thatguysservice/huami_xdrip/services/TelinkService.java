package com.thatguysservice.huami_xdrip.services;

import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG_FORCE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.INTENT_FUNCTION_KEY;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.R;
import com.thatguysservice.huami_xdrip.UtilityModels.ForegroundServiceStarter;
import com.thatguysservice.huami_xdrip.models.BgData;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.services.telink.PvvxDisplayFrame;
import com.thatguysservice.huami_xdrip.services.telink.TelinkGattWriter;
import com.thatguysservice.huami_xdrip.services.telink.TelinkThermometers;
import com.thatguysservice.huami_xdrip.utils.framework.WakeLockTrampoline;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shows glucose on Telink based BLE thermometers pvvx ATC_MiThermometer custom firmware
 */
public class TelinkService extends Service {

    private static final String CMD_STALE_CHECK = "telink_stale_check";
    public static final String CMD_ALARM_BEEP_CHANGED = "telink_alarm_beep_changed";
    public static final String EXTRA_ALARM_BEEP = "beep";
    public static final String CMD_SNOOZE = "telink_snooze";
    public static final String EXTRA_SNOOZE_ON = "on";

    private static final int RETRY_DELAY = (int) (Constants.SECOND_IN_MS * 30);
    private static final int MAX_RETRIES = 3;
    private static final long STALE_AFTER = Constants.SECOND_IN_MS * PvvxDisplayFrame.AGE_STALE_SEC;
    private static final long STALE_REFRESH = Constants.MINUTE_IN_MS * 5;
    private static final long STALE_GIVE_UP = Constants.MINUTE_IN_MS * 60;

    protected String TAG = this.getClass().getSimpleName();

    private ForegroundServiceStarter foregroundServiceStarter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TelinkGattWriter writer;
    private PendingIntent retryIntent;
    private PendingIntent staleIntent;

    // Touched from the main thread only
    private PvvxDisplayFrame pendingFrame;
    private List<String> pendingMacs = new ArrayList<>();
    private int retries;
    private long lastReadingTime;
    private PvvxDisplayFrame lastGlucoseFrame;
    private String lastSentKey = "";

    public static boolean shouldServiceRun() {
        return MiBandEntry.isTelinkServiceEnabled();
    }

    public static void bgForce(BgData bgData, boolean force) {
        if (!shouldServiceRun() || bgData == null || bgData.isNoBgData()) {
            return;
        }
        Intent intent = new Intent(HuamiXdrip.getAppContext(), TelinkService.class);
        intent.putExtra(INTENT_FUNCTION_KEY, CMD_UPDATE_BG);
        intent.putExtra("valueMgdl", bgData.getValueMgdl());
        intent.putExtra("deltaMgdl", bgData.getDeltaMgdl());
        intent.putExtra("deltaName", bgData.getDeltaName());
        intent.putExtra("timeStamp", bgData.getTimeStamp());
        intent.putExtra("doMgdl", bgData.isDoMgdl());
        intent.putExtra("isHigh", bgData.isBgHigh());
        intent.putExtra("isLow", bgData.isBgLow());
        intent.putExtra("force", force);
        Helper.startService(intent);
    }

    public static List<String> getMacs() {
        return TelinkThermometers.load().macs();
    }

    // Valid, distinct addresses separated by commas, semicolons or spaces
    public static List<String> parseMacs(String text) {
        List<String> result = new ArrayList<>();
        for (String mac : (text == null ? "" : text).split("[,;\\s]+")) {
            mac = normalizeMac(mac);
            if (TelinkGattWriter.isValidMac(mac) && !result.contains(mac)) {
                result.add(mac);
            }
        }
        return result;
    }

    // "A4:C1:38:45:4E:F3", "A4-C1-38-45-4E-F3", "A4C138454EF3" or pvvx's "A4C138454EF3A6A8" (+ random MAC)
    public static String normalizeMac(String mac) {
        String hex = mac.replaceAll("[:\\-.]", "").trim().toUpperCase(Locale.US);
        if (!hex.matches("[0-9A-F]{12}([0-9A-F]{4})?")) {
            return mac.trim().toUpperCase(Locale.US);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i += 2) {
            if (i > 0) sb.append(':');
            sb.append(hex, i, i + 2);
        }
        return sb.toString();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = Helper.getWakeLock("Telink service", 60000);
        try {
            if (!shouldServiceRun()) {
                UserError.Log.d(TAG, "Telink Service is NOT set be active - shutting down");
                stopSelf();
                return START_NOT_STICKY;
            }
            if (intent != null) {
                final String function = intent.getStringExtra(INTENT_FUNCTION_KEY);
                if (function != null) {
                    handleCommand(function, intent);
                }
            }
            return START_STICKY;
        } finally {
            Helper.releaseWakeLock(wl);
        }
    }

    private void handleCommand(String function, Intent intentIn) {
        switch (function) {
            case CMD_UPDATE_BG:
                handleNewReading(intentIn);
                break;
            case CMD_UPDATE_BG_FORCE:
                // retry timer
                if (pendingFrame != null && !pendingMacs.isEmpty()) {
                    UserError.Log.d(TAG, "retry send last data TELINK, attempt " + retries);
                    send(pendingFrame, pendingMacs, null);
                }
                break;
            case CMD_SNOOZE:
                snooze(Boolean.parseBoolean(intentIn.getStringExtra(EXTRA_SNOOZE_ON)));
                break;
            case CMD_ALARM_BEEP_CHANGED:
                applyAlarmBeep(Boolean.parseBoolean(intentIn.getStringExtra(EXTRA_ALARM_BEEP)));
                break;
            case CMD_STALE_CHECK:
                handleStaleCheck();
                break;
            default:
                break;
        }
    }

    private void handleNewReading(Intent intentIn) {
        final List<String> macs = getMacs();
        if (macs.isEmpty()) {
            UserError.Log.e(TAG, "No valid thermometer MAC address configured");
            return;
        }
        final long timeStamp = intentIn.getLongExtra("timeStamp", 0);
        final String key = timeStamp + "|" + macs;
        if (key.equals(lastSentKey) && !intentIn.getBooleanExtra("force", false)) {
            return; // already shown, spare the thermometer battery
        }
        lastReadingTime = timeStamp;
        if (Helper.msSince(timeStamp) >= STALE_AFTER) {
            UserError.Log.d(TAG, "Ignoring stale reading");
            return;
        }

        PvvxDisplayFrame frame = PvvxDisplayFrame.glucose(
                intentIn.getDoubleExtra("valueMgdl", 0),
                intentIn.getDoubleExtra("deltaMgdl", 0),
                intentIn.getStringExtra("deltaName"),
                intentIn.getBooleanExtra("doMgdl", true),
                intentIn.getBooleanExtra("isHigh", false),
                intentIn.getBooleanExtra("isLow", false),
                timeStamp);
        frame = withAlarmState(frame, MiBandEntry.isTelinkAlarmBeepEnabled());

        lastGlucoseFrame = frame;
        retries = 0;
        send(frame, macs, key);
        scheduleStaleCheck(Math.max(Constants.SECOND_IN_MS, STALE_AFTER - Helper.msSince(timeStamp)));
    }

    private void handleStaleCheck() {
        final long age = Helper.msSince(lastReadingTime);
        if (age < STALE_AFTER) {
            return; // a newer reading arrived meanwhile and scheduled its own check
        }
        lastSentKey = "";
        retries = 0;
        if (lastGlucoseFrame != null && age < STALE_GIVE_UP) {
            final int minutes = (int) (age / Constants.MINUTE_IN_MS);
            UserError.Log.d(TAG, "Reading is " + minutes + " min old, showing stale marker");
            send(lastGlucoseFrame.asStale(minutes), getMacs(), null);
            scheduleStaleCheck(STALE_REFRESH);
        } else {
            // too old, or the reading was lost with a service restart
            UserError.Log.d(TAG, "No recent reading, switching thermometers back to their own display");
            lastGlucoseFrame = null;
            send(PvvxDisplayFrame.REVERT_TO_THERMOMETER, getMacs(), null);
        }
    }

    public static boolean isSnoozed() {
        return MiBandEntry.getTelinkSnoozeUntil() > Helper.tsl();
    }

    // Alarm bits per the beep option; while snoozed none, plus ALARM_SNOOZED for a key snooze
    private static PvvxDisplayFrame withAlarmState(PvvxDisplayFrame frame, boolean beep) {
        final boolean snoozed = beep && isSnoozed();
        return frame.withAlarmSignal(beep && !snoozed).withSnoozed(snoozed);
    }

    // Snooze for the configured time or cancel it, resending the last reading at once
    private void snooze(boolean on) {
        if (on) {
            final long until = Helper.tsl() + Constants.MINUTE_IN_MS * MiBandEntry.getTelinkSnoozeMinutes();
            MiBandEntry.setTelinkSnoozeUntil(until);
            UserError.Log.d(TAG, "Alarm snoozed until " + Helper.dateTimeText(until));
        } else {
            MiBandEntry.setTelinkSnoozeUntil(0);
            UserError.Log.d(TAG, "Alarm snooze cancelled");
        }
        if (lastGlucoseFrame != null) {
            lastGlucoseFrame = withAlarmState(lastGlucoseFrame, MiBandEntry.isTelinkAlarmBeepEnabled());
            retries = 0;
            send(lastGlucoseFrame, getMacs(), null);
        }
    }

    private void onSnoozeKey(String mac) {
        if (!isSnoozed()) {
            Helper.static_toast_long(getString(R.string.telink_snooze_key_pressed));
            snooze(true);
        }
    }

    // The alarm beep option changed: resend the last reading at once
    private void applyAlarmBeep(boolean beep) {
        if (lastGlucoseFrame == null) {
            return;
        }
        lastGlucoseFrame = withAlarmState(lastGlucoseFrame, beep);
        retries = 0;
        send(lastGlucoseFrame, getMacs(), null);
    }

    private void send(PvvxDisplayFrame frame, List<String> macs, String key) {
        cancelRetryTimer();
        pendingFrame = frame;
        pendingMacs = new ArrayList<>(macs);
        final List<String> targets = new ArrayList<>(macs);
        executor.execute(() -> {
            final PowerManager.WakeLock wl = Helper.getWakeLock("Telink write", 60000 * targets.size());
            final List<String> failed = new ArrayList<>();
            try {
                for (String mac : targets) {
                    if (!writer.write(mac, frame) && !retryAfterPause(mac, frame)) {
                        failed.add(mac);
                    }
                }
            } finally {
                Helper.releaseWakeLock(wl);
            }
            if (!failed.isEmpty()) UserError.Log.e(TAG, "Failed to update " + failed);
            Helper.runOnUiThread(() -> onSendFinished(frame, failed, key));
        });
    }

    // A connection right after the previous one tends to fail
    private boolean retryAfterPause(String mac, PvvxDisplayFrame frame) {
        Helper.threadSleep(500);
        return writer.write(mac, frame);
    }

    private void onSendFinished(PvvxDisplayFrame frame, List<String> failed, String key) {
        if (frame != pendingFrame) {
            // A newer frame has been queued meanwhile, it owns the retry state
            return;
        }
        if (failed.isEmpty()) {
            if (key != null) lastSentKey = key;
            pendingMacs.clear();
            return;
        }
        pendingMacs = failed;
        if (++retries <= MAX_RETRIES) {
            setRetryTimer();
        } else {
            UserError.Log.e(TAG, "Giving up on " + failed);
        }
    }

    private void setRetryTimer() {
        if (shouldServiceRun()) {
            retryIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.TELINK_SERVICE_RETRY_ID, CMD_UPDATE_BG_FORCE);
            Helper.wakeUpIntent(HuamiXdrip.getAppContext(), RETRY_DELAY, retryIntent);
        }
    }

    private void cancelRetryTimer() {
        Helper.cancelAlarm(HuamiXdrip.getAppContext(), retryIntent);
    }

    private void scheduleStaleCheck(long delay) {
        Helper.cancelAlarm(HuamiXdrip.getAppContext(), staleIntent);
        staleIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.TELINK_SERVICE_STALE_ID, CMD_STALE_CHECK);
        Helper.wakeUpIntent(HuamiXdrip.getAppContext(), delay, staleIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startInForeground() {
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), this);
        foregroundServiceStarter.start();
    }

    @Override
    public void onCreate() {
        UserError.Log.d(TAG, "starting service");
        startInForeground();
        writer = new TelinkGattWriter(this, mac -> Helper.runOnUiThread(() -> onSnoozeKey(mac)));
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        UserError.Log.d(TAG, "killing service");
        cancelRetryTimer();
        Helper.cancelAlarm(HuamiXdrip.getAppContext(), staleIntent);
        executor.shutdownNow();
        foregroundServiceStarter.stop();
        super.onDestroy();
    }
}
