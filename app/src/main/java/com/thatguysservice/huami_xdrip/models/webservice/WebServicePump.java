package com.thatguysservice.huami_xdrip.models.webservice;

import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

public class WebServicePump {
    double reservoir;
    double iob;
    double bat;

    public WebServicePump( Bundle bundle) {
        String pumpJSON = bundle.getString("pumpJSON");
        JSONObject json = null;
        try {
            json = new JSONObject(pumpJSON);
        } catch (JSONException e) {
        }
        try {
            reservoir = json.getDouble("reservoir");
        } catch (JSONException e) {
        }
        try {
            iob = json.getDouble("bolusiob");
        } catch (JSONException e) {
        }
        try {
            bat = (json.getDouble("battery"));
        } catch (JSONException e) {
        }
    }
}
