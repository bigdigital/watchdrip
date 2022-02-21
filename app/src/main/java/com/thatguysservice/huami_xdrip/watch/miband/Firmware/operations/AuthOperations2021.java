package com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations;

import android.annotation.SuppressLint;

import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.R;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.watch.miband.Const;
import com.thatguysservice.huami_xdrip.watch.miband.MiBand;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandService;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandType;
import com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes;

import java.util.Random;
import java.util.UUID;

import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.AUTH_RESPONSE;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.AUTH_SUCCESS;

public class AuthOperations2021 extends AuthOperations {
    static {
        System.loadLibrary("tiny-edhc");
    }

    private final byte[] reassembleBuffer = new byte[512];
    private byte[] privateKey = new byte[24];
    private int lastSequenceNumber = 0;
    private int reassembleBuffer_pointer = 0;
    private int reassembleBuffer_expectedBytes = 0;

    public AuthOperations2021(MiBandType mibandType, MiBandService service) {
        super(mibandType, service);
    }

    private native byte[] ecdh_lib_get_public_key(byte[] private_key);

    private native byte[] ecdh_lib_get_shared_key(byte[] private_key, byte[] public_key);

    private byte[] generatePublicKey() {
        Random r = new Random();
        r.nextBytes(privateKey);
        return ecdh_lib_get_public_key(privateKey);
    }

    @Override
    public boolean isV2Protocol(){
        return true;
    }

    private void testECDC() {
        byte[] privateKey = new byte[]{(byte) 0xE7, (byte) 0xD9, (byte) 0x45, (byte) 0x6D, (byte) 0xBC, (byte) 0xFC, (byte) 0xC1, (byte) 0xAD, (byte) 0xA6, (byte) 0x24, (byte) 0x33, (byte) 0xB2, (byte) 0xE0, (byte) 0xA9, (byte) 0x51, (byte) 0x06, (byte) 0x36, (byte) 0x88, (byte) 0x84, (byte) 0x62, (byte) 0x2F, (byte) 0xD6, (byte) 0x8E, (byte) 0x76};
        byte[] remotePublicEC = new byte[]{(byte) 0x2C, (byte) 0xFC, (byte) 0x4E, (byte) 0xFF, (byte) 0xCA, (byte) 0xC8, (byte) 0x7B, (byte) 0x43, (byte) 0x56, (byte) 0xDF, (byte) 0x24, (byte) 0xB3, (byte) 0x1F, (byte) 0xA9, (byte) 0x1E, (byte) 0xF3, (byte) 0xA1, (byte) 0x76, (byte) 0x82, (byte) 0xEE, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x4E, (byte) 0x5C, (byte) 0x03, (byte) 0xD5, (byte) 0x32, (byte) 0x79, (byte) 0xEA, (byte) 0x19, (byte) 0x84, (byte) 0x6D, (byte) 0x9F, (byte) 0xF8, (byte) 0x05, (byte) 0x34, (byte) 0xAF, (byte) 0xF3, (byte) 0xC4, (byte) 0x59, (byte) 0x0B, (byte) 0xD3, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        byte[] publicEC = ecdh_lib_get_public_key(privateKey);
        byte[] sharedEC = ecdh_lib_get_shared_key(privateKey, remotePublicEC);
        //publicec_test = 541B584FB82A91086D7918CE0E35A0D42D95BEB1000000006B15CBBC7412B2465C0569E442AF30F8AD86909B04000000
        //sharedEC_test = 1B9C1215CD43BB55518487FF790134C6BB49F35C06000000AAD39AEC46BA13373D9768B1E56B91B6FF10B86200000000
        UserError.Log.d(TAG, "publicEC:" + Helper.bytesToHex(publicEC));
        UserError.Log.d(TAG, "sharedEC:" + Helper.bytesToHex(sharedEC));
    }

    private byte[] getAuthKeyRequest() {
        init(48 + 4);
        putData((byte) 0x04);
        putData((byte) 0x02);
        putData((byte) 0x00);
        putData((byte) 0x02);
        putData(generatePublicKey());
        return getBytes();
    }

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_V2_READ;
    }

    @Override
    public void startAuthorisation() {
        service.writeToChunkedV2(OperationCodes.CHUNKED_V2_ENDPOINT_AUTH, service.getNextHandle(), getAuthKeyRequest(), false);
    }


    @SuppressLint("CheckResult")
    public void processAuthCharacteristic(byte[] value) {
        if (value.length > 1 && value[0] == 0x03) {
            int sequenceNumber = value[4];
            int headerSize;
            if (sequenceNumber == 0 && value[9] == (byte) OperationCodes.CHUNKED_V2_ENDPOINT_AUTH && value[10] == 0x00 && value[11] == AUTH_RESPONSE && value[12] == 0x04 && value[13] == AUTH_SUCCESS) {
                reassembleBuffer_pointer = 0;
                headerSize = 14;
                reassembleBuffer_expectedBytes = value[5] - 3;
            } else if (sequenceNumber > 0) {
                if (sequenceNumber != lastSequenceNumber + 1) {
                    UserError.Log.e(TAG, "unexpected sequence number");
                    return;
                }
                headerSize = 5;
            } else if (value[9] == (byte) OperationCodes.CHUNKED_V2_ENDPOINT_AUTH && value[10] == 0x00 && value[11] == AUTH_RESPONSE && value[12] == 0x05 && value[13] == AUTH_SUCCESS) {
                if (MiBand.getPersistentAuthMac().isEmpty()) {
                    MiBand.setPersistentAuthMac(service.getAddress());
                    MiBand.setPersistentAuthKey(Helper.bytesToHex(getLocalKey()), service.getAddress());
                    String msg = String.format(HuamiXdrip.getAppContext().getString(R.string.miband_success_auth_text), MiBand.getMibandType());
                    Helper.static_toast_long(msg);
                    UserError.Log.e(TAG, msg);
                }
                service.changeNextState();
                return;
            } else {
                UserError.Log.e(TAG, "Unhandled auth characteristic changed");
                super.processAuthCharacteristic(value);
                return;
            }

            int bytesToCopy = value.length - headerSize;
            System.arraycopy(value, headerSize, reassembleBuffer, reassembleBuffer_pointer, bytesToCopy);
            reassembleBuffer_pointer += bytesToCopy;

            lastSequenceNumber = sequenceNumber;
            if (reassembleBuffer_pointer == reassembleBuffer_expectedBytes) {
                byte[] remoteRandom = new byte[16];
                byte[] remotePublicEC = new byte[48];

                System.arraycopy(reassembleBuffer, 0, remoteRandom, 0, 16);
                System.arraycopy(reassembleBuffer, 16, remotePublicEC, 0, 48);
                byte[] shared_key = ecdh_lib_get_shared_key(privateKey, remotePublicEC);
                service.encryptedSequenceNr = ((shared_key[0] & 0xff) | ((shared_key[1] & 0xff) << 8) | ((shared_key[2] & 0xff) << 16) | ((shared_key[3] & 0xff) << 24));

                byte[] secretKey = getLocalKey();
                byte[] finalSharedSessionAES = new byte[16];
                for (int i = 0; i < 16; i++) {
                    finalSharedSessionAES[i] = (byte) (shared_key[i + 8] ^ secretKey[i]);
                }
                service.sharedSessionKey = finalSharedSessionAES;
                try {
                    byte[] encryptedRandom1 = AuthOperations.encryptAES(remoteRandom, secretKey);
                    byte[] encryptedRandom2 = AuthOperations.encryptAES(remoteRandom, finalSharedSessionAES);
                    if (encryptedRandom1.length == 16 && encryptedRandom2.length == 16) {
                        byte[] command = new byte[33];
                        command[0] = 0x05;
                        System.arraycopy(encryptedRandom1, 0, command, 1, 16);
                        System.arraycopy(encryptedRandom2, 0, command, 17, 16);
                        UserError.Log.d(TAG, "Sending double encrypted random to device");
                        service.writeToChunkedV2(OperationCodes.CHUNKED_V2_ENDPOINT_AUTH, service.getNextHandle(), command, false);
                    }
                } catch (Exception e) {
                    UserError.Log.e(TAG, ("AES encryption failed"));
                }
            }
            return;
        }
        super.processAuthCharacteristic(value);
    }
}
