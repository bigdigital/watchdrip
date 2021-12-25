package com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations;

import android.annotation.SuppressLint;

import com.thatguysservice.huami_xdrip.models.JoH;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.utils.chiper.CRC16;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceState;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceStateVergeNew;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandService;
import com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes;

import java.util.Arrays;

import static com.thatguysservice.huami_xdrip.services.JamBaseBluetoothSequencer.BaseState.SLEEP;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_REBOOT;

public class FirmwareOperationsNew extends FirmwareOperations {

    private final byte COMMAND_REQUEST_PARAMETERS = (byte) 0xd0;
    private final byte COMMAND_SEND_FIRMWARE_INFO = (byte) 0xd2;
    private final byte COMMAND_START_TRANSFER = (byte) 0xd3;
    private final byte REPLY_UPDATE_PROGRESS = (byte) 0xd4;
    private final byte COMMAND_COMPLETE_TRANSFER = (byte) 0xd5;
    private final byte COMMAND_FINALIZE_UPDATE = (byte) 0xd6;


    private int mChunkLength = -1;

    public FirmwareOperationsNew(byte[] file, SequenceState sequenceState, MiBandService service) {
        super(file, sequenceState, service);
    }

    public byte[] getRequestParametersCommand() {
        return new byte[]{COMMAND_REQUEST_PARAMETERS};
    }

    @SuppressLint("CheckResult")
    @Override
    public synchronized void processFirmwareOperationSequence() {
        String seq = getSequence();
        switch (seq) {
            case SequenceStateVergeNew.REQUEST_PARAMETERS: {
                nextSequence();
                connection.writeCharacteristic(getFirmwareCharacteristicUUID(), getRequestParametersCommand())
                        .subscribe(val -> {
                                    if (d)
                                        UserError.Log.d(TAG, "Wrote getRequestParametersCommand: " + JoH.bytesToHex(val));
                                },
                                throwable -> {
                                    UserError.Log.e(TAG, "Could not write getRequestParametersCommand: " + throwable);
                                    resetFirmwareState(false);
                                }
                        );
                break;
            }

            default: {
                super.processFirmwareOperationSequence();
            }
        }
    }

    @Override
    public void processFirmwareOperationNotifications(byte[] value) {
        if (value.length != 3 && value.length != 6 && value.length != 11) {
            UserError.Log.e(TAG, "Notifications should be 3, 6 or 11 bytes long.");
            return;
        }

        boolean success = (value[2] == OperationCodes.SUCCESS) || ((value[1] == REPLY_UPDATE_PROGRESS) && value.length == 6);
        String seq = getSequence();
        if (value[0] == OperationCodes.RESPONSE && success) {
            try {
                switch (value[1]) {
                    case COMMAND_REQUEST_PARAMETERS: {
                        if (seq.equals(SequenceStateVergeNew.WAITING_REQUEST_PARAMETERS_RESPONSE)) {
                            mChunkLength = (value[4] & 0xff) | ((value[5] & 0xff) << 8);
                            UserError.Log.d(TAG, ("got chunk length of " + mChunkLength));
                            nextSequence();
                            processFirmwareSequence();
                        }
                    }
                    case COMMAND_SEND_FIRMWARE_INFO: {
                        if (seq.equals(SequenceStateVergeNew.WAITING_TRANSFER_SEND_WF_INFO_RESPONSE)) {
                            nextSequence();
                            processFirmwareSequence();
                        }
                        break;
                    }
                    case COMMAND_START_TRANSFER: {
                        if (seq.equals(SequenceStateVergeNew.WAITING_TRANSFER_FW_START_RESPONSE)) {
                            nextSequence();
                            sendFirmwareDataChunk(0);
                        }
                        break;
                    }
                    case OperationCodes.COMMAND_FIRMWARE_START_DATA:
                        sendChecksum();
                        break;
                    case REPLY_UPDATE_PROGRESS:
                        int offset = (value[2] & 0xff) | ((value[3] & 0xff) << 8) | ((value[4] & 0xff) << 16) | ((value[5] & 0xff) << 24);
                        if (d)
                            UserError.Log.d(TAG, "update progress " + offset + " bytes");
                        sendFirmwareDataChunk(offset);
                        break;
                    case COMMAND_COMPLETE_TRANSFER:
                        sendFinalize();
                        break;
                    case COMMAND_FINALIZE_UPDATE: {
                        if (seq.equals(SequenceStateVergeNew.WAITING_FINALIZE_FW_DATA)) {
                            retryCount = 0;
                            nextSequence();
                            processFirmwareSequence();
                        }
                        break;
                    }
                    case COMMAND_FIRMWARE_REBOOT: {
                        UserError.Log.e(TAG, "Reboot command successfully sent.");
                        resetFirmwareState(true);
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

    protected byte[] getWatcfaceIdCommand(){
        int fwSize = getSize();
        byte[] fwBytes = getBytes();
        byte[] sizeBytes = fromUint32(fwSize);
        return new byte[]{OperationCodes.COMMAND_WATCHFACE_UID, 0x00,
                sizeBytes[0],
                sizeBytes[1],
                sizeBytes[2],
                sizeBytes[3],
                fwBytes[18],
                fwBytes[19],
                fwBytes[20],
                fwBytes[21]
        };
    }

    @Override
    public byte[] getFwInfoCommand() {
        int fwSize = getSize();
        byte[] sizeBytes = fromUint32(fwSize);
        int crc32 = (int) JoH.checksum(fw);
        byte[] crcBytes = fromUint32(crc32);
        byte[] chunkSizeBytes = fromUint16(mChunkLength);
        byte[] bytes = new byte[]{
                COMMAND_SEND_FIRMWARE_INFO,
                getFirmwareType().value,
                sizeBytes[0],
                sizeBytes[1],
                sizeBytes[2],
                sizeBytes[3],
                crcBytes[0],
                crcBytes[1],
                crcBytes[2],
                crcBytes[3],
                chunkSizeBytes[0],
                chunkSizeBytes[1],
                0, // ??
                0, // index
                1, // count
                sizeBytes[0], // total size? right now it is equal to the size above
                sizeBytes[1],
                sizeBytes[2],
                sizeBytes[3]
        };
        return bytes;
    }

    @Override
    public byte[] getFirmwareStartCommand() {
        return new byte[]{COMMAND_START_TRANSFER, (byte) 0x1};
    }

    @Override
    public byte[] getChecksumCommand() {
        byte[] bytes = CRC16.calculate(fw, 0, fw.length);
        return new byte[]{
                OperationCodes.COMMAND_FIRMWARE_CHECKSUM,
                bytes[0],
                bytes[1],
        };
    }

    @SuppressLint("CheckResult")
    private void sendFirmwareDataChunk(int offset) {
        byte[] fwbytes = getBytes();
        int len = getSize();
        int remaining = len - offset;
        final int packetLength = getPacketLength();

        int chunkLength = mChunkLength;
        if (remaining < mChunkLength) {
            chunkLength = remaining;
        }

        int packets = chunkLength / packetLength;
        int chunkProgress = 0;

        if (remaining <= 0) {
            sendTransferComplete();
            return;
        }
        for (int i = 0; i < packets; i++) {
            byte[] fwChunk = Arrays.copyOfRange(fwbytes, offset + i * packetLength, offset + i * packetLength + packetLength);
            // int finalI = i;
            connection.writeCharacteristic(getFirmwareDataCharacteristicUUID(), fwChunk).subscribe(val -> {
                    },
                    throwable -> {
                        if (d)
                            UserError.Log.e(TAG, "Could not write fwChunk: " + throwable);
                        resetFirmwareState(false);
                    }
            );
            chunkProgress += packetLength;
        }

        if (chunkProgress < chunkLength) {
            byte[] lastChunk = Arrays.copyOfRange(fwbytes, offset + packets * packetLength, offset + packets * packetLength + (chunkLength - chunkProgress));
            connection.writeCharacteristic(getFirmwareDataCharacteristicUUID(), lastChunk)
                    .subscribe(val -> {
                            },
                            throwable -> {
                                if (d)
                                    UserError.Log.e(TAG, "Could not write last fwChunk: " + throwable);
                                resetFirmwareState(false);
                            }
                    );
        }

        int progressPercent = (int) ((((float) (offset + chunkLength)) / len) * 100);
        if (d)
            UserError.Log.d(TAG, "Uploading progress: " + progressPercent);
    }

    @SuppressLint("CheckResult")
    protected void sendTransferComplete() {
        if (d)
            UserError.Log.d(TAG, "Transfer complete");

        connection.writeCharacteristic(getFirmwareCharacteristicUUID(), new byte[]{
                COMMAND_COMPLETE_TRANSFER})
                .subscribe(val -> {

                        },
                        throwable -> {
                            if (d)
                                UserError.Log.e(TAG, "Could not write transfer complete cmd: " + throwable);
                            resetFirmwareState(false);
                        }
                );
    }

    @SuppressLint("CheckResult")
    protected void sendFinalize() {
        if (d)
            UserError.Log.d(TAG, "Finalize firmware");
        nextSequence();
        connection.writeCharacteristic(getFirmwareCharacteristicUUID(), new byte[]{
                COMMAND_FINALIZE_UPDATE})
                .subscribe(val -> {
                            processFirmwareSequence();
                        },
                        throwable -> {
                            if (d)
                                UserError.Log.e(TAG, "Could not write Finalize firmware cmd: " + throwable);
                            resetFirmwareState(false);
                        }
                );
    }

}
