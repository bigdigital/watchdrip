// INodeCallback.aidl
package com.xiaomi.xms.wearable.node;

// Declare any non-default types here with import statements
import com.xiaomi.xms.wearable.Status;
import com.xiaomi.xms.wearable.node.Node;
import java.util.List;

interface INodeCallback {
    void onNodesConnected(in List<Node> devices);

    void onFailure(in Status status);
}