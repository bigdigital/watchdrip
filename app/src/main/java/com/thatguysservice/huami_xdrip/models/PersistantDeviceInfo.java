package com.thatguysservice.huami_xdrip.models;

public class PersistantDeviceInfo {
    private String macAddress;
    private String authKey;
    private String name;

    public PersistantDeviceInfo(String name, String macAddress, String authKey) {
        this.macAddress = macAddress;
        this.authKey = authKey;
        this.name = name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
