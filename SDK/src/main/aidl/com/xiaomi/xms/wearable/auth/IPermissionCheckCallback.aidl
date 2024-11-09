// IPermissionCheckCallback.aidl
package com.xiaomi.xms.wearable.auth;

// Declare any non-default types here with import statements
import com.xiaomi.xms.wearable.Status;

interface IPermissionCheckCallback {
    void onPermissionGranted(boolean result);

    void onFailure(in Status status);
}