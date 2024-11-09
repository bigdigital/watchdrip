// IPermissionCallback.aidl
package com.xiaomi.xms.wearable.auth;

// Declare any non-default types here with import statements
import com.xiaomi.xms.wearable.Status;
import com.xiaomi.xms.wearable.auth.Permission;

interface IPermissionCallback {
    void onPermissionGranted(in Permission[] result);

    void onFailure(in Status status);
}