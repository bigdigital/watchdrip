package com.thatguysservice.huami_xdrip.watch.miband;

import static com.polidea.rxandroidble2.RxBleConnection.GATT_MTU_MAXIMUM;
import static com.polidea.rxandroidble2.RxBleConnection.GATT_MTU_MINIMUM;
import static com.polidea.rxandroidble2.RxBleConnection.GATT_WRITE_MTU_OVERHEAD;
import static com.thatguysservice.huami_xdrip.HuamiXdrip.gs;
import static com.thatguysservice.huami_xdrip.models.Helper.bytesToHex;
import static com.thatguysservice.huami_xdrip.models.Helper.emptyString;
import static com.thatguysservice.huami_xdrip.services.BaseBluetoothSequencer.BaseState.CLOSE;
import static com.thatguysservice.huami_xdrip.services.BaseBluetoothSequencer.BaseState.CLOSED;
import static com.thatguysservice.huami_xdrip.services.BaseBluetoothSequencer.BaseState.INIT;
import static com.thatguysservice.huami_xdrip.services.BaseBluetoothSequencer.BaseState.SLEEP;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_ADD_HR;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_ADD_STEPS;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_ADD_TREATMENT;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_ALERT;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_CANCEL_ALERT;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_AFTER_MISSING_ALARM;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_BG_FORCE_REMOTE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_REFRESH;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_UPDATE_BG_AS_NOTIFICATION;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_WATCHDOG;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_XDRIP_APP_GOT_RESPONSE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_XDRIP_APP_NO_RESPONSE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_MESSAGE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_REPLY_MSG;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_SNOOZE_ALERT;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_STAT_INFO;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG_FORCE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.INTENT_FUNCTION_KEY;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.INTENT_REPLY_CODE_OK;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.bgForce;
import static com.thatguysservice.huami_xdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_ALARM;
import static com.thatguysservice.huami_xdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_CALL;
import static com.thatguysservice.huami_xdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_CANCEL;
import static com.thatguysservice.huami_xdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_MESSAGE;
import static com.thatguysservice.huami_xdrip.watch.miband.Const.PREFERRED_MTU_SIZE;
import static com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry.isForceNewProtocol;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_ACK_FIND_PHONE_IN_PROGRESS;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_DISABLE_CALL;

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
import com.thatguysservice.huami_xdrip.models.BgData;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.DeviceInfo;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.StatisticInfo;
import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.models.webservice.WebServiceData;
import com.thatguysservice.huami_xdrip.repository.BgDataRepository;
import com.thatguysservice.huami_xdrip.services.BaseBluetoothSequencer;
import com.thatguysservice.huami_xdrip.services.BroadcastService;
import com.thatguysservice.huami_xdrip.services.XiaomiWearService;
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
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperation;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperations2020;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperationsNew;
import com.thatguysservice.huami_xdrip.watch.miband.message.AlertLevelMessage;
import com.thatguysservice.huami_xdrip.watch.miband.message.AlertMessage;
import com.thatguysservice.huami_xdrip.watch.miband.message.DeviceEvent;
import com.thatguysservice.huami_xdrip.watch.miband.message.DisplayControllMessage;
import com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes;
import com.thatguysservice.huami_xdrip.webservice.WebServer;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import lombok.Getter;

/**
 * <p>
 * Data communication with MiBand compatible bands/watches
 */

public class MiBandService extends BaseBluetoothSequencer {
    protected static final int MINIMUM_BATTERY_LEVEL = 12;

    static final List<UUID> huntCharacterstics = new ArrayList<>();
    private static final long RETRY_PERIOD_MS = Constants.SECOND_IN_MS * 30; // sleep for max ms if we have had no signal
    private static final long BG_UPDATE_NO_DATA_INTERVAL = 30 * Constants.MINUTE_IN_MS; //minutes
    private static final long BG_WEB_SERVER_NO_DATA_INTERVAL = 12 * Constants.MINUTE_IN_MS; //minutes
    private static final long CONNECTION_TIMEOUT = 5 * Constants.MINUTE_IN_MS; //minutes
    private static final long RESTORE_NIGHT_MODE_DELAY = (Constants.SECOND_IN_MS * 7);
    private static final int QUEUE_EXPIRED_TIME = 30; //second
    private static final int QUEUE_DELAY = 0; //ms
    private static final int CALL_ALERT_DELAY = (int) (Constants.SECOND_IN_MS * 10);
    private static final int WATCHDOG_DELAY = (int) (Constants.MINUTE_IN_MS * 5);
    private static final int MESSAGE_DELAY = (int) (Constants.SECOND_IN_MS * 5);
    private static final int CGI_WAIT_TIMEOUT = (int) (Constants.SECOND_IN_MS * 5);
    private static final int CGI_WAIT_DELAY = 100;
    static BatteryInfo batteryInfo = new BatteryInfo();
    static private long bgWakeupTime;

    static {
        huntCharacterstics.add(Const.UUID_CHAR_HEART_RATE_MEASUREMENT);
    }

    private final PoorMansConcurrentLinkedDeque<QueueMessage> messageQueue = new PoorMansConcurrentLinkedDeque<>();
    public boolean useV2ChunkedProtocol = false;
    public Boolean isNeedToRestoreNightMode = false;
    public byte[] sharedSessionKey;
    public int encryptedSequenceNr;
    public byte handle = 0;
    private int mMTU = GATT_MTU_MINIMUM;
    private Subscription authSubscription;
    private Subscription notifSubscriptionDeviceEvent;
    private Subscription notifSubscriptionHeartRateMeasurement;
    private Subscription notifSubscriptionStepsMeasurement;
    private Boolean isNeedToCheckRevision = true;
    private Boolean isNeedToAuthenticate = true;
    private boolean isWaitingCallResponse = false;
    private boolean isWaitingAddTreatmentResponse = false;
    private boolean isNightMode = false;
    private MediaPlayer player;
    private PendingIntent bgServiceIntent;
    private PendingIntent watchdogIntent;
    private QueueMessage queueItem;
    private boolean prevReadingStatusIsStale = false;
    private String activeAlertType;
    private String missingAlertMessage;
    private int ringerModeBackup;
    private BgData bgData;
    private BgData bgDataLatest;
    private DeviceInfo deviceInfo = new DeviceInfo();
    private BgDataRepository bgDataRepository;
    private Bundle latestBgDataBundle;
    private WebServer webServer;
    private boolean isConnectionStopped = true;
    private WebServer.CommonGatewayInterface CGI_getInfoResponse = new WebServer.CommonGatewayInterface() {
        @Override
        public String run(Map<String, List<String>> params) {
            UserError.Log.d(TAG, "CGI_getInfoResponse");
            if (latestBgDataBundle == null || bgDataLatest == null) {
                return "{}";
            }
            boolean includeGraph = false;
            if (params.containsKey("graph")) {
                List<String> graph = params.get("graph");
                if (graph.get(0).equals("1")) {
                    includeGraph = true;
                }
            }
            return new WebServiceData(bgDataLatest, latestBgDataBundle, includeGraph).getGson();
        }
    };

    private WebServer.CommonGatewayInterface CGI_addTreatments = new WebServer.CommonGatewayInterface() {
        @Override
        public String run(Map<String, List<String>> params) throws Exception {
            UserError.Log.d(TAG, "CGI_addTreatments");
            Double carbs = 0.0;
            Double insulin = 0.0;
            try {
                carbs = Double.valueOf(params.get("carbs").get(0));
            } catch (Exception e) {
            }

            try {
                insulin = Double.valueOf(params.get("insulin").get(0));
            } catch (Exception e) {
            }
            if (addTreatment(carbs, insulin)) {
                isWaitingAddTreatmentResponse = true;
                int i = 0;
                while (i < CGI_WAIT_TIMEOUT) {
                    if (!isWaitingAddTreatmentResponse) {
                        isWaitingAddTreatmentResponse = false;
                        return "Treatment added";
                    }
                    Helper.threadSleep(CGI_WAIT_DELAY);
                    i = i + CGI_WAIT_DELAY;
                }
                isWaitingAddTreatmentResponse = false;
                throw new TimeoutException("Timed out after waiting response from xdrip " + CGI_WAIT_TIMEOUT + " mseconds");
            }
            throw new Exception("Parameters not specified");
        }
    };

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

    public BgDataRepository getBgDataRepository() {
        return bgDataRepository;
    }

    public byte getNextHandle() {
        handle++;
        UserError.Log.e(TAG, "Handle number: " + handle);
        return handle;
    }

    public int getMTU() {
        return mMTU;
    }

    public void setMTU(int mMTU) {
        if (MiBandEntry.isNeedToDisableHightMTU()) {
            UserError.Log.e(TAG, "High MTU is not allowed, ignoring");
            this.mMTU = GATT_MTU_MINIMUM;
            return;
        }
        this.mMTU = mMTU;
        if (this.mMTU > GATT_MTU_MAXIMUM) this.mMTU = GATT_MTU_MAXIMUM;
        if (this.mMTU < GATT_MTU_MINIMUM) this.mMTU = GATT_MTU_MINIMUM;
    }

    @Override
    public void onCreate() {
        UserError.Log.e(TAG, "Creating service ");
        bgDataRepository = BgDataRepository.getInstance();
        updateWebServer();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        UserError.Log.e(TAG, "Killing service ");
        stopWebServer();
        super.onDestroy();
    }

    private boolean readyToProcessCommand(String function) {
        boolean result = I.state.equals(SLEEP) || I.state.equals(CLOSED) || I.state.equals(CLOSE) || I.state.equals(INIT) || I.state.equals(MiBandState.CONNECT_NOW);
        if (!result && function.equals(CMD_LOCAL_WATCHDOG)) {
            stopConnection();
            changeState(SLEEP);
            UserError.Log.e(TAG, "Watchdog!!!");
            return true;
        }
        if (!result && I.state.equals(MiBandState.AUTHORIZE_FAILED_SLEEP) && MiBandType.supportPairingKey(MiBand.getMibandType())) {
            return true;
        }
        if (!isConnected()) {
            return true;
        }
        if (!result)
            UserError.Log.d(TAG, "readyToProcessCommand not ready because state :" + I.state.toString());
        return result;
    }

    private void checkDeviceAuthState() {
        final String authMac = MiBand.getPersistentAuthMac();
        String macPref = MiBand.getMacPref();
        deviceInfo.setDevice(MiBand.getMibandType());
        if (!MiBand.isAuthenticated()) {
            isNeedToAuthenticate = true;
        } else if (!authMac.equalsIgnoreCase(macPref)) { //flush old auth info for new devie
            String model = MiBand.getModel();
            MiBand.setPersistentAuthMac("");
            UserError.Log.d(TAG, "Found new device: " + deviceInfo.getDeviceName());
            isNeedToAuthenticate = true;
        } else {
            String authPrefKey = MiBand.getAuthKeyPref();
            String authPersistantKey = MiBand.getPersistentAuthKey();

            if (!authPersistantKey.equalsIgnoreCase(authPrefKey) || authPersistantKey.isEmpty()) {
                MiBand.setPersistentAuthMac(""); //flush old auth info
                isNeedToAuthenticate = true;
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final PowerManager.WakeLock wl = Helper.getWakeLock("Miband service", 60000);
        try {
            if (shouldServiceRun()) {
                String function = null;
                if (intent != null) {
                    function = intent.getStringExtra(INTENT_FUNCTION_KEY);
                    if (function != null) {
                        UserError.Log.d(TAG, "onStartCommand, function:" + function);
                        if (!handleGlobalCommand(function, intent.getExtras())) {
                            resetWatchdog();
                            return START_STICKY;
                        }
                    }
                }

                if (MiBandEntry.isDeviceEnabled()) {
                    isConnectionStopped = false;
                    checkDeviceAuthState();
                    String macPref = MiBand.getMacPref();
                    if (emptyString(macPref)) {
                        // if mac not set then start up a scan and do nothing else
                        new FindNearby().scan(getBgDataRepository());
                    } else {
                        if (!setAddress(macPref)) {
                            return START_STICKY;
                        }
                        if (function != null) {
                            if (function.equals(CMD_LOCAL_REFRESH) && !Helper.pratelimit("miband-refresh-" + macPref, 5)) {
                                return START_STICKY;
                            } else {
                                if (function.equals(CMD_LOCAL_AFTER_MISSING_ALARM) || function.equals(CMD_CANCEL_ALERT)) {
                                    messageQueue.addFirst(new QueueMessage(function, intent.getExtras()));
                                    handleDeviceCommand();
                                } else {
                                    messageQueue.add(new QueueMessage(function, intent.getExtras()));
                                    if (readyToProcessCommand(function)) {
                                        handleDeviceCommand();
                                    }
                                }
                            }
                        } else {
                            // no specific function
                        }
                    }
                } else {
                    stopConnection();
                }
                return START_STICKY;
            } else {
                deactivateService();
                return START_NOT_STICKY;
            }
        } finally {
            Helper.releaseWakeLock(wl);
        }
    }

    private void deactivateService() {
        UserError.Log.d(TAG, "Service is NOT set be active - shutting down");
        stopBgUpdateTimer();
        stopConnection();
        prevReadingStatusIsStale = false;
        stopSelf();
    }

    private void stopWatchdog() {
        Helper.cancelAlarm(HuamiXdrip.getAppContext(), watchdogIntent);
    }

    private void resetWatchdog() {
        stopWatchdog();
        watchdogIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.MIBAND_SERVICE_WATCHDOG_ID, CMD_LOCAL_WATCHDOG);
        Helper.wakeUpIntent(HuamiXdrip.getAppContext(), WATCHDOG_DELAY, watchdogIntent);
    }

    private boolean handleGlobalCommand(@NotNull String functionName, Bundle bundle) {
        switch (functionName) {
            case CMD_STAT_INFO:
                StatisticInfo statisticInfo = new StatisticInfo(bundle);
                Helper.static_toast_long(statisticInfo.toString());
                break;
            case CMD_LOCAL_XDRIP_APP_NO_RESPONSE:
                bgDataRepository.setNewConnectionState(HuamiXdrip.gs(R.string.xdrip_app_no_response));
                startBgTimer();
                return false;
            case CMD_UPDATE_BG:
                updateLatestBgData(bundle);
                if (isNightMode) {
                    break;
                }
                startBgTimer();
                break;
            case CMD_UPDATE_BG_FORCE:
                updateLatestBgData(bundle);
                startBgTimer();
                break;
            case CMD_LOCAL_BG_FORCE_REMOTE:
                bgForce();
                return false;
            case CMD_LOCAL_REFRESH:
                updateWebServer();
                whenToRetryNextBgTimer(); //recalculate isNightMode
                break;
            case CMD_REPLY_MSG:
                String replyMsg = bundle.getString(BroadcastService.INTENT_REPLY_MSG);
                String replyCode = bundle.getString(BroadcastService.INTENT_REPLY_CODE);
                UserError.Log.e(TAG, "replyMsg:" + replyMsg);
                UserError.Log.e(TAG, "replyCode:" + replyCode);
                if (replyCode.equals(BroadcastService.INTENT_REPLY_CODE_NOT_REGISTERED)) {
                    bgForce();
                    break;
                }
                if (isWaitingAddTreatmentResponse && replyCode.equals(INTENT_REPLY_CODE_OK)) {
                    isWaitingAddTreatmentResponse = false;
                }
                break;
        }
        return true;
    }

    private void updateWebServer() {
        if (MiBandEntry.isWebServerEnabled()) {
            if (webServer == null) {
                try {
                    webServer = new WebServer();
                    webServer.registerCGI("/info.json", CGI_getInfoResponse);
                    webServer.registerCGI("/add_treatments", CGI_addTreatments);
                    webServer.start();
                    UserError.Log.d(TAG, "WebServer started");
                } catch (Exception e) {
                    webServer = null;
                    UserError.Log.d(TAG, "Cannot create WebServer: " + e.getMessage());
                }
            }
        } else {
            stopWebServer();
        }
    }

    private void stopWebServer() {
        if (webServer != null) {
            webServer.stop();
            webServer = null;
            UserError.Log.d(TAG, "WebServer stopped");
        }
    }

    private boolean handleDeviceCommand() {
        if (messageQueue.isEmpty()) {
            return false;
        }
        do {
            queueItem = messageQueue.poll();
        } while (queueItem.isExpired() && !messageQueue.isEmpty());
        if (queueItem.isExpired()) return false;
        UserError.Log.d(TAG, "handleDeviceCommand func: " + queueItem.functionName);
        ((MiBandState) mState).resetSequence();
        resetWatchdog();
        switch (queueItem.functionName) {
            case CMD_LOCAL_REFRESH:
                ((MiBandState) mState).setSettingsSequence();
                break;
            case CMD_MESSAGE:
                ((MiBandState) mState).setQueueSequence();
                queueItem.setMessageType(MIBAND_NOTIFY_TYPE_MESSAGE);
                break;
            case CMD_ALERT:
                activeAlertType = queueItem.bundle.getString("type");
                if (activeAlertType == null) {
                    break;
                }
                if (activeAlertType.equals(Const.BG_ALERT_TYPE)) {
                    if (!MiBandEntry.areAlertsEnabled()) break;
                } else {
                    if (!MiBandEntry.areOtherAlertsEnabled()) break;
                }

                ((MiBandState) mState).setAlarmSequence();
                queueItem.setMessageType(MIBAND_NOTIFY_TYPE_ALARM);
                if (isNightMode) {
                    messageQueue.addFirst(new QueueMessage(CMD_LOCAL_BG_FORCE_REMOTE));
                }
                break;
            case CMD_SNOOZE_ALERT:
                int snoozeMinutes = queueItem.bundle.getInt("snoozeMinutes", -1000);
                if (snoozeMinutes == -1000) {
                    UserError.Log.d(TAG, "wrong snooze minutes");
                    break;
                }
                String alertName = queueItem.bundle.getString("alertName");
                long nextAlertAt = queueItem.bundle.getLong("nextAlertAt", Helper.tsl());
                String msgText1 = String.format(HuamiXdrip.gs(R.string.miband_alert_snooze_text), alertName, snoozeMinutes, Helper.hourMinuteString(nextAlertAt));
                UserError.Log.d(TAG, msgText1);
                messageQueue.addFirst(getMessageQueue(msgText1, HuamiXdrip.gs(R.string.miband_alert_snooze_title_text)));
                break;
            case CMD_CANCEL_ALERT:
                if (!I.state.equals(MiBandState.WAITING_USER_RESPONSE)) break;
                setWaitingCallResponse(false);
                vibrateAlert(AlertLevelMessage.AlertLevelType.NoAlert); //disable call
                String msgText2 = HuamiXdrip.gs(R.string.miband_alert_snoozed_remotely_text);
                messageQueue.addFirst(getMessageQueue(msgText2, HuamiXdrip.gs(R.string.miband_alert_snooze_title_text)));
                ((MiBandState) mState).setQueueSequence();
                break;
            case CMD_LOCAL_AFTER_MISSING_ALARM:
                if (!I.state.equals(MiBandState.WAITING_USER_RESPONSE)) break;
                setWaitingCallResponse(false);
                vibrateAlert(AlertLevelMessage.AlertLevelType.NoAlert); //disable call
                if (!missingAlertMessage.isEmpty()) {
                    String msgText = HuamiXdrip.getAppContext().getString(R.string.miband_alert_missing_text) + missingAlertMessage;
                    messageQueue.addFirst(getMessageQueue(HuamiXdrip.getAppContext().getString(R.string.miband_alert_missing_title_text), msgText));
                }
                ((MiBandState) mState).setQueueSequence();
                break;
            case CMD_UPDATE_BG:
                updateBgData();
                if (isNightMode) {
                    UserError.Log.d(TAG, "Skip bg update because of night mode");
                    break;
                }
                boolean curReadingStatusIsStale = isStaleReading();

                if (prevReadingStatusIsStale && curReadingStatusIsStale) {
                    UserError.Log.d(TAG, "Skip bg update because of staleReading");
                    changeState(SLEEP);
                    break;
                }

                prevReadingStatusIsStale = curReadingStatusIsStale;
                ((MiBandState) mState).setSendReadingSequence();
                break;
            case CMD_UPDATE_BG_FORCE:
                updateBgData();

                ((MiBandState) mState).setSendReadingSequence();
                break;
            case CMD_LOCAL_UPDATE_BG_AS_NOTIFICATION:
                ((MiBandState) mState).setSendReadingSequence();
                break;
            case CMD_LOCAL_WATCHDOG:
                stopConnection();
                changeState(SLEEP);
                UserError.Log.e(TAG, "Watchdog Handle!!!");
                break;
        }
        if (((MiBandState) mState).isStartSequence()) {
            changeState(INIT);
        } else {
            return handleDeviceCommand();
        }
        return true;
    }

    private boolean isStaleReading() {
        return bgData == null || bgData.isStale() || bgData.isNoBgData();
    }

    private long whenToRetryNextBgTimer() {
        final long bg_time;

        Calendar expireDate = Calendar.getInstance();
        long currTimeMillis = Helper.tsl();

        long interval = BG_UPDATE_NO_DATA_INTERVAL;
        if (MiBandEntry.isWebServerEnabled() && !MiBandEntry.isDeviceEnabled()) {
            interval = BG_WEB_SERVER_NO_DATA_INTERVAL;
        }
        isNightMode = false;
        int dayInterval = MiBandEntry.getDayModeInterval();
        if (dayInterval != 0) {
            interval = dayInterval * Constants.MINUTE_IN_MS;
            isNightMode = true;
        }

        expireDate.setTimeInMillis(System.currentTimeMillis() + interval);

        if (MiBandEntry.isDeviceEnabled() && MiBandEntry.isNightModeEnabled()) {
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
        UserError.Log.d(TAG, "isNightMode: " + isNightMode);
        return bg_time;
    }

    private void stopBgUpdateTimer() {
        Helper.cancelAlarm(HuamiXdrip.getAppContext(), bgServiceIntent);
        bgWakeupTime = 0;
        isNightMode = false;
    }

    private void startBgTimer() {
        stopBgUpdateTimer();
        if (shouldServiceRun()) {
            final long retry_in = whenToRetryNextBgTimer();
            UserError.Log.d(TAG, "Scheduling next BgTimer in: " + Helper.niceTimeScalar(retry_in) + " @ " + Helper.dateTimeText(retry_in + Helper.tsl()));
            bgServiceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.MIBAND_SERVICE_BG_RETRY_ID, CMD_LOCAL_BG_FORCE_REMOTE);
            Helper.wakeUpIntent(HuamiXdrip.getAppContext(), retry_in, bgServiceIntent);
            bgWakeupTime = Helper.tsl() + retry_in;
        } else {
            UserError.Log.d(TAG, "Retry timer was not scheduled");
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

                    Intent intent = new Intent();
                    intent.putExtra(BroadcastService.INTENT_ALERT_TYPE, activeAlertType);
                    BroadcastService.handleCommand(CMD_SNOOZE_ALERT, intent);

                    startBgTimer();
                    changeState(SLEEP);
                }
                setWaitingCallResponse(false);
                break;
            case DeviceEvent.CALL_IGNORE:
                UserError.Log.d(TAG, "call ignored");
                if (I.state.equals(MiBandState.WAITING_USER_RESPONSE)) {
                    changeState(MiBandState.WAITING_MIFIT_SILENCE);
                }
                setWaitingCallResponse(false);
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
                if ((Helper.ratelimit("band_find phone_sound", 3))) {
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
            //UserError.Log.d(TAG, "Service: " + getUUIDName(service.getUuid()));
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                //UserError.Log.d(TAG, "-- Character: " + getUUIDName(characteristic.getUuid()));
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
                    deviceInfo.setBatteryLevel(batteryInfo.getLevelInPercent());
                    UserError.Log.d(TAG, "Battery level: " + batteryInfo.getLevelInPercent() + "%");
                    changeNextState();
                }, throwable -> {
                    UserError.Log.e(TAG, "Could not read battery info: " + throwable);
                    changeNextState();
                });
    }

    @SuppressLint("CheckResult")
    private void readRSSI() {
        I.connection.readRssi().subscribe(
                rssi -> {
                    UserError.Log.d(TAG, "RSSI: " + rssi);
                    deviceInfo.setRssi(rssi);
                    changeNextState();
                },
                throwable -> {
                    UserError.Log.d(TAG, "Cannot receive RSSI:" + throwable);
                    changeNextState();
                }
        );
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

    private Boolean sendBgAsNotification() {
        if (isStaleReading()) {
            return false;
        } else {
            AlertMessage message = new AlertMessage();
            try {
                String messageText = "BG: " + bgData.unitizedBgValue() + " " + bgData.getSlopeArrow();
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
                    message.queueChunkedMessage(this, AlertMessage.CustomIcon.APP_11, messageText.toUpperCase(), null, messageText.toUpperCase());
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
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

    private void setWaitingCallResponse(boolean state) {
        if (isWaitingCallResponse != state) {
            isWaitingCallResponse = state;
            UserError.Log.d(TAG, "change isWaitingCallResponse:" + isWaitingCallResponse);
        }
    }


    private void queueMessage() {
        String message = queueItem.bundle.getString("message");
        AlertMessage alertMessage = new AlertMessage();
        String messageType = queueItem.getMessageType();
        if (message == null) {
            setWaitingCallResponse(false);
            changeNextState();
            return;
        }
        switch (messageType != null ? messageType : "null") {
            case MIBAND_NOTIFY_TYPE_CALL:
                setWaitingCallResponse(true);
                new QueueMe()
                        .setBytes(alertMessage.getAlertMessageOld(message, AlertMessage.AlertCategory.Call))
                        .setDescription("Send call alert: " + message)
                        .setQueueWriteCharacterstic(alertMessage.getCharacteristicUUID())
                        .expireInSeconds(QUEUE_EXPIRED_TIME)
                        .setDelayMs(QUEUE_DELAY)
                        .queue();
                UserError.Log.d(TAG, "Queued call alert: " + message);
                break;
            case MIBAND_NOTIFY_TYPE_CANCEL:
                if (isWaitingCallResponse) {
                    vibrateAlert(AlertLevelMessage.AlertLevelType.NoAlert); //disable call
                    setWaitingCallResponse(false);
                    UserError.Log.d(TAG, "Call disabled");
                }
                break;
            case MIBAND_NOTIFY_TYPE_ALARM:
                setWaitingCallResponse(true);
                new QueueMe()
                        .setBytes(alertMessage.getAlertMessageOld(message, AlertMessage.AlertCategory.Call))
                        .setDescription("Sent glucose alert: " + message)
                        .setQueueWriteCharacterstic(alertMessage.getCharacteristicUUID())
                        .expireInSeconds(QUEUE_EXPIRED_TIME)
                        .setDelayMs(QUEUE_DELAY)
                        .queue();
                missingAlertMessage = message;
                stopBgUpdateTimer();
                bgServiceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.MIBAND_SERVICE_BG_RETRY_ID, CMD_LOCAL_AFTER_MISSING_ALARM);
                Helper.wakeUpIntent(HuamiXdrip.getAppContext(), CALL_ALERT_DELAY, bgServiceIntent);
                AudioManager audioManager = (AudioManager) HuamiXdrip.getAppContext().getSystemService(Context.AUDIO_SERVICE);
                ringerModeBackup = audioManager.getRingerMode();
                UserError.Log.d(TAG, "Backup ringer mode: " + ringerModeBackup);
                break;
            case MIBAND_NOTIFY_TYPE_MESSAGE:
                String title = queueItem.bundle.getString("title");
                if (title == null) title = "";
                message = message.replace("@", "");
                UserError.Log.d(TAG, "Queuing message alert: " + message);
                MiBandType type = MiBand.getMibandType();
                if (type == MiBandType.MI_BAND2) {
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
                    if (MiBandType.AMAZFITGTS2_MINI == type) {
                        message = title;
                    }
                    alertMessage.queueChunkedMessage(this, AlertMessage.CustomIcon.APP_11, title, null, message);
                }
                break;
            default: // glucose
                break;
        }
        changeNextState();
    }

    @SuppressLint("CheckResult")
    private void authPhase() {
        extendWakeLock(30000);
        isNeedToAuthenticate = false;
        RxBleConnection connection = I.connection;
        UserError.Log.d(TAG, "Authorizing");
        if (I.connection == null) {
            UserError.Log.d(TAG, "Cannot enable as connection is null!");
            return;
        }

        AuthOperations authOperations = MiBandType.getAuthOperations(MiBand.getMibandType(), this);
        UserError.Log.d(TAG, "authOperations: " + authOperations.getClass().getSimpleName());
        if (!authOperations.initAuthKey()) {
            changeState(MiBandState.AUTHORIZE_FAILED);
            return;
        }

        useV2ChunkedProtocol = MiBandEntry.isForceNewProtocol();
        if (!useV2ChunkedProtocol) {
            useV2ChunkedProtocol = authOperations.isV2Protocol();
        }

        MiBandEntry.isForceNewProtocol();

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
                                UserError.Log.d(TAG, "Disconnected exception: authSubscription");
                            } else if (throwable instanceof TimeoutException) {
                                //check if it is normal timeout
                                if (!MiBand.isAuthenticated()) {
                                    String errorText = gs(R.string.miband_auth_fail);
                                    UserError.Log.d(TAG, errorText);
                                    Helper.static_toast_long(errorText);
                                }
                            }
                            unsubscribeAuthSubscription();
                            releaseWakeLock();
                        }));
    }

    private void unsubscribeAuthSubscription() {
        if (authSubscription != null) {
            authSubscription.unsubscribe();
            authSubscription = null;
        }
    }

    @SuppressLint("CheckResult")
    private void installWatchface() {
        extendWakeLock(1 * Constants.MINUTE_IN_MS);
        UserError.Log.d(TAG, "Install WatchFace");

        boolean sendBGNotification = false;
        if (deviceInfo.getRssi() < MiBandEntry.getRSSITreshold()) {
            UserError.Log.d(TAG, "Too weak BT connection");
            UserError.Log.d(TAG, "RSSI:" + deviceInfo.getRssi() + " RSSI Treshold:" + MiBandEntry.getRSSITreshold());

            sendBGNotification = true;
        }
        if (deviceInfo.getBatteryLevel() < MINIMUM_BATTERY_LEVEL) {
            UserError.Log.d(TAG, "Battery is too low");
            sendBGNotification = true;
        }
        if (sendBGNotification) {
            changeState(SLEEP);
            Helper.startService(MiBandService.class, INTENT_FUNCTION_KEY, CMD_LOCAL_UPDATE_BG_AS_NOTIFICATION);
            return;
        }

        if (I.connection == null) {
            UserError.Log.d(TAG, "Cannot enable as connection is null!");
            return;
        }
        FirmwareOperationsNew firmware;
        SequenceState sequenceState;
        try {
            MiBandType mibandType = MiBand.getMibandType();
            Version version = new Version(MiBand.getVersion());
            if (mibandType == MiBandType.AMAZFITGTR && version.compareTo(new Version("1.0.0.00")) < 0) {//probably gtr42
                mibandType = MiBandType.AMAZFITGTR_42;
            }
            WatchFaceGenerator wfGen = new WatchFaceGenerator(getBaseContext().getAssets(),
                    mibandType);
            byte[] fwArray = wfGen.genWatchFace(queueItem.bundle, bgData);
            if (fwArray == null || fwArray.length == 0) {
                resetFirmwareState(false, "Empty image");
                return;
            }
            if (mibandType == MiBandType.MI_BAND4) {
                sequenceState = new SequenceStateMiBand4();
                firmware = new FirmwareOperationsNew(fwArray, sequenceState, this);
            } else if (mibandType == MiBandType.MI_BAND5 || mibandType == MiBandType.AMAZFIT5) {
                sequenceState = new SequenceStateMiBand5();
                firmware = new FirmwareOperationsNew(fwArray, sequenceState, this);
            } else if (mibandType == MiBandType.MI_BAND6) {
                sequenceState = new SequenceStateMiBand6();
                firmware = new FirmwareOperations2020(fwArray, sequenceState, this);
            } else if (mibandType == MiBandType.AMAZFITGTR || mibandType == MiBandType.AMAZFITGTR_LITE) {
                if ((version.compareTo(new Version("1.3.7.16")) >= 0)) {
                    sequenceState = new SequenceStateVergeNew();
                    firmware = new FirmwareOperations2020(fwArray, sequenceState, this);
                } else {
                    sequenceState = new SequenceStateMiBand5();
                    firmware = new FirmwareOperationsNew(fwArray, sequenceState, this);
                }
            } else if (mibandType == MiBandType.AMAZFITGTR_42) {
                sequenceState = new SequenceStateVergeOld();
                firmware = new FirmwareOperationsNew(fwArray, sequenceState, this);
            } else if (mibandType == MiBandType.AMAZFITGTS2_MINI) {
                sequenceState = new SequenceStateGTS2Mini();
                firmware = new FirmwareOperations2020(fwArray, sequenceState, this);
            } else if (mibandType == MiBandType.AMAZFITGTS || mibandType == MiBandType.AMAZFIT_TREX_PRO) {
                if (version.compareTo(new Version("0.1.1.16")) >= 0) {
                    if (mibandType == MiBandType.AMAZFIT_TREX_PRO) {
                        sequenceState = new SequenceStateVerge2();
                    } else {
                        sequenceState = new SequenceStateVergeNew();
                    }

                    firmware = new FirmwareOperations2020(fwArray, sequenceState, this);
                } else {
                    sequenceState = new SequenceStateMiBand5();
                    firmware = new FirmwareOperationsNew(fwArray, sequenceState, this);
                }
            } else if (MiBandType.isVerge2(mibandType)) {
                sequenceState = new SequenceStateVerge2();
                firmware = new FirmwareOperations2020(fwArray, sequenceState, this);
            } else if (MiBandType.isBip(mibandType)) {
                sequenceState = new SequenceStateBip();
                firmware = new FirmwareOperation(fwArray, sequenceState, this);
            } else if (MiBandType.isBipS(mibandType)) {
                if (!(version.compareTo(new Version("4.0.0.00")) >= 0) && (version.compareTo(new Version("2.1.1.50")) >= 0) || (version.compareTo(new Version("4.1.5.55")) >= 0)) {
                    sequenceState = new SequenceStateBipS();
                    firmware = new FirmwareOperations2020(fwArray, sequenceState, this);
                } else {
                    sequenceState = new SequenceStateMiBand5();
                    firmware = new FirmwareOperationsNew(fwArray, sequenceState, this);
                }
            } else {
                resetFirmwareState(false, "Not supported band type");
                return;
            }
        } catch (Exception e) {
            resetFirmwareState(false, "FirmwareOperations error " + e.getMessage());
            return;
        }
        UserError.Log.d(TAG, "FirmwareOperations type: " + firmware.getClass().getSimpleName());
        UserError.Log.d(TAG, "SequenceState type: " + sequenceState.getClass().getSimpleName());

        UserError.Log.d(TAG, "Begin uploading Watchface, length: " + firmware.getSize());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            UserError.Log.d(TAG, "Requesting high priority connection");
            requestConnectionPriority(I.connection, BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        }
        int mtu = I.connection.getMtu();
        setMTU(mtu);
        firmware.nextSequence();
        firmware.processFirmwareSequence();
        changeNextState();
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

        if (I.state.equals(CLOSED) || I.state.equals(CLOSE) || !isConnected()) return;
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
        DisplayControllMessage dispControl = new DisplayControllMessage();
        isNeedToRestoreNightMode = false;

        writeToConfiguration(dispControl.setNightModeCmd(nightMode, start, end));
    }

    private void handleHeartrate(byte[] value) {
        if (Helper.ratelimit("miband-heartrate-limit", 60)) {
            if (value.length == 2 && value[0] == 0) {
                int hrValue = (value[1] & 0xff);

                Intent intent = new Intent();
                intent.putExtra("value", hrValue);
                BroadcastService.handleCommand(CMD_ADD_HR, intent);
            }
        }
    }

    private boolean addTreatment(Double carbs, Double insulin) {
        if (carbs == 0 && insulin == 0) return false;

        Intent intent = new Intent();
        intent.putExtra("carbs", carbs)
                .putExtra("insulin", insulin);
        BroadcastService.handleCommand(CMD_ADD_TREATMENT, intent);
        return true;
    }

    private void handleRealtimeSteps(byte[] value) {
        if (value == null) {
            return;
        }
        if (Helper.ratelimit("miband-heartrate-limit", 60)) {
            if (value.length == 13) {
                byte[] stepsValue = new byte[]{value[1], value[2]};
                int steps = FirmwareOperationsNew.toUint16(stepsValue);

                Intent intent = new Intent();
                intent.putExtra("value", steps);
                BroadcastService.handleCommand(CMD_ADD_STEPS, intent);
            } else {
                UserError.Log.d(TAG, "Unrecognized realtime steps value: " + bytesToHex(value));
            }
        }
    }

    @SuppressLint("CheckResult")
    private void enableNotifications() {
        UserError.Log.d(TAG, "enableNotifications called");
        if (I.connection == null) {
            UserError.Log.d(TAG, "Cannot enable as connection is null!");
            changeState(CLOSE);
            return;
        }
        if (I.isNotificationEnabled) {
            UserError.Log.d(TAG, "Notifications already enabled");
            changeNextState();
            return;
        }
        I.connection.requestMtu(PREFERRED_MTU_SIZE).subscribe();

        enableHeartRateNotification();
        enableStepsNotification();
        enableDeviceEventNotification();
        I.isNotificationEnabled = true;
        changeNextState();
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
                            UserError.Log.d(TAG, "Throwable in HeartRate notification: " + throwable);
                            if (throwable instanceof BleCharacteristicNotFoundException) {
                                UserError.Log.d(TAG, "HeartRate Characteristic not found for notification");
                            } else {
                                UserError.Log.d(TAG, "Disconnected exception: HeartRate");
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
                                UserError.Log.d(TAG, "Disconnected exception: Steps");
                            }
                        }
                ));
    }


    private void enableDeviceEventNotification() {
        if (notifSubscriptionDeviceEvent != null) {
            notifSubscriptionDeviceEvent.unsubscribe();
        }
        UserError.Log.d(TAG, "Requesting to enable device event notifications");
        notifSubscriptionDeviceEvent = new Subscription(I.connection.setupNotification(Const.UUID_CHARACTERISTIC_DEVICEEVENT)
                .doOnNext(notificationObservable -> {
                    I.isNotificationEnabled = true;
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
                            } else {
                                UserError.Log.d(TAG, "Disconnected exception: deviceEvent");
                            }
                        }
                ));
    }


    @Override
    protected synchronized boolean automata() {
        //UserError.Log.d(TAG, "Automata called in " + TAG);
        extendWakeLock(10000);
        if (shouldServiceRun()) {
            bgDataRepository.setNewConnectionState(I.state);
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
                    if (!MiBandType.supportGraph(MiBand.getMibandType())
                            || MiBandEntry.isNeedSendReadingAsNotification()
                            || queueItem.functionName.equals(CMD_LOCAL_UPDATE_BG_AS_NOTIFICATION)) {
                        Boolean result = sendBgAsNotification();
                        if (result) changeState(MiBandState.VIBRATE_AFTER_READING);
                        else changeState(MiBandState.SEND_QUEUE);
                        break;
                    }
                    changeNextState();
                    break;
                case MiBandState.GET_RSSI:
                    readRSSI();
                    changeNextState();
                    break;
                case MiBandState.WAIT_GET_RSSI:
                    break;
                case MiBandState.INSTALL_WATCHFACE:
                    installWatchface();
                    break;
                case MiBandState.INSTALL_WATCHFACE_IN_PROGRESS:
                    break;
                case MiBandState.INSTALL_WATCHFACE_FINISHED:
                    break;
                case MiBandState.RESTORE_NIGHTMODE:
                    if (isNeedToRestoreNightMode) {
                        // do nothing because something happen with connection while sending nightmode
                        extendWakeLock(RESTORE_NIGHT_MODE_DELAY * Constants.SECOND_IN_MS);
                        Helper.threadSleep(RESTORE_NIGHT_MODE_DELAY);
                        if (I.state.equals(CLOSED) || I.state.equals(CLOSE) || !isConnected())
                            break;
                        setNightMode();
                        if (I.state.equals(CLOSED) || I.state.equals(CLOSE) || !isConnected())
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
                case MiBandState.WAIT_BATTERY_INFO:
                    break;
                case MiBandState.QUEUE_MESSAGE:
                    queueMessage();
                    break;
                case MiBandState.WAITING_USER_RESPONSE:
                    UserError.Log.d(TAG, "isWaitingCallResponse " + isWaitingCallResponse);
                    if (!isWaitingCallResponse) {
                        changeState(SLEEP);
                    }
                    break;
                case MiBandState.WAITING_MIFIT_SILENCE:
                    //restore ringer mode because mifit won't restore it
                    extendWakeLock(2 * Constants.SECOND_IN_MS);
                    Helper.threadSleep((Constants.SECOND_IN_MS));
                    AudioManager audioManager = (AudioManager) HuamiXdrip.getAppContext().getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setRingerMode(ringerModeBackup);
                    UserError.Log.d(TAG, "Restore ringer mode: " + ringerModeBackup);
                    startBgTimer();
                    changeNextState();
                    break;
                case MiBandState.AUTHORIZE_FAILED:
                    unsubscribeAuthSubscription();
                    changeNextState();
                    break;
                case SLEEP:
                    if (!handleDeviceCommand()) {
                        stopWatchdog();
                    }
                    break;
                case CLOSED:
                    stopConnection();
                    return super.automata();
                default:
                    return super.automata();
            }
        } else {
            UserError.Log.d(TAG, "Service should not be running inside automata");
        }
        return true; // lies
    }

    private void stopConnection() {
        if (!isConnectionStopped) {
            if (isConnected()) {
                unsubscribeAuthSubscription();
                if (notifSubscriptionDeviceEvent != null) {
                    notifSubscriptionDeviceEvent.unsubscribe();
                    notifSubscriptionDeviceEvent = null;
                }
                if (notifSubscriptionStepsMeasurement != null) {
                    notifSubscriptionStepsMeasurement.unsubscribe();
                    notifSubscriptionStepsMeasurement = null;
                }
                if (notifSubscriptionHeartRateMeasurement != null) {
                    notifSubscriptionHeartRateMeasurement.unsubscribe();
                    notifSubscriptionHeartRateMeasurement = null;
                }
                I.isNotificationEnabled = false;
                stopConnect(I.address);
            }
            isNeedToAuthenticate = true;
            setWaitingCallResponse(false);
            messageQueue.clear();
            handle = 0;
            setMTU(GATT_MTU_MINIMUM); //reset mtu
            setRetryTimerReal(); // local retry strategy
            bgDataRepository.setNewConnectionState(HuamiXdrip.gs(R.string.watch_disconnected));
            isConnectionStopped = true;
            isNeedToCheckRevision = true;
            stopWatchdog();
        }
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
            I.retry_time = Helper.wakeUpIntent(HuamiXdrip.getAppContext(), retry_in, I.serviceIntent);
            I.wakeup_time = Helper.tsl() + retry_in;
        } else {
            UserError.Log.d(TAG, "Not setting retry timer as service should not be running");
        }
    }

    private void cancelRetryTimer() {
        Helper.cancelAlarm(HuamiXdrip.getAppContext(), I.serviceIntent);
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
            int checksum = (int) Helper.checksum(encryptable_payload, 0, length + 4);
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

    private void updateLatestBgData(Bundle bundle) {
        latestBgDataBundle = bundle;
        bgDataLatest = new BgData(bundle);
        bgDataRepository.setNewBgData(bgDataLatest);
        bgDataRepository.setNewConnectionState(HuamiXdrip.gs(R.string.xdrip_app_received_data));

        XiaomiWearService.bgForce(new WebServiceData(bgDataLatest, latestBgDataBundle, true).getGson());
    }

    private void updateBgData() {
        bgData = new BgData(queueItem.bundle);
        MiBand.setUnit(bgData.isDoMgdl());
    }

    public void updateConnectionState(String status) {
        bgDataRepository.setNewConnectionState(status);
    }

    public QueueMessage getMessageQueue(String title, String message) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("message", message);
        QueueMessage msg = new QueueMessage(CMD_MESSAGE, bundle);
        msg.setMessageType(MIBAND_NOTIFY_TYPE_MESSAGE);

        return msg;
    }

    public static class MiBandState extends BaseBluetoothSequencer.BaseState {
        public static final String AUTHORIZE_FAILED = "Authorization Failed";
        static final String SEND_BG = "Send BG";
        static final String SEND_SETTINGS = "Updating Settings";
        static final String QUEUE_MESSAGE = "Queue Message";
        static final String WAITING_USER_RESPONSE = "Waiting User Response";
        static final String WAITING_MIFIT_SILENCE = "Waiting MiFit Silence";
        static final String AUTHENTICATE = "Authenticate";
        static final String AUTHORIZE = "Authorization";
        static final String AUTHORIZE_FAILED_SLEEP = "Authorization failed, sleep";
        static final String GET_MODEL_NAME = "Getting model name";
        static final String GET_SOFT_REVISION = "Getting software revision";
        static final String ENABLE_NOTIFICATIONS = "Enabling Notifications";
        static final String GET_BATTERY_INFO = "Request Battery";
        static final String WAIT_BATTERY_INFO = "Waiting Battery Info";
        static final String GET_RSSI = "Request RSSI";
        static final String WAIT_GET_RSSI = "Waiting RSSI info";
        static final String INSTALL_WATCHFACE = "Preparing Watchface";
        static final String INSTALL_WATCHFACE_IN_PROGRESS = "Watchface Uploading";
        static final String INSTALL_WATCHFACE_FINISHED = "Watchface Uploading Finished";
        static final String RESTORE_NIGHTMODE = "Restore Nightmode";
        static final String VIBRATE_AFTER_READING = "Vibrate";

        private static final String TAG = "MiBandStateSequence";
        private boolean startSequence = false;

        public boolean isStartSequence() {
            return startSequence;
        }

        public boolean resetSequence() {
            return startSequence = false;
        }

        void prepareInitialSequences() {
            startSequence = true;
            sequence.clear();
            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(GET_MODEL_NAME);
            sequence.add(GET_SOFT_REVISION);
            sequence.add(AUTHENTICATE);
            sequence.add(AUTHORIZE);
            sequence.add(ENABLE_NOTIFICATIONS);
            sequence.add(GET_BATTERY_INFO);
            sequence.add(WAIT_BATTERY_INFO);
            sequence.add(GET_RSSI);
            sequence.add(WAIT_GET_RSSI);
        }

        void prepareFinalSequences() {
            sequence.add(SEND_QUEUE);
            sequence.add(QUEUE_SENDING);
            sequence.add(SLEEP);
            sequence.add(AUTHORIZE_FAILED);
            sequence.add(AUTHORIZE_FAILED_SLEEP);
        }

        void setSendReadingSequence() {
            UserError.Log.d(TAG, "SET UPDATE WATCHFACE SEQUENCE");
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
            sequence.add(QUEUE_SENDING);
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

    public class QueueMessage {
        @Getter
        private String functionName;
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

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }

        private void setExpire() {
            this.expireAt = Helper.tsl() + (Constants.MINUTE_IN_MS * 10);
        }

        boolean isExpired() {
            return expireAt != 0 && expireAt < Helper.tsl();
        }
    }
}
