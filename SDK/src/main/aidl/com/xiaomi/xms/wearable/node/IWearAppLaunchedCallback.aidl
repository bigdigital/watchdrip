// IWearAppLaunchedCallback.aidl
package com.xiaomi.xms.wearable.node;

// Declare any non-default types here with import statements
import com.xiaomi.xms.wearable.Status;

interface IWearAppLaunchedCallback {
    void onWearAppLaunched(in Status status);
}