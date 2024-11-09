// IMessageCallback.aidl
package com.xiaomi.xms.wearable.message;

// Declare any non-default types here with import statements
import com.xiaomi.xms.wearable.Status;

interface IMessageCallback {
    void onMessageSent(in Status status);
}