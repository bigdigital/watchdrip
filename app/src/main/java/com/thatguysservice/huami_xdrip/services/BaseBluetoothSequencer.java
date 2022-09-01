package com.thatguysservice.huami_xdrip.services;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleAlreadyConnectedException;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanSettings;
import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.UtilityModels.RxBleProvider;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.Pref;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.utils.bt.BackgroundScanReceiver;
import com.thatguysservice.huami_xdrip.utils.bt.BtCallBack;
import com.thatguysservice.huami_xdrip.utils.bt.BtCallBack2;
import com.thatguysservice.huami_xdrip.utils.bt.DisconnectReceiver;
import com.thatguysservice.huami_xdrip.utils.bt.ReplyProcessor;
import com.thatguysservice.huami_xdrip.utils.bt.Subscription;
import com.thatguysservice.huami_xdrip.utils.framework.PoorMansConcurrentLinkedDeque;
import com.thatguysservice.huami_xdrip.utils.time.SlidingWindowConstraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.schedulers.Schedulers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.thatguysservice.huami_xdrip.models.Helper.emptyString;
import static com.thatguysservice.huami_xdrip.services.BaseBluetoothSequencer.BaseState.CLOSE;
import static com.thatguysservice.huami_xdrip.services.BaseBluetoothSequencer.BaseState.CLOSED;
import static com.thatguysservice.huami_xdrip.services.BaseBluetoothSequencer.BaseState.CONNECT_NOW;
import static com.thatguysservice.huami_xdrip.services.BaseBluetoothSequencer.BaseState.DISCOVER;
import static com.thatguysservice.huami_xdrip.services.BaseBluetoothSequencer.BaseState.INIT;
import static com.thatguysservice.huami_xdrip.services.BaseBluetoothSequencer.BaseState.SEND_QUEUE;
import static com.thatguysservice.huami_xdrip.services.BaseBluetoothSequencer.BaseState.SLEEP;
import static com.thatguysservice.huami_xdrip.utils.bt.ScanMeister.SCAN_FOUND_CALLBACK;

public abstract class BaseBluetoothSequencer extends BaseBluetoothService implements BtCallBack, BtCallBack2/*, BtCallBack3 */ {

    private static final HashMap<UUID, String> mapToName = new HashMap<>();
    // connection handling
    private static final int SCAN_REQUEST_CODE = 142;   // just for unique pending intent
    private static final int MAX_QUEUE_RETRIES = 3;
    protected final RxBleClient rxBleClient = RxBleProvider.getSingleton();
    protected volatile Inst I;
    protected BaseState mState;
    protected String connection_state;
    private volatile String myid;
    private PendingIntent scanCallBack = null;

    // address handling

    {
        setMyid(TAG);
    }

    public static String getUUIDName(final UUID uuid) {
        if (uuid == null) return "null";
        final String result = mapToName.get(uuid);
        return result != null ? result : "Unknown uuid: " + uuid.toString();
    }

    protected boolean isConnected(){
        return I.isConnected();
    }

    // does the system think we are connected to a device
    public static boolean isConnectedToDevice(final String mac) {
        if (emptyString(mac)) {
            return false;
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) HuamiXdrip.getAppContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return false;
        }
        boolean foundConnectedDevice = false;
        for (BluetoothDevice bluetoothDevice : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
            if (bluetoothDevice.getAddress().equalsIgnoreCase(mac)) {
                foundConnectedDevice = true;
                break;
            }
        }
        return foundConnectedDevice;
    }

    protected synchronized void setMyid(final String id) {
        UserError.Log.d(TAG, "Setting myid to: " + id);
        myid = id;
        I = Inst.get(id);
    }

    protected boolean setAddress(String newAddress) {
        DisconnectReceiver.addCallBack(this, TAG);
        //ConnectReceiver.addCallBack(this, TAG);
        if (emptyString(newAddress)) return false;
        newAddress = newAddress.toUpperCase();

        if (!Helper.validateMacAddress(newAddress)) {
            final String msg = "Invalid MAC address: " + newAddress;
            if (Helper.quietratelimit("jam-invalid-mac", 60)) {
                UserError.Log.wtf(TAG, msg);
                Helper.static_toast_long(msg);
            }
            return false;
        }

        if (I.address == null || !I.address.equals(newAddress)) {
            final String oldAddress = I.address;
            I.address = newAddress;
            newAddressEvent(oldAddress, newAddress);
        }
        return true;
    }

    public String getAddress() {
        return I.address;
    }

    protected void newAddressEvent(final String oldAddress, final String newAddress) {

        // out with the old
        if (oldAddress != null) {
            stopConnect(oldAddress);
            stopWatching(oldAddress);
        }

        // in with the new
        I.bleDevice = rxBleClient.getBleDevice(newAddress);
        watchConnection(newAddress);
    }

    public synchronized void btCallback2(final String mac, final String status, final String name, final Bundle bundle) {
        // currently we are only using this callback to implement faux auto-connect
        if (status.equals(SCAN_FOUND_CALLBACK)) {
            rxBleClient.getBackgroundScanner().stopBackgroundBleScan(scanCallBack);
            if (Helper.ratelimit("jambase-btcb2-" + mac, 2)) {
                stopConnect(mac);
                realEstablishConnection(mac, false); // don't auto connect as we did that via scan
            }
        }
    }

    private String getIntentFilterName() {
        return BackgroundScanReceiver.getACTION_NAME();
    }

    private void registerScanReceiver() {
        if (scanCallBack == null) {
            scanCallBack = PendingIntent.getBroadcast(HuamiXdrip.getAppContext(), SCAN_REQUEST_CODE,
                    new Intent(HuamiXdrip.getAppContext(), BackgroundScanReceiver.class).setAction(getIntentFilterName()).putExtra("CallingClass", this.getClass().getSimpleName()), PendingIntent.FLAG_UPDATE_CURRENT);
        }
        BackgroundScanReceiver.addCallBack2(this, this.getClass().getSimpleName());
    }

    private void unregisterScanReceiver() {
        if (scanCallBack != null) {
            try {
                rxBleClient.getBackgroundScanner().stopBackgroundBleScan(scanCallBack);
            } catch (Exception e) {
                UserError.Log.d(TAG, "Error removing background scanner callback: " + e);
            }
            BackgroundScanReceiver.removeCallBack(this.getClass().getSimpleName());
        }
    }

    protected synchronized void startConnect(final String address) {
        if (emptyString(address)) {
            UserError.Log.e(TAG, "Cannot connect as address is null");
            return;
        }
        if (address.equals("00:00:00:00:00:00")) {
            UserError.Log.d(TAG, "Not trying to connect to all zero mac");
            return;
        }
        if (isConnected()) {
            UserError.Log.d(TAG, "Already connected  - skipping connect");
            changeNextState();
            return;
        }
        stopConnect(address); // or do something like check if we are already connected
        setConnectionStatte(false);
        resetBluetoothIfWeSeemToAlreadyBeConnected(address); // TODO might be a race condition here if we are already disconnecting - maybe we should check twice

        if (I.autoConnect && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && Pref.getBoolean("bluetooth_allow_background_scans", true)) {
            UserError.Log.d(TAG, "Trying background scan connect: " + scanCallBack + " " + address);
            try {

                rxBleClient.getBackgroundScanner()
                        .scanBleDeviceInBackground(scanCallBack,
                                new ScanSettings.Builder()
                                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                                        // .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH) // doesn't work on samsung - annoying as could persist forever otherwise
                                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                                        .build(),
                                new ScanFilter.Builder().setDeviceAddress(address).build());

            } catch (Exception e) {
                UserError.Log.e(TAG, "Cannot background scan: " + e);
            }

        } else {
            realEstablishConnection(address, I.autoConnect);
        }
    }

    // also called from callback
    private void realEstablishConnection(final String address, final boolean autoConnect) {
        UserError.Log.d(TAG, "Trying connect: " + address + " autoconnect: " + autoConnect);
        // Attempt to establish a connection
        I.connectionSubscription = new Subscription(I.bleDevice.establishConnection(autoConnect)
                .timeout(I.connectTimeoutMinutes, TimeUnit.MINUTES)
                // .flatMap(RxBleConnection::discoverServices)
                // .observeOn(AndroidSchedulers.mainThread())
                // .doOnUnsubscribe(this::clearSubscription)
                .subscribeOn(Schedulers.io())
                .doFinally(this::establishConnectionFinally)
                .subscribe(this::onConnectionReceived, this::onConnectionFailure));


    }

    private void establishConnectionFinally() {
        UserError.Log.d(TAG, "Establish connection finally called");
    }

    protected synchronized void stopConnect(final String address) {
        UserError.Log.d(TAG, "Stopping connection with: " + address);
        //UserError.Log.d(TAG, "Stopping connection with: " + address + backTrace());

        if (I.connectionSubscription != null) {
            I.connectionSubscription.unsubscribe();
            UserError.Log.d(TAG, "Unsubscribed in StopConnect");
        }
        stopDiscover();
        I.connection = null; // TODO IS THIS ACTUALLY CORRECT???
        setConnectionStatte(false);
    }

    protected synchronized void watchConnection(final String address) {
        UserError.Log.d(TAG, "Starting to watch connection with: " + address);
        /// / Listen for connection state changes
        I.stateSubscription = new Subscription(I.bleDevice.observeConnectionStateChanges()
                // .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::onConnectionStateChange, throwable -> {
                    UserError.Log.wtf(TAG, "Got Error from state subscription: " + throwable);
                }));
    }

    protected synchronized void stopWatching(final String address) {
        UserError.Log.d(TAG, "Stopping watching: " + address);
        if (I.stateSubscription != null) {
            I.stateSubscription.unsubscribe();
        }
    }

    // We have connected to the device!
    private void onConnectionReceived(RxBleConnection this_connection) {
        //msg("Connected");
        // TODO check connection already exists - close etc?
        I.connection = this_connection;
        I.lastConnected = Helper.tsl();
        setConnectionStatte(true);
        UserError.Log.d(TAG, "Initial connection going for service discovery");
        changeState(DISCOVER);
    }


    /*@Override
    public void btCallback3(final String mac, final String status, final String name, final Bundle bundle, final BluetoothDevice device) {
        UserError.Log.d(TAG, "Received callback: " + mac + " " + status);
        if (device != null && I.useReconnectHandler && device.getAddress().equals(I.address)) {
            BtReconnect.checkReconnect(device);
        }
    }*/

    private void onConnectionFailure(Throwable throwable) {
        UserError.Log.d(TAG, "received: onConnectionFailure: " + throwable);
        if (throwable instanceof BleAlreadyConnectedException) {
            UserError.Log.d(TAG, "Already connected - advancing to next stage");
            setConnectionStatte(true);
            changeNextState();
        } else if (throwable instanceof BleDisconnectedException) {
            if (((BleDisconnectedException) throwable).state == 133) {
                if (I.retry133) {
                    if (Helper.ratelimit(TAG + "133recon", 60)) {
                        if (I.state.equals(CONNECT_NOW)) {
                            UserError.Log.d(TAG, "Automatically retrying connection");
                            Inevitable.task(TAG + "133recon", 3000, new Runnable() {
                                @Override
                                public void run() {
                                    changeState(CONNECT_NOW);
                                }
                            });
                        }
                    }
                }
            } else if (I.autoConnect) {
                UserError.Log.d(TAG, "Auto reconnect persist");
                changeState(CONNECT_NOW);
            }
            else{
                UserError.Log.d(TAG, "Closing connections");
                changeState(CLOSE);
            }
        }
    }

    @Override
    public void btCallback(String address, String status) {
        UserError.Log.d(TAG, "Processing callback: " + address + " :: " + status);
        if (I.address == null) return;
        if (address.equals(I.address)) {
            switch (status) {
                case "DISCONNECTED":
                    if (Helper.ratelimit("diconnected-from-" + address, 15)) {
                        //I.isConnected = false;
                        stopConnect(I.address);
                    } else {
                        UserError.Log.d(TAG, "Not processing disconnection callback due to debounce");
                    }
                    break;
                case "SCAN_FOUND":
                    break;
                case "SCAN_TIMEOUT":
                    break;
                case "SCAN_FAILED":
                    break;

                default:
                    UserError.Log.e(TAG, "Unknown status callback for: " + address + " with " + status);
            }
        } else {
            UserError.Log.d(TAG, "Ignoring: " + status + " for " + address + " as we are using: " + I.address);
        }
    }


    // service discovery

    protected void setConnectionStatte(boolean state) {
        I.setIsConnected(state);
    }

    protected synchronized void onConnectionStateChange(final RxBleConnection.RxBleConnectionState newState) {

        connection_state = "Unknown";
        switch (newState) {
            case CONNECTING:
                connection_state = "Connecting";
                //  connecting_time = JoH.tsl();
                break;
            case CONNECTED:
                setConnectionStatte(true);
                I.retry_backoff = 0; // reset counter
                connection_state = "Connected";

                break;
            case DISCONNECTING:
                setConnectionStatte(false);
                connection_state = "Disconnecting";

                break;
            case DISCONNECTED:
                stopConnect(I.address);
                //I.isConnected = false;
                connection_state = "Disconnected";

                changeState(CLOSE);
                break;
        }

        UserError.Log.d(TAG, "Connection state changed to: " + connection_state);
    }

    public synchronized void discover_services() {
        //  if (state == DISCOVER) {
        if (I.discoverOnce && I.isDiscoveryComplete) {
            UserError.Log.d(TAG, "Skipping service discovery as already completed");
            changeNextState();
        } else {
            if (I.connection != null) {
                UserError.Log.d(TAG, "Discovering services");
                stopDiscover();
                I.discoverSubscription = new Subscription(I.connection.discoverServices(10, TimeUnit.SECONDS)
                        .subscribe(this::onServicesDiscovered, this::onDiscoverFailed));
            } else {
                UserError.Log.e(TAG, "No connection when in DISCOVER state - reset");
                // These are normally just ghosts that get here, not really connected
                if (I.resetWhenAlreadyConnected) {
                    if (Helper.ratelimit("jam-sequencer-reset", 10)) {
                        changeState(CLOSE);
                    }
                }

            }
            //} else {
            //     UserError.Log.wtf(TAG, "Attempt to discover when not in DISCOVER state");
            //  }
        }
    }

    protected synchronized void stopDiscover() {
        if (I.discoverSubscription != null) {
            I.discoverSubscription.unsubscribe();
        }
    }

// end service discovery


    // state automata's

    protected void onServicesDiscovered(RxBleDeviceServices services) {
        UserError.Log.d(TAG, "Services discovered okay in base sequencer");
        final Object obj = new Object();
        for (BluetoothGattService service : services.getBluetoothGattServices()) {
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                I.characteristics.put(characteristic.getUuid(), obj);
            }
        }
    }

    protected void onDiscoverFailed(Throwable throwable) {
        UserError.Log.e(TAG, "Discover failure: " + throwable.toString());
        tryGattRefresh(I.connection);
        changeState(CLOSE);
        // incrementErrors();
    }

    public synchronized void changeState(final String new_state) {
        final String state = I.state;
        if (state == null || new_state == null) return;
        if ((state.equals(new_state)) && !state.equals(INIT) && !state.equals(SLEEP)) {
            if (!state.equals(CLOSE)) {
                UserError.Log.d(TAG, "Already in state: " + new_state.toUpperCase());
             //   UserError.Log.d(TAG, Helper.backTrace());
            //    changeState(CLOSE);
            }
        } else {
            if ((state.equals(CLOSED) || state.equals(CLOSE)) && new_state.equals(CLOSE)) {
                UserError.Log.d(TAG, "Not closing as already closed");
            } else {
                UserError.Log.d(TAG, "---Changing state from: " + state  + " to " + new_state);
                I.state = new_state;
                background_automata(I.backgroundStepDelay);
            }
        }
    }

    public void changeNextState() {
        changeState(mState.next());
    }

    protected synchronized boolean alwaysConnected() {
        if (isConnected() || I.state.equals(CONNECT_NOW)) {
            UserError.Log.d(TAG, "Always connected passes");
            return true;
        }
        if (Helper.ratelimit(TAG + "auto-reconnect", 1)) {
            UserError.Log.d(TAG, "alwaysConnected() requesting connect");
            changeState(CONNECT_NOW);
        } else {
            UserError.Log.d(TAG, "Too frequent reconnect calls");
            setRetryTimerReal();
        }
        return false;
    }

    protected void setRetryTimerReal() {
        throw new RuntimeException("Must define setRetryTimerReal() if you are going to use it");
    }

    // Queue


    /// Queue Handling

    @Override
    protected synchronized boolean automata() {

        UserError.Log.d(TAG, "automata state: " + I.state);
        extendWakeLock(3000);
        try {
            switch (I.state) {
                case INIT:
                    UserError.Log.d(TAG, "INIT State does nothing unless overridden");
                    break;
                case CONNECT_NOW:
                    if (!isConnected()) {
                        if (Helper.ratelimit("jambase connect" + I.address, 1)) {
                            startConnect(I.address);
                        } else {
                            UserError.Log.d(TAG, "Blocking duplicate connect within 1 second");
                        }
                    } else {
                        changeState(mState.next());
                    }
                    break;
                case DISCOVER:
                    discover_services();
                    break;
                case SEND_QUEUE:
                    startQueueSend();
                    break;

                case CLOSE:
                    stopConnect(I.address);
                    changeState(CLOSED);
                    break;

                case CLOSED:
                    if (I.autoReConnect) {
                        // TODO use sliding window constraint
                        if (I.reconnectConstraint != null) {
                            if (I.reconnectConstraint.checkAndAddIfAcceptable(1)) {
                                UserError.Log.d(TAG, "Attempting auto-reconnect");
                                if (Helper.isBluetoothEnabled(HuamiXdrip.getAppContext())) {
                                    changeState(CONNECT_NOW);
                                } else {
                                    Inevitable.task("Auto-reconnect", 8000, () -> changeState(CONNECT_NOW)); // delay for bluetooth cycling
                                }
                            } else {
                                UserError.Log.d(TAG, "Not attempting auto-reconnect due to constraint");
                            }
                        } else {
                            UserError.Log.e(TAG, "No reconnectConstraint is null");
                        }

                    }
                    break;

                default:
                    return false;

            }
            return true;
        } finally {
            //
        }
    }

    private void addToWriteQueue(final QueueMe queueMe, final boolean unique, final boolean atHead) {
        if (queueMe.byteslist == null) {
            queueMe.byteslist = new LinkedList<>();
        }
        final boolean multiple = queueMe.byteslist.size() > 1;
        for (final byte[] bytes : queueMe.byteslist) {
            if (unique) {
                if (doesWriteQueueContainBytes(bytes)) continue; // skip if duplicate bytes
            }
            UUID queueWriteCharacterstic = queueMe.queueWriteCharacterstic;
            if (queueWriteCharacterstic == null)
                queueWriteCharacterstic = I.queue_write_characterstic;
            if (atHead) {
                I.write_queue.addFirst(new QueueItem(queueWriteCharacterstic, bytes, queueMe.timeout_seconds, queueMe.delay_ms, queueMe.description, queueMe.expect_reply, queueMe.expireAt)
                        .setRunnable(queueMe.runnable));

            } else {
                I.write_queue.add(new QueueItem(queueWriteCharacterstic, bytes, queueMe.timeout_seconds, queueMe.delay_ms, queueMe.description, queueMe.expect_reply, queueMe.expireAt)
                        .setRunnable(queueMe.runnable));

            }
        }
        if (queueMe.start_now) startQueueSend();
    }

    private boolean doesWriteQueueContainBytes(final byte[] bytes) {
        if (bytes == null) return false;
        synchronized (I.write_queue) {
            // TODO a more efficient way to do this
            for (final QueueItem item : I.write_queue.toArray(new QueueItem[1])) {
                if (item != null) {
                    if (!item.isExpired()) {
                        if (Arrays.equals(bytes, item.data)) return true;
                    }
                }
            }
        }
        return false;
    }

    public void emptyQueue() {
        I.write_queue.clear();
    }

    public void startQueueSend() {
        Inevitable.task("sequence-start-queue " + I.address, 0, new Runnable() {
            @Override
            public void run() {
                writeMultipleFromQueue(I.write_queue);
            }
        });
    }

    private synchronized void writeMultipleFromQueue(final PoorMansConcurrentLinkedDeque<QueueItem> queue) {
        if (isConnected()) {
            QueueItem item = queue.poll();
            while (item != null && item.isExpired()) {
                UserError.Log.d(TAG, "Item expired from queue early: (expiry: " + Helper.dateTimeText(item.expireAt) + " " + item.description);
                item = queue.poll();
            }
            if (item != null) {
                if (!item.isExpired()) {
                    UserError.Log.d(TAG, "Starting queue send for item: " + item.description);
                    writeQueueItem(queue, item);
                } else {
                    // TODO this is very much an edge case now
                    UserError.Log.d(TAG, "Item expired from queue: (expiry: " + Helper.dateTimeText(item.expireAt) + " " + item.description);
                    writeMultipleFromQueue(queue);
                }
            } else {
                UserError.Log.d(TAG, "write queue empty");
                changeState(mState.next()); // check if this logic is sound
            }
        } else {
            UserError.Log.d(TAG, "CANNOT WRITE QUEUE AS DISCONNECTED");
        }
    }

    @SuppressLint("CheckResult")
    private void writeQueueItem(final PoorMansConcurrentLinkedDeque<QueueItem> queue, final QueueItem item) {
        extendWakeLock(2000 + item.post_delay);
        if (I.connection == null) {
            UserError.Log.e(TAG, "Cannot write queue item: " + item.description + " as we have no connection!");
            return;
        }

        if (item.queueWriteCharacterstic == null) {
            UserError.Log.e(TAG, "Write characteristic not set in queue write");
            return;
        }
        UserError.Log.d(TAG, "Writing to characteristic: " + item.queueWriteCharacterstic + " " + item.description);
        I.connection.writeCharacteristic(item.queueWriteCharacterstic, item.getData())
                .timeout(item.timeoutSeconds, TimeUnit.SECONDS)
                .subscribe(Value -> {
                    UserError.Log.d(TAG, "Wrote request: " + item.description + " -> " + Helper.bytesToHex(Value));
                    if (item.expectReply) expectReply(queue, item);
                    if (item.post_delay > 0) {
                        // always sleep if set as new item might appear in queue
                        final long sleep_time = item.post_delay + (item.description.contains("WAKE UP") ? 2000 : 0);
                        if (sleep_time != 100) UserError.Log.d(TAG, "sleeping " + sleep_time);
                        Helper.threadSleep(sleep_time);
                    }
                    if (item.runnable != null) {
                        item.runnable.run(); // TODO should this be handled by expect reply in that case?
                    }
                    if (!item.expectReply) {
                        writeMultipleFromQueue(queue); // start next item immediately
                    }
                    throw new OperationSuccess("write complete: " + item.description);
                }, throwable -> {
                    if (!(throwable instanceof OperationSuccess)) {
                        UserError.Log.d(TAG, "Throwable in: " + item.description + " -> " + throwable);
                        item.retries++;
                        if (!(throwable instanceof BleDisconnectedException)) {
                            if (item.retries > MAX_QUEUE_RETRIES) {
                                UserError.Log.d(TAG, item.description + " failed max retries @ " + item.retries + " shutting down queue");
                                queue.clear(); /// clear too?
                                //changeState(CLOSE); // TODO put on switch
                            } else {
                                writeQueueItem(queue, item);
                            }
                        } else {
                            UserError.Log.d(TAG, "Disconnected so not attempting retries");
                            setConnectionStatte(false);
                        }
                    } else {
                        // not disconnecting on success
                    }
                });
    }

    private void expectReply(final PoorMansConcurrentLinkedDeque<QueueItem> queue, final QueueItem item) {
        final long wait_time = 3000;
        Inevitable.task("expect-reply-" + I.address + "-" + item.description, wait_time, new Runnable() {
            @Override
            public void run() {
                if (Helper.msSince(I.lastProcessedIncomingData) > wait_time) {
                    UserError.Log.d(TAG, "GOT NO REPLY FOR: " + item.description + " @ " + item.retries);
                    item.retries++;
                    if (item.retries <= MAX_QUEUE_RETRIES) {
                        UserError.Log.d(TAG, "Retrying due to no reply: " + item.description);
                        writeQueueItem(queue, item);
                    }
                }
            }
        });
    }

    // Turn off anything we may have turned on
    protected void shutDown() {
        stopConnect(I.address);
        stopWatching(I.address);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerScanReceiver();
    }

    // Life Cycle

    @Override
    public void onDestroy() {
        shutDown();
        DisconnectReceiver.removeCallBack(TAG);
        //ConnectReceiver.removeCallBack(TAG);
        unregisterScanReceiver();
        super.onDestroy();
    }

    public void resetBluetoothIfWeSeemToAlreadyBeConnected(final String mac) {
        if (isConnectedToDevice(mac)) {
            if (Pref.getBooleanDefaultFalse("bluetooth_watchdog")) {
                if (Helper.ratelimit("jamsequencer-restart-bluetooth", 1200)) {
                    UserError.Log.e(TAG, "Restarting bluetooth as device reports we are connected but we can't find our connection");
                    Helper.niceRestartBluetooth(HuamiXdrip.getAppContext());
                } else {
                    UserError.Log.d(TAG, "Cannot restart bluetooth due to rate limit but we seem to be connected");
                }
            }
        }
    }

    // Instance management
    public static class Inst {

        private static final ConcurrentHashMap<String, Inst> singletons = new ConcurrentHashMap<>();
        public final ConcurrentHashMap<UUID, Object> characteristics = new ConcurrentHashMap<>();
        private final PoorMansConcurrentLinkedDeque<QueueItem> write_queue = new PoorMansConcurrentLinkedDeque<>();
        public volatile Subscription connectionSubscription;
        public volatile Subscription stateSubscription;
        public volatile Subscription discoverSubscription;
        public volatile RxBleDevice bleDevice;
        public volatile RxBleConnection connection;

        public volatile String address; // use setAddress() to initiate! don't write directly
        private volatile boolean isConnected;
        public volatile boolean isNotificationEnabled;
        public volatile boolean isDiscoveryComplete;
        public volatile String state;
        public volatile UUID queue_write_characterstic;
        public volatile long lastProcessedIncomingData = -1;
        public volatile int backgroundStepDelay = 100;
        public volatile int connectTimeoutMinutes = 7;
        public volatile boolean autoConnect = false;
        public volatile boolean autoReConnect = false;
        public volatile boolean retry133 = true;
        public volatile boolean discoverOnce = false;
        public volatile boolean resetWhenAlreadyConnected = false;
        public PendingIntent serviceIntent;
        public SlidingWindowConstraint reconnectConstraint;
        public long retry_time;
        public long retry_backoff;
        public long wakeup_time;
        long failover_time;
        long last_wake_up_time;
        @Getter
        long lastConnected;
        private PendingIntent serviceFailoverIntent;

        {
            state = INIT;
        }


        private Inst() {
        }

        public static Inst get(final String id) {
            if (id == null) return null;
            final Inst conn = singletons.get(id);
            if (conn != null) return conn;
            synchronized (BaseBluetoothSequencer.class) {
                final Inst double_check = singletons.get(id);
                if (double_check != null) return double_check;
                final Inst singleton = new Inst();
                singletons.put(id, singleton);
                return singleton;
            }
        }

        public void setIsConnected(boolean isConnected) {
            this.isConnected = isConnected;
        }

        public boolean isConnected() {
            return isConnected;
        }

        public int getQueueSize() {
            return write_queue.size();
        }

    }


    // utils

    public static class BaseState {
        public static final String INIT = "Initializing";
        public static final String CONNECT_NOW = "Connecting";
        public static final String SEND_QUEUE = "Sending Queue";
        public static final String DISCOVER = "Discover Services";
        public static final String SLEEP = "Sleeping";
        public static final String CLOSE = "Closing";
        public static final String CLOSED = "Closed";
        protected final List<String> sequence = new ArrayList<>();
        private Inst LI;

        {
            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(SEND_QUEUE);
            //sequence.add(DISCOVER); // handled by initial connection callback
            sequence.add(SLEEP);
        }


        public BaseState setLI(final Inst LI) {
            this.LI = LI;
            return this;
        }


        public String next() {
            try {
                if (LI.state.equals(SLEEP))
                    return SLEEP; // will not auto-advance out of sleep state
                return sequence.get(sequence.indexOf(LI.state) + 1);
            } catch (Exception e) {
                return SLEEP;
            }
        }

    }

    @RequiredArgsConstructor
    private class QueueItem {
        final UUID queueWriteCharacterstic;
        final byte[] data;
        final int timeoutSeconds;
        final long post_delay;
        public final String description;
        final boolean expectReply;
        final long expireAt;
        int retries = 0;
        Runnable runnable;

        boolean isExpired() {
            return expireAt != 0 && expireAt < Helper.tsl();
        }

        QueueItem setRunnable(Runnable runnable) {
            this.runnable = runnable;
            return this;
        }

        byte[] getData() {
            if (data != null) {
                return data;
            } else {
                return null;
            }
        }

    }

    public class QueueMe {

        List<byte[]> byteslist;
        long delay_ms = 100;
        int timeout_seconds = 10;
        long expireAt;
        boolean start_now;
        boolean expect_reply;
        String description = "Vanilla Queue Item";
        UUID queueWriteCharacterstic;
        Runnable runnable;
        ReplyProcessor processor;

        public QueueMe setByteList(List<byte[]> byteList) {
            this.byteslist = byteList;
            return this;
        }

        public QueueMe setBytes(final byte[] bytes) {
            final List<byte[]> byteList = new LinkedList<>();
            byteList.add(bytes);
            this.byteslist = byteList;
            return this;
        }

        public QueueMe setTimeout(final int timeout) {
            this.timeout_seconds = timeout;
            return this;
        }

        public QueueMe setDelayMs(final int delay) {
            this.delay_ms = delay;
            return this;
        }

        public QueueMe expireInSeconds(final int timeout) {
            this.expireAt = Helper.tsl() + (Constants.SECOND_IN_MS * timeout);
            return this;
        }

        public QueueMe setDescription(String description) {
            this.description = description;
            return this;
        }

        public QueueMe setRunnable(Runnable runnable) {
            this.runnable = runnable;
            return this;
        }

        public QueueMe setProcessor(ReplyProcessor runnable) {
            this.processor = runnable;
            return this;
        }


        public UUID getQueueWriteCharacterstic() {
            return queueWriteCharacterstic;
        }

        public QueueMe setQueueWriteCharacterstic(UUID queueWriteCharacterstic) {
            this.queueWriteCharacterstic = queueWriteCharacterstic;
            return this;
        }


        public QueueMe now() {
            this.start_now = true;
            return this;
        }

        public QueueMe expectReply() {
            this.expect_reply = true;
            return this;
        }

        public void queue() {
            this.start_now = false; // make sure disabled
            add();
        }

        public void queueUnique() {
            this.start_now = false; // make sure disabled
            add(true);
        }

        public void send() {
            this.start_now = true; // make sure enabled
            add();
        }

        private void add() {
            add(false); // don't unique by default
        }

        public void insert() {     // insert to head of queue
            addToWriteQueue(this, false, true);
        }

        private void add(final boolean unique) {
            //addToWriteQueue(byteslist, delay_ms, timeout_seconds, start_now, description, expect_reply, expireAt, runnable);
            addToWriteQueue(this, unique, false);
        }

    }

}
