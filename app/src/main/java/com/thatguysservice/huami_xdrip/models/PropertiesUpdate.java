package com.thatguysservice.huami_xdrip.models;

import androidx.databinding.BaseObservable;

public class PropertiesUpdate extends BaseObservable {
    private String key;
    private String value;

    public PropertiesUpdate(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
