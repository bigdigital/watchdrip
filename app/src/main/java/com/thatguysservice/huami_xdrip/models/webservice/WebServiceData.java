package com.thatguysservice.huami_xdrip.models.webservice;

import android.os.Bundle;

import com.google.gson.Gson;
import com.thatguysservice.huami_xdrip.models.BgData;

public class WebServiceData {
    public WebServiceStatus status;
    public WebServiceBgInfo bg;
    public WebServiceTreatment treatment;
    public WebServicePump pump;
    public WebServiceExternalStatus external;
    public WebServiceGraphData graph;

    public WebServiceData(BgData bgData, Bundle bgDataBundle, Boolean includeGraph) {
        this.status = new WebServiceStatus(bgData.isDoMgdl(), bgDataBundle);
        this.bg = new WebServiceBgInfo(bgData);
        this.treatment = new WebServiceTreatment(bgDataBundle);
        this.pump = new WebServicePump(bgDataBundle);
        this.external = new WebServiceExternalStatus(bgDataBundle);
        if (includeGraph) {
            this.graph = new WebServiceGraphData(bgDataBundle);
        }
    }

    public String getGson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
