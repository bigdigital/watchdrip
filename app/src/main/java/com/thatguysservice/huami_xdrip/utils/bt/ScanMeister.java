package com.thatguysservice.huami_xdrip.utils.bt;

import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.PowerManager;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;
import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.UtilityModels.RxBleProvider;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.HandleBleScanException;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.UserError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import lombok.NoArgsConstructor;

import static com.thatguysservice.huami_xdrip.models.Helper.ratelimit;


// jamorham

// Stand alone bluetooth scanner using BtCallBack

// TODO report missing location services via toast????

@NoArgsConstructor
public class ScanMeister {

    private static final String TAG = ScanMeister.class.getSimpleName();
    protected static final String STOP_SCAN_TASK_ID = "stop_meister_scan";
    private static final int DEFAULT_SCAN_SECONDS = 30;
    protected static final int MINIMUM_RSSI = -1000; // -97; // ignore all quieter than this
    // TODO can this be static if we want to support multiple scanners?????
    protected static volatile Subscription scanSubscription;
    protected final RxBleClient rxBleClient = RxBleProvider.getSingleton();
    private final ConcurrentHashMap<String, BtCallBack> callbacks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BtCallBack2> callbacks2 = new ConcurrentHashMap<>();
    private final PowerManager.WakeLock wl = Helper.getWakeLock("jam-bluetooth-meister", 1000);
    protected int scanSeconds = DEFAULT_SCAN_SECONDS;
    protected volatile boolean stopOnFirstMatch = true;
    protected volatile String address;
    protected volatile List<String> name;
    protected volatile ScanFilter customFilter;
    protected boolean wideSearch = false;
    protected boolean legacyNoFilterWorkaround = false;
    private static String lastFailureReason = "";

    public static final String SCAN_TIMEOUT_CALLBACK = "SCAN_TIMEOUT";
    public static final String SCAN_FAILED_CALLBACK = "SCAN_FAILED";
    public static final String SCAN_FOUND_CALLBACK = "SCAN_FOUND";


    // TODO Log errors when location disabled etc

    public ScanMeister(String address) {
        this.address = address;
    }

    public ScanMeister setScanSeconds(int seconds) {
        this.scanSeconds = seconds;
        return this;
    }

    {
        RxJavaPlugins.setErrorHandler(e -> UserError.Log.d(TAG, "RxJavaError: " + e.getMessage()));
    }

    public ScanMeister setAddress(String address) {
        this.address = address;
        return this;
    }

    public ScanMeister setName(String name) {
        if (this.name == null) {
            this.name = new ArrayList<>();
        }
        this.name.add(name);
        return this;
    }

    public ScanMeister setFilter(final ScanFilter filter) {
        this.customFilter = filter;
        return this;
    }

    public ScanMeister unlimitedMatches() {
        this.stopOnFirstMatch = false;
        return this;
    }

    public ScanMeister allowWide() {
        this.wideSearch = true;
        return this;
    }

    public ScanMeister legacyNoFilterWorkaround() {
        this.legacyNoFilterWorkaround = true;
        return this;
    }

    // Callback boiler plate v1 callbacks
    public ScanMeister addCallBack(BtCallBack callback, String name) {
        callbacks.put(name, callback);
        return this;
    }

    // Callback boiler plate v2 callbacks
    public ScanMeister addCallBack2(BtCallBack2 callback, String name) {
        callbacks2.put(name, callback);
        return this;
    }

    public void removeCallBack(String name) {
        callbacks.remove(name);
    }

    protected synchronized void processCallBacks(String address, String status) {
        processCallBacks(address, status, null, null);
    }

    protected synchronized void processCallBacks(String address, String status, String name, Bundle bundle) {
        if (address == null) address = "NULL";
        boolean called_back = false;
        UserError.Log.d(TAG, "Processing callbacks for " + address + " " + status);
        for (Map.Entry<String, BtCallBack> entry : callbacks.entrySet()) {
            UserError.Log.d(TAG, "Callback: " + entry.getKey());
            entry.getValue().btCallback(address, status);
            called_back = true;
        }
        for (Map.Entry<String, BtCallBack2> entry : callbacks2.entrySet()) {
            UserError.Log.d(TAG, "Callback2: " + entry.getKey());
            entry.getValue().btCallback2(address, status, name, null);
            called_back = true;
        }
        if (!called_back) {
            UserError.Log.d(TAG, "No callbacks registered!!");
        }
    }

    ///

    public synchronized void scan() {
        extendWakeLock((scanSeconds + 1) * Constants.SECOND_IN_MS);
        stopScan("Scan start");
        UserError.Log.d(TAG, "startScan called: hunting: " + address + " " + name);

        ScanFilter filter = this.customFilter;
        if (filter == null) {
            final ScanFilter.Builder builder = new ScanFilter.Builder();
            if (address != null) {
                try {
                    builder.setDeviceAddress(address);
                } catch (IllegalArgumentException e) {
                    UserError.Log.wtf(TAG, "Invalid bluetooth address: " + address);
                }
            }
            // TODO scanning by name doesn't build a filter
            filter = builder.build();
        } else {
            UserError.Log.d(TAG,"Overriding with custom filter");
        }


        scanSubscription = new Subscription(rxBleClient.scanBleDevices(
                new ScanSettings.Builder()
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build(), legacyNoFilterWorkaround ? ScanFilter.empty() : filter)
                .timeout(scanSeconds, TimeUnit.SECONDS) // is unreliable
                .subscribeOn(Schedulers.io())
                .subscribe(this::onScanResult, this::onScanFailure));

        Inevitable.task(STOP_SCAN_TASK_ID, scanSeconds * Constants.SECOND_IN_MS, this::stopScanWithTimeoutCallback);
    }

    public void stop() {
        stopScan("Scan stop");
    }

    protected void stopScanWithTimeoutCallback() {
        stopScan("Stop with Timeout");
        processCallBacks(address, SCAN_TIMEOUT_CALLBACK);
    }

    protected synchronized void stopScan(String source) {
        UserError.Log.d(TAG, "stopScan called from: " + source);
        if (scanSubscription != null) {
            scanSubscription.unsubscribe();
            UserError.Log.d(TAG, "stopScan stopped scan");
            scanSubscription = null;
            Inevitable.kill(STOP_SCAN_TASK_ID);
        }
    }


    // Successful result from our bluetooth scan
    protected synchronized void onScanResult(ScanResult bleScanResult) {

        if (!wideSearch && address == null && name == null) {
            UserError.Log.d(TAG, "Address has been set to null, stopping scan.");
            stopScan("Address nulled");
            return;
        }

        try {
            for (ParcelUuid p : bleScanResult.getScanRecord().getServiceUuids()) {
                UserError.Log.d(TAG,"SERVICE: "+p.getUuid());
            }

        } catch (Exception e) {
            //
        }
        final int rssi = bleScanResult.getRssi();
        if (rssi > MINIMUM_RSSI) {
            //final String this_name = bleScanResult.getBleDevice().getName();
            final String this_address = bleScanResult.getBleDevice().getMacAddress();
            String this_name = "";
            if (name != null || customFilter != null) {
                this_name = bleScanResult.getBleDevice().getName();
            }
            final boolean matches = (customFilter != null)
                    || ((address != null && address.equalsIgnoreCase(this_address))
                    || (name != null && this_name != null && name.contains(this_name)));
            if (matches || Helper.quietratelimit("scanmeister-show-result", 2)) {
                UserError.Log.d(TAG, "Found a device: " + this_address + " " + this_name + " rssi: " + rssi + "  " + (matches ? "-> MATCH" : ""));
            }
            if (matches && stopOnFirstMatch) {
                stopScan("Got match");
                Helper.threadSleep(500);
                processCallBacks(this_address, SCAN_FOUND_CALLBACK, this_name, null);
                releaseWakeLock();
            }
            if (matches && !stopOnFirstMatch) {
                // TODO deposit good information in bundle - TODO: dry
                processCallBacks(this_address, SCAN_FOUND_CALLBACK, this_name, null);
            }

        } else {
            if (Helper.quietratelimit("log-low-rssi", 2)) {
                UserError.Log.d(TAG, "Low rssi device: " + bleScanResult.getBleDevice().getMacAddress() + " rssi: " + rssi);
            }
        }
    }


    // Failed result from our bluetooth scan
    protected synchronized void onScanFailure(Throwable throwable) {
        UserError.Log.d(TAG, "onScanFailure: " + throwable);
        if (throwable instanceof BleScanException) {
            final String info = HandleBleScanException.handle(TAG, (BleScanException) throwable);
            UserError.Log.d(TAG, "Scan failure: " + info);
            if (!lastFailureReason.equals(info) || ratelimit("scanmeister-fail-error", 600)) {
                UserError.Log.e(TAG, "Failed to scan: " + info);
                lastFailureReason = info;
            }
            if (((BleScanException) throwable).getReason() == BleScanException.BLUETOOTH_DISABLED) {
                // Attempt to turn bluetooth on
                if (ratelimit("bluetooth_toggle_on", 30)) {
                    UserError.Log.d(TAG, "Pause before Turn Bluetooth on");
                    Helper.threadSleep(2000);
                    UserError.Log.e(TAG, "Trying to Turn Bluetooth on");
                    Helper.setBluetoothEnabled(HuamiXdrip.getAppContext(), true);
                }
            }
            processCallBacks(address, SCAN_FAILED_CALLBACK);
        } else if (throwable instanceof TimeoutException) {
            // note this code path not always reached - see inevitable task
            processCallBacks(address, SCAN_TIMEOUT_CALLBACK);
        }

        stopScan("Scan failure");
        releaseWakeLock();
    }


    protected synchronized void extendWakeLock(long ms) {
        Helper.releaseWakeLock(wl); // lets not get too messy
        wl.acquire(ms);
    }

    protected void releaseWakeLock() {
        Helper.releaseWakeLock(wl);
    }

}
