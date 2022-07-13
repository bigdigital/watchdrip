package com.thatguysservice.huami_xdrip.models;

public class CallInfo {
    public static final int CALL_UNDEFINED = 0;
    public static final int CALL_ACCEPT = 1;
    public static final int CALL_INCOMING = 2;
    public static final int CALL_OUTGOING = 3;
    public static final int CALL_REJECT = 4;
    public static final int CALL_START = 5;
    public static final int CALL_END = 6;

    public String number;
    public String name;
    public int command;
}
