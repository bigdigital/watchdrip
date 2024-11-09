// IWearAppInstalledCallback.aidl
package com.xiaomi.xms.wearable.node;

// Declare any non-default types here with import statements
import com.xiaomi.xms.wearable.Status;

interface IWearAppInstalledCallback {
    void onWearAppInstalled(boolean result);

    void onFailure(in Status status);
}