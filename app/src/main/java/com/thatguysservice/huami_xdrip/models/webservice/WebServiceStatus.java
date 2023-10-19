package com.thatguysservice.huami_xdrip.models.webservice;

import android.os.Bundle;

import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.utils.PowerStateReceiver;

public class WebServiceStatus {
    public long now;
    public boolean isMgdl;
    public int bat;

    public WebServiceStatus(boolean isMgdl, Bundle bundle) {
        this.now = Helper.tsl();
        this.isMgdl = isMgdl;
        //this.bat = bundle.getInt("phoneBattery");
        this.bat = PowerStateReceiver.getBatteryPercentage(HuamiXdrip.getAppContext());
    }
}
