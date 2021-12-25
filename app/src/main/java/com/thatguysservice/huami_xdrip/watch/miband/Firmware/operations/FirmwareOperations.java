package com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations;

import android.annotation.SuppressLint;

import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble2.exceptions.BleCharacteristicNotFoundException;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.thatguysservice.huami_xdrip.models.JoH;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.utils.bt.Subscription;
import com.thatguysservice.huami_xdrip.watch.miband.Const;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceState;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceStateMiBand5;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceStateVergeNew;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandService;
import com.thatguysservice.huami_xdrip.watch.miband.message.AlertLevelMessage;
import com.thatguysservice.huami_xdrip.watch.miband.message.DisplayControllMessage;
import com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.polidea.rxandroidble2.RxBleConnection.GATT_WRITE_MTU_OVERHEAD;
import static com.thatguysservice.huami_xdrip.services.JamBaseBluetoothSequencer.BaseState.SLEEP;
import static com.thatguysservice.huami_xdrip.watch.miband.message.DisplayControllMessage.NightMode.Sheduled;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_CHECKSUM;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_INIT;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_START_DATA;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_UNKNOWN_MIBAND5;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_UPDATE_SYNC;

public class FirmwareOperations {
    public static final boolean d = true;
    private static final long MAX_RETRIES = 5;
    public static int FIRMWARE_SYNC_PACKET = 175;
    protected String TAG = MiBandService.class.getSimpleName();
    protected int retryCount = 0;
    byte[] fw;
    SequenceState mState;
    MiBandService service;
    RxBleConnection connection;
    private FirmwareType firmwareType = FirmwareType.WATCHFACE;
    private Subscription watchfaceSubscription;

    public FirmwareOperations(byte[] file, SequenceState sequenceState, MiBandService service) {
        fw = file;
        mState = sequenceState;
        this.service = service;

        connection = service.getConection();
    }

    public static byte fromUint8(int value) {
        return (byte) (value & 0xff);
    }

    public static byte[] fromUint16(int value) {
        return new byte[]{
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
        };
    }

    public static byte[] fromUint24(int value) {
        return new byte[]{
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
        };
    }

    public static byte[] fromUint32(int value) {
        return new byte[]{
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
                (byte) ((value >> 24) & 0xff),
        };
    }

    public static int toUint16(byte... bytes) {
        return (bytes[0] & 0xff) | ((bytes[1] & 0xff) << 8);
    }

    public static byte[] readAll(InputStream in, long maxLen) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(8192, in.available()));
        byte[] buf = new byte[8192];
        int read;
        long totalRead = 0;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
            totalRead += read;
            if (totalRead > maxLen) {
                throw new IOException("Too much data to read into memory. Got already " + totalRead);
            }
        }
        return out.toByteArray();
    }

    protected FirmwareType getFirmwareType() {
        return firmwareType;
    }

    public byte[] getBytes() {
        return fw;
    }

    protected int getPacketLength() {
        return service.getMTU() - GATT_WRITE_MTU_OVERHEAD;
    }

    public String getSequence() {
        return mState.getSequence();
    }

    public void nextSequence() {
        String oldState = mState.getSequence();
        String new_state = mState.next();
        UserError.Log.d(TAG, "Changing firmware state from: " + oldState + " to " + new_state);
    }

    public int getSize() {
        return fw.length;
    }

    protected byte[] getFwInfoCommand() {
        int fwSize = getSize();
        byte[] sizeBytes = fromUint24(fwSize);
        byte[] bytes = new byte[10];
        int i = 0;
        bytes[i++] = COMMAND_FIRMWARE_INIT;
        bytes[i++] = getFirmwareType().value;
        bytes[i++] = sizeBytes[0];
        bytes[i++] = sizeBytes[1];
        bytes[i++] = sizeBytes[2];
        bytes[i++] = 0; // TODO: what is that?
        int crc32 = (int) JoH.checksum(fw);
        byte[] crcBytes = fromUint32(crc32);
        bytes[i++] = crcBytes[0];
        bytes[i++] = crcBytes[1];
        bytes[i++] = crcBytes[2];
        bytes[i] = crcBytes[3];
        return bytes;
    }

    protected byte[] prepareFWUploadInitCommand() {
        return new byte[]{COMMAND_FIRMWARE_INIT, (byte) 0xFF};
    }

    protected byte[] getChecksumCommand() {
        return new byte[]{COMMAND_FIRMWARE_CHECKSUM};
    }

    protected byte[] getSyncCommand() {
        return new byte[]{COMMAND_FIRMWARE_UPDATE_SYNC};
    }

    protected byte[] getFirmwareStartCommand() {
        return new byte[]{COMMAND_FIRMWARE_START_DATA, (byte) 0x1};
    }

    protected byte[] getUnknownMiBand5Command() {
        return new byte[]{COMMAND_FIRMWARE_UNKNOWN_MIBAND5};
    }

    protected UUID getFirmwareCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_FIRMWARE;
    }

    protected UUID getFirmwareDataCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_FIRMWARE_DATA;
    }

    public void processFirmwareOperationNotifications(byte[] value) {
        if (value.length != 3 && value.length != 11) {
            UserError.Log.e(TAG, "Notifications should be 3 or 11 bytes long.");
            return;
        }
        boolean success = value[2] == OperationCodes.SUCCESS;
        String seq = getSequence();
        if (value[0] == OperationCodes.RESPONSE && success) {

            try {
                switch (value[1]) {
                    case OperationCodes.COMMAND_FIRMWARE_INIT: {
                        if (seq.equals(SequenceState.WAITING_PREPARE_UPLOAD_RESPONSE) ||
                                seq.equals(SequenceState.WAITING_TRANSFER_SEND_WF_INFO_RESPONSE)) {
                            nextSequence();
                            processFirmwareSequence();
                        }
                        break;
                    }
                    case OperationCodes.COMMAND_FIRMWARE_START_DATA: {
                        sendFirmwareData();
                        break;
                    }
                    case OperationCodes.COMMAND_FIRMWARE_CHECKSUM: {
                        if (seq.equals(SequenceState.WAITING_SEND_CHECKSUM_RESPONSE)) {
                            nextSequence();
                            processFirmwareSequence();
                        }
                        break;
                    }
                    case OperationCodes.COMMAND_FIRMWARE_REBOOT: {
                        UserError.Log.e(TAG, "Reboot command successfully sent.");
                        resetFirmwareState(true);
                        break;
                    }
                    case OperationCodes.COMMAND_FIRMWARE_UNKNOWN_MIBAND5: {
                        if (seq.equals(SequenceStateMiBand5.WAITING_UNKNOWN_REQUEST_RESPONSE)) {
                            nextSequence();
                            processFirmwareSequence();
                        }
                        break;
                    }
                    default: {
                        resetFirmwareState(false, "Unexpected response during firmware update");
                    }
                }
            } catch (Exception ex) {
                resetFirmwareState(false);
            }
        } else {
            String errorMessage = null;
            boolean sendBGNotification = false;
            if (value[2] == OperationCodes.LOW_BATTERY_ERROR) {
                errorMessage = "Cannot upload watchface, low battery, please charge device";
                sendBGNotification = true;
            } else if (value[2] == OperationCodes.WORKOUT_RUNNING) {
                errorMessage = "Cannot upload watchface, workout mode running on band";
            } else if (value[2] == OperationCodes.TIMER_RUNNING) {
                errorMessage = "Cannot upload watchface, timer running on band";
            } else if (value[2] == OperationCodes.ON_CALL) {
                errorMessage = "Cannot upload watchface, call in progress";
            } else {
                errorMessage = "Unexpected notification during firmware update:" + JoH.bytesToHex(value);
            }
            resetFirmwareState(false, errorMessage);
            if (sendBGNotification) {
                JoH.startService(MiBandService.class, "function", "update_bg_as_notification");
                service.changeState(SLEEP);
            }
        }
    }

    public synchronized void processFirmwareSequence() {
        if (d)
            UserError.Log.d(TAG, "processFirmwareSequence seq:" + getSequence().toString());
        processFirmwareOperationSequence();
    }

    @SuppressLint("CheckResult")
    public synchronized void processFirmwareOperationSequence() {
        switch (getSequence()) {
            case SequenceState.SET_NIGHTMODE: {
                service.isNeedToRestoreNightMode = true;
                DisplayControllMessage dispControl = new DisplayControllMessage();
                Calendar sheduledCalendar = Calendar.getInstance();
                sheduledCalendar.set(Calendar.HOUR_OF_DAY, 0);
                sheduledCalendar.set(Calendar.MINUTE, 0);
                Date sheduledDate = sheduledCalendar.getTime();
                service.writeToConfiguration(dispControl.setNightModeCmd(Sheduled, sheduledDate, sheduledDate));
                nextSequence();
                processFirmwareSequence();
                break;
            }

            case SequenceState.PREPARE_UPLOAD: {
                nextSequence();
                connection.writeCharacteristic(getFirmwareCharacteristicUUID(), prepareFWUploadInitCommand())
                        .subscribe(valB -> {
                                    UserError.Log.d(TAG, "Wrote prepareFWUploadInitCommand: " + JoH.bytesToHex(valB));
                                },
                                throwable -> {
                                    UserError.Log.e(TAG, "Could not write prepareFWUploadInitCommand: " + throwable);
                                    resetFirmwareState(false);
                                }
                        );
                break;
            }

            case SequenceState.TRANSFER_FW_START: {
                nextSequence();
                connection.writeCharacteristic(getFirmwareCharacteristicUUID(), getFirmwareStartCommand())
                        .subscribe(valB -> {
                                    UserError.Log.d(TAG, "Wrote Start command: " + JoH.bytesToHex(valB));
                                },
                                throwable -> {
                                    UserError.Log.e(TAG, "Could not write Start command: " + throwable);
                                    resetFirmwareState(false);
                                }
                        );
                break;
            }
            case SequenceState.TRANSFER_SEND_WF_INFO: {
                nextSequence();
                connection.writeCharacteristic(getFirmwareCharacteristicUUID(), getFwInfoCommand())
                        .subscribe(valB -> {
                                    UserError.Log.d(TAG, "Wrote getFwInfoCommand: " + JoH.bytesToHex(valB));
                                },
                                throwable -> {
                                    UserError.Log.e(TAG, "Could not write firmware info: " + throwable);
                                    resetFirmwareState(false);
                                }
                        );
                break;
            }
            case SequenceStateMiBand5.UNKNOWN_REQUEST: {
                nextSequence();
                connection.writeCharacteristic(getFirmwareCharacteristicUUID(), getUnknownMiBand5Command())
                        .subscribe(valB -> {
                                    UserError.Log.d(TAG, "Wrote getUnknownMiBand5Command: " + JoH.bytesToHex(valB));
                                },
                                throwable -> {
                                    UserError.Log.e(TAG, "Could not write getUnknownMiBand5Command: " + throwable);
                                    resetFirmwareState(false);
                                }
                        );
                break;
            }
            case SequenceState.TRANSFER_WF_ID: {
                service.writeToConfiguration(getWatcfaceIdCommand());
                nextSequence();
                processFirmwareSequence();
                break;
            }
            case SequenceState.NOTIFICATION_ENABLE: {
                watchfaceSubscription = new Subscription(
                        connection.setupNotification(getFirmwareCharacteristicUUID())
                                .timeout(300, TimeUnit.SECONDS) // WARN
                                .doOnNext(notificationObservable -> {
                                            if (d)
                                                UserError.Log.d(TAG, "Notification for firmware enabled");
                                            nextSequence();
                                            processFirmwareSequence();
                                        }
                                )
                                .flatMap(notificationObservable -> notificationObservable)
                                .subscribe(bytes -> {
                                    // incoming notifications
                                    if (d)
                                        UserError.Log.d(TAG, "Received firmware notification bytes: " + JoH.bytesToHex(bytes));
                                    processFirmwareOperationNotifications(bytes);
                                }, throwable -> {
                                    UserError.Log.d(TAG, "Throwable in firmware notification: " + throwable);
                                    if (throwable instanceof BleCharacteristicNotFoundException) {
                                        // maybe legacy - ignore for now but needs better handling
                                        UserError.Log.d(TAG, "Characteristic not found for notification");
                                    } else if (throwable instanceof BleCannotSetCharacteristicNotificationException) {
                                        UserError.Log.e(TAG, "Problems setting notifications - disconnecting");
                                    } else if (throwable instanceof BleDisconnectedException) {
                                        UserError.Log.d(TAG, "Disconnected while enabling notifications");
                                    } else if (throwable instanceof TimeoutException) {
                                        UserError.Log.d(TAG, "Timeout");
                                    }
                                    resetFirmwareState(false);
                                }));
                break;
            }
            case SequenceState.CHECKSUM_VERIFIED: {
                retryCount = 0;
                nextSequence();
                processFirmwareSequence();
                break;
            }
            case SequenceState.FORCE_DISABLE_VIBRATION: {
                forceDisableVibration();
                break;
            }
            case SequenceState.FW_UPLOADING_FINISHED: {
                if (getFirmwareType() == FirmwareType.FIRMWARE) {
                    //send reboot for firmware
                } else {
                    UserError.Log.e(TAG, "Watch Face has been installed successfully");
                    service.changeNextState();
                    resetFirmwareState(true);
                }
                break;
            }
        }
    }

    protected byte[] getWatcfaceIdCommand() {
        return OperationCodes.COMMAND_MIBAND5_UNKNOW_INIT;
    }

    @SuppressLint("CheckResult")
    protected void forceDisableVibration() {
        AlertLevelMessage message = new AlertLevelMessage();
        connection.writeCharacteristic(message.getCharacteristicUUID(), message.getPeriodicVibrationMessage((byte) 1, (short) 0, (short) 50))
                .subscribe(val -> {
                            if (d)
                                UserError.Log.d(TAG, "Wrote COMMAND_DISABLE_CALL: " + JoH.bytesToHex(val));
                            if (retryCount >= MAX_RETRIES) {
                                nextSequence();
                                processFirmwareSequence();
                            } else {
                                //Thread.sleep(1);
                                processFirmwareSequence();
                            }
                            if (getSequence().equals(SequenceStateVergeNew.FORCE_DISABLE_VIBRATION)) {
                                retryCount++;
                            }
                        },
                        throwable -> {
                            if (d)
                                UserError.Log.e(TAG, "Could not write COMMAND_DISABLE_CALL: " + throwable);
                            service.changeNextState();
                            resetFirmwareState(true);
                        }
                );
    }

    @SuppressLint("CheckResult")
    protected void sendFirmwareData() {
        byte[] fwbytes = getBytes();
        int len = getSize();

        final int packetLength = getPacketLength();
        if (d)
            UserError.Log.d(TAG, "Firmware packet lengh: " + packetLength);
        int packets = len / packetLength;

        // going from 0 to len
        int firmwareProgress = 0;
        for (int i = 0; i < packets; i++) {
            byte[] fwChunk = Arrays.copyOfRange(fwbytes, i * packetLength, i * packetLength + packetLength);
            int finalI = i;
            connection.writeCharacteristic(getFirmwareDataCharacteristicUUID(), fwChunk).subscribe(val -> {
                        if (d)
                            UserError.Log.d(TAG, "Wrote Chunk:" + finalI);
                    },
                    throwable -> {
                        if (d)
                            UserError.Log.e(TAG, "Could not write fwChunk: " + throwable);
                        resetFirmwareState(false);
                    }
            );
            firmwareProgress += packetLength;
            int progressPercent = (int) ((((float) firmwareProgress) / len) * 100);
            if ((i > 0) && (i % FirmwareOperations.FIRMWARE_SYNC_PACKET == 0)) {
                connection.writeCharacteristic(getFirmwareCharacteristicUUID(), getSyncCommand()).subscribe(val -> {
                            if (d)
                                UserError.Log.d(TAG, "Wrote Sync" + progressPercent + "%");
                        },
                        throwable -> {
                            if (d)
                                UserError.Log.e(TAG, "Could not write Sync: " + throwable);
                            resetFirmwareState(false);
                        }
                );
            }
        }
        if (firmwareProgress < len) { //last chunk
            byte[] fwChunk = Arrays.copyOfRange(fwbytes, packets * packetLength, len);
            connection.writeCharacteristic(getFirmwareDataCharacteristicUUID(), fwChunk)
                    .subscribe(val -> {
                                if (d)
                                    UserError.Log.d(TAG, "Wrote last fwChunk");
                            },
                            throwable -> {
                                if (d)
                                    UserError.Log.e(TAG, "Could not write last fwChunk: " + throwable);
                                resetFirmwareState(false);
                            }
                    );
        }
        nextSequence();
        sendChecksum();
    }

    protected void sendChecksum() {
        connection.writeCharacteristic(getFirmwareCharacteristicUUID(), getChecksumCommand())
                .subscribe(val -> {
                            if (d)
                                UserError.Log.d(TAG, "Wrote getChecksumCommand");
                            nextSequence();
                        },
                        throwable -> {
                            if (d)
                                UserError.Log.e(TAG, "Could not write getChecksumCommand: " + throwable);
                            resetFirmwareState(false);
                        }
                );
    }

    public void resetFirmwareState(Boolean result) {
        resetFirmwareState(result, null);
    }

    public void resetFirmwareState(Boolean result, String customText) {
        if (watchfaceSubscription != null) {
            watchfaceSubscription.unsubscribe();
            watchfaceSubscription = null;
        }
        mState.setSequenceState(SLEEP);
        service.resetFirmwareState(result, customText);
    }

    public enum FirmwareType {
        FIRMWARE((byte) 0),
        FONT((byte) 1),
        RES((byte) 2),
        RES_COMPRESSED((byte) 130),
        GPS((byte) 3),
        GPS_CEP((byte) 4),
        GPS_ALMANAC((byte) 5),
        WATCHFACE((byte) 8),
        FONT_LATIN((byte) 11),
        INVALID(Byte.MIN_VALUE);

        public final byte value;

        FirmwareType(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }


}
