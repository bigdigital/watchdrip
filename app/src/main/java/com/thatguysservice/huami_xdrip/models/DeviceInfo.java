package com.thatguysservice.huami_xdrip.models;

import androidx.databinding.BaseObservable;

import com.thatguysservice.huami_xdrip.watch.miband.MiBandType;

public class DeviceInfo extends BaseObservable {
    private MiBandType miBandType;
    private int rssi = 0;

    public String getDeviceName() {
        return miBandType.toString();
    }

    public void setDevice(MiBandType miBandType) {
        this.miBandType = miBandType;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
