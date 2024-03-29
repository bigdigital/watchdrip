package com.thatguysservice.huami_xdrip.services;

import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleCustomOperation;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;
import com.thatguysservice.huami_xdrip.UtilityModels.ForegroundServiceStarter;
import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.models.HandleBleScanException;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.Pref;
import com.thatguysservice.huami_xdrip.models.database.UserError;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;


// jamorham base class for reactive bluetooth services

public abstract class BaseBluetoothService extends Service {

    private final PowerManager.WakeLock wl = Helper.getWakeLock("jam-bluetooth-generic", 1000);
    protected String TAG = this.getClass().getSimpleName();
    private volatile boolean background_launch_waiting = false;
    protected static final long TOLERABLE_JITTER = 10000;

    protected ForegroundServiceStarter foregroundServiceStarter;
    protected Service service;

    protected String handleBleScanException(BleScanException bleScanException) {
        return HandleBleScanException.handle(TAG, bleScanException);
    }


    {
        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                if (!e.getCause().toString().contains("OperationSuccess")) {
                    UserError.Log.e(TAG, "RxJavaError: " + e.getMessage());
                }
            } else {
                UserError.Log.wtf(TAG, "RxJavaError2:"  + e.getMessage());
            }
        });
    }

    public synchronized void background_automata(final int timeout) {
        if (background_launch_waiting) {
            UserError.Log.d(TAG, "Blocked by existing background automata pending");
            return;
        }
        final PowerManager.WakeLock wl = Helper.getWakeLock(TAG + "-background", timeout + 1000);
        background_launch_waiting = true;
        new Thread(() -> {
            Helper.threadSleep(timeout);
            background_launch_waiting = false;
            automata();
            Helper.releaseWakeLock(wl);
        }).start();
    }

    protected synchronized boolean automata() {
        throw new RuntimeException("automata stub - not implemented!");
    }

    protected synchronized void extendWakeLock(long ms) {
        Helper.releaseWakeLock(wl); // lets not get too messy
        wl.acquire(ms);
    }

    protected synchronized void releaseWakeLock() {
        Helper.releaseWakeLock(wl);
    }


    protected class OperationSuccess extends RuntimeException {
        public OperationSuccess(String message) {
            super(message);
            UserError.Log.d(TAG, "Operation Success: " + message);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void tryGattRefresh(RxBleConnection connection) {
        if (connection == null) return;
        if (Helper.ratelimit("gatt-refresh", 60)) {
            if (Pref.getBoolean("use_gatt_refresh", true)) {
                try {
                    if (connection != null)
                        UserError.Log.d(TAG, "Trying gatt refresh queue");
                    connection.queue((new GattRefreshOperation(0))).timeout(2, TimeUnit.SECONDS).subscribe(
                            readValue -> {
                                UserError.Log.d(TAG, "Refresh OK: " + readValue);
                            }, throwable -> {
                                UserError.Log.d(TAG, "Refresh exception: " + throwable);
                            });
                } catch (NullPointerException e) {
                    UserError.Log.d(TAG, "Probably harmless gatt refresh exception: " + e);
                } catch (Exception e) {
                    UserError.Log.d(TAG, "Got exception trying gatt refresh: " + e);
                }
            } else {
                UserError.Log.d(TAG, "Gatt refresh rate limited");
            }
        }
    }

    protected static class GattRefreshOperation implements RxBleCustomOperation<Void> {
        private long delay_ms = 500;

        GattRefreshOperation() {
        }

        public GattRefreshOperation(long delay_ms) {
            this.delay_ms = delay_ms;
        }

        @Override
        public Observable<Void> asObservable(BluetoothGatt bluetoothGatt,
                                             RxBleGattCallback rxBleGattCallback,
                                             Scheduler scheduler) throws Throwable {

            return Observable.fromCallable(() -> refreshDeviceCache(bluetoothGatt))
                    .delay(delay_ms, TimeUnit.MILLISECONDS, Schedulers.computation())
                    .subscribeOn(scheduler);
        }

        private Void refreshDeviceCache(final BluetoothGatt gatt) {
            UserError.Log.d("BaseBluetooth", "Gatt Refresh " + (Helper.refreshDeviceCache("BaseBluetooth", gatt) ? "succeeded" : "failed"));
            return null;
        }
    }

    protected static byte[] nn(final byte[] array) {
        if (array == null) {
            if (Helper.ratelimit("never-null", 60)) {
                UserError.Log.wtf("NeverNull", "Attempt to pass null!!! " + Helper.backTrace());
                return new byte[1];
            }
        }
        return array;
    }

    protected void startInForeground() {
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), service);
        foregroundServiceStarter.start();
        foregroundStatus();
    }

    protected void foregroundStatus() {
        Inevitable.task("jam-base-foreground-status", 2000, () -> UserError.Log.d("FOREGROUND", service.getClass().getSimpleName() + (Helper.isServiceRunningInForeground(service.getClass()) ? " is running in foreground" : " is not running in foreground")));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        service = this;
        UserError.Log.d("FOREGROUND", "Current Service: " + service.getClass().getSimpleName());
        startInForeground();
    }

}
