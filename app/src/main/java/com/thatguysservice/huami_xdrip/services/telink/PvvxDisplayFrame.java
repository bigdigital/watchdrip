package com.thatguysservice.huami_xdrip.services.telink;

import com.thatguysservice.huami_xdrip.models.Constants;

import java.util.Arrays;

/**
 * "Show external data" frame understood by the pvvx ATC_MiThermometer custom firmware
 * (Xiaomi LYWSD03MMC, MHO-C401, CGG1 and other Telink TLSR825x based thermometers).
 * Written without response to characteristic 0x1F1F, see external_data_t in the firmware's app.h.
 * <p>
 * {@link Layout#STANDARD}, most devices, 8 bytes:
 * <pre>
 * [0]   0x22 (CMD_ID_EXTDATA)
 * [1:3] big number   int16 LE, value * 10  (-99.5 .. 1999.5)
 * [3:5] small number int16 LE              (-9 .. 99)
 * [5:7] validity     uint16 LE, seconds    (0xFFFF = show only external data until reboot)
 * [7]   flags        bits0-2 smiley, bit3 "%", bit4 battery, bits5-7 unit symbol
 * </pre>
 * {@link Layout#SINGLE_NUMBER}, MJWSD05MMC, MJWSD05MMC_EN, LYWSD02MMC, 8 bytes:
 * <pre>
 * [0]   0x22
 * [1:5] number       int32 LE, value * 100 (-999.50 .. 19995.50), shown with 2 decimals up to 199.99
 * [5:7] validity     uint16 LE, seconds
 * [7]   flags        bits0-2 smiley, bit3 battery, bits4-7 unit symbol
 * </pre>
 * The firmware fork adds CMD_ID_EXTDATA_AGE, see {@link #buildWithAge(long)}.
 */
public class PvvxDisplayFrame {
    public static final byte CMD_ID_EXTDATA = 0x22;
    // Firmware fork: the thermometer counts the data age itself and shows "(ooo)" when stale
    public static final byte CMD_ID_EXTDATA_AGE = 0x2E;
    public static final int AGE_STALE_SEC = 15 * 60;
    // 9th byte of CMD_ID_EXTDATA_AGE (EXT_ALARM_* in the firmware's lcd.h)
    public static final int ALARM_HIGH = 0x01;
    public static final int ALARM_LOW = 0x02;
    // the app took over the thermometer's key snooze
    public static final int ALARM_SNOOZED = 0x80;
    // CMD_ID_EXTDATA_AGE answer [0x2E][status]: the key snoozed the alarm
    public static final int STATUS_SNOOZE = 0x01;
    public static final int VALIDITY_FOREVER = 0xFFFF;

    public enum Layout {STANDARD, SINGLE_NUMBER}

    // CMD_ID_DEV_ID hw_version of the devices with a single s32 number in external_data_t
    private static final int HW_VER_MJWSD05MMC = 9;
    private static final int HW_VER_MJWSD05MMC_EN = 12;
    private static final int HW_VER_LYWSD02MMC = 49;

    private static final int SMILEY_SNOOZED = 2; // " -^- ": sad without the contour
    private static final int SMILEY_HAPPY = 5; // "(^_^)"
    private static final int SMILEY_SAD = 6;   // "(-^-)"
    private static final int SMILEY_STALE = 7; // "(ooo)"

    // The unit symbol's top/middle/bottom segments show the trend: up, 45 up, flat, 45 down, down
    private static final int[] TREND_STANDARD = {
            0x20,   // "°Г" top
            0x60,   // "°F" top + middle
            0x40,   // " -" middle
            0xC0,   // " =" middle + bottom
            0x80,   // " _" bottom
    };
    private static final int[] TREND_SINGLE_NUMBER = {
            0x01 << 4,  // "°г" top
            0x03 << 4,  // top + middle
            0x02 << 4,  // " -" middle
            0x06 << 4,  // middle + lower "|"
            0x04 << 4,  // " |" lower
    };
    private static final int TREND_UP = 0, TREND_45_UP = 1, TREND_FLAT = 2, TREND_45_DOWN = 3, TREND_DOWN = 4;

    private static final int BIG_MAX_X10 = 19995;
    private static final int NUMBER_MAX_X100 = 1999949; // 1999950 and above is drawn as "Hi"
    private static final int SMALL_MAX = 99;

    // Back to the thermometer's own display (validity 1 s), the same in both layouts
    public static final PvvxDisplayFrame REVERT_TO_THERMOMETER = new PvvxDisplayFrame(0, 0, null, true, 0, 1, -1, 0, false, false);

    private final double valueMgdl;
    private final double deltaMgdl;
    private final String deltaName;
    private final boolean doMgdl;
    private final int alarmFlags; // ALARM_HIGH / ALARM_LOW
    private final boolean outOfRange;
    private final int validitySec;
    private final boolean revert;
    private final int staleMinutes; // -1 for a fresh reading
    private final long readingTime; // when the sensor measured the value, 0 if unknown
    private final boolean alarmSignal; // alarm bits go to the thermometer (CMD_ID_EXTDATA_AGE 9th byte)
    private final boolean snoozed; // the app snoozes alarms, tells the thermometer with ALARM_SNOOZED

    public static PvvxDisplayFrame glucose(double valueMgdl, double deltaMgdl, String deltaName, boolean doMgdl, boolean isHigh, boolean isLow) {
        return glucose(valueMgdl, deltaMgdl, deltaName, doMgdl, isHigh, isLow, 0);
    }

    public static PvvxDisplayFrame glucose(double valueMgdl, double deltaMgdl, String deltaName, boolean doMgdl, boolean isHigh, boolean isLow, long readingTime) {
        final int alarmFlags = (isHigh ? ALARM_HIGH : 0) | (isLow ? ALARM_LOW : 0);
        return new PvvxDisplayFrame(valueMgdl, deltaMgdl, deltaName, doMgdl, alarmFlags, VALIDITY_FOREVER, -1, readingTime, true, false);
    }

    private PvvxDisplayFrame(double valueMgdl, double deltaMgdl, String deltaName, boolean doMgdl, int alarmFlags, int validitySec, int staleMinutes, long readingTime, boolean alarmSignal, boolean snoozed) {
        this.valueMgdl = valueMgdl;
        this.deltaMgdl = deltaMgdl;
        this.deltaName = deltaName;
        this.doMgdl = doMgdl;
        this.alarmFlags = alarmFlags;
        this.outOfRange = alarmFlags != 0;
        this.validitySec = validitySec;
        this.revert = validitySec != VALIDITY_FOREVER;
        this.staleMinutes = staleMinutes;
        this.readingTime = readingTime;
        this.alarmSignal = alarmSignal;
        this.snoozed = snoozed;
    }

    // Stale marker: "(ooo)", no trend, the age in minutes as the small number
    public PvvxDisplayFrame asStale(int minutes) {
        return new PvvxDisplayFrame(valueMgdl, deltaMgdl, deltaName, doMgdl, alarmFlags, validitySec, Math.max(0, minutes), readingTime, alarmSignal, snoozed);
    }

    // Whether the alarm bits go to the thermometer (beeps), the face shows high/low either way
    public PvvxDisplayFrame withAlarmSignal(boolean send) {
        return new PvvxDisplayFrame(valueMgdl, deltaMgdl, deltaName, doMgdl, alarmFlags, validitySec, staleMinutes, readingTime, send, snoozed);
    }

    // Snoozed by the app: sends ALARM_SNOOZED and shows " -^- ", use with withAlarmSignal(false)
    public PvvxDisplayFrame withSnoozed(boolean snoozed) {
        return new PvvxDisplayFrame(valueMgdl, deltaMgdl, deltaName, doMgdl, alarmFlags, validitySec, staleMinutes, readingTime, alarmSignal, snoozed);
    }

    // Not a stale marker or the switch back to the thermometer display
    public boolean isFreshReading() {
        return !revert && staleMinutes < 0;
    }

    public long getReadingTime() {
        return readingTime;
    }

    /**
     * CMD_ID_EXTDATA_AGE frame, standard layout: small number = reading age in seconds, validity =
     * stale threshold, 9th byte = alarm flags.
     */
    public byte[] buildWithAge(long ageSec) {
        final int age = (int) Math.max(0, Math.min(0xFFFF, ageSec));
        final byte[] bytes = Arrays.copyOf(build(Layout.STANDARD), 9);
        bytes[0] = CMD_ID_EXTDATA_AGE;
        bytes[3] = (byte) age;
        bytes[4] = (byte) (age >> 8);
        bytes[5] = (byte) AGE_STALE_SEC;
        bytes[6] = (byte) (AGE_STALE_SEC >> 8);
        bytes[8] = (byte) ((alarmSignal ? alarmFlags : 0) | (snoozed ? ALARM_SNOOZED : 0));
        return bytes;
    }

    public static Layout layoutFor(int hwVersion) {
        switch (hwVersion) {
            case HW_VER_MJWSD05MMC:
            case HW_VER_MJWSD05MMC_EN:
            case HW_VER_LYWSD02MMC:
                return Layout.SINGLE_NUMBER;
            default:
                return Layout.STANDARD;
        }
    }

    public byte[] build(Layout layout) {
        if (revert) {
            return frame(0, validitySec, 0);
        }
        final boolean stale = staleMinutes >= 0;
        final int smiley = stale ? SMILEY_STALE : !outOfRange ? SMILEY_HAPPY : snoozed ? SMILEY_SNOOZED : SMILEY_SAD;
        final int trend = trendIndex();
        if (layout == Layout.SINGLE_NUMBER) {
            // One decimal is enough for mmol/l, the display still pads it to two
            int numberX100 = doMgdl
                    ? (int) Math.round(valueMgdl) * 100
                    : (int) Math.round(valueMgdl * Constants.MGDL_TO_MMOLL * 10) * 10;
            numberX100 = Math.max(0, Math.min(NUMBER_MAX_X100, numberX100));
            final int flags = smiley | (stale ? 0 : TREND_SINGLE_NUMBER[trend]);
            return frame(numberX100, validitySec, flags);
        }

        int bigX10;
        int small;
        if (doMgdl) {
            bigX10 = (int) Math.round(valueMgdl) * 10;
            small = (int) Math.round(Math.abs(deltaMgdl));
        } else {
            bigX10 = (int) Math.round(valueMgdl * Constants.MGDL_TO_MMOLL * 10);
            // No decimal point on the small number, so show mmol/l delta in 0.1 units
            small = (int) Math.round(Math.abs(deltaMgdl) * Constants.MGDL_TO_MMOLL * 10);
        }
        bigX10 = Math.max(0, Math.min(BIG_MAX_X10, bigX10));
        if (stale) small = staleMinutes;
        small = Math.min(SMALL_MAX, small);
        final int flags = smiley | (stale ? 0 : TREND_STANDARD[trend]);
        return frame((bigX10 & 0xFFFF) | (small << 16), validitySec, flags);
    }

    private int trendIndex() {
        if (deltaName != null) {
            switch (deltaName) {
                case "DoubleUp":
                case "SingleUp":
                    return TREND_UP;
                case "FortyFiveUp":
                    return TREND_45_UP;
                case "Flat":
                    return TREND_FLAT;
                case "FortyFiveDown":
                    return TREND_45_DOWN;
                case "SingleDown":
                case "DoubleDown":
                    return TREND_DOWN;
                default:
                    break;
            }
        }
        // trend unknown: the delta direction, the small number is unsigned
        if (deltaMgdl > 0) return TREND_UP;
        if (deltaMgdl < 0) return TREND_DOWN;
        return TREND_FLAT;
    }

    // Both layouts are: cmd, 4 value bytes (LE), validity (u16 LE), flags
    private static byte[] frame(int value, int validitySec, int flags) {
        return new byte[]{
                CMD_ID_EXTDATA,
                (byte) value, (byte) (value >> 8), (byte) (value >> 16), (byte) (value >> 24),
                (byte) validitySec, (byte) (validitySec >> 8),
                (byte) flags
        };
    }
}
