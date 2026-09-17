package com.thatguysservice.huami_xdrip.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.thatguysservice.huami_xdrip.UtilityModels.BgGraphCompontens;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.aaps.AapsGraphCache;
import com.thatguysservice.huami_xdrip.models.aaps.AapsStatusLineCache;
import com.thatguysservice.huami_xdrip.models.aaps.AapsTreatmentCache;
import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

import org.json.JSONObject;

import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG_FORCE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.INTENT_BG_SOURCE_AAPS;

/**
 * Consumes AndroidAPS's "info.nightscout.androidaps.status" broadcast (sent by
 * its Tizen sync plugin)
 */
public class AapsStatusReceiver extends BroadcastReceiver {
    private static final String TAG = AapsStatusReceiver.class.getSimpleName();
    private static final long STALE_THRESHOLD_MS = 15 * 60 * 1000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Bundle extras = intent.getExtras();
            if (extras == null || !extras.containsKey("glucoseMgdl")) {
                return;
            }
            double valueMgdl = extras.getDouble("glucoseMgdl", -1000);
            if (valueMgdl <= 0) {
                return;
            }
            long timeStamp = extras.getLong("glucoseTimeStamp", -1);
            double deltaMgdl = extras.getDouble("deltaMgdl", 0);
            String slopeArrow = extras.getString("slopeArrow");
            double high = extras.getDouble("high", Double.MAX_VALUE);
            double low = extras.getDouble("low", -Double.MAX_VALUE);
            boolean doMgdl = !"mmol".equalsIgnoreCase(extras.getString("units", "mg/dl"));
            // AAPS reports high/low in whatever unit it's currently displaying
            // (see Preferences.get(UnitDoubleKey), which calls
            // valueInCurrentUnitsDetect()) - unlike glucoseMgdl, which is
            // always mg/dl. Normalize so every mg/dl comparison below is
            // apples-to-apples.
            if (!doMgdl) {
                if (extras.containsKey("high")) {
                    high *= Constants.MMOLL_TO_MGDL;
                }
                if (extras.containsKey("low")) {
                    low *= Constants.MMOLL_TO_MGDL;
                }
            }

            Bundle bgBundle = new Bundle();
            bgBundle.putDouble("bg.valueMgdl", valueMgdl);
            bgBundle.putDouble("bg.deltaValueMgdl", deltaMgdl);
            bgBundle.putLong("bg.timeStamp", timeStamp);
            bgBundle.putBoolean("bg.isStale", timeStamp > 0 && (System.currentTimeMillis() - timeStamp) > STALE_THRESHOLD_MS);
            bgBundle.putBoolean("doMgdl", doMgdl);
            bgBundle.putString("bg.deltaName", slopeArrow);
            bgBundle.putBoolean("bg.isHigh", valueMgdl >= high);
            bgBundle.putBoolean("bg.isLow", valueMgdl <= low);
            bgBundle.putBoolean(INTENT_BG_SOURCE_AAPS, true);

            if (extras.containsKey("pumpReservoir") || extras.containsKey("iob") || extras.containsKey("pumpBattery")) {
                JSONObject pumpJson = new JSONObject();
                try {
                    pumpJson.put("reservoir", extras.getDouble("pumpReservoir", -1));
                    pumpJson.put("bolusiob", extras.getDouble("iob", -1)); // total IOB (bolus+basal)
                    pumpJson.put("battery", extras.getInt("pumpBattery", -1));
                } catch (Exception ignored) {
                }
                bgBundle.putString("pumpJSON", pumpJson.toString());
            }

            if (timeStamp > 0) {
                AapsGraphCache.addReading(timeStamp, valueMgdl);
            }

            if (!AapsGraphCache.isEmpty()) {
                bgBundle.putInt("fuzzer", BgGraphCompontens.FUZZER);
                bgBundle.putLong("start", AapsGraphCache.oldestTimestamp());
                bgBundle.putLong("end", AapsGraphCache.newestTimestamp());
                bgBundle.putParcelable("graph.inRange", AapsGraphCache.buildInRangeLine(low, high, doMgdl));
                bgBundle.putParcelable("graph.low", AapsGraphCache.buildLowLine(low, doMgdl));
                bgBundle.putParcelable("graph.high", AapsGraphCache.buildHighLine(high, doMgdl));
                bgBundle.putParcelable("graph.lowLine", AapsGraphCache.buildLowThresholdLine(low, doMgdl));
                bgBundle.putParcelable("graph.highLine", AapsGraphCache.buildHighThresholdLine(high, doMgdl));
            }

            String statusLine = AapsStatusLineCache.getStatusLine();
            if (statusLine != null) {
                bgBundle.putString("external.statusLine", statusLine);
                bgBundle.putLong("external.timeStamp", AapsStatusLineCache.getTimestamp());
            }

            // Note: intentionally not setting "predict.IOB"/"predict.BWP" here -
            // those are xDrip+'s own computed predictions, not something AAPS
            // broadcasts directly, and DisplayData/WebServiceTreatment already
            // handle them being absent gracefully.
            if (!AapsTreatmentCache.isEmpty()) {
                bgBundle.putDouble("treatment.insulin", AapsTreatmentCache.getInsulin());
                bgBundle.putDouble("treatment.carbs", AapsTreatmentCache.getCarbs());
                bgBundle.putLong("treatment.timeStamp", AapsTreatmentCache.getTimestamp());
                bgBundle.putParcelable("graph.treatment", AapsTreatmentCache.buildTreatmentLine(low, high, doMgdl));
            }

            UserError.Log.d(TAG, "Received AAPS status broadcast, BG: " + valueMgdl);
            MiBandEntry.sendToService(CMD_UPDATE_BG_FORCE, bgBundle);
        } catch (Exception e) {
            UserError.Log.e(TAG, "onReceive Error: " + e);
        }
    }
}
