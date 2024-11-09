// INotifyCallback.aidl
package com.xiaomi.xms.wearable.notify;

// Declare any non-default types here with import statements
import com.xiaomi.xms.wearable.Status;

interface INotifyCallback {
    // padding method number
    void padding();

    void onResult(in Status status);
}