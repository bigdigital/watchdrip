package com.thatguysservice.huami_xdrip.models.webservice;

import android.os.Bundle;

public class WebServiceTreatment {
    public Double insulin;
    public Double carbs;
    public Long time;

    public WebServiceTreatment(Bundle bundle) {
        this.insulin = bundle.getDouble("treatment.insulin", -1);
        this.carbs = bundle.getDouble("treatment.carbs", -1);
        this.time = bundle.getLong("treatment.timeStamp", -1);
        if ( this.insulin == -1)  this.insulin = null;
        if ( this.carbs == -1)  this.carbs = null;
        if ( this.time == -1)  this.time = null;
    }
}

