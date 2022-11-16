package com.thatguysservice.huami_xdrip.models.webservice;

import android.os.Bundle;

import com.thatguysservice.huami_xdrip.models.Helper;

public class WebServiceStatus {
    public long now;
    public boolean isMgdl;
    public int bat;

    public WebServiceStatus(boolean isMgdl, Bundle bundle) {
        this.now = Helper.tsl();
        this.isMgdl = isMgdl;
        this.bat = bundle.getInt("phoneBattery");
    }
}
