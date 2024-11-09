// IPermissionsCheckCallback.aidl
package com.xiaomi.xms.wearable.auth;

// Declare any non-default types here with import statements
import com.xiaomi.xms.wearable.Status;

interface IPermissionsCheckCallback {
    void onPermissionGranted(in boolean[] result);

    void onFailure(in Status status);
}