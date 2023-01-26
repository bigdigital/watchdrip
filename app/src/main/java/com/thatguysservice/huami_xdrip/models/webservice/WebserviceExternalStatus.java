package com.thatguysservice.huami_xdrip.models.webservice;

import android.os.Bundle;

public class WebServiceExternalStatus {
    public Long time;
    public String externalStatusLine;

    public WebServiceExternalStatus(Bundle bundle) {
        this.externalStatusLine = bundle.getString("external.statusline", "");
        this.time = bundle.getLong("external.timeStamp", -1);
     
        if (this.externalStatusLine.equals("")) this.externalStatusLine = null;
        if (this.time == -1) this.time = null;
    }
}
