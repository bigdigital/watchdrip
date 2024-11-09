package com.xiaomi.xms.wearable.node;

/**
 * @author user
 */
public interface OnDataChangedListener {
    void onDataChanged(String id, DataItem data, DataSubscribeResult result);
}
