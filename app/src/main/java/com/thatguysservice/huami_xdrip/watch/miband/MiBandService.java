package com.thatguysservice.huami_xdrip.watch.miband;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;

import androidx.annotation.RequiresApi;

import com.polidea.rxandroidble2.ConnectionParameters;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble2.exceptions.BleCharacteristicNotFoundException;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.R;
import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.JoH;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.services.BroadcastService;
import com.thatguysservice.huami_xdrip.services.JamBaseBluetoothSequencer;
import com.thatguysservice.huami_xdrip.utils.Version;
import com.thatguysservice.huami_xdrip.utils.bt.Subscription;
import com.thatguysservice.huami_xdrip.utils.framework.PoorMansConcurrentLinkedDeque;
import com.thatguysservice.huami_xdrip.utils.framework.WakeLockTrampoline;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceState;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceStateBip;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceStateBipS;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceStateGTS2Mini;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceStateMiBand4;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceStateMiBand5;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceStateMiBand6;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceStateVerge2;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceStateVergeNew;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceStateVergeOld;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceGenerator;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.AuthOperations;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperationOld;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperations;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperationsNew;
import com.thatguysservice.huami_xdrip.watch.miband.message.AlertLevelMessage;
import com.thatguysservice.huami_xdrip.watch.miband.message.AlertMessage;
import com.thatguysservice.huami_xdrip.watch.miband.message.DeviceEvent;
import com.thatguysservice.huami_xdrip.watch.miband.message.DisplayControllMessage;
import com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import lombok.Getter;

import static com.polidea.rxandroidble2.RxBleConnection.GATT_MTU_MAXIMUM;
import static com.polidea.rxandroidble2.RxBleConnection.GATT_MTU_MINIMUM;
import static com.polidea.rxandroidble2.RxBleConnection.GATT_WRITE_MTU_OVERHEAD;
import static com.thatguysservice.huami_xdrip.models.JoH.bytesToHex;
import static com.thatguysservice.huami_xdrip.models.JoH.emptyString;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_ADD_HR;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_ADD_STEPS;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_ALERT;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_AFTER_ALARM;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_REFRESH;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_MESSAGE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG_FORCE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.INTENT_FUNCTION_KEY;
import static com.thatguysservice.huami_xdrip.services.JamBaseBluetoothSequencer.BaseState.CLOSE;
import static com.thatguysservice.huami_xdrip.services.JamBaseBluetoothSequencer.BaseState.CLOSED;
import static com.thatguysservice.huami_xdrip.services.JamBaseBluetoothSequencer.BaseState.INIT;
import static com.thatguysservice.huami_xdrip.services.JamBaseBluetoothSequencer.BaseState.SLEEP;
import static com.thatguysservice.huami_xdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_ALARM;
import static com.thatguysservice.huami_xdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_CALL;
import static com.thatguysservice.huami_xdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_CANCEL;
import static com.thatguysservice.huami_xdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_MESSAGE;
import static com.thatguysservice.huami_xdrip.watch.miband.Const.PREFERRED_MTU_SIZE;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_ACK_FIND_PHONE_IN_PROGRESS;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_DISABLE_CALL;

/**
 * <p>
 * Data communication with MiBand compatible bands/watches
 */

public class MiBandService extends JamBaseBluetoothSequencer {

    static final List<UUID> huntCharacterstics = new ArrayList<>();
    private static final long RETRY_PERIOD_MS = Constants.SECOND_IN_MS * 30; // sleep for max ms if we have had no signal
    private static final long BG_UPDATE_NO_DATA_INTERVAL = 30 * Constants.MINUTE_IN_MS; //minutes
    private static final long CONNECTION_TIMEOUT = 5 * Constants.MINUTE_IN_MS; //minutes
    private static final long RESTORE_NIGHT_MODE_DELAY = (Constants.SECOND_IN_MS * 7);
    private static final int QUEUE_EXPIRED_TIME = 30; //second
    private static final int QUEUE_DELAY = 0; //ms
    private static final int CALL_ALERT_DELAY = (int) (Constants.SECOND_IN_MS * 10);
    private static final int MESSAGE_DELAY = (int) (Constants.SECOND_IN_MS * 5);
    static BatteryInfo batteryInfo = new BatteryInfo();
    static private long bgWakeupTime;
    public boolean useV2ChunkedProtocol = false;

    static {
        huntCharacterstics.add(Const.UUID_CHAR_HEART_RATE_MEASUREMENT);
    }

    private final PoorMansConcurrentLinkedDeque<QueueMessage> messageQueue = new PoorMansConcurrentLinkedDeque<>();
    public Boolean isNeedToRestoreNightMode = false;
    private int mMTU = GATT_MTU_MINIMUM;
    private Subscription authSubscription;
    private Subscription notifSubscriptionDeviceEvent;
    private Subscription notifSubscriptionHeartRateMeasurement;
    private Subscription notifSubscriptionStepsMeasurement;
    private Boolean isNeedToCheckRevision = true;
    private Boolean isNeedToAuthenticate = true;
    private boolean isWaitingCallResponse = false;
    private boolean isNightMode = false;
    private MediaPlayer player;
    private PendingIntent bgServiceIntent;
    private MiBandType prevDeviceType = MiBandType.UNKNOWN;
    private QueueMessage queueItem;
    private boolean prevReadingStatusIsStale = false;
    private String activeAlertType;
    private String missingAlertMessage;

    public byte[] sharedSessionKey;
    public int encryptedSequenceNr;
    public byte handle = 0;
    private int ringerModeBackup;

    public byte getNextHandle() {
        handle++;
        UserError.Log.e(TAG, "Handle number: " + handle);
        return handle;
    }

    {
        mState = new MiBandState().setLI(I);
        I.backgroundStepDelay = 0;
        //I.autoConnect = true;
        //I.playSounds = true;
        I.connectTimeoutMinutes = (int) CONNECTION_TIMEOUT;
        startBgTimer();
    }

    private static boolean isBetweenValidTime(Date startTime, Date endTime, Date currentTime) {
        //Start Time
        Calendar StartTime = Calendar.getInstance();
        StartTime.setTime(startTime);
        StartTime.set(1, 1, 1);

        Calendar EndTime = Calendar.getInstance();
        EndTime.setTime(endTime);
        EndTime.set(1, 1, 1);

        //Current Time
        Calendar CurrentTime = Calendar.getInstance();
        CurrentTime.setTime(currentTime);
        CurrentTime.set(1, 1, 1);
        if (EndTime.compareTo(StartTime) > 0) {
            return (CurrentTime.compareTo(StartTime) >= 0) && (CurrentTime.compareTo(EndTime) <= 0);
        } else if (EndTime.compareTo(StartTime) < 0) {
            return (CurrentTime.compareTo(EndTime) < 0) || (CurrentTime.compareTo(StartTime) > 0);
        } else {
            return false;
        }
    }

    public int getMTU() {
        return mMTU;
    }

    public void setMTU(int mMTU) {
        this.mMTU = mMTU;
        if (this.mMTU > GATT_MTU_MAXIMUM) this.mMTU = GATT_MTU_MAXIMUM;
        if (this.mMTU < GATT_MTU_MINIMUM) this.mMTU = GATT_MTU_MINIMUM;
    }


    @Override
    public void onCreate() {
        UserError.Log.e("MiBandService", "Creating service ");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        UserError.Log.e("MiBandService", "Killing service ");
        super.onDestroy();
    }

    private boolean readyToProcessCommand() {
        boolean result = I.state.equals(SLEEP) || I.state.equals(CLOSED) || I.state.equals(CLOSE) || I.state.equals(INIT) || I.state.equals(MiBandState.CONNECT_NOW);
        if (!result && I.state.equals(MiBandState.AUTHORIZE_FAILED_SLEEP) && MiBandType.supportPairingKey(MiBand.getMibandType())) {
            return true;
        }
        if (!I.isConnected) {
            return true;
        }
        if (!result)
            UserError.Log.d(TAG, "readyToProcessCommand not ready because state :" + I.state.toString());
        return result;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final PowerManager.WakeLock wl = JoH.getWakeLock("Miband service", 60000);
        try {
            if (shouldServiceRun()) {
                final String authMac = MiBand.getPersistentAuthMac();
                String mac = MiBand.getMac();
                MiBandType currDevice = MiBand.getMibandType();
                if ((currDevice != prevDeviceType) && currDevice != MiBandType.UNKNOWN) {
                    prevDeviceType = currDevice;
                    UserError.Log.d(TAG, "Found new device: " + currDevice.toString());
                    MiBandEntry.sendPrefIntent(MIBAND_INTEND_STATES.UPDATE_PREF_SCREEN, 0, "");
                }
                if (!authMac.equalsIgnoreCase(mac) || authMac.isEmpty()) {
                    prevDeviceType = MiBand.getMibandType();
                    if (!authMac.isEmpty()) {
                        String model = MiBand.getModel();
                        MiBand.setPersistentAuthMac(""); //flush old auth info
                        MiBand.setModel(model, mac);
                    }
                    isNeedToAuthenticate = true;
                }
                if (emptyString(mac)) {
                    // if mac not set then start up a scan and do nothing else
                    new FindNearby().scan();
                } else {
                    setAddress(mac);
                    if (intent != null) {
                        final String function = intent.getStringExtra(INTENT_FUNCTION_KEY);
                        if (function != null) {
                            UserError.Log.d(TAG, "onStartCommand was called with function:" + function);
                            //TODO handle intents
                            if (function.equals(CMD_LOCAL_REFRESH) && !JoH.pratelimit("miband-refresh-" + MiBand.getMac(), 5)) {
                                return START_STICKY;
                            } else {
                                if (function.equals(CMD_LOCAL_AFTER_ALARM)) {
                                    messageQueue.addFirst(new QueueMessage(function, intent.getExtras()));
                                    handleCommand();
                                } else {
                                    messageQueue.add(new QueueMessage(function, intent.getExtras()));
                                    if (readyToProcessCommand()) {
                                        handleCommand();
                                    }
                                }
                            }
                        } else {
                            // no specific function
                        }
                    }
                }
                return START_STICKY;
            } else {
                UserError.Log.d(TAG, "Service is NOT set be active - shutting down");
                stopBgUpdateTimer();
                stopConnection();
                changeState(CLOSE);
                prevReadingStatusIsStale = false;
                stopSelf();
                return START_NOT_STICKY;
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private void handleCommand() {
        if (messageQueue.isEmpty()) return;
        do {
            queueItem = messageQueue.poll();
        } while (queueItem.isExpired() && !messageQueue.isEmpty());
        if (queueItem.isExpired()) return;

        switch (queueItem.functionName) {
            case CMD_LOCAL_REFRESH:
                whenToRetryNextBgTimer(); //recalculate isNightMode
                ((MiBandState) mState).setSettingsSequence();
                break;
            case CMD_MESSAGE:
                ((MiBandState) mState).setQueueSequence();
                queueItem.setMessageType(MIBAND_NOTIFY_TYPE_MESSAGE);
                break;
            case CMD_ALERT:
                ((MiBandState) mState).setAlarmSequence();
                queueItem.setMessageType(MIBAND_NOTIFY_TYPE_ALARM);
                if (isNightMode) {
                    messageQueue.addFirst(new QueueMessage("update_bg_force"));
                }
                break;
            case CMD_LOCAL_AFTER_ALARM:
                if (!I.state.equals(MiBandState.WAITING_USER_RESPONSE)) break;
                vibrateAlert(AlertLevelMessage.AlertLevelType.NoAlert); //disable call
                if (!missingAlertMessage.isEmpty()) {
                    String msgText = HuamiXdrip.getAppContext().getString(R.string.miband_alert_missing_text) + missingAlertMessage;
                    messageQueue.addFirst(getMessageQueue(msgText, HuamiXdrip.getAppContext().getString(R.string.miband_alert_missing_title_text)));
                }
                ((MiBandState) mState).setQueueSequence();
                break;
            case CMD_UPDATE_BG:
                if (isNightMode) {
                    UserError.Log.d(TAG, "Skip bg update because of night mode");
                    return;
                }
                boolean curReadingStatusIsStale = isStaleReading();

                if (prevReadingStatusIsStale && curReadingStatusIsStale) {
                    UserError.Log.d(TAG, "Skip bg update because of staleReading");
                    return;
                }

                prevReadingStatusIsStale = curReadingStatusIsStale;
                startBgTimer();
                ((MiBandState) mState).setSendReadingSequence();
                break;
            case CMD_UPDATE_BG_FORCE:
                startBgTimer();
                ((MiBandState) mState).setSendReadingSequence();
                break;
            case "update_bg_as_notification":
                ((MiBandState) mState).setSendReadingSequence();
                break;
            default:
                handleCommand();
                return;
        }

        changeState(INIT);
    }

    private boolean isStaleReading() {
        return queueItem.bundle.getBoolean("bg.isStale", true);
    }

    private long whenToRetryNextBgTimer() {
        final long bg_time;

        Calendar expireDate = Calendar.getInstance();
        long currTimeMillis = JoH.tsl();
        expireDate.setTimeInMillis(System.currentTimeMillis() + BG_UPDATE_NO_DATA_INTERVAL);
        isNightMode = false;
        if (MiBandEntry.isNightModeEnabled()) {
            int nightModeInterval = MiBandEntry.getNightModeInterval();
            if (nightModeInterval != MiBandEntry.NIGHT_MODE_INTERVAL_STEP) {
                Calendar currCal = Calendar.getInstance();
                Date curr = currCal.getTime();
                Date start = MiBandEntry.getNightModeStart();
                Date end = MiBandEntry.getNightModeEnd();
                boolean result = isBetweenValidTime(start, end, curr);
                UserError.Log.d(TAG, "isBetweenValidTime: " + result);
                if (result) {
                    Calendar futureCal = Calendar.getInstance();
                    futureCal.setTimeInMillis(currTimeMillis + nightModeInterval * Constants.MINUTE_IN_MS);

                    Date futureDate = futureCal.getTime();
                    if (!isBetweenValidTime(start, end, futureDate)) {
                        Calendar calEndCal = Calendar.getInstance();
                        calEndCal.setTime(end);
                        futureCal.set(Calendar.HOUR_OF_DAY, calEndCal.get(Calendar.HOUR_OF_DAY));
                        futureCal.set(Calendar.MINUTE, calEndCal.get(Calendar.MINUTE));
                    }
                    expireDate = (Calendar) futureCal.clone();
                    isNightMode = true;
                }
            }
        }
        bg_time = expireDate.getTimeInMillis() - currTimeMillis;
        return bg_time;
    }

    private void stopBgUpdateTimer() {
        JoH.cancelAlarm(HuamiXdrip.getAppContext(), bgServiceIntent);
        bgWakeupTime = 0;
        isNightMode = false;
    }

    private void startBgTimer() {
        stopBgUpdateTimer();
        if (shouldServiceRun() && MiBand.isAuthenticated() && !MiBandEntry.isNeedSendReadingAsNotification()) {
            final long retry_in = whenToRetryNextBgTimer();
            UserError.Log.d(TAG, "Scheduling next BgTimer in: " + JoH.niceTimeScalar(retry_in) + " @ " + JoH.dateTimeText(retry_in + JoH.tsl()));
            bgServiceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.MIBAND_SERVICE_BG_RETRY_ID, "update_bg_force");
            JoH.wakeUpIntent(HuamiXdrip.getAppContext(), retry_in, bgServiceIntent);
            bgWakeupTime = JoH.tsl() + retry_in;
        } else {
            UserError.Log.d(TAG, "Retry timer was not sheduled");
        }
    }

    private void acknowledgeFindPhone() {
        UserError.Log.d(TAG, "acknowledgeFindPhone");
        I.connection.writeCharacteristic(Const.UUID_CHARACTERISTIC_3_CONFIGURATION, COMMAND_ACK_FIND_PHONE_IN_PROGRESS)
                .subscribe(val -> {
                    UserError.Log.d(TAG, "Wrote acknowledgeFindPhone: " + bytesToHex(val));
                }, throwable -> {
                    UserError.Log.e(TAG, "Could not write acknowledgeFindPhone: " + throwable);
                });
    }

    private void handleDeviceEvent(byte[] value) {
        if (value == null || value.length == 0) {
            return;
        }
        switch (value[0]) {
            case DeviceEvent.CALL_REJECT:
                UserError.Log.d(TAG, "call rejected");
                if (I.state.equals(MiBandState.WAITING_USER_RESPONSE)) {
                    String alertName = "";
                    int snoozeMinutes = 0;
                    String msgText = "";
                    double next_alert_at = JoH.ts();
                    if (activeAlertType.equals(Const.BG_ALERT_TYPE)) {
                       /* if (ActiveBgAlert.currentlyAlerting()) {
                            ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
                            if (activeBgAlert == null) {
                                UserError.Log.e(TAG, "Error, snooze was called but no alert is active");
                            } else {
                                AlertType alert = ActiveBgAlert.alertTypegetOnly();
                                if (alert != null) {
                                    alertName = alert.name;
                                    snoozeMinutes = alert.default_snooze;
                                }
                                AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), -1, true);
                                next_alert_at = activeBgAlert.next_alert_at;
                            }
                        } TODO cancel alert
*/
                    } else {
                    /*    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        snoozeMinutes = (int) MissedReadingService.getOtherAlertSnoozeMinutes(prefs, activeAlertType);
                        UserNotification.snoozeAlert(activeAlertType, snoozeMinutes);
                        UserNotification userNotification = UserNotification.GetNotificationByType(activeAlertType);
                        if (userNotification != null) {
                            next_alert_at = userNotification.timestamp;
                        }*/ //TODO snoze alert
                    }
                    msgText = String.format(HuamiXdrip.getAppContext().getString(R.string.miband_alert_snooze_text), alertName, snoozeMinutes, JoH.hourMinuteString((long) next_alert_at));
                    UserError.Log.d(TAG, msgText);
                    messageQueue.addFirst(getMessageQueue( msgText, HuamiXdrip.getAppContext().getString(R.string.miband_alert_snooze_title_text)));
                    startBgTimer();
                    handleCommand();
                }
                isWaitingCallResponse = false;
                break;
            case DeviceEvent.CALL_IGNORE:
                UserError.Log.d(TAG, "call ignored");
                if (I.state.equals(MiBandState.WAITING_USER_RESPONSE)) {
                    changeState(MiBandState.WAITING_MIFIT_SILENCE);
                }
                isWaitingCallResponse = false;
                break;
            case DeviceEvent.BUTTON_PRESSED:
                UserError.Log.d(TAG, "button pressed");
                break;
            case DeviceEvent.BUTTON_PRESSED_LONG:
                UserError.Log.d(TAG, "button long-pressed ");
                break;
            case DeviceEvent.START_NONWEAR:
                UserError.Log.d(TAG, "non-wear start detected");
                break;
            case DeviceEvent.ALARM_TOGGLED:
                UserError.Log.d(TAG, "An alarm was toggled");
                break;
            case DeviceEvent.FELL_ASLEEP:
                UserError.Log.d(TAG, "Fell asleep");
                break;
            case DeviceEvent.WOKE_UP:
                UserError.Log.d(TAG, "Woke up");
                break;
            case DeviceEvent.STEPSGOAL_REACHED:
                UserError.Log.d(TAG, "Steps goal reached");
                break;
            case DeviceEvent.TICK_30MIN:
                UserError.Log.d(TAG, "Tick 30 min (?)");
                break;
            case DeviceEvent.FIND_PHONE_START:
                UserError.Log.d(TAG, "find phone started");
                if ((JoH.ratelimit("band_find phone_sound", 3))) {
                    //player = JoH.playSoundUri(getResourceURI(R.raw.default_alert));
                }
                acknowledgeFindPhone();
                break;
            case DeviceEvent.FIND_PHONE_STOP:
                UserError.Log.d(TAG, "find phone stopped");
                if (player != null && player.isPlaying()) player.stop();
                break;
            case DeviceEvent.MUSIC_CONTROL:
                UserError.Log.d(TAG, "got music control");
                switch (value[1]) {
                    case 0:
                        UserError.Log.d(TAG, "Music app Event.PLAY");
                        break;
                    case 1:
                        UserError.Log.d(TAG, "Music app Event.PAUSE");
                        break;
                    case 3:
                        UserError.Log.d(TAG, "Music app Event.NEXT");
                        break;
                    case 4:
                        UserError.Log.d(TAG, "Music app Event.PREVIOUS");
                        break;
                    case 5:
                        UserError.Log.d(TAG, "Music app Event.VOLUMEUP");
                        break;
                    case 6:
                        UserError.Log.d(TAG, "Music app Event.VOLUMEDOWN");
                        break;
                    case (byte) 224:
                        UserError.Log.d(TAG, "Music app started");
                        break;
                    case (byte) 225:
                        UserError.Log.d(TAG, "Music app terminated");
                        break;
                    default:
                        UserError.Log.d(TAG, "unhandled music control event " + value[1]);
                        return;
                }
                break;
            case DeviceEvent.MTU_REQUEST:
                int mtu = (value[2] & 0xff) << 8 | value[1] & 0xff;
                UserError.Log.d(TAG, "device announced MTU of " + mtu);
                if (!MiBandEntry.isNeedToDisableHightMTU())
                    setMTU(mtu);
                break;
            default:
                UserError.Log.d(TAG, "unhandled event " + value[0]);
        }
    }

    @Override
    protected void onServicesDiscovered(RxBleDeviceServices services) {
        boolean found = false;
        for (BluetoothGattService service : services.getBluetoothGattServices()) {
            UserError.Log.d(TAG, "Service: " + getUUIDName(service.getUuid()));
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                UserError.Log.d(TAG, "-- Character: " + getUUIDName(characteristic.getUuid()));

                for (final UUID check : huntCharacterstics) {
                    if (characteristic.getUuid().equals(check)) {
                        found = true;
                    }
                }
            }
        }
        if (found) {
            I.isDiscoveryComplete = true;
            I.discoverOnce = true;
            changeNextState();
        } else {
            UserError.Log.e(TAG, "Could not find characteristic during service discovery. This is very unusual");
        }
    }

    @SuppressLint("CheckResult")
    private void getSoftwareRevision() {
        I.connection.readCharacteristic(Const.UUID_CHAR_SOFTWARE_REVISION_STRING).subscribe(
                readValue -> {
                    String revision = new String(readValue);
                    UserError.Log.d(TAG, "Got software revision: " + revision);
                    MiBand.setVersion(revision, MiBand.getPersistentAuthMac());
                    isNeedToCheckRevision = false;
                    changeNextState();
                }, throwable -> {
                    UserError.Log.e(TAG, "Could not read software revision: " + throwable);
                    changeNextState();
                });
    }

    @SuppressLint("CheckResult")
    private void getBatteryInfo() {
        I.connection.readCharacteristic(Const.UUID_CHARACTERISTIC_6_BATTERY_INFO).subscribe(
                readValue -> {
                    UserError.Log.d(TAG, "Got battery info: " + bytesToHex(readValue));
                    batteryInfo = new BatteryInfo(readValue);
                }, throwable -> {
                    UserError.Log.e(TAG, "Could not read battery info: " + throwable);
                });
    }

    @SuppressLint("CheckResult")
    private void getModelName() {
        I.connection.readCharacteristic(Const.UUID_CHAR_DEVICE_NAME).subscribe(
                readValue -> {
                    String name = new String(readValue);
                    UserError.Log.d(TAG, "Got device name: " + name);
                    MiBand.setModel(name, MiBand.getPersistentAuthMac());
                    changeNextState();
                }, throwable -> {
                    UserError.Log.e(TAG, "Could not read device name: " + throwable);
                    changeNextState();
                });
    }

    private Boolean sendBG() {
       /*  BgReading last = BgReading.last();
        AlertMessage message = new AlertMessage();
        if (last == null || last.isStale()) {
            return false;
        } else {
            String messageText = "BG: " + last.displayValue(null) + " " + last.displaySlopeArrow();
            UserError.Log.uel(TAG, "Send alert msg: " + messageText);
            if (MiBand.getMibandType() == MiBandType.MI_BAND2) {
                new QueueMe()
                        .setBytes(message.getAlertMessageOld(messageText.toUpperCase(), AlertMessage.AlertCategory.SMS_MMS))
                        .setDescription("Send alert msg: " + messageText)
                        .setQueueWriteCharacterstic(message.getCharacteristicUUID())
                        .expireInSeconds(QUEUE_EXPIRED_TIME)
                        .setDelayMs(QUEUE_DELAY)
                        .queue();
            } else {
                //new QueueMe()
                //        .setBytes(message.getAlertMessage(AlertMessage.AlertCategory.CustomHuami, AlertMessage.CustomIcon.APP_11, messageText.toUpperCase(), messageText.toUpperCase(), ))
                //        .setDescription("Send alert msg: " + messageText)
                //        .setQueueWriteCharacterstic(message.getCharacteristicUUID())
                 //       .expireInSeconds(QUEUE_EXPIRED_TIME)
                  //      .setDelayMs(QUEUE_DELAY)
                //        .queue();
                message.queueChunkedMessage(this, AlertMessage.CustomIcon.APP_11, messageText.toUpperCase(), null, messageText.toUpperCase());
            }
        }*/
        return true;
    }

    private void vibrateAlert(AlertLevelMessage.AlertLevelType level) {
        if (level == AlertLevelMessage.AlertLevelType.NoAlert) {
            new QueueMe()
                    .setBytes(COMMAND_DISABLE_CALL)
                    .setDescription("Send specific disable command for " + level)
                    .setQueueWriteCharacterstic(Const.UUID_CHARACTERISTIC_CHUNKEDTRANSFER)
                    .expireInSeconds(QUEUE_EXPIRED_TIME)
                    .setDelayMs(QUEUE_DELAY)
                    .queue();
        }

        AlertLevelMessage message = new AlertLevelMessage();
        new QueueMe()
                .setBytes(message.getAlertLevelMessage(level))
                .setDescription("Send vibrateAlert: " + level)
                .setQueueWriteCharacterstic(message.getCharacteristicUUID())
                .expireInSeconds(QUEUE_EXPIRED_TIME)
                .setDelayMs(QUEUE_DELAY)
                .queue();
    }

    private void periodicVibrateAlert(int count, int activeVibrationTime, int pauseVibrationTime) {
        AlertLevelMessage message = new AlertLevelMessage();
        new QueueMe()
                .setBytes(message.getPeriodicVibrationMessage((byte) count, (short) activeVibrationTime, (short) pauseVibrationTime))
                .setDescription(String.format("Send periodicVibrateAlert c:%d a:%d p:%d", count, activeVibrationTime, pauseVibrationTime))
                .setQueueWriteCharacterstic(message.getCharacteristicUUID())
                .expireInSeconds(QUEUE_EXPIRED_TIME)
                .setDelayMs((activeVibrationTime + pauseVibrationTime) * count)
                .queue();
    }

    private void sendSettings() {
        setNightMode();
    }

    private void queueMessage() {
        String message = queueItem.bundle.getString("message");
        AlertMessage alertMessage = new AlertMessage();
        String messageType = queueItem.getMessageType();
        switch (messageType != null ? messageType : "null") {
            case MIBAND_NOTIFY_TYPE_CALL:
                new QueueMe()
                        .setBytes(alertMessage.getAlertMessageOld(message, AlertMessage.AlertCategory.Call))
                        .setDescription("Send call alert: " + message)
                        .setQueueWriteCharacterstic(alertMessage.getCharacteristicUUID())
                        .setRunnable(() -> isWaitingCallResponse = true)
                        .expireInSeconds(QUEUE_EXPIRED_TIME)
                        .setDelayMs(QUEUE_DELAY)
                        .queue();
                UserError.Log.d(TAG, "Queued call alert: " + message);
                break;
            case MIBAND_NOTIFY_TYPE_CANCEL:
                if (isWaitingCallResponse) {
                    vibrateAlert(AlertLevelMessage.AlertLevelType.NoAlert); //disable call
                    isWaitingCallResponse = false;
                    UserError.Log.d(TAG, "Call disabled");
                }
                break;
            case MIBAND_NOTIFY_TYPE_ALARM:
                new QueueMe()
                        .setBytes(alertMessage.getAlertMessageOld(message, AlertMessage.AlertCategory.Call))
                        .setDescription("Sent glucose alert: " + message)
                        .setQueueWriteCharacterstic(alertMessage.getCharacteristicUUID())
                        .expireInSeconds(QUEUE_EXPIRED_TIME)
                        .setDelayMs(QUEUE_DELAY)
                        .queue();
                activeAlertType = queueItem.bundle.getString("type");
                missingAlertMessage = message;
                stopBgUpdateTimer();
                bgServiceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.MIBAND_SERVICE_BG_RETRY_ID, CMD_LOCAL_AFTER_ALARM);
                JoH.wakeUpIntent(HuamiXdrip.getAppContext(), CALL_ALERT_DELAY, bgServiceIntent);
                AudioManager audioManager = (AudioManager)HuamiXdrip.getAppContext().getSystemService(Context.AUDIO_SERVICE);
                ringerModeBackup = audioManager.getRingerMode();
                break;
            case MIBAND_NOTIFY_TYPE_MESSAGE:
                String title = queueItem.bundle.getString("title");
                message = message.replace("@", "");
                UserError.Log.d(TAG, "Queuing message alert: " + message);

                if (MiBand.getMibandType() == MiBandType.MI_BAND2) {
                    new QueueMe()
                            .setBytes(alertMessage.getAlertMessageOld(message, AlertMessage.AlertCategory.SMS_MMS))
                            .setDescription("Sent message: " + message)
                            .setQueueWriteCharacterstic(alertMessage.getCharacteristicUUID())
                            .expireInSeconds(QUEUE_EXPIRED_TIME)
                            .setDelayMs(MESSAGE_DELAY)
                            .queue();
                } else {
                    /*new QueueMe()
                            .setBytes(alertMessage.getAlertMessage(AlertMessage.AlertCategory.CustomHuami, AlertMessage.CustomIcon.RED_WHITE_FIRE_8, title, message))
                            .setDescription("Sent message: " + message)
                            .setQueueWriteCharacterstic(alertMessage.getCharacteristicUUID())
                            .expireInSeconds(QUEUE_EXPIRED_TIME)
                            .setDelayMs(MESSAGE_DELAY)
                            .queue();*/
                    alertMessage.queueChunkedMessage(this, AlertMessage.CustomIcon.APP_11, title, null, message);
                }
                break;
            default: // glucose
                break;
        }
        // this parent method might get called multiple times
        Inevitable.task("miband-s-queue", 200, () -> changeNextState());
    }

    @SuppressLint("CheckResult")
    private void authPhase() {
        extendWakeLock(30000);
        RxBleConnection connection = I.connection;
        UserError.Log.d(TAG, "Authorizing");
        if (I.connection == null) {
            UserError.Log.d(TAG, "Cannot enable as connection is null!");
            return;
        }

        AuthOperations authOperations = MiBandType.getAuthOperations(MiBand.getMibandType(), this);
        if (!authOperations.initAuthKey()) {
            changeState(MiBandState.AUTHORIZE_FAILED);
            return;
        }
        useV2ChunkedProtocol = authOperations.isV2Protocol();
        authSubscription = new Subscription(
                connection.setupNotification(authOperations.getCharacteristicUUID())
                        .timeout(20, TimeUnit.SECONDS)
                        .doOnNext(notificationObservable -> {
                                    UserError.Log.d(TAG, "Notification for auth enabled");
                                    authOperations.startAuthorisation();
                                }
                        )
                        .flatMap(notificationObservable -> notificationObservable)
                        .subscribe(bytes -> {
                            // incoming notifications
                            UserError.Log.d(TAG, "Received auth notification bytes: " + bytesToHex(bytes));
                            authOperations.processAuthCharacteristic(bytes);
                        }, throwable -> {
                            UserError.Log.d(TAG, "Throwable in authSubscription Notification: " + throwable);
                            if (throwable instanceof BleCharacteristicNotFoundException) {
                                // maybe legacy - ignore for now but needs better handling
                                UserError.Log.d(TAG, "Characteristic not found for notification");
                            } else if (throwable instanceof BleCannotSetCharacteristicNotificationException) {
                                UserError.Log.e(TAG, "Problems setting notifications - disconnecting");
                            } else if (throwable instanceof BleDisconnectedException) {
                                UserError.Log.d(TAG, "Disconnected while enabling notifications");
                            } else if (throwable instanceof TimeoutException) {
                                //check if it is normal timeout
                                if (!MiBand.isAuthenticated()) {
                                    String errorText = "Authentication failed due to authentication timeout. When your Band vibrates and blinks, tap it a few times in a row.";
                                    UserError.Log.d(TAG, errorText);
                                    JoH.static_toast_long(errorText);
                                }
                            }
                            unsubscribeAuthSubscription();
                            changeState(CLOSE);
                            releaseWakeLock();
                        }));
    }

    private void unsubscribeAuthSubscription(){
        if (authSubscription != null) {
            authSubscription.unsubscribe();
        }
    }

    @SuppressLint("CheckResult")
    private void installWatchface() {
        extendWakeLock(1 * Constants.MINUTE_IN_MS);
        UserError.Log.d(TAG, "Install WatchFace");
        if (I.connection == null) {
            UserError.Log.d(TAG, "Cannot enable as connection is null!");
            return;
        }
        FirmwareOperations firmware;
        try {
            MiBandType mibandType = MiBand.getMibandType();
            Version version = new Version(MiBand.getVersion());
            if (mibandType == MiBandType.AMAZFITGTR && version.compareTo(new Version("1.0.0.00")) < 0) {//probably gtr42
                mibandType = MiBandType.AMAZFITGTR_42;
            }
            WatchFaceGenerator wfGen = new WatchFaceGenerator(getBaseContext().getAssets(),
                    mibandType);
            byte[] fwArray = wfGen.genWatchFace(queueItem.bundle);
            if (fwArray == null || fwArray.length == 0) {
                resetFirmwareState(false, "Empty image");
                return;
            }
            SequenceState sequenceState;
            if (mibandType == MiBandType.MI_BAND4) {
                sequenceState = new SequenceStateMiBand4();
                firmware = new FirmwareOperations(fwArray, sequenceState, this);
            } else if (mibandType == MiBandType.MI_BAND5 || mibandType == MiBandType.AMAZFIT5) {
                sequenceState = new SequenceStateMiBand5();
                firmware = new FirmwareOperations(fwArray, sequenceState, this);
            } else if (mibandType == MiBandType.MI_BAND6) {
                    sequenceState = new SequenceStateMiBand6();
                    firmware = new FirmwareOperationsNew(fwArray, sequenceState, this);
            } else if (mibandType == MiBandType.AMAZFITGTR || mibandType == MiBandType.AMAZFITGTR_LITE) {
                if ((version.compareTo(new Version("1.3.7.16")) >= 0)) {
                    sequenceState = new SequenceStateVergeNew();
                    firmware = new FirmwareOperationsNew(fwArray, sequenceState, this);
                } else {
                    sequenceState = new SequenceStateMiBand5();
                    firmware = new FirmwareOperations(fwArray, sequenceState, this);
                }
            } else if (mibandType == MiBandType.AMAZFITGTR_42) {
                sequenceState = new SequenceStateVergeOld();
                firmware = new FirmwareOperations(fwArray, sequenceState, this);
            } else if ( mibandType == MiBandType.AMAZFITGTS2_MINI) {
                sequenceState = new SequenceStateGTS2Mini();
                firmware = new FirmwareOperationsNew(fwArray, sequenceState, this);
            } else if (mibandType == MiBandType.AMAZFITGTS || mibandType == MiBandType.AMAZFIT_TREX) {
                if (version.compareTo(new Version("0.1.1.16")) >= 0) {
                    sequenceState = new SequenceStateVergeNew();
                    firmware = new FirmwareOperationsNew(fwArray, sequenceState, this);
                } else {
                    sequenceState = new SequenceStateMiBand5();
                    firmware = new FirmwareOperations(fwArray, sequenceState, this);
                }
            } else if (MiBandType.isVerge2(mibandType)) {
                sequenceState = new SequenceStateVerge2();
                firmware = new FirmwareOperationsNew(fwArray, sequenceState, this);
            } else if (MiBandType.isBip(mibandType)) {
                sequenceState = new SequenceStateBip();
                firmware = new FirmwareOperationOld(fwArray, sequenceState, this);
            } else if (MiBandType.isBipS(mibandType)) {
                if (!(version.compareTo(new Version("4.0.0.00")) >= 0) && (version.compareTo(new Version("2.1.1.50")) >= 0) || (version.compareTo(new Version("4.1.5.55")) >= 0)) {
                    sequenceState = new SequenceStateBipS();
                    firmware = new FirmwareOperationsNew(fwArray, sequenceState, this);
                } else {
                    sequenceState = new SequenceStateMiBand5();
                    firmware = new FirmwareOperations(fwArray, sequenceState, this);
                }
            } else {
                resetFirmwareState(false, "Not supported band type");
                return;
            }
        } catch (Exception e) {
            resetFirmwareState(false, "FirmwareOperations error " + e.getMessage());
            return;
        }
        UserError.Log.d(TAG, "Begin uploading Watchface, length: " + firmware.getSize());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            UserError.Log.d(TAG, "Requesting high priority connection");
            requestConnectionPriority(I.connection, BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        }
        int mtu = I.connection.getMtu();
        if (!MiBandEntry.isNeedToDisableHightMTU())
            setMTU(mtu);
        firmware.nextSequence();
        firmware.processFirmwareSequence();
    }

    public void resetFirmwareState(Boolean result, String customText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestConnectionPriority(I.connection, BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
        }
        emptyQueue();
        String finishText = customText;
        if (customText == null) {
            if (!result)
                finishText = HuamiXdrip.getAppContext().getResources().getString(R.string.miband_watchface_istall_error);
            else
                finishText = HuamiXdrip.getAppContext().getResources().getString(R.string.miband_watchface_istall_success);
        }
        UserError.Log.d(TAG, "resetFirmwareState result:" + result + ":" + finishText);

        if (!result) {
            prevReadingStatusIsStale = false; //try to resend readings on the next bg update
        }

        if (I.state.equals(CLOSED) || I.state.equals(CLOSE) || I.isConnected == false) return;
        changeState(MiBandState.RESTORE_NIGHTMODE);
    }

    @RequiresApi(26)
    private Observable<ConnectionParameters> requestConnectionPriority(RxBleConnection rxBleConnection, int priority) {
        return Observable.merge(
                rxBleConnection.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH, 1, TimeUnit.MILLISECONDS).toObservable(),
                rxBleConnection.observeConnectionParametersUpdates().take(1)
        );
    }


    @SuppressLint("CheckResult")
    private void setNightMode() {
        UserError.Log.d(TAG, "Restore night mode");
        Date start = null, end = null;
        DisplayControllMessage.NightMode nightMode = DisplayControllMessage.NightMode.Off;
        if (MiBandEntry.isNightModeEnabled()) {
            nightMode = DisplayControllMessage.NightMode.Sheduled;
            start = MiBandEntry.getNightModeStart();
            end = MiBandEntry.getNightModeEnd();
        }
        RxBleConnection connection = I.connection;
        DisplayControllMessage dispControl = new DisplayControllMessage();
        isNeedToRestoreNightMode = false;

        writeToConfiguration(dispControl.setNightModeCmd(nightMode, start, end));
    }

    private void handleHeartrate(byte[] value) {
        if (value.length == 2 && value[0] == 0) {
            int hrValue = (value[1] & 0xff);
            HuamiXdrip.getAppContext().startService(new Intent(HuamiXdrip.getAppContext(), BroadcastService.class).putExtra(INTENT_FUNCTION_KEY, CMD_ADD_HR).putExtra("value", hrValue));
        }
    }

    private void handleRealtimeSteps(byte[] value) {
        if (value == null) {
            return;
        }
        if (value.length == 13) {
            byte[] stepsValue = new byte[]{value[1], value[2]};
            int steps = FirmwareOperations.toUint16(stepsValue);
            HuamiXdrip.getAppContext().startService(new Intent(HuamiXdrip.getAppContext(), BroadcastService.class).putExtra(INTENT_FUNCTION_KEY, CMD_ADD_STEPS).putExtra("value", steps));
        } else {
            UserError.Log.d(TAG, "Unrecognized realtime steps value: " + bytesToHex(value));
        }
    }

    @SuppressLint("CheckResult")
    private void enableNotifications() {
        UserError.Log.d(TAG, "enableNotifications called");
        if (I.connection == null) {
            UserError.Log.d(TAG, "Cannot enable as connection is null!");
            return;
        }
        if (I.isNotificationEnabled) {
            UserError.Log.d(TAG, "Notifications already enabled");
            changeNextState();
            return;
        }


        enableHeartRateNotification();
        enableStepsNotification();

        if (notifSubscriptionDeviceEvent != null) {
            notifSubscriptionDeviceEvent.unsubscribe();
        }
        UserError.Log.d(TAG, "Requesting to enable device event notifications");

        I.connection.requestMtu(PREFERRED_MTU_SIZE).subscribe();

        notifSubscriptionDeviceEvent = new Subscription(I.connection.setupNotification(Const.UUID_CHARACTERISTIC_DEVICEEVENT)
                .doOnNext(notificationObservable -> {
                    I.isNotificationEnabled = true;
                    changeNextState();
                }).flatMap(notificationObservable -> notificationObservable)
                //.timeout(5, TimeUnit.SECONDS)
                .observeOn(Schedulers.newThread())
                .subscribe(bytes -> {
                            // incoming notifications
                            UserError.Log.d(TAG, "Received device notification bytes: " + bytesToHex(bytes));
                            handleDeviceEvent(bytes);
                        }, throwable -> {
                            UserError.Log.d(TAG, "Throwable in notifSubscriptionDeviceEvent notification: " + throwable);
                            I.isNotificationEnabled = false;
                            if (throwable instanceof BleCharacteristicNotFoundException) {
                                // maybe legacy - ignore for now but needs better handling
                                UserError.Log.d(TAG, "Characteristic not found for notification");
                                changeNextState();
                            } else {
                                UserError.Log.d(TAG, "Disconnected exception");
                                isNeedToAuthenticate = true;
                                messageQueue.clear();
                                changeState(CLOSE);
                            }
                        }
                ));
    }

    private void enableHeartRateNotification() {
        if (MiBandEntry.isNeedToCollectHR()) {
            if (notifSubscriptionHeartRateMeasurement != null) return;
        } else {
            if (notifSubscriptionHeartRateMeasurement != null) {
                notifSubscriptionHeartRateMeasurement.unsubscribe();
                notifSubscriptionHeartRateMeasurement = null;
                return;
            }
        }

        UserError.Log.d(TAG, "Requesting to enable HR notifications");

        notifSubscriptionHeartRateMeasurement = new Subscription(I.connection.setupNotification(Const.UUID_CHAR_HEART_RATE_MEASUREMENT)
                .flatMap(notificationObservable -> notificationObservable)
                .observeOn(Schedulers.newThread())
                .subscribe(bytes -> {
                            // incoming notifications
                            /*if (d)
                                UserError.Log.d(TAG, "Received HR notification bytes: " + bytesToHex(bytes));*/
                            handleHeartrate(bytes);
                        }, throwable -> {
                            notifSubscriptionHeartRateMeasurement.unsubscribe();
                            notifSubscriptionHeartRateMeasurement = null;
                            UserError.Log.d(TAG, "Throwable in HR notification: " + throwable);
                            if (throwable instanceof BleCharacteristicNotFoundException) {
                                UserError.Log.d(TAG, "HR Characteristic not found for notification");
                            } else {
                                UserError.Log.d(TAG, "HR Disconnected exception");
                            }
                        }
                ));
    }

    private void enableStepsNotification() {
        if (MiBandEntry.isNeedToCollectSteps()) {
            if (notifSubscriptionStepsMeasurement != null) return;
        } else {
            if (notifSubscriptionStepsMeasurement != null) {
                notifSubscriptionStepsMeasurement.unsubscribe();
                notifSubscriptionStepsMeasurement = null;
                return;
            }
        }

        UserError.Log.d(TAG, "Requesting to enable steps notifications");

        notifSubscriptionStepsMeasurement = new Subscription(I.connection.setupNotification(Const.UUID_CHARACTERISTIC_7_REALTIME_STEPS)
                .flatMap(notificationObservable -> notificationObservable)
                .observeOn(Schedulers.newThread())
                .subscribe(bytes -> {
                            // incoming notifications
                            /*if (d)
                                UserError.Log.d(TAG, "Received steps notification bytes: " + bytesToHex(bytes));*/
                            handleRealtimeSteps(bytes);
                        }, throwable -> {
                            notifSubscriptionStepsMeasurement.unsubscribe();
                            notifSubscriptionStepsMeasurement = null;
                            UserError.Log.d(TAG, "Throwable in steps notification: " + throwable);
                            if (throwable instanceof BleCharacteristicNotFoundException) {
                                UserError.Log.d(TAG, "steps Characteristic not found for notification");
                            } else {
                                UserError.Log.d(TAG, "steps Disconnected exception");
                            }
                        }
                ));
    }

    @Override
    protected synchronized boolean automata() {
        UserError.Log.d(TAG, "Automata called in" + TAG);
        extendWakeLock(2000);
        if (shouldServiceRun()) {
            switch (I.state) {
                case INIT:
                    // connect by default
                    changeNextState();
                    break;
                case MiBandState.GET_MODEL_NAME:
                    cancelRetryTimer();
                    if (isNeedToRestoreNightMode) {
                        setNightMode();
                    }
                    if (MiBand.getModel().isEmpty()) {
                        getModelName();
                    } else changeNextState();
                    break;
                case MiBandState.GET_SOFT_REVISION:
                    if (MiBand.getVersion().isEmpty() || isNeedToCheckRevision)
                        getSoftwareRevision();
                    else changeNextState();
                    break;
                case MiBandState.AUTHENTICATE:
                    if (isNeedToAuthenticate) {
                        changeState(MiBandState.AUTHORIZE);
                    } else {
                        changeState(MiBandState.ENABLE_NOTIFICATIONS);
                    }
                    break;
                case MiBandState.AUTHORIZE:
                    authPhase();
                    break;
                case MiBandState.ENABLE_NOTIFICATIONS:
                    isNeedToAuthenticate = false;
                    unsubscribeAuthSubscription();
                    enableNotifications();
                    break;
                case MiBandState.SEND_SETTINGS:
                    sendSettings();
                    changeNextState();
                    break;
                case MiBandState.SEND_BG:
                    if (!MiBandEntry.isNeedSendReading()) {
                        changeState(MiBandState.SEND_QUEUE);
                        break;
                    }
                    final String bgAsNotification = queueItem.functionName;
                    if (!MiBandType.supportGraph(MiBand.getMibandType())
                            || MiBandEntry.isNeedSendReadingAsNotification()
                            || bgAsNotification.equals("update_bg_as_notification")) {
                        Boolean result = sendBG();
                        if (result) changeState(MiBandState.VIBRATE_AFTER_READING);
                        else changeState(MiBandState.SEND_QUEUE);
                        break;
                    }
                    changeState(MiBandState.INSTALL_WATCHFACE);
                    break;
                case MiBandState.INSTALL_WATCHFACE:
                    installWatchface();
                    changeNextState();
                    break;
                case MiBandState.INSTALL_WATCHFACE_IN_PROGRESS:
                    break;
                case MiBandState.INSTALL_WATCHFACE_FINISHED:
                    break;
                case MiBandState.RESTORE_NIGHTMODE:
                    if (isNeedToRestoreNightMode) {
                        // do nothing because something happen with connection while sending nightmode
                        extendWakeLock(RESTORE_NIGHT_MODE_DELAY * Constants.SECOND_IN_MS);
                        JoH.threadSleep(RESTORE_NIGHT_MODE_DELAY);
                        if (I.state.equals(CLOSED) || I.state.equals(CLOSE) || !I.isConnected)
                            break;
                        setNightMode();
                        if (I.state.equals(CLOSED) || I.state.equals(CLOSE) || !I.isConnected)
                            break;
                    }
                    changeNextState();
                    break;
                case MiBandState.VIBRATE_AFTER_READING:
                    if (MiBandEntry.isVibrateOnReadings() && !MiBandEntry.isNeedSendReadingAsNotification())
                        vibrateAlert(AlertLevelMessage.AlertLevelType.VibrateAlert);
                    changeNextState();
                    break;
                case MiBandState.GET_BATTERY_INFO:
                    getBatteryInfo();
                    changeNextState();
                    break;
                case MiBandState.QUEUE_MESSAGE:
                    queueMessage();
                    break;
                case MiBandState.WAITING_USER_RESPONSE:
                    break;
                case MiBandState.WAITING_MIFIT_SILENCE:
                    //restore ringer mode because mifit won't restore it
                    extendWakeLock(2 * Constants.SECOND_IN_MS);
                    JoH.threadSleep((Constants.SECOND_IN_MS));
                    AudioManager audioManager = (AudioManager)HuamiXdrip.getAppContext().getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setRingerMode(ringerModeBackup);
                    startBgTimer();
                    changeNextState();
                    break;
                case MiBandState.AUTHORIZE_FAILED:
                    unsubscribeAuthSubscription();
                    changeNextState();
                    break;
                case SLEEP:
                    handleCommand();
                    break;
                case CLOSED:
                    stopConnection();
                    return super.automata();
                default:
                    return super.automata();
            }
        } else {
            UserError.Log.d(TAG, "Service should not be running inside automata");
            stopSelf();
        }
        return true; // lies
    }

    private void stopConnection() {
        isNeedToAuthenticate = true;
        isWaitingCallResponse = false;
        messageQueue.clear();
        handle = 0;
        setMTU(GATT_MTU_MINIMUM); //reset mtu
        setRetryTimerReal(); // local retry strategy
    }

    @Override
    public void resetBluetoothIfWeSeemToAlreadyBeConnected(String mac) {
        //super.resetBluetoothIfWeSeemToAlreadyBeConnected(mac); //do not reset
    }

    private boolean shouldServiceRun() {
        return MiBandEntry.isEnabled();
    }

    @Override
    protected void setRetryTimerReal() {
        if (shouldServiceRun() && MiBand.isAuthenticated()) {
            final long retry_in = whenToRetryNext();
            UserError.Log.d(TAG, "setRetryTimerReal: Restarting in: " + (retry_in / Constants.SECOND_IN_MS) + " seconds");
            I.serviceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.MIBAND_SERVICE_RETRY_ID, "message");
            I.retry_time = JoH.wakeUpIntent(HuamiXdrip.getAppContext(), retry_in, I.serviceIntent);
            I.wakeup_time = JoH.tsl() + retry_in;
        } else {
            UserError.Log.d(TAG, "Not setting retry timer as service should not be running");
        }
    }

    private void cancelRetryTimer() {
        JoH.cancelAlarm(HuamiXdrip.getAppContext(), I.serviceIntent);
        I.wakeup_time = 0;
    }

    private long whenToRetryNext() {
        I.retry_backoff = RETRY_PERIOD_MS;
        return I.retry_backoff;
    }

    public RxBleConnection getConection() {
        return I.connection;
    }

    public void writeToConfiguration(byte[] data) {
        if (useV2ChunkedProtocol) {
            data = ArrayUtils.insert(0, data, (byte) 1);
            writeToChunkedV2(OperationCodes.CHUNKED_V2_ENDPOINT_COMPAT, getNextHandle(), data, true);
        } else {
            I.connection.writeCharacteristic(Const.UUID_CHARACTERISTIC_3_CONFIGURATION, data)
                    .subscribe(val -> {
                                UserError.Log.d(TAG, "Wrote configuration command: " + bytesToHex(val));
                            },
                            throwable -> {
                                UserError.Log.e(TAG, "Could not write configuration command: " + throwable);
                            }
                    );
        }
    }

    public void writeChunked(int type, byte[] data) {
        if (useV2ChunkedProtocol && type > 0) {
            boolean encrypt = true;
            if (type == 1 && (data[1] == 2)) { // don't encypt current weather
                encrypt = false;
            }

            byte[] command = ArrayUtils.addAll(new byte[]{0x00, 0x00, (byte) (0xc0 | type), 0x00}, data);
            writeToChunkedV2(OperationCodes.CHUNKED_V2_ENDPOINT_COMPAT, getNextHandle(), command, encrypt);
        } else {
            writeChunkedV1(type, data);
        }
    }

    public void writeToChunkedV2(int type, byte handle, byte[] data, boolean encrypt) {
        int remaining = data.length;
        int length = data.length;
        byte count = 0;
        int header_size = 11;

        if (encrypt) {
            byte[] messagekey = new byte[16];
            for (int i = 0; i < 16; i++) {
                messagekey[i] = (byte) (sharedSessionKey[i] ^ handle);
            }
            int encrypted_length = length + 8;
            int overflow = encrypted_length % 16;
            if (overflow > 0) {
                encrypted_length += (16 - overflow);
            }

            byte[] encryptable_payload = new byte[encrypted_length];
            System.arraycopy(data, 0, encryptable_payload, 0, length);
            encryptable_payload[length] = (byte) (encryptedSequenceNr & 0xff);
            encryptable_payload[length + 1] = (byte) ((encryptedSequenceNr >> 8) & 0xff);
            encryptable_payload[length + 2] = (byte) ((encryptedSequenceNr >> 16) & 0xff);
            encryptable_payload[length + 3] = (byte) ((encryptedSequenceNr >> 24) & 0xff);
            encryptedSequenceNr++;
            int checksum = (int) JoH.checksum(encryptable_payload, 0, length + 4);
            encryptable_payload[length + 4] = (byte) (checksum & 0xff);
            encryptable_payload[length + 5] = (byte) ((checksum >> 8) & 0xff);
            encryptable_payload[length + 6] = (byte) ((checksum >> 16) & 0xff);
            encryptable_payload[length + 7] = (byte) ((checksum >> 24) & 0xff);
            remaining = encrypted_length;
            try {
                data = AuthOperations.encryptAES(encryptable_payload, messagekey);
            } catch (Exception e) {
                UserError.Log.e(TAG, "error while encrypting");
                return;
            }

        }

        while (remaining > 0) {
            int MAX_CHUNKLENGTH = getMTU() - GATT_WRITE_MTU_OVERHEAD - header_size;
            int copybytes = Math.min(remaining, MAX_CHUNKLENGTH);
            byte[] chunk = new byte[copybytes + header_size];

            byte flags = 0;
            if (encrypt) {
                flags |= 0x08;
            }
            if (count == 0) {
                flags |= 0x01;
                chunk[5] = (byte) (length & 0xff);
                chunk[6] = (byte) ((length >> 8) & 0xff);
                chunk[7] = (byte) ((length >> 16) & 0xff);
                chunk[8] = (byte) ((length >> 24) & 0xff);
                chunk[9] = (byte) (type & 0xff);
                chunk[10] = (byte) ((type >> 8) & 0xff);
            }
            if (remaining <= MAX_CHUNKLENGTH) {
                flags |= 0x06; // last chunk?
            }
            chunk[0] = 0x03;
            chunk[1] = flags;
            chunk[2] = 0;
            chunk[3] = handle;
            chunk[4] = count;

            System.arraycopy(data, data.length - remaining, chunk, header_size, copybytes);
           /* new QueueMe()
                    .setBytes(chunk)
                    .setDescription("Sent chunk")
                    .setQueueWriteCharacterstic(Const.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_V2_WRITE)
                    .expireInSeconds(QUEUE_EXPIRED_TIME)
                    .setDelayMs(QUEUE_DELAY)
                    .queue();*/

            I.connection.writeCharacteristic(Const.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_V2_WRITE, chunk).subscribe(val -> {
                        UserError.Log.d(TAG, "Wrote Chunk:" + bytesToHex(val));
                    },
                    throwable -> {
                        UserError.Log.e(TAG, "Could not write Chunk: " + throwable);
                    }
            );
            remaining -= copybytes;
            header_size = 5;
            count++;
        }

       // startQueueSend();
    }

    public void writeChunkedV1(int type, byte[] data) {
        final int MAX_CHUNKLENGTH = getMTU() - 6;
        int remaining = data.length;
        byte count = 0;
        while (remaining > 0) {
            int copybytes = Math.min(remaining, MAX_CHUNKLENGTH);
            byte[] chunk = new byte[copybytes + 3];

            byte flags = 0;
            if (remaining <= MAX_CHUNKLENGTH) {
                flags |= 0x80; // last chunk
                if (count == 0) {
                    flags |= 0x40; // weird but true
                }
            } else if (count > 0) {
                flags |= 0x40; // consecutive chunk
            }

            chunk[0] = 0;
            chunk[1] = (byte) (flags | type);
            chunk[2] = (byte) (count & 0xff);

            System.arraycopy(data, count++ * MAX_CHUNKLENGTH, chunk, 3, copybytes);
            new QueueMe()
                    .setBytes(chunk)
                    .setDescription("Sent chunk")
                    .setQueueWriteCharacterstic(Const.UUID_CHARACTERISTIC_CHUNKEDTRANSFER)
                    .expireInSeconds(QUEUE_EXPIRED_TIME)
                    .setDelayMs(QUEUE_DELAY)
                    .queue();
            remaining -= copybytes;
        }
    }

    public enum MIBAND_INTEND_STATES {
        UPDATE_PREF_SCREEN,
        UPDATE_PREF_DATA
    }

    public static class MiBandState extends JamBaseBluetoothSequencer.BaseState {
        static final String SEND_BG = "Setting Time";
        static final String SEND_SETTINGS = "Updating Settings";
        static final String QUEUE_MESSAGE = "Queue message";
        static final String WAITING_USER_RESPONSE = "WAITING_USER_RESPONSE";
        static final String WAITING_MIFIT_SILENCE = "Waiting Mifit Silence";
        static final String AUTHENTICATE = "Authenticate";
        static final String AUTHORIZE = "Authorize phase";
        public static final String AUTHORIZE_FAILED = "Authorization failed handle";
        static final String AUTHORIZE_FAILED_SLEEP = "Authorization failed sleep";
        static final String GET_MODEL_NAME = "Getting model name";
        static final String GET_SOFT_REVISION = "Getting software revision";
        static final String ENABLE_NOTIFICATIONS = "Enable notification";
        static final String GET_BATTERY_INFO = "Getting battery info";
        static final String INSTALL_WATCHFACE = "Watchface installation";
        static final String INSTALL_WATCHFACE_IN_PROGRESS = "Watchface installation in progress";
        static final String INSTALL_WATCHFACE_FINISHED = "Watchface installation finished";
        static final String RESTORE_NIGHTMODE = "RESTORE_NIGHTMODE";
        static final String VIBRATE_AFTER_READING = "Vibrate";

        private static final String TAG = "MiBandStateSequence";

        void prepareInitialSequences() {
            sequence.clear();
            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(GET_MODEL_NAME);
            sequence.add(GET_SOFT_REVISION);
            sequence.add(AUTHENTICATE);
            sequence.add(AUTHORIZE);
            sequence.add(ENABLE_NOTIFICATIONS);
        }

        void prepareFinalSequences() {
            sequence.add(SEND_QUEUE);
            sequence.add(GET_BATTERY_INFO);
            sequence.add(SLEEP);
            sequence.add(AUTHORIZE_FAILED);
            sequence.add(AUTHORIZE_FAILED_SLEEP);
        }

        void setSendReadingSequence() {
            UserError.Log.d(TAG, "SET UPDATE WATCHFACE DATA SEQUENCE");
            prepareInitialSequences();
            sequence.add(SEND_BG);
            sequence.add(INSTALL_WATCHFACE);
            sequence.add(INSTALL_WATCHFACE_IN_PROGRESS);
            sequence.add(INSTALL_WATCHFACE_FINISHED);
            sequence.add(RESTORE_NIGHTMODE);
            sequence.add(VIBRATE_AFTER_READING);
            prepareFinalSequences();
        }

        void setAlarmSequence() {
            UserError.Log.d(TAG, "SET ALARM SEQUENCE");
            prepareInitialSequences();
            sequence.add(QUEUE_MESSAGE);
            sequence.add(SEND_QUEUE);
            sequence.add(WAITING_USER_RESPONSE);
            sequence.add(WAITING_MIFIT_SILENCE);
            sequence.add(SLEEP);
            sequence.add(AUTHORIZE_FAILED);
        }

        void setQueueSequence() {
            UserError.Log.d(TAG, "SET QUEUE SEQUENCE");
            prepareInitialSequences();
            sequence.add(QUEUE_MESSAGE);
            prepareFinalSequences();
        }

        void setSettingsSequence() {
            UserError.Log.d(TAG, "SET SETTINGS SEQUENCE");
            prepareInitialSequences();
            sequence.add(SEND_SETTINGS);
            prepareFinalSequences();
        }
    }

    public QueueMessage getMessageQueue(String title, String message){
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("message", message);
        QueueMessage msg = new QueueMessage( CMD_MESSAGE, bundle);
        msg.setMessageType(MIBAND_NOTIFY_TYPE_MESSAGE);

        return msg;
    }

    public class QueueMessage {
        @Getter
        private String functionName;

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }

        private String messageType = "";
        @Getter
        private long expireAt;

        @Getter
        private Bundle bundle;

        public QueueMessage(String functionName) {
            this.functionName = functionName;
            setExpire();
        }

        public QueueMessage(String functionName, Bundle bundle) {
            this.functionName = functionName;
            this.bundle = bundle;
            setExpire();
        }

        private void setExpire(){
            this.expireAt = JoH.tsl() + (Constants.MINUTE_IN_MS * 10);
        }

        boolean isExpired() {
            return expireAt != 0 && expireAt < JoH.tsl();
        }
    }
}
