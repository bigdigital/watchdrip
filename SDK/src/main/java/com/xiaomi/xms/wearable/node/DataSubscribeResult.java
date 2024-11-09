package com.xiaomi.xms.wearable.node;

/**
 * @author user
 */
public class DataSubscribeResult {
    public static final int RESULT_CHARGING_FINISH = 3;
    public static final int RESULT_CHARGING_QUIT = 2;
    public static final int RESULT_CHARGING_START = 1;
    public static final int RESULT_CONNECTION_CONNECTED = 1;
    public static final int RESULT_CONNECTION_DISCONNECTED = 2;
    public static final int RESULT_SLEEP_IN = 1;
    public static final int RESULT_SLEEP_OUT = 2;
    public static final int RESULT_WARNING_ACTIVE_HEART_RATE_HIGH = 3;
    public static final int RESULT_WARNING_ACTIVE_HEART_RATE_LOW = 4;
    public static final int RESULT_WARNING_HEART_RATE_HIGH = 1;
    public static final int RESULT_WARNING_HEART_RATE_LOW = 2;
    public static final int RESULT_WEARING_OFF = 2;
    public static final int RESULT_WEARING_ON = 1;
    private int chargingStatus;
    private int connectedStatus;
    private int sleepStatus;
    private int warningStatus;
    private int wearingStatus;

    public int getChargingStatus() {
        return this.chargingStatus;
    }

    public void setChargingStatus(int v) {
        this.chargingStatus = v;
    }

    public int getConnectedStatus() {
        return this.connectedStatus;
    }

    public void setConnectedStatus(int v) {
        this.connectedStatus = v;
    }

    public int getSleepStatus() {
        return this.sleepStatus;
    }

    public void setSleepStatus(int v) {
        this.sleepStatus = v;
    }

    public int getWarningStatus() {
        return this.warningStatus;
    }

    public void setWarningStatus(int v) {
        this.warningStatus = v;
    }

    public int getWearingStatus() {
        return this.wearingStatus;
    }

    public void setWearingStatus(int v) {
        this.wearingStatus = v;
    }
}
