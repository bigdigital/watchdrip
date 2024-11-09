package com.xiaomi.xms.wearable.notify;

import android.content.Context;
import android.os.RemoteException;

import com.xiaomi.xms.wearable.BaseApi;
import com.xiaomi.xms.wearable.Status;

/**
 * @author user
 */
public class NotifyApi extends BaseApi {
    public NotifyApi(Context context) {
        super(context);
    }

    public void sendNotify(String id, String title, String message, Result<Status> result) {
        NotificationData data = new NotificationData(title, message);
        try {
            apiClient.proxyHandle.sendNotify(id, data, new INotifyCallback.Stub() {
                @Override
                public void padding() throws RemoteException {

                }

                @Override
                public void onResult(Status status) throws RemoteException {
                    result.onResult(status);
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
