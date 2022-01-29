package com.thatguysservice.huami_xdrip.utils.framework;

import android.os.Build;

import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.PersistentStore;
import com.thatguysservice.huami_xdrip.models.UserError;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.thatguysservice.huami_xdrip.models.Helper.buggy_samsung;


/**
 * jamorham
 *
 * Samsung have made modifications to the Android framework which breaks compatibility with the
 * published reference documentation. This diminishes the user experience and the required features
 * available to developers. Until they fix these bugs we attempt to work-around them...
 */

@RequiredArgsConstructor
public class SamsungFixes {

    // TODO this overlaps with ob1 implementation
    // TODO eventually individual implementations of this should consolidate here.
    private static final String BUGGY_SAMSUNG_ENABLED = "buggy-samsung-enabled";
    private static final long TOLERABLE_JITTER = 10000;

    private final String TAG;
    @Getter
    private long max_wakeup_jitter;

    // TODO change log.e to log.d for production

    public long evaluate(final long wakeup_time) {
        if (wakeup_time > 0) {
            final long wakeup_jitter = Helper.msSince(wakeup_time);
            //UserError.Log.e(TAG, "debug jitter: " + wakeup_jitter);
            if (wakeup_jitter < 0) {
                UserError.Log.e(TAG, "Woke up Early..");
            } else {
                if (wakeup_jitter > 1000) {
                    UserError.Log.d(TAG, "Wake up, time jitter: " + Helper.niceTimeScalar(wakeup_jitter));
                    if ((wakeup_jitter > TOLERABLE_JITTER) && (!buggy_samsung) && isSamsung()) {
                        UserError.Log.wtf(TAG, "Enabled Buggy Samsung workaround due to jitter of: " + Helper.niceTimeScalar(wakeup_jitter));
                        buggy_samsung = true;
                        PersistentStore.incrementLong(BUGGY_SAMSUNG_ENABLED);
                        max_wakeup_jitter = 0;
                    } else {
                        max_wakeup_jitter = Math.max(max_wakeup_jitter, wakeup_jitter);
                        checkWasBuggy();
                    }
                }
            }
            return wakeup_jitter;
        }
        return 0; // no wakeup time specified

    }

    // enable if we have historic markers showing previous enabling
    public void checkWasBuggy() {
        if (!buggy_samsung && isSamsung() && PersistentStore.getLong(BUGGY_SAMSUNG_ENABLED) > 4) {
            UserError.Log.e(TAG, "Enabling buggy samsung due to persistent metric");
            buggy_samsung = true;
        }
    }

    public static boolean isSamsung() {
        return Build.MANUFACTURER.toLowerCase().contains("samsung");
    }

}
