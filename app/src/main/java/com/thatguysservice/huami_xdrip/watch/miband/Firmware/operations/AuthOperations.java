package com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations;

import android.annotation.SuppressLint;
import android.os.Environment;

import com.polidea.rxandroidble2.RxBleConnection;
import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.R;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.utils.HexDump;
import com.thatguysservice.huami_xdrip.utils.chiper.CipherUtils;
import com.thatguysservice.huami_xdrip.watch.miband.Const;
import com.thatguysservice.huami_xdrip.watch.miband.MiBand;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandService;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandType;
import com.thatguysservice.huami_xdrip.watch.miband.message.BaseMessage;
import com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.AUTH_FAIL;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.AUTH_MIBAND4_CODE_FAIL;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.AUTH_MIBAND4_FAIL;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.AUTH_REQUEST_RANDOM_AUTH_NUMBER;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.AUTH_RESPONSE;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.AUTH_SEND_ENCRYPTED_AUTH_NUMBER;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.AUTH_SEND_KEY;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.AUTH_SUCCESS;


public class AuthOperations extends BaseMessage {
    protected MiBandService service;
    protected MiBandType mibandType;
    protected String TAG = MiBandService.class.getSimpleName();
    protected RxBleConnection connection;

    protected byte[] localKey;
    private byte authFlags = OperationCodes.AUTH_BYTE;
    private byte cryptFlags = OperationCodes.AUTH_CRYPT_FLAG;

    public AuthOperations(MiBandType mibandType, MiBandService service) {
        this.service = service;
        this.mibandType = mibandType;
        this.connection = service.getConection();
    }

    public boolean isV2Protocol(){
        return false;
    }

    public static Boolean isValidAuthKey(String authKey) {
        return (authKey.length() == 32) && authKey.matches("[a-zA-Z0-9]+");
    }

    public static String getAuthCodeFromFilesSystem(String mac) {
        String authKey = "";
        String macFileName = mac.replace(":", "").toUpperCase();
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/freemyband/" + "miband" + macFileName + ".txt";
        File f = new File(fileName);
        if (f.exists() && f.isFile()) {
            try {
                FileInputStream fin = null;
                fin = new FileInputStream(f);
                BufferedReader br = new BufferedReader(new InputStreamReader(fin));

                String line = null;
                while ((line = br.readLine()) != null) {
                    // System.out.println(line);
                    String[] splited = line.split(";");
                    if (splited[0].equalsIgnoreCase(mac)) {
                        authKey = splited[1];
                    } else continue;
                }
                br.close();
            } catch (FileNotFoundException e) {

            } catch (IOException e) {
            }
        }
        return authKey.toLowerCase();
    }

    public static byte[] encryptAES(final byte[] keyBytes, byte[] secretKey) {
        try {
            final SecretKeySpec newKey = new SecretKeySpec(secretKey, "AES");
            final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, newKey);
            return cipher.doFinal(keyBytes);
        } catch (Exception e) {
            return null;
        }
    }

    public byte[] getLocalKey() {
        return localKey;
    }

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_AUTH;
    }

    public boolean initAuthKey() {
        String authKey = MiBand.getPersistentAuthKey();
        if (MiBandType.supportPairingKey(MiBand.getMibandType())) {
            if (authKey.isEmpty()) {
                authKey = MiBand.getAuthKey();
                if (authKey.isEmpty()) {
                    authKey = getAuthCodeFromFilesSystem(MiBand.getMac());
                }
                if (!isValidAuthKey(authKey)) {
                    Helper.static_toast_long(String.format(HuamiXdrip.getAppContext().getString(R.string.miband_wrong_auth_text), MiBand.getMibandType()));
                    return false;
                } else {
                    MiBand.setAuthKey(authKey);
                }
            }
        }
        if (!isValidAuthKey(authKey)) {
            authKey = "";
        }
        UserError.Log.d(TAG, "authKey: " + authKey);
        initLocalKey(authKey);

        return true;
    }

    @SuppressLint("CheckResult")
    public void processAuthCharacteristic(byte[] value) {
        if (value[0] == AUTH_RESPONSE &&
                value[1] == AUTH_SEND_KEY &&
                value[2] == AUTH_SUCCESS) {
            connection.writeCharacteristic(getCharacteristicUUID(), getAuthKeyRequest()) //get random key from band
                    .subscribe(val -> {
                        UserError.Log.d(TAG, "Wrote OPCODE_AUTH_REQ1: " + Helper.bytesToHex(val));
                    }, throwable -> {
                        UserError.Log.e(TAG, "Could not write OPCODE_AUTH_REQ1: " + throwable);
                    });
        } else if (value[0] == AUTH_RESPONSE &&
                (value[1]) == (cryptFlags | AUTH_REQUEST_RANDOM_AUTH_NUMBER) &&
                value[2] == AUTH_SUCCESS) {
            byte[] tmpValue = Arrays.copyOfRange(value, 3, 19);
            try {
                byte[] authReply = calculateAuthReply(tmpValue);
                connection.writeCharacteristic(getCharacteristicUUID(), authReply) //get random key from band
                        .subscribe(val -> {
                            UserError.Log.d(TAG, "Wrote OPCODE_AUTH_REQ2: " + Helper.bytesToHex(val));
                        }, throwable -> {
                            UserError.Log.e(TAG, "Could not write OPCODE_AUTH_REQ2: " + throwable);
                        });
            } catch (Exception e) {
                Helper.static_toast_long(e.getMessage());
                UserError.Log.e(TAG, (e.getMessage()));
                service.changeState(MiBandService.MiBandState.AUTHORIZE_FAILED);
            }
        } else if (value[0] == AUTH_RESPONSE &&
                (value[1] & 0x0f) == AUTH_SEND_ENCRYPTED_AUTH_NUMBER &&
                value[2] == AUTH_SUCCESS) {
            if (MiBand.getPersistentAuthMac().isEmpty()) {
                MiBand.setPersistentAuthMac(MiBand.getMac());
                MiBand.setPersistentAuthKey(Helper.bytesToHex(getLocalKey()), MiBand.getPersistentAuthMac());
                String msg = String.format(HuamiXdrip.getAppContext().getString(R.string.miband_success_auth_text), MiBand.getMibandType());
                Helper.static_toast_long(msg);
                UserError.Log.e(TAG, msg);
            }
            service.changeNextState();
        } else if (value[0] == AUTH_RESPONSE &&
                (((value[2] & 0x0f) == AUTH_FAIL) || (value[2] == AUTH_MIBAND4_FAIL) || (value[2] == AUTH_MIBAND4_CODE_FAIL))) {
            MiBand.setPersistentAuthKey("", MiBand.getPersistentAuthMac());
            String msg = String.format(HuamiXdrip.getAppContext().getString(R.string.miband_error_auth_text), MiBand.getMibandType());
            Helper.static_toast_long(msg);
            UserError.Log.e(TAG, msg);
            service.changeState(MiBandService.MiBandState.AUTHORIZE_FAILED);
        }
    }

    @SuppressLint("CheckResult")
    public void startAuthorisation() {
        if (MiBand.isAuthenticated()) {
            connection.writeCharacteristic(getCharacteristicUUID(), getAuthKeyRequest()) //get random key from band
                    .subscribe(val -> {
                        UserError.Log.d(TAG, "Wrote getAuthKeyRequest: " + Helper.bytesToHex(val));
                    }, throwable -> {
                        UserError.Log.e(TAG, "Could not getAuthKeyRequest: " + throwable);
                    });
        } else {
            connection.writeCharacteristic(getCharacteristicUUID(), getAuthCommand())
                    .subscribe(characteristicValue -> {
                                UserError.Log.d(TAG, "Wrote getAuthCommand, got: " + Helper.bytesToHex(characteristicValue));
                            },
                            throwable -> {
                                UserError.Log.e(TAG, "Could not write getAuthCommand: " + throwable);
                            }
                    );
        }
    }

    private void initLocalKey(String authKey) {
        if (MiBandType.useAlternativeAuthFlag(mibandType)) {
            authFlags = OperationCodes.AUTH_BYTE_ALTERNATIVE;
        }

        if (MiBandType.useAlternativeCryptFlag(mibandType)) {
            cryptFlags = OperationCodes.AUTH_CRYPT_FLAG_ALTERNATIVE;
        }
        localKey = CipherUtils.getRandomKey();
        if (authKey != null && !authKey.isEmpty()) {
            byte[] srcBytes = authKey.trim().getBytes();
            if (authKey.length() == 34 && authKey.substring(0, 2).equals("0x")) {
                srcBytes = HexDump.hexStringToByteArray(authKey.substring(2));
            } else srcBytes = HexDump.hexStringToByteArray(authKey);
            System.arraycopy(srcBytes, 0, localKey, 0, Math.min(srcBytes.length, 16));
        }

        UserError.Log.d(TAG, "localKey: " + Helper.bytesToHex(localKey));
    }

    // write key to device
    private byte[] getAuthCommand() {
        init(18);
        putData(AUTH_SEND_KEY);
        putData(authFlags);
        putData(getLocalKey());
        return getBytes();
    }

    private byte[] getAuthKeyRequest() {
        if (cryptFlags == 0x00) {
            init(2);
            putData(AUTH_REQUEST_RANDOM_AUTH_NUMBER);
            putData(authFlags);
        } else {
            init(5);
            putData((byte) (cryptFlags | AUTH_REQUEST_RANDOM_AUTH_NUMBER));
            putData(authFlags);
            putData((byte) 0x02);
            putData((byte) 0x01);
            putData((byte) 0x00);
        }
        return getBytes();
    }

    private byte[] calculateAuthReply(byte[] responseAuthKey) {
        UserError.Log.d(TAG, "Calculating localKey reply for: " + Helper.bytesToHex(getLocalKey()));
        final byte[] result = encryptAES(responseAuthKey, getLocalKey());
        if (result == null) throw new RuntimeException("Cannot calculate auth reply");
        UserError.Log.d(TAG, "Derived: " + Helper.bytesToHex(result));
        init(2 + result.length);
        putData((byte) (AUTH_SEND_ENCRYPTED_AUTH_NUMBER | cryptFlags));
        putData(authFlags);
        putData(result);
        return getBytes();
    }
}
