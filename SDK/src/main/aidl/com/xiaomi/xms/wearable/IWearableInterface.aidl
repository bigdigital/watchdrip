// IWearableInterface.aidl
package com.xiaomi.xms.wearable;

// Declare any non-default types here with import statements
import com.xiaomi.xms.wearable.auth.Permission;
import com.xiaomi.xms.wearable.auth.IPermissionCheckCallback;
import com.xiaomi.xms.wearable.auth.IPermissionsCheckCallback;
import com.xiaomi.xms.wearable.auth.IPermissionCallback;
import com.xiaomi.xms.wearable.node.INodeCallback;
import com.xiaomi.xms.wearable.node.IWearAppInstalledCallback;
import com.xiaomi.xms.wearable.node.IWearAppLaunchedCallback;
import com.xiaomi.xms.wearable.node.DataItem;
import com.xiaomi.xms.wearable.node.IDataCallback;
import com.xiaomi.xms.wearable.node.IDataListener;
import com.xiaomi.xms.wearable.message.IMessageCallback;
import com.xiaomi.xms.wearable.message.IMessageListener;
import com.xiaomi.xms.wearable.IServiceConnectedListener;
import com.xiaomi.xms.wearable.notify.NotificationData;
import com.xiaomi.xms.wearable.notify.INotifyCallback;

interface IWearableInterface {
    int getServiceApiLevel();

    void checkPermission(String id, in Permission permission, in IPermissionCheckCallback listener);

    void checkPermissions(String id, in Permission[] permissions, in IPermissionsCheckCallback listener);

    void requestPermission(String id, in Permission[] permissions, in IPermissionCallback listener);

    void getConnectedNodes(in INodeCallback callback);

    void isWearAppInstalled(String packageName, in IWearAppInstalledCallback callback);

    void launchWearApp(String id, String packageName, in IWearAppLaunchedCallback callback);

    void query(String id, in DataItem item, in IDataCallback callback);

    void subscribe(String id, in DataItem item, in IDataListener listener);

    void unsubscribe(String id, in DataItem item);

    void sendMessage(String id, in byte[] data, in IMessageCallback callback);

    void addListener(String id, in IMessageListener listener, in IMessageCallback callback);

    void removeListener(String id, in IMessageCallback callback);

    void setServiceConnectedListener(in IServiceConnectedListener listener);

    void sendNotify(String id, in NotificationData data, in INotifyCallback callback);
}