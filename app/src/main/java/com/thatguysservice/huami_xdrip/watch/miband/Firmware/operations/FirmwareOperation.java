package com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations;

import android.annotation.SuppressLint;

import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceState;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.Sequence.SequenceStateVergeNew;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandService;
import com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes;

import java.util.Arrays;

import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_INIT;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_FIRMWARE_START_DATA;

public class FirmwareOperation extends FirmwareOperationsNew {


    public FirmwareOperation(byte[] file, SequenceState sequenceState, MiBandService service) {
        super(file, sequenceState, service);
    }


    @SuppressLint("CheckResult")
    @Override
    public synchronized void processFirmwareOperationSequence() {
        String seq = getSequence();
        switch (seq) {
            case SequenceState.TRANSFER_FW_START: {
                nextSequence();
                connection.writeCharacteristic(getFirmwareCharacteristicUUID(), getFirmwareStartCommand())
                        .subscribe(valB -> {
                                    UserError.Log.d(TAG, "Wrote Start command: " + Helper.bytesToHex(valB));
                                    processFirmwareSequence();
                                },
                                throwable -> {
                                    UserError.Log.e(TAG, "Could not write Start command: " + throwable);
                                    resetFirmwareState(false);
                                }
                        );
                break;
            }
            case SequenceStateVergeNew.TRANSFER_FW_DATA: {
                sendFirmwareData();
                break;
            }
            default: {
                super.processFirmwareOperationSequence();
            }
        }
    }

    @Override
    public byte[] getFwInfoCommand() {
        int fwSize = getSize();
        byte[] sizeBytes = fromUint24(fwSize);
        int arraySize = 4;
        boolean isFirmwareCode = getFirmwareType() == FirmwareType.FIRMWARE;
        if (!isFirmwareCode) {
            arraySize++;
        }
        byte[] bytes = new byte[arraySize];
        int i = 0;
        bytes[i++] = COMMAND_FIRMWARE_INIT;
        bytes[i++] = sizeBytes[0];
        bytes[i++] = sizeBytes[1];
        bytes[i++] = sizeBytes[2];
        if (!isFirmwareCode) {
            bytes[i] = getFirmwareType().value;
        }
        return bytes;
    }

    @Override
    protected byte[] prepareFWUploadInitCommand() {
        return new byte[]{COMMAND_FIRMWARE_INIT, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0xFF};
    }

    @SuppressLint("CheckResult")
    protected void sendFirmwareData() {
        byte[] fwbytes = getBytes();
        int len = getSize();

        int packetLength = getPacketLength();
        if (packetLength > 200) packetLength = 200;
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
            if ((i > 0) && (i % FirmwareOperationsNew.FIRMWARE_SYNC_PACKET == 0)) {
                connection.writeCharacteristic(getFirmwareCharacteristicUUID(), getSyncCommand()).subscribe(val -> {
                            if (!fwStateWasReseted) {
                                updateWfProgress(progressPercent);
                            }
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
    }

    @Override
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
                    case COMMAND_FIRMWARE_START_DATA: {
                        nextSequence();
                        sendChecksum();
                        break;
                    }
                    default: {
                        super.processFirmwareOperationNotifications(value);
                    }
                }
            } catch (Exception ex) {
                resetFirmwareState(false);
            }
        } else {
            super.processFirmwareOperationNotifications(value);
        }
    }

    private static int getCRC16(byte[] seq) {
        int crc = 0xFFFF;

        for (byte b : seq) {
            crc = ((crc >>> 8) | (crc << 8)) & 0xffff;
            crc ^= (b & 0xff);//byte to int, trunc sign
            crc ^= ((crc & 0xff) >> 4);
            crc ^= (crc << 12) & 0xffff;
            crc ^= ((crc & 0xFF) << 5) & 0xffff;
        }
        crc &= 0xffff;
        return crc;
    }

    @Override
    public byte[] getChecksumCommand() {
        byte[] bytes = fromUint16(getCRC16(fw));
        return new byte[]{
                OperationCodes.COMMAND_FIRMWARE_CHECKSUM,
                bytes[0],
                bytes[1],
        };
    }

    @Override
    protected byte[] getFirmwareStartCommand() {
        return new byte[]{COMMAND_FIRMWARE_START_DATA};
    }

}
