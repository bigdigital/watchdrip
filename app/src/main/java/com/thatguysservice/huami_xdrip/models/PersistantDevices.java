package com.thatguysservice.huami_xdrip.models;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.thatguysservice.huami_xdrip.watch.miband.MiBand;

import java.util.ArrayList;
import java.util.List;

public class PersistantDevices {
    private static final int MAX_DEVICES = 10;
    private List<PersistantDeviceInfo> devices;

    public PersistantDevices() {
        this.devices = new ArrayList<>();
    }

    public PersistantDevices(List<PersistantDeviceInfo> devices) {
        this.devices = devices;
    }

    public PersistantDevices(String jsonDevices) {
        Gson gson = new Gson();
        PersistantDevices devices = null;
        try {
            devices = gson.fromJson(jsonDevices, PersistantDevices.class);
        } catch (JsonSyntaxException e) {
        }
        if (devices == null) {
            this.devices = new ArrayList<>();
            return;
        }
        this.devices = devices.getDevices();
    }

    public int count() {
        return devices.size();
    }

    public void updateDevice(PersistantDeviceInfo device, int index) {
        devices.set(index, device);
        MiBand.setDevices(this);
    }

    public PersistantDeviceInfo getDeviceByIndex(int index) {
        return devices.get(index);
    }

    public List<PersistantDeviceInfo> getDevices() {
        return devices;
    }

    public boolean addDevice(PersistantDeviceInfo device) {
        if (count() < MAX_DEVICES) {
            devices.add(device);
            MiBand.setDevices(this);
            return true;
        }
        return false;
    }

    public boolean removeDevice(int index) {
        if (count() <= 1 ) return false;
        devices.remove(index);
        MiBand.setDevices(this);
        return true;
    }

    public String getJsonSting() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
