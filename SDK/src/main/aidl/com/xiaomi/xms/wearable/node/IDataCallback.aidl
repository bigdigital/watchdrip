// IDataCallback.aidl
package com.xiaomi.xms.wearable.node;

// Declare any non-default types here with import statements
import com.xiaomi.xms.wearable.Status;
import com.xiaomi.xms.wearable.node.DataItem;

interface IDataCallback {
     void onResult(in DataItem item, in Bundle bundle);

     void onFailure(in Status status);
}