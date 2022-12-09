package com.thatguysservice.huami_xdrip.models.webservice;

import android.os.Bundle;

public class WebServiceTreatment {
    public Double insulin;
    public Double carbs;
    public Long time;
    public String predictIOB;
    public String predictBWP;

    public WebServiceTreatment(Bundle bundle) {
        this.insulin = bundle.getDouble("treatment.insulin", -1);
        this.carbs = bundle.getDouble("treatment.carbs", -1);
        this.time = bundle.getLong("treatment.timeStamp", -1);
        this.predictIOB = bundle.getString("predict.IOB");
        this.predictBWP = bundle.getString("predict.BWP");

        if (this.predictIOB.isEmpty()) {
            this.predictIOB = null;
        } else {
            this.predictIOB = this.predictIOB.replace(",", ".");
            this.predictIOB = this.predictIOB + "u".replace(".0u", "u");
        }
        if (this.predictBWP.isEmpty()) {
            this.predictBWP = null;
        } else {
            this.predictBWP = this.predictBWP.replace(",", ".");
            this.predictBWP = this.predictBWP.replace("\u224F", "");
            this.predictBWP = this.predictBWP.replace("\u26A0", "!");
        }
        if (this.insulin == -1) this.insulin = null;
        if (this.carbs == -1) this.carbs = null;
        if (this.time == -1) this.time = null;
    }
}

