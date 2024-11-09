package com.xiaomi.xms.wearable.node;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import com.xiaomi.xms.wearable.BaseApi;
import com.xiaomi.xms.wearable.Status;

import java.util.List;

/**
 * @author user
 */
public class NodeApi extends BaseApi {
    public NodeApi(Context context) {
        super(context);
    }

    public void getConnectedNodes(Callback<List<Node>> callback) {
        try {
            apiClient.proxyHandle.getConnectedNodes(new INodeCallback.Stub() {
                @Override
                public void onNodesConnected(List<Node> devices) throws RemoteException {
                    callback.onSuccess(devices);
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

    public void isWearAppInstalled(String packageName, Callback<Boolean> callback) {
        try {
            apiClient.proxyHandle.isWearAppInstalled(packageName, new IWearAppInstalledCallback.Stub() {
                @Override
                public void onWearAppInstalled(boolean result) throws RemoteException {
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

    public void launchWearApp(String id, String packageName, Result<Status> result) {
        try {
            apiClient.proxyHandle.launchWearApp(id, packageName, new IWearAppLaunchedCallback.Stub() {
                @Override
                public void onWearAppLaunched(Status status) throws RemoteException {
                    result.onResult(status);
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void query(String id, DataItem data, Callback<DataQueryResult> callback) {
        try {
            apiClient.proxyHandle.query(id, data, new IDataCallback.Stub() {
                @Override
                public void onResult(DataItem item, Bundle bundle) throws RemoteException {
                    DataQueryResult result = new DataQueryResult();
                    if (item.getType() == DataItem.ITEM_CONNECTION.getType()) {
                        result.isConnected = bundle.getInt(DataItem.KEY_CONNECTION_STATUS, 0) == 1;
                    } else if (item.getType() == DataItem.ITEM_CHARGING.getType()) {
                        result.isCharging = bundle.getBoolean(DataItem.KEY_CHARGING_STATUS);
                    } else if (item.getType() == DataItem.ITEM_SLEEP.getType()) {
                        result.isSleeping = bundle.getBoolean(DataItem.KEY_SLEEP_STATUS);
                    } else if (item.getType() == DataItem.ITEM_WEARING.getType()) {
                        result.isWearing = bundle.getBoolean(DataItem.KEY_WEARING_STATUS);
                    } else if (item.getType() == DataItem.ITEM_BATTERY.getType()) {
                        result.battery = bundle.getInt(DataItem.KEY_BATTERY_STATUS);
                    }
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

    public void subscribe(String id, DataItem data, OnDataChangedListener listener) {
        try {
            apiClient.onDataChangedListener.put(data.getType(), listener);
            apiClient.proxyHandle.subscribe(id, data, apiClient.dataListener);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void unsubscribe(String id, DataItem data) {
        try {
            apiClient.onDataChangedListener.remove(data.getType());
            apiClient.proxyHandle.unsubscribe(id, data);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
