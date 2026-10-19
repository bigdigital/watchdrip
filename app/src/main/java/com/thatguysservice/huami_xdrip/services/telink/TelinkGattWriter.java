package com.thatguysservice.huami_xdrip.services.telink;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;

import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.Pref;
import com.thatguysservice.huami_xdrip.models.database.UserError;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Short GATT connection to a pvvx thermometer: connect, write one frame, disconnect.
 * Blocking, call it from a background thread. The device id (frame layout) is cached per MAC.
 */
public class TelinkGattWriter {
    private static final String TAG = "TelinkGattWriter";

    public static final UUID UUID_CHARACTERISTIC_CMD = UUID.fromString("00001f1f-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final byte CMD_ID_DEV_ID = 0x00;

    private static final int DEV_ID_SIZE = 12; // dev_id_t
    private static final int SERVICE_SCREEN = 0x20;

    private static final String PREF_HW_VERSION_PREFIX = "telink_hw_version_";
    private static final String PREF_SERVICES_PREFIX = "telink_services_";
    // 1: firmware fork with CMD_ID_EXTDATA_AGE, 0: stock, unset: unknown
    private static final String PREF_EXT_AGE_PREFIX = "telink_ext_age_";
    // Stock firmware is trusted until the app restarts, the thermometer may get reflashed
    private static final Set<String> stockThisRun = ConcurrentHashMap.newKeySet();

    private static final long CONNECT_TIMEOUT_SEC = 20;
    private static final long DISCOVER_TIMEOUT_SEC = 10;
    private static final long DESCRIPTOR_TIMEOUT_SEC = 3;
    private static final long RESPONSE_TIMEOUT_SEC = 3;
    private static final long WRITE_TIMEOUT_SEC = 3;
    private static final long DISCONNECT_TIMEOUT_SEC = 3;
    private static final long SETTLE_MS = 300; // the thermometer updates the link parameters after connecting

    private final Context context;
    private final Listener listener;

    public interface Listener {
        // The thermometer's key snoozed the alarm, called from the writer's thread
        void onSnoozeKey(String mac);
    }

    private volatile CountDownLatch connectedLatch;
    private volatile CountDownLatch discoveredLatch;
    private volatile CountDownLatch descriptorLatch;
    private volatile CountDownLatch responseLatch;
    private volatile CountDownLatch writtenLatch;
    private volatile CountDownLatch disconnectedLatch;
    private volatile boolean servicesOk;
    private volatile int expectedResponse = -1;
    private volatile byte[] response;
    private boolean notificationsOn;

    public TelinkGattWriter(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    // Same rule as BluetoothAdapter.checkBluetoothAddress(), without Android (unit tests)
    public static boolean isValidMac(String mac) {
        return mac != null && mac.matches("[0-9A-F]{2}(:[0-9A-F]{2}){5}");
    }

    private static String macKey(String prefix, String mac) {
        return prefix + mac.replace(":", "").toUpperCase(Locale.US);
    }

    /**
     * @return true when the frame was handed to the thermometer
     */
    @SuppressLint("MissingPermission")
    public synchronized boolean write(String mac, PvvxDisplayFrame frame) {
        final BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter adapter = manager != null ? manager.getAdapter() : null;
        if (adapter == null || !adapter.isEnabled()) {
            UserError.Log.e(TAG, "Bluetooth is off");
            return false;
        }
        if (!isValidMac(mac)) {
            UserError.Log.e(TAG, "Invalid MAC address: " + mac);
            return false;
        }
        if (!frame.isFreshReading() && Pref.getInt(macKey(PREF_EXT_AGE_PREFIX, mac), -1) == 1) {
            return true; // the firmware fork marks stale data itself
        }

        connectedLatch = new CountDownLatch(1);
        discoveredLatch = new CountDownLatch(1);
        disconnectedLatch = new CountDownLatch(1);
        servicesOk = false;
        notificationsOn = false;

        BluetoothGatt gatt = null;
        try {
            final BluetoothDevice device = adapter.getRemoteDevice(mac);
            UserError.Log.d(TAG, "Connecting to " + mac);
            gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE);
            if (gatt == null) {
                UserError.Log.e(TAG, "connectGatt returned null");
                return false;
            }
            if (!connectedLatch.await(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                UserError.Log.e(TAG, mac + " not reachable (connect timeout)");
                return false;
            }
            if (!gatt.discoverServices() || !discoveredLatch.await(DISCOVER_TIMEOUT_SEC, TimeUnit.SECONDS) || !servicesOk) {
                UserError.Log.e(TAG, mac + " service discovery failed");
                return false;
            }
            BluetoothGattCharacteristic characteristic = findCharacteristic(gatt);
            if (characteristic == null && refreshServices(gatt)) {
                // stale GATT cache (reflashed firmware) or an empty one right after a previous connection
                characteristic = findCharacteristic(gatt);
            }
            if (characteristic == null) {
                UserError.Log.e(TAG, mac + " has no 0x1F1F characteristic, is the pvvx firmware installed?");
                return false;
            }

            int hwVersion = Pref.getInt(macKey(PREF_HW_VERSION_PREFIX, mac), -1);
            long services = Pref.getLong(macKey(PREF_SERVICES_PREFIX, mac), -1);
            if (hwVersion < 0 || services < 0) {
                byte[] r = request(gatt, characteristic, new byte[]{CMD_ID_DEV_ID}, DEV_ID_SIZE);
                if (r == null) {
                    r = request(gatt, characteristic, new byte[]{CMD_ID_DEV_ID}, DEV_ID_SIZE);
                }
                if (r == null) {
                    UserError.Log.e(TAG, mac + " did not report its device id, assuming standard layout");
                } else {
                    hwVersion = u16(r, 2);
                    final int swVersion = u16(r, 4);
                    services = u16(r, 8) | ((long) u16(r, 10) << 16);
                    UserError.Log.d(TAG, String.format(Locale.US, "%s: hw_version %d, firmware %x.%x, services 0x%08X",
                            mac, hwVersion, swVersion >> 4, swVersion & 0x0F, services));
                    Pref.setInt(macKey(PREF_HW_VERSION_PREFIX, mac), hwVersion);
                    Pref.setLong(macKey(PREF_SERVICES_PREFIX, mac), services);
                }
            }
            if (services >= 0 && (services & SERVICE_SCREEN) == 0) {
                UserError.Log.e(TAG, mac + " has no screen, can't show glucose on it");
                return false;
            }

            final PvvxDisplayFrame.Layout layout = PvvxDisplayFrame.layoutFor(hwVersion);
            if (layout == PvvxDisplayFrame.Layout.STANDARD && frame.isFreshReading() && sendWithAge(gatt, characteristic, mac, frame)) {
                UserError.Log.d(TAG, "Frame with age sent to " + mac);
            } else {
                final byte[] bytes = frame.build(layout);
                if (!writeAndWait(gatt, characteristic, bytes)) {
                    UserError.Log.e(TAG, mac + " write rejected");
                    return false;
                }
                UserError.Log.d(TAG, "Frame " + Helper.bytesToHex(bytes) + " (" + layout + ") sent to " + mac);
            }

            Helper.threadSleep(200); // let the write without response go out before disconnecting
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (SecurityException e) {
            UserError.Log.e(TAG, "Missing Bluetooth permission: " + e.getMessage());
            return false;
        } catch (Exception e) {
            UserError.Log.e(TAG, "Write to " + mac + " failed: " + e);
            return false;
        } finally {
            close(gatt);
        }
    }

    /**
     * Sends the reading with CMD_ID_EXTDATA_AGE if the firmware may support it, the answer tells
     * every time (stock firmware replies "error").
     *
     * @return false when the standard frame has to be sent instead
     */
    private boolean sendWithAge(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, String mac, PvvxDisplayFrame frame) throws InterruptedException {
        final String key = macKey(PREF_EXT_AGE_PREFIX, mac);
        final int known = Pref.getInt(key, -1);
        if (known == 0 && stockThisRun.contains(mac)) {
            return false;
        }
        byte[] answer = request(gatt, characteristic, frame.buildWithAge(ageSec(frame)), 2);
        if (answer == null) {
            answer = request(gatt, characteristic, frame.buildWithAge(ageSec(frame)), 2);
        }
        if (answer == null) {
            UserError.Log.e(TAG, mac + " did not answer the data age command");
            return known == 1; // otherwise fall back, so the value still shows up
        }
        // [0x2E][status], stock firmware answers [0x2E][0xFF] "error"
        final boolean supported = (answer[1] & 0xFF) != 0xFF;
        if (supported && (answer[1] & PvvxDisplayFrame.STATUS_SNOOZE) != 0) {
            UserError.Log.d(TAG, mac + " key pressed to snooze the alarm");
            listener.onSnoozeKey(mac);
        }
        if (supported != (known == 1)) {
            UserError.Log.d(TAG, mac + (supported ? " shows the data age itself (firmware fork)" : " runs stock firmware, using the standard frame"));
            Pref.setInt(key, supported ? 1 : 0);
        }
        if (!supported) stockThisRun.add(mac);
        return supported;
    }

    // Age when sent (after connecting), so retries stay exact
    private static long ageSec(PvvxDisplayFrame frame) {
        return frame.getReadingTime() > 0 ? Helper.msSince(frame.getReadingTime()) / 1000 : 0;
    }

    // Sends a command and waits for the answer: a notification starting with the same command id
    @SuppressLint("MissingPermission")
    @SuppressWarnings("deprecation")
    private byte[] request(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] cmd, int minSize) throws InterruptedException {
        if (!notificationsOn) {
            gatt.setCharacteristicNotification(characteristic, true);
            final BluetoothGattDescriptor cccd = characteristic.getDescriptor(UUID_CCCD);
            if (cccd != null) {
                descriptorLatch = new CountDownLatch(1);
                final byte[] enable = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                boolean started;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    started = gatt.writeDescriptor(cccd, enable) == BluetoothGatt.GATT_SUCCESS;
                } else {
                    cccd.setValue(enable);
                    started = gatt.writeDescriptor(cccd);
                }
                if (started) {
                    descriptorLatch.await(DESCRIPTOR_TIMEOUT_SEC, TimeUnit.SECONDS);
                }
            }
            notificationsOn = true;
            Helper.threadSleep(SETTLE_MS); // a command sent meanwhile goes unanswered
        }
        response = null;
        responseLatch = new CountDownLatch(1);
        expectedResponse = cmd[0] & 0xFF;
        try {
            if (!writeAndWait(gatt, characteristic, cmd) || !responseLatch.await(RESPONSE_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                return null;
            }
        } finally {
            expectedResponse = -1;
        }
        final byte[] r = response;
        return r != null && r.length >= minSize ? r : null;
    }

    private boolean writeAndWait(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) throws InterruptedException {
        writtenLatch = new CountDownLatch(1);
        if (!writeCharacteristic(gatt, characteristic, value)) {
            return false;
        }
        // Android accepts the next write only after this one has been reported
        writtenLatch.await(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS);
        return true;
    }

    private static int u16(byte[] b, int offset) {
        return (b[offset] & 0xFF) | ((b[offset + 1] & 0xFF) << 8);
    }

    @SuppressLint("MissingPermission")
    private void close(BluetoothGatt gatt) {
        if (gatt == null) return;
        try {
            gatt.disconnect();
            disconnectedLatch.await(DISCONNECT_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (Exception e) {
            // ignore, closing anyway
        }
        try {
            gatt.close();
        } catch (Exception e) {
            // ignore
        }
    }

    // Clears Android's GATT cache (hidden BluetoothGatt.refresh()) and discovers the services again
    @SuppressLint("MissingPermission")
    private boolean refreshServices(BluetoothGatt gatt) throws InterruptedException {
        try {
            gatt.getClass().getMethod("refresh").invoke(gatt);
        } catch (Exception e) {
            UserError.Log.e(TAG, "GATT cache refresh not available: " + e);
        }
        discoveredLatch = new CountDownLatch(1);
        servicesOk = false;
        return gatt.discoverServices() && discoveredLatch.await(DISCOVER_TIMEOUT_SEC, TimeUnit.SECONDS) && servicesOk;
    }

    private static BluetoothGattCharacteristic findCharacteristic(BluetoothGatt gatt) {
        for (BluetoothGattService service : gatt.getServices()) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHARACTERISTIC_CMD);
            if (characteristic != null) return characteristic;
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    @SuppressWarnings("deprecation")
    private static boolean writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] frame) {
        final int writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return gatt.writeCharacteristic(characteristic, frame, writeType) == BluetoothGatt.GATT_SUCCESS;
        }
        characteristic.setWriteType(writeType);
        characteristic.setValue(frame);
        return gatt.writeCharacteristic(characteristic);
    }

    private void onNotification(byte[] value) {
        final CountDownLatch latch = responseLatch;
        if (value != null && value.length > 0 && (value[0] & 0xFF) == expectedResponse && latch != null) {
            response = value.clone();
            latch.countDown();
        }
    }

    private static void release(CountDownLatch latch) {
        if (latch != null) latch.countDown();
    }

    private final BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            UserError.Log.d(TAG, "Connection state " + newState + " status " + status);
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                connectedLatch.countDown();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Release every waiter, whatever step we were at
                release(connectedLatch);
                release(discoveredLatch);
                release(descriptorLatch);
                release(responseLatch);
                release(writtenLatch);
                release(disconnectedLatch);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            servicesOk = status == BluetoothGatt.GATT_SUCCESS;
            discoveredLatch.countDown();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            release(descriptorLatch);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            release(writtenLatch);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            // Android 13+
            onNotification(value);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // Android 12 and older
            onNotification(characteristic.getValue());
        }
    };
}
