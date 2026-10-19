package com.thatguysservice.huami_xdrip;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static com.thatguysservice.huami_xdrip.services.telink.PvvxDisplayFrame.Layout.SINGLE_NUMBER;
import static com.thatguysservice.huami_xdrip.services.telink.PvvxDisplayFrame.Layout.STANDARD;

import com.thatguysservice.huami_xdrip.services.TelinkService;
import com.thatguysservice.huami_xdrip.services.telink.PvvxDisplayFrame;
import com.thatguysservice.huami_xdrip.services.telink.TelinkThermometers;

import org.junit.Test;

public class PvvxDisplayFrameTest {

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) result[i] = (byte) values[i];
        return result;
    }

    @Test
    public void mgdlInRangeRising() {
        // 123 mg/dL -> 1230 = 0x04CE, delta 7, forever, smile + bracket + top segments
        assertArrayEquals(bytes(0x22, 0xCE, 0x04, 7, 0, 0xFF, 0xFF, 0x25),
                PvvxDisplayFrame.glucose(123, 7, "SingleUp", true, false, false).build(STANDARD));
    }

    @Test
    public void mmolHighFalling() {
        // 216 mg/dL = 12.0 mmol/l -> 120, delta -9 mg/dL = 0.5 mmol/l -> 5, sad + bracket + bottom bar
        assertArrayEquals(bytes(0x22, 120, 0, 5, 0, 0xFF, 0xFF, 0x86),
                PvvxDisplayFrame.glucose(216, -9, "SingleDown", false, true, false).build(STANDARD));
    }

    @Test
    public void unknownTrendUsesDeltaSignAndClampsSmall() {
        // delta -150 clamps to 99, trend not computable -> bottom bar from the sign
        assertArrayEquals(bytes(0x22, 0x90, 0x01, 99, 0, 0xFF, 0xFF, 0x86),
                PvvxDisplayFrame.glucose(40, -150, "NOT COMPUTABLE", true, false, true).build(STANDARD));
    }

    @Test
    public void revert() {
        assertArrayEquals(bytes(0x22, 0, 0, 0, 0, 1, 0, 0), PvvxDisplayFrame.REVERT_TO_THERMOMETER.build(STANDARD));
        assertArrayEquals(bytes(0x22, 0, 0, 0, 0, 1, 0, 0), PvvxDisplayFrame.REVERT_TO_THERMOMETER.build(SINGLE_NUMBER));
    }

    @Test
    public void singleNumberMgdl() {
        // 123 mg/dL -> 12300 = 0x300C, happy (5) + top segment (1 << 4)
        assertArrayEquals(bytes(0x22, 0x0C, 0x30, 0, 0, 0xFF, 0xFF, 0x15),
                PvvxDisplayFrame.glucose(123, 7, "SingleUp", true, false, false).build(SINGLE_NUMBER));
    }

    @Test
    public void singleNumberMmol() {
        // 101 mg/dL = 5.6 mmol/l -> 560 = 0x0230, sad (6) + middle and lower segments (6 << 4)
        assertArrayEquals(bytes(0x22, 0x30, 0x02, 0, 0, 0xFF, 0xFF, 0x66),
                PvvxDisplayFrame.glucose(101, -3, "FortyFiveDown", false, false, true).build(SINGLE_NUMBER));
    }

    @Test
    public void layoutByHwVersion() {
        assertEquals(STANDARD, PvvxDisplayFrame.layoutFor(0));   // LYWSD03MMC B1.4
        assertEquals(STANDARD, PvvxDisplayFrame.layoutFor(10));  // LYWSD03MMC B1.5
        assertEquals(STANDARD, PvvxDisplayFrame.layoutFor(-1));  // unknown
        assertEquals(SINGLE_NUMBER, PvvxDisplayFrame.layoutFor(9));
        assertEquals(SINGLE_NUMBER, PvvxDisplayFrame.layoutFor(12));
        assertEquals(SINGLE_NUMBER, PvvxDisplayFrame.layoutFor(49));
    }

    @Test
    public void macFormats() {
        assertEquals("A4:C1:38:45:4E:F3", TelinkService.normalizeMac("A4C138454EF3A6A8"));
        assertEquals("A4:C1:38:45:4E:F3", TelinkService.normalizeMac("a4c138454ef3"));
        assertEquals("A4:C1:38:45:4E:F3", TelinkService.normalizeMac(" a4:c1:38:45:4e:f3 "));
        assertEquals("A4:C1:38:45:4E:F3", TelinkService.normalizeMac("A4-C1-38-45-4E-F3"));
    }

    @Test
    public void staleMarker() {
        PvvxDisplayFrame fresh = PvvxDisplayFrame.glucose(123, 7, "SingleUp", true, false, false);
        // same value, small number = 20 minutes old, "(ooo)" face, no trend symbol
        assertArrayEquals(bytes(0x22, 0xCE, 0x04, 20, 0, 0xFF, 0xFF, 0x07), fresh.asStale(20).build(STANDARD));
        assertArrayEquals(bytes(0x22, 0x0C, 0x30, 0, 0, 0xFF, 0xFF, 0x07), fresh.asStale(20).build(SINGLE_NUMBER));
        // age is capped at 99
        assertArrayEquals(bytes(0x22, 0xCE, 0x04, 99, 0, 0xFF, 0xFF, 0x07), fresh.asStale(150).build(STANDARD));
    }

    @Test
    public void ageFrameForFirmwareFork() {
        // 0x2E, value as usual, reading already 190 s old = 0x00BE, stale after 900 s = 0x0384, no alarm
        assertArrayEquals(bytes(0x2E, 0xCE, 0x04, 0xBE, 0x00, 0x84, 0x03, 0x25, 0x00),
                PvvxDisplayFrame.glucose(123, 7, "SingleUp", true, false, false).buildWithAge(190));
        // age clamped to what fits in 16 bits
        assertArrayEquals(bytes(0x2E, 0xCE, 0x04, 0xFF, 0xFF, 0x84, 0x03, 0x25, 0x00),
                PvvxDisplayFrame.glucose(123, 7, "SingleUp", true, false, false).buildWithAge(100000));
        // alarm flags in the 9th byte: 250 mg/dL high = 0x01, 60 mg/dL low = 0x02 (sad face 0x26 / 0x46)
        assertArrayEquals(bytes(0x2E, 0xC4, 0x09, 0, 0, 0x84, 0x03, 0x46, 0x01),
                PvvxDisplayFrame.glucose(250, 0, "Flat", true, true, false).buildWithAge(0));
        assertArrayEquals(bytes(0x2E, 0x58, 0x02, 0, 0, 0x84, 0x03, 0x46, 0x02),
                PvvxDisplayFrame.glucose(60, 0, "Flat", true, false, true).buildWithAge(0));
        assertTrue(PvvxDisplayFrame.glucose(123, 7, "SingleUp", true, false, false).isFreshReading());
        assertFalse(PvvxDisplayFrame.glucose(123, 7, "SingleUp", true, false, false).asStale(20).isFreshReading());
        assertFalse(PvvxDisplayFrame.REVERT_TO_THERMOMETER.isFreshReading());
    }

    @Test
    public void alarmBeepOptionOnlyClearsTheAlarmByte() {
        PvvxDisplayFrame high = PvvxDisplayFrame.glucose(250, 0, "Flat", true, true, false);
        // beep option off: no alarm byte, the sad face stays
        assertArrayEquals(bytes(0x2E, 0xC4, 0x09, 0, 0, 0x84, 0x03, 0x46, 0x00),
                high.withAlarmSignal(false).buildWithAge(0));
        // and back on
        assertArrayEquals(bytes(0x2E, 0xC4, 0x09, 0, 0, 0x84, 0x03, 0x46, 0x01),
                high.withAlarmSignal(false).withAlarmSignal(true).buildWithAge(0));
    }

    @Test
    public void snoozedFrameIsQuietAndAcknowledgesTheKey() {
        PvvxDisplayFrame high = PvvxDisplayFrame.glucose(250, 0, "Flat", true, true, false);
        // snoozed: " -^- ", no alarm bits, 0x80 tells the thermometer the app took over its key snooze
        assertArrayEquals(bytes(0x2E, 0xC4, 0x09, 0, 0, 0x84, 0x03, 0x42, 0x80),
                high.withAlarmSignal(false).withSnoozed(true).buildWithAge(0));
        // snooze over: the alarm is sent again
        assertArrayEquals(bytes(0x2E, 0xC4, 0x09, 0, 0, 0x84, 0x03, 0x46, 0x01),
                high.withAlarmSignal(false).withSnoozed(true).withAlarmSignal(true).withSnoozed(false).buildWithAge(0));
    }

    @Test
    public void severalMacs() {
        assertEquals(java.util.Arrays.asList("A4:C1:38:45:4E:F3", "A4:C1:38:19:5A:19"),
                TelinkService.parseMacs("A4:C1:38:45:4E:F3, a4c138195a19;A4C138454EF3 garbage"));
        assertEquals(0, TelinkService.parseMacs(" ").size());
        assertEquals(0, TelinkService.parseMacs(null).size());
    }

    @Test
    public void thermometerList() {
        TelinkThermometers list = new TelinkThermometers();
        list.setMac(list.add("Bedroom"), "A4:C1:38:45:4E:F3");
        list.setMac(list.add("Hall"), "A4:C1:38:19:5A:19");
        assertEquals(2, list.count());
        assertEquals("Bedroom", list.get(0).getName());
        // a thermometer without MAC yet is kept, but gets no data
        list.add("Kitchen");
        assertEquals(java.util.Arrays.asList("A4:C1:38:45:4E:F3", "A4:C1:38:19:5A:19"), list.macs());
        list.remove(0);
        assertEquals("A4:C1:38:19:5A:19", list.get(0).getMac());
    }
}
