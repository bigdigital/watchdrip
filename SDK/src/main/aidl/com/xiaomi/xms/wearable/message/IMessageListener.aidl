// IMessageListener.aidl
package com.xiaomi.xms.wearable.message;

// Declare any non-default types here with import statements

interface IMessageListener {
    void onMessageReceived(String id, in byte[] data);
}