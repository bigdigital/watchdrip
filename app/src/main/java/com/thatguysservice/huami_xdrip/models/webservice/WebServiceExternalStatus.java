package com.thatguysservice.huami_xdrip.models.webservice;

import android.os.Bundle;

public class WebServiceExternalStatus {
    public Long time;
    public String statusLine;

    public WebServiceExternalStatus(Bundle bundle) {
        this.statusLine = bundle.getString("external.statusLine", "");
        this.time = bundle.getLong("external.timeStamp", -1);
     
        if (this.statusLine.equals("")) this.statusLine = null;
        if (this.time == -1) this.time = null;
    }
}
