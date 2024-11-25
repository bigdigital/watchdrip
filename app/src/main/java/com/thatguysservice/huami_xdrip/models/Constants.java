package com.thatguysservice.huami_xdrip.models;

/**
 * Various constants
 */
public class Constants {
    public static final double MMOLL_TO_MGDL = 18.0182;
    public static final double MGDL_TO_MMOLL = 1 / MMOLL_TO_MGDL;

    public static final long SECOND_IN_MS = 1_000;
    public static final long MINUTE_IN_MS = 60_000;
    public static final long HOUR_IN_MS = 3_600_000;
    public static final long DAY_IN_MS = 86_400_000;
    public static final long WEEK_IN_MS = DAY_IN_MS * 7;
    public static final long MONTH_IN_MS = DAY_IN_MS * 30;

    /* Notification IDs */

    public static final int MIBAND_SERVICE_RETRY_ID = 1026;
    public static final int MIBAND_SERVICE_BG_RETRY_ID = 1027;
    public static final int MIBAND_SERVICE_XDRIP_NO_RESPONCE_ID = 1028;
    public static final int MIBAND_SERVICE_WATCHDOG_ID = 1029;

    public static final int XIAOMI_SERVICE_RETRY_ID = 1030;

    public static final int REQUEST_ID_READ_WRITE_PERMISSIONS = 1023;
    public static final int REQUEST_ID_BLUETOOTH_PERMISSIONS = 1024;
}
