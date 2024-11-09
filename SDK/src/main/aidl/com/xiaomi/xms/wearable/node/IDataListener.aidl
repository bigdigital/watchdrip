// IDataListener.aidl
package com.xiaomi.xms.wearable.node;

// Declare any non-default types here with import statements
import com.xiaomi.xms.wearable.node.DataItem;

interface IDataListener {
    void onDataChanged(String id, in DataItem item, in Bundle bundle);
}