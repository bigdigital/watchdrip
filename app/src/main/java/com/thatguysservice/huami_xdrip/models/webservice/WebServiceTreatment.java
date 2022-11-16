package com.thatguysservice.huami_xdrip.models.webservice;

import android.os.Bundle;

public class WebServiceTreatment {
    public double insulin;
    public double carbs;
    public long time;

    public WebServiceTreatment(Bundle bundle) {
        this.insulin = bundle.getDouble("treatment.insulin", -1);
        this.carbs = bundle.getDouble("treatment.carbs", -1);
        this.time = bundle.getLong("treatment.timeStamp", -1);
    }
}

