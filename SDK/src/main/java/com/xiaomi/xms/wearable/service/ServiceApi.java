package com.xiaomi.xms.wearable.service;

import android.content.Context;

import com.xiaomi.xms.wearable.BaseApi;

/**
 * @author user
 */
public class ServiceApi extends BaseApi {
    public ServiceApi(Context context) {
        super(context);
    }

    public int getServiceApiLevel() {
        try {
            return apiClient.proxyHandle.getServiceApiLevel();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void registerServiceConnectionListener(OnServiceConnectionListener listener) {
        apiClient.onServiceConnectionListener.add(listener);
        if (!apiClient.isBind && apiClient.wearableInterface != null) {
            listener.onServiceConnected();
        }
    }

    public void unregisterServiceConnectionListener(OnServiceConnectionListener listener) {
        apiClient.onServiceConnectionListener.remove(listener);
    }
}
