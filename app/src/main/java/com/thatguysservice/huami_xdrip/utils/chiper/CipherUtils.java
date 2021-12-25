package com.thatguysservice.huami_xdrip.utils.chiper;

import android.util.Base64;
import android.util.Log;

import com.thatguysservice.huami_xdrip.models.JoH;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CipherUtils {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    static final String TAG = "jamorham cip";
    static final byte[] errorbyte = {};

    public static byte[] encrypt(byte[] ivBytes, byte[] keyBytes, byte[] textBytes) {
        try {
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);
            return cipher.doFinal(textBytes);
        } catch (Exception e) {
            Log.e(TAG, "Error during encryption: " + e.toString());
            return errorbyte;
        }
    }

    public static byte[] decrypt(byte[] ivBytes, byte[] keyBytes, byte[] textBytes) {
        try {
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec);
            return cipher.doFinal(textBytes);
        } catch (Exception e) {
            return errorbyte;
        }
    }

    private static byte[] getKeyBytes(String mykey) {
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(mykey.getBytes(Charset.forName("UTF-8")));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Password creation exception: " + e.toString());
            return errorbyte;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String hex) {
        try {
            int length = hex.length();
            byte[] bytes = new byte[length / 2];
            for (int i = 0; i < length; i += 2) {
                bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
            }
            return bytes;
        } catch (Exception e){
            Log.e(TAG,"Got Exception: "+e.toString());
            return new byte[0];
        }
    }

    public static String getSHA256(byte[] mydata) {
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("SHA256");
            digest.update(mydata);
            return bytesToHex(digest.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA hash exception: " + e.toString());
            return null;
        }
    }

    public static String getSHA256(String mykey) {
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("SHA256");
            digest.update(mykey.getBytes(Charset.forName("UTF-8")));
            return bytesToHex(digest.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA hash exception: " + e.toString());
            return null;
        }
    }

    public static String getMD5(String mykey) {
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(mykey.getBytes(Charset.forName("UTF-8")));
            return bytesToHex(digest.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "MD5 hash exception: " + e.toString());
            return null;
        }
    }

    public static byte[] getRandomKey() {
        byte[] keybytes = new byte[16];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(keybytes);
        return keybytes;
    }

    public static String getRandomHexKey() {
        return bytesToHex(getRandomKey());
    }
}


