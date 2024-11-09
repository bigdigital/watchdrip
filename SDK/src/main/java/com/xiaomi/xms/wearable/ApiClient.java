package com.xiaomi.xms.wearable;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.util.ArrayMap;

import com.xiaomi.xms.wearable.message.OnMessageReceivedListener;
import com.xiaomi.xms.wearable.node.DataItem;
import com.xiaomi.xms.wearable.node.DataSubscribeResult;
import com.xiaomi.xms.wearable.node.IDataListener;
import com.xiaomi.xms.wearable.node.OnDataChangedListener;
import com.xiaomi.xms.wearable.service.OnServiceConnectionListener;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author user
 */
public class ApiClient {
    private static ApiClient instance;
    public final IWearableInterface proxyHandle;
    public final IDataListener dataListener;
    public final ArrayMap<Integer, OnDataChangedListener> onDataChangedListener = new ArrayMap<>();
    public final List<OnServiceConnectionListener> onServiceConnectionListener = new ArrayList<>();
    public final ServiceConnection connection;
    private final LinkedList<InvokeInfo> worklist = new LinkedList<>();
    private final Context context;
    public volatile IWearableInterface wearableInterface;
    public volatile OnMessageReceivedListener onMessageReceivedListener;
    public boolean isBind;

    public ApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.connection = new ServiceConnection() {
            @Override
            public void onBindingDied(ComponentName name) {
                openService();
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                IWearableInterface iWearableInterface = null;
                if (service != null) {
                    IInterface iInterface = service.queryLocalInterface(IWearableInterface.DESCRIPTOR);
                    iWearableInterface = iInterface != null & iInterface instanceof IWearableInterface ?
                            (IWearableInterface) iInterface : IWearableInterface.Stub.asInterface(service);
                }
                wearableInterface = iWearableInterface;
                isBind = false;
                for (OnServiceConnectionListener listener : onServiceConnectionListener) {
                    listener.onServiceConnected();
                }
                synchronized (worklist) {
                    for (InvokeInfo invokeInfo : worklist) {
                        try {
                            invokeInfo.target.invoke(wearableInterface, invokeInfo.args);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                    worklist.clear();
                }
                try {
                    wearableInterface.setServiceConnectedListener(new IServiceConnectedListener.Stub() {
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                wearableInterface = null;
                isBind = false;
                synchronized (worklist) {
                    worklist.clear();
                }

                for (OnServiceConnectionListener listener : onServiceConnectionListener) {
                    listener.onServiceDisconnected();
                }

            }
        };
        this.dataListener = new IDataListener.Stub() {
            @Override
            public void onDataChanged(String command, DataItem item, Bundle bundle) throws RemoteException {
                if (onDataChangedListener == null) {
                    return;
                }
                int type = item.getType();
                OnDataChangedListener listener = onDataChangedListener.get(type);
                if (listener != null) {
                    DataSubscribeResult subscribeResult = new DataSubscribeResult();
                    if (type == DataItem.ITEM_CONNECTION.getType()) {
                        subscribeResult.setConnectedStatus(bundle.getInt(DataItem.KEY_CONNECTION_STATUS, 0));
                    } else if (type == DataItem.ITEM_CHARGING.getType()) {
                        subscribeResult.setChargingStatus(bundle.getInt(DataItem.KEY_CHARGING_STATUS, 0));
                    } else if (type == DataItem.ITEM_SLEEP.getType()) {
                        subscribeResult.setSleepStatus(bundle.getInt(DataItem.KEY_SLEEP_STATUS, 0));
                    } else if (type == DataItem.ITEM_WEARING.getType()) {
                        subscribeResult.setWearingStatus(bundle.getInt(DataItem.KEY_WEARING_STATUS, 0));
                    }
                    listener.onDataChanged(command, item, subscribeResult);
                }
            }
        };
        this.proxyHandle = (IWearableInterface) Proxy.newProxyInstance(IWearableInterface.class.getClassLoader(), new Class<?>[]{IWearableInterface.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (wearableInterface == null && !isBind) {
                    openService();
                }
                if (wearableInterface == null && isBind) {
                    synchronized (worklist) {
                        worklist.addLast(new InvokeInfo(method, args));
                        return null;
                    }
                }
                return wearableInterface == null ? null : method.invoke(wearableInterface, args);
            }
        });
        openService();
    }

    public static ApiClient getInstance(Context context) {
        synchronized (ApiClient.class) {
            if (instance == null) {
                instance = new ApiClient(context);
            }
        }
        return instance;
    }

    public void openService() {
        if (this.wearableInterface == null && !this.isBind) {
            Intent intent0 = new Intent("com.xiaomi.wearable.XMS_WEARABLE_SERVICE");
            intent0.setPackage("com.mi.health");
            if (this.context.getPackageManager().resolveService(intent0, 0) == null) {
                intent0.setPackage("com.xiaomi.wearable");
            }

            this.isBind = this.context.bindService(intent0, this.connection, Context.BIND_AUTO_CREATE);
        }
    }

    public static class InvokeInfo {
        public Method target;
        public Object[] args;

        public InvokeInfo(Method target, Object[] args) {
            this.target = target;
            this.args = args;
        }
    }
}
