package com.thatguysservice.huami_xdrip.watch.miband.message;

import com.thatguysservice.huami_xdrip.watch.miband.Const;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_NIGHT_MODE_OFF;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_NIGHT_MODE_SCHEDULED;
import static com.thatguysservice.huami_xdrip.watch.miband.message.OperationCodes.COMMAND_NIGHT_MODE_SUNSET;

public class DisplayControllMessage extends BaseMessage {
    private static final String TAG = DisplayControllMessage.class.getSimpleName();

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_3_CONFIGURATION;
    }

    public byte[] setNightModeCmd(NightMode nightMode, Date start, Date end) {
        byte[] data = null;
        switch (nightMode) {
            case Sunset:
                data = COMMAND_NIGHT_MODE_SUNSET;
                break;
            case Off:
                data = COMMAND_NIGHT_MODE_OFF;
                break;
            case Sheduled:
                data = COMMAND_NIGHT_MODE_SCHEDULED.clone();

                Calendar calendar = GregorianCalendar.getInstance();
                calendar.setTime(start);
                data[2] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                data[3] = (byte) calendar.get(Calendar.MINUTE);

                calendar.setTime(end);
                data[4] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                data[5] = (byte) calendar.get(Calendar.MINUTE);

                break;
        }
        return data;
    }

    public enum NightMode {
        Off,
        Sunset,
        Sheduled;
    }
}
