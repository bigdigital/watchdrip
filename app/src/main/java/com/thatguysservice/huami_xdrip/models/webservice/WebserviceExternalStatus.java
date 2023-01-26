package com.thatguysservice.huami_xdrip.models.webservice;

import android.os.Bundle;

public class WebServiceExternalStatus {
    public Long time;
    public String externalStatusLine;

    public WebServiceTreatment(Bundle bundle) {
        this.externalStatusLine = bundle.getString("external.statusline", "");
     
        if (this.externalStatusLine.equals("")) this.externalStatusLine = null;
    }
}
