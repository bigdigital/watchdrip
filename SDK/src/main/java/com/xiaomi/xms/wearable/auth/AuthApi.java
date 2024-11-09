package com.xiaomi.xms.wearable.auth;

import android.content.Context;
import android.os.RemoteException;

import com.xiaomi.xms.wearable.BaseApi;
import com.xiaomi.xms.wearable.Status;

/**
 * @author user
 */
public class AuthApi extends BaseApi {
    public AuthApi(Context context) {
        super(context);
    }


    public void checkPermission(String id, Permission permission, Callback<Boolean> callback) {
        try {
            if (apiClient.wearableInterface == null) {
                throw new IllegalStateException();
            }
            apiClient.proxyHandle.checkPermission(id, permission, new IPermissionCheckCallback.Stub() {
                @Override
                public void onPermissionGranted(boolean result) throws RemoteException {
                    callback.onSuccess(result);
                }

                @Override
                public void onFailure(Status status) throws RemoteException {
                    callback.onFailure(status);
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void checkPermissions(String id, Permission[] permissions, Callback<Boolean[]> callback) {
        try {
            if (apiClient.wearableInterface == null) {
                throw new IllegalStateException();
            }
            apiClient.proxyHandle.checkPermissions(id, permissions, new IPermissionsCheckCallback.Stub() {
                @Override
                public void onPermissionGranted(boolean[] result) throws RemoteException {
                    Boolean[] tmp = new Boolean[result.length];
                    for (int i = 0; i < result.length; i++) {
                        tmp[i] = result[i];
                    }
                    callback.onSuccess(tmp);
                }

                @Override
                public void onFailure(Status status) throws RemoteException {
                    callback.onFailure(status);
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void requestPermission(String id, Permission[] permissions, Callback<Permission[]> callback) {
        try {
            if (apiClient.wearableInterface == null) {
                throw new IllegalStateException();
            }
            apiClient.proxyHandle.requestPermission(id, permissions, new IPermissionCallback.Stub() {
                @Override
                public void onPermissionGranted(Permission[] result) throws RemoteException {
                    callback.onSuccess(result);
                }

                @Override
                public void onFailure(Status status) throws RemoteException {
                    callback.onFailure(status);
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
