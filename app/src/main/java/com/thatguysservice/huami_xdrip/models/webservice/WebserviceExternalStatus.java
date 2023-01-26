package com.thatguysservice.huami_xdrip.models.webservice;

import android.os.Bundle;

public class WebServiceExternalStatus {
    public Long time;
    public String externalStatusLine;

    public WebServiceTreatment(Bundle bundle) {
        this.externalStatusLine = bundle.getString("external.statusline", "");
        this.time = bundle.getDouble("external.timeStamp", -1);
     
        if (this.externalStatusLine.equals("")) this.externalStatusLine = null;
        if (this.time == -1) this.time = null;
    }
}
