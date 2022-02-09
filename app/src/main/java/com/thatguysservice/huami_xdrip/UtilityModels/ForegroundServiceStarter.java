package com.thatguysservice.huami_xdrip.UtilityModels;

import android.app.Service;
import android.content.Context;
import android.os.Build;

import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.R;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.UserError;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST;
import static com.thatguysservice.huami_xdrip.UtilityModels.Notifications.ongoingNotificationId;

public class ForegroundServiceStarter {
    private static final String TAG = "FOREGROUND";

    final private Service mService;
    final private Context mContext;
    final private boolean run_service_in_foreground;
    //final private Handler mHandler;


    public ForegroundServiceStarter(Context context, Service service) {
        mContext = context;
        mService = service;
        //mHandler = new Handler(Looper.getMainLooper());

        run_service_in_foreground = shouldRunCollectorInForeground();
    }

    public static boolean shouldRunCollectorInForeground() {
        // Force foreground with Oreo and above
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
    }

    public void start() {
        if (mService == null) {
            UserError.Log.e(TAG, "SERVICE IS NULL - CANNOT START!");
            return;
        }
        if (run_service_in_foreground) {
            UserError.Log.d(TAG, "should be moving to foreground");
            foregroundStatus();
            UserError.Log.d(TAG, "CALLING START FOREGROUND: " + mService.getClass().getSimpleName());
                /*
                  When is a foreground service not a foreground service?
                  When it's started from the background of course!
                  On android 11, even though the user explicitly grants us permission to use
                  background location, we still have to request to use it on a foreground
                  service, but only when it isn't re-started with the app open.
                  Even then the restrictions seem to be applied inconsistently!
                 */
            try {
                Notifications.createNotificationChannels(mContext);
                mService.startForeground(ongoingNotificationId, Notifications.createNotification(HuamiXdrip.getAppContext().getString(R.string.huami_xdrip_running), mContext));
            } catch (IllegalArgumentException e) {
                UserError.Log.e(TAG, "Got exception trying to use Android 10+ service starting for " + mService.getClass().getSimpleName() + " " + e);
            }

            //     }
            // });
        }
    }

    public void stop() {
        if (run_service_in_foreground) {
            UserError.Log.d(TAG, "should be moving out of foreground");
            mService.stopForeground(true);
        }
    }

    protected void foregroundStatus() {
        Inevitable.task("foreground-status", 2000, () -> UserError.Log.d("XFOREGROUND", mService.getClass().getSimpleName() + (Helper.isServiceRunningInForeground(mService.getClass()) ? " is running in foreground" : " is not running in foreground")));
    }


}
