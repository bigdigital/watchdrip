package com.thatguysservice.huami_xdrip.services;

import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_UPDATE_BG_FORCE;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.INTENT_FUNCTION_KEY;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.services.broadcastservice.models.Settings;
import com.google.gson.Gson;
import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.UtilityModels.ForegroundServiceStarter;
import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.utils.framework.WakeLockTrampoline;
import com.thatguysservice.huami_xdrip.watch.miband.MiBand;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;

import org.json.JSONObject;


public class GarminService extends Service {

    protected String TAG = this.getClass().getSimpleName();
    private ForegroundServiceStarter foregroundServiceStarter;
    private long lastTime=0;
    private PendingIntent serviceIntent;

    private static final int SEND_DELAY = (int) (Constants.SECOND_IN_MS * 10);
    private String json="";
    private ConnectIQ connectIQ=null;
    private Context context;

    public boolean isSdkReady=false;
    String appID="d626027eb78b4b8a90269934fd55328b";
    private IQApp app = new IQApp(appID);
    public List<IQDevice> devices=null;
    public static boolean shouldServiceRun() {
        return MiBandEntry.isGarminServiceEnabled();
    }

    public static void bgForce(String jsonString) {
        if (shouldServiceRun()) {
            Helper.startService(GarminService.class, INTENT_FUNCTION_KEY, CMD_UPDATE_BG, "json", jsonString);
        }
    }

    protected void setRetryTimer() {
        if (shouldServiceRun()) {
            serviceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.GARMIN_SERVICE_RETRY_ID, CMD_UPDATE_BG_FORCE);
            Helper.wakeUpIntent(HuamiXdrip.getAppContext(), SEND_DELAY, serviceIntent);
        }
    }

    private void cancelRetryTimer() {
        Helper.cancelAlarm(HuamiXdrip.getAppContext(), serviceIntent);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = Helper.getWakeLock("Miband service", 60000);
        try {
            if (shouldServiceRun()) {
                if (intent != null) {
                    final String function = intent.getStringExtra(INTENT_FUNCTION_KEY);
                    if (function != null) {
                        handleCommand(function, intent);
                    } else {
                        // no specific function
                    }
                }

                return START_STICKY;
            } else {
                UserError.Log.d(TAG, "Service is NOT set be active - shutting down");
                stopSelf();
                return START_NOT_STICKY;
            }
        } finally {
            Helper.releaseWakeLock(wl);
        }
    }

    public void inicializarConnectIQ(List<Object> message, long time){
        if(isSdkReady) {
            try {
                devices = connectIQ.getConnectedDevices();
                UserError.Log.e("GARMIN", "Devices detected: " + devices.size());
                if (!devices.isEmpty()) {
                    for (IQDevice device : devices) {
                        UserError.Log.d("GARMIN", "Connected device: " + device);
                        try {
                            if (isSdkReady && device != null && connectIQ.getDeviceStatus(device) == IQDevice.IQDeviceStatus.CONNECTED) {
                                connectIQ.getApplicationInfo(appID, device, new ConnectIQ.IQApplicationInfoListener() {
                                    @Override
                                    public void onApplicationInfoReceived(IQApp appRec) {
                                        if (appRec != null) {
                                            if (appRec.getStatus() == IQApp.IQAppStatus.INSTALLED) {
                                                json = "";
                                                //lastTime = time;
                                                cancelRetryTimer();
                                                UserError.Log.e("GARMIN", "App installed");
                                                try {
                                                    UserError.Log.e("GARMIN", "Sending message: " + message);
                                                    lastTime = time;
                                                    connectIQ.sendMessage(device, app, message, new ConnectIQ.IQSendMessageListener() {
                                                        @Override
                                                        public void onMessageStatus(IQDevice device, IQApp app, ConnectIQ.IQMessageStatus status) {
                                                            /*if (status != ConnectIQ.IQMessageStatus.SUCCESS) {
                                                                lastTime=0;
                                                            }*/
                                                        }
                                                    });
                                                } catch (Exception e) {
                                                    //uninicializarConnectIQ();
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onApplicationNotInstalled(String applicationId) {
                                        UserError.Log.e("GARMIN", "App NOT installed");
                                    }
                                });
                            }
                        } catch (Exception e) {
//            throw new RuntimeException(e);
                        }
                    }
                }
            } catch (Exception e) {
                //uninicializarConnectIQ();
            }
        }
    }
	
    public void sendToWatch(List<Object> message, long time) {
        inicializarConnectIQ(message, time);
    }

    private void handleCommand(String function, Intent intentIn) {
        switch (function) {
            case CMD_UPDATE_BG_FORCE:
                if(!json.isEmpty()) {
                    UserError.Log.d(TAG, "retry send last data GARMIN");
                    updateWearBg(json);
                    cancelRetryTimer();
                    setRetryTimer();
                }
                else
                {
                    cancelRetryTimer();
                }
                break;
            case CMD_UPDATE_BG:
                json = intentIn.getStringExtra("json");
                cancelRetryTimer();
                setRetryTimer();
                updateWearBg(json);
                break;
            default:
                return;
        }
    }



    private void updateWearBg(String jsonString) {
        if(!jsonString.isEmpty()) {
            try {
                JSONObject objJSON = new JSONObject(jsonString);
                JSONObject bgObject = objJSON.getJSONObject("bg");
                long time = bgObject.getLong("time");
                String val = bgObject.getString("val");
                String delta = bgObject.getString("delta");
                Boolean isHigh = bgObject.getBoolean("isHigh");
                Boolean isLow = bgObject.getBoolean("isLow");
                String trend = bgObject.getString("trend");

                if(lastTime!=time) {
                    ArrayList<Object> message = new ArrayList<Object>();
                    message.add(jsonString);
                    sendToWatch(message, time);
                }
                else
                {
                    cancelRetryTimer();
                }
            }
            catch(Exception e)
            {
                UserError.Log.e("GARMIN", "Excepcion: "+e.getMessage());
            }
        }
        else {
            cancelRetryTimer();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    protected void startInForeground() {
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), this);
        foregroundServiceStarter.start();
    }

    public void createConnectIQ() {
        //if(lastTime != time) {
            connectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS);
            connectIQ.initialize(this, true, new ConnectIQ.ConnectIQListener() {
                @Override
                public void onSdkReady() {
                    try {
                        UserError.Log.e("GARMIN", "SDK Ready");
                        isSdkReady = true;
                        //inicializarConnectIQ(message, time);
                    } catch (Exception e) {
                        isSdkReady = false;
                    }
                }

                @Override
                public void onInitializeError(ConnectIQ.IQSdkErrorStatus status) {
                    UserError.Log.e("GARMIN", "Error inicializando SDK: " + status);
                    isSdkReady = false;
                }

                @Override
                public void onSdkShutDown() {
                    isSdkReady = false;
                }
            });
        //}
    }

    @Override
    public void onCreate() {
        UserError.Log.d(TAG, "starting service");
        startInForeground();
        createConnectIQ();
        super.onCreate();
    }

    public void destroyConnectIQ() {
        if(connectIQ!=null) {
            try {
                connectIQ.shutdown(this);
                isSdkReady = false;
            } catch (Exception e) {

            }
        }
    }


    @Override
    public void onDestroy() {
        UserError.Log.d(TAG, "killing service");
        cancelRetryTimer();
        foregroundServiceStarter.stop();
        destroyConnectIQ();
        super.onDestroy();
    }
}
