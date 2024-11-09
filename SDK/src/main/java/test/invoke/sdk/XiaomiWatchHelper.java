package test.invoke.sdk;

import android.content.Context;
import android.text.TextUtils;

import com.xiaomi.xms.wearable.BaseApi;
import com.xiaomi.xms.wearable.Status;
import com.xiaomi.xms.wearable.Wearable;
import com.xiaomi.xms.wearable.auth.AuthApi;
import com.xiaomi.xms.wearable.auth.Permission;
import com.xiaomi.xms.wearable.message.MessageApi;
import com.xiaomi.xms.wearable.message.OnMessageInitListener;
import com.xiaomi.xms.wearable.message.OnMessageReceivedListener;
import com.xiaomi.xms.wearable.node.DataItem;
import com.xiaomi.xms.wearable.node.DataQueryResult;
import com.xiaomi.xms.wearable.node.Node;
import com.xiaomi.xms.wearable.node.NodeApi;
import com.xiaomi.xms.wearable.node.OnDataChangedListener;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author user
 */
public class XiaomiWatchHelper {
    private final Context context;
    private boolean hasAvailableDevices = true;
    private boolean isRegister;
    private String lastMonitorDeviceId;
    private OnDataChangedListener monitorListener;
    private OnMessageReceivedListener receiver;
    private OnMessageInitListener initMessageListener;

    private XiaomiWatchHelper(Context context) {
        this.context = context;
    }

    public static XiaomiWatchHelper getInstance(Context context) {
        return new XiaomiWatchHelper(context);
    }

    private void hasAvailableDevices(Context context, BaseApi.Callback<Node> callback) {
        if (!this.hasAvailableDevices) {
            return;
        }
        NodeApi nodeApi = Wearable.getNodeApi(context);
        nodeApi.getConnectedNodes(new BaseApi.Callback<List<Node>>() {
            @Override
            public void onSuccess(List<Node> obj) {
                hasAvailableDevices = obj.isEmpty();
                for (Node each : obj) {
                    callback.onSuccess(each);
                }
            }

            @Override
            public void onFailure(Status status) {
                callback.onFailure(status);
            }
        });
    }

    private void tryToRequestPermission(Node node, BaseApi.Callback<Node> callback) {
        this.checkPermission(node, new BaseApi.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean obj) {
                callback.onSuccess(node);
            }

            @Override
            public void onFailure(Status status) {
                callback.onFailure(status);
            }
        });
    }

    private void getIsConnected(Node device, BaseApi.Callback<Node> callback) {
        NodeApi nodeApi = Wearable.getNodeApi(context);
        nodeApi.query(device.getId(), DataItem.ITEM_CONNECTION, new BaseApi.Callback<DataQueryResult>() {
            @Override
            public void onSuccess(DataQueryResult obj) {
                if (obj == null || !obj.isConnected) {
                    return;
                }
                if (!TextUtils.equals(lastMonitorDeviceId, device.getId())) {
                    lastMonitorDeviceId = device.getId();
                    if (monitorListener != null) {
                        nodeApi.unsubscribe(lastMonitorDeviceId, DataItem.ITEM_CONNECTION);
                        monitorListener = null;
                    }
                    nodeApi.subscribe(device.getId(), DataItem.ITEM_CONNECTION, getMonitorListener());
                }
                callback.onSuccess(device);
            }

            @Override
            public void onFailure(Status status) {
                callback.onFailure(status);
            }
        });
    }
    private void checkPermission(Node device, BaseApi.Callback<Boolean> callback) {
        AuthApi authApi = Wearable.getAuthApi(context);
        Permission[] permissions = new Permission[]{Permission.DEVICE_MANAGER, Permission.NOTIFY};
        authApi.checkPermissions(device.getId(), permissions, new BaseApi.Callback<Boolean[]>() {
            @Override
            public void onSuccess(Boolean[] obj) {
                boolean grant = true;
                for (Boolean each : obj) {
                    if (!each) {
                        grant = false;
                        break;
                    }
                }
                if (grant) {
                    callback.onSuccess(true);
                } else {
                    requestPermission(device, permissions, new BaseApi.Callback<Permission[]>() {
                        @Override
                        public void onSuccess(Permission[] obj) {
                            callback.onSuccess(true);
                        }

                        @Override
                        public void onFailure(Status status) {
                            callback.onFailure(status);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Status status) {
                callback.onFailure(status);
            }
        });
    }

    private OnDataChangedListener getMonitorListener() {
        synchronized (XiaomiWatchHelper.class) {
            if (monitorListener == null) {
                monitorListener = (id, data, result) -> sendUpdateMessageToWear();
            }
        }
        return monitorListener;
    }

    private void registerReceiverInternal(Node device) {
        if (receiver == null) {
            throw new IllegalStateException("Not set receiver");
        }
        MessageApi messageApi = Wearable.getMessageApi(context);

        if (isRegister && TextUtils.equals(device.getId(), lastMonitorDeviceId)) {
            messageApi.removeListener(device.getId(), obj -> {
            });
        }
        messageApi.addListener(device.getId(), receiver, obj -> {
        });
        isRegister = true;
    }

    /**
     * 向Mi Fitness或小米健康运动请求权限
     * 当前仅有两种权限，详见{@link Permission}常量定义
     *
     * @param device      目标id
     * @param permissions 权限
     * @param callback    结果回调
     */
    public void requestPermission(Node device, Permission[] permissions, BaseApi.Callback<Permission[]> callback) {
        AuthApi authApi = Wearable.getAuthApi(context);
        authApi.requestPermission(device.getId(), permissions, callback);
    }

    public void sendMessageToWear(String id, byte[] data, BaseApi.Result<Status> result) {
        MessageApi messageApi = Wearable.getMessageApi(context);
        messageApi.sendMessage(id, data, result);
    }

    public void launchApp(String packageName, BaseApi.Result<Status> result)
    {
        NodeApi nodeApi = Wearable.getNodeApi(context);
        nodeApi.launchWearApp(lastMonitorDeviceId,packageName,result);
    }

    /**
     * 指定目标设备发送消息
     *
     * @param id     目标id
     * @param data   发送的数据
     * @param result 结果回调
     */
    public void sendMessageToWear(String id, String data, BaseApi.Result<Status> result) {
        sendMessageToWear(id, data.getBytes(StandardCharsets.UTF_8), result);
    }

    /**
     * 发送消息
     *
     * @param data   发送的数据
     * @param result 结果回调
     */
    public void sendMessageToWear(String data, BaseApi.Result<Status> result) {
        if (lastMonitorDeviceId == null) {
            result.onResult(Status.RESULT_DISCONNECTED);
            return;
        }
        sendMessageToWear(lastMonitorDeviceId, data.getBytes(StandardCharsets.UTF_8), result);
    }

    /**
     * 设置消息接收监听
     */
    public void setReceiver(OnMessageReceivedListener receiver) {
        this.receiver = receiver;
    }

    /**
     * 设备初始化连接监听
     */
    public void setInitMessageListener(OnMessageInitListener connectListener) {
        this.initMessageListener = connectListener;
    }

    /**
     * 注册消息接收器
     */
    public void registerMessageReceiver() {
        hasAvailableDevices(context, new BaseApi.Callback<Node>() {
            @Override
            public void onSuccess(Node obj) {
                tryToRequestPermission(obj, new BaseApi.Callback<Node>() {
                    @Override
                    public void onSuccess(Node obj) {
                        getIsConnected(obj, new BaseApi.Callback<Node>() {
                            @Override
                            public void onSuccess(Node obj) {
                                registerReceiverInternal(obj);
                            }

                            @Override
                            public void onFailure(Status status) {

                            }
                        });
                    }

                    @Override
                    public void onFailure(Status status) {

                    }
                });
            }

            @Override
            public void onFailure(Status status) {

            }
        });
    }

    /**
     * 发送通知事件
     *
     * @param title   标题
     * @param message 内容
     * @param result  结果回调
     */
    public void sendNotify(String title, String message, BaseApi.Result<Status> result) {
        hasAvailableDevices(context, new BaseApi.Callback<Node>() {
            @Override
            public void onSuccess(Node device) {
                checkPermission(device, new BaseApi.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean obj) {
                        if (!obj) {
                            return;
                        }
                        getIsConnected(device, new BaseApi.Callback<Node>() {
                            @Override
                            public void onSuccess(Node obj) {
                                Wearable.getNotifyApi(context).sendNotify(obj.getId(), title, message, result);
                            }

                            @Override
                            public void onFailure(Status status) {

                            }
                        });
                    }

                    @Override
                    public void onFailure(Status status) {

                    }
                });
            }

            @Override
            public void onFailure(Status status) {

            }
        });
    }

    /**
     * 连接设备
     */
    public void sendUpdateMessageToWear() {
        hasAvailableDevices(context, new BaseApi.Callback<Node>() {
            @Override
            public void onSuccess(Node obj) {
                checkPermission(obj, new BaseApi.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        getIsConnected(obj, new BaseApi.Callback<Node>() {
                            @Override
                            public void onSuccess(Node obj) {
                                if (initMessageListener != null) {
                                    initMessageListener.init(obj);
                                }
                            }

                            @Override
                            public void onFailure(Status status) {

                            }
                        });
                    }

                    @Override
                    public void onFailure(Status status) {

                    }
                });
            }

            @Override
            public void onFailure(Status status) {

            }
        });
    }

    public void setReCheckConnectDevice() {
        hasAvailableDevices = true;
    }

    public void unRegisterWatchHelper() {
        if (monitorListener != null) {
            if (lastMonitorDeviceId != null) {
                NodeApi nodeApi = Wearable.getNodeApi(context);
                nodeApi.unsubscribe(lastMonitorDeviceId, DataItem.ITEM_CONNECTION);
            }
            monitorListener = null;
        }
        MessageApi messageApi0 = Wearable.getMessageApi(context);
        if (lastMonitorDeviceId != null) {
            messageApi0.removeListener(lastMonitorDeviceId, obj -> {
            });
            lastMonitorDeviceId = null;
        }
        isRegister = false;
    }
}
