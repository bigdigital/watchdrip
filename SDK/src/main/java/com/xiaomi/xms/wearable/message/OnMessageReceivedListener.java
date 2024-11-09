package com.xiaomi.xms.wearable.message;

/**
 * @author user
 */
public interface OnMessageReceivedListener {
    void onMessageReceived(String id, byte[] message);
}
