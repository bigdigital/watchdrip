package com.xiaomi.xms.wearable.message;

import android.content.Context;
import android.os.RemoteException;

import com.xiaomi.xms.wearable.BaseApi;
import com.xiaomi.xms.wearable.Status;

/**
 * @author user
 */
public class MessageApi extends BaseApi {
    public MessageApi(Context context) {
        super(context);
    }

    public void addListener(String id, OnMessageReceivedListener listener, Result<Status> result) {
        try {
            if (apiClient.onMessageReceivedListener != null) {
                throw new IllegalStateException("you have registered");
            }
            apiClient.proxyHandle.addListener(id, new IMessageListener.Stub() {
                @Override
                public void onMessageReceived(String id, byte[] message) throws RemoteException {
                    listener.onMessageReceived(id, message);
                }
            }, new IMessageCallback.Stub() {
                @Override
                public void onMessageSent(Status status) throws RemoteException {
                    if (status.isSuccess()) {
                        apiClient.onMessageReceivedListener = listener;
                    }
                    result.onResult(status);
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void removeListener(String id, Result<Status> result) {
        try {
            if (apiClient.onMessageReceivedListener == null) {
                throw new IllegalStateException("you have not registered");
            }
            apiClient.proxyHandle.removeListener(id, new IMessageCallback.Stub() {
                @Override
                public void onMessageSent(Status status) throws RemoteException {
                    if (status.isSuccess()) {
                        apiClient.onMessageReceivedListener = null;
                    }
                    result.onResult(status);
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    public void sendMessage(String id, byte[] data, Result<Status> result) {
        try {
            apiClient.proxyHandle.sendMessage(id, data, new IMessageCallback.Stub() {
                @Override
                public void onMessageSent(Status status) throws RemoteException {
                    result.onResult(status);
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
