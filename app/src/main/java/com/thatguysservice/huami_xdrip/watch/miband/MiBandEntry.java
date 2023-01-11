package com.thatguysservice.huami_xdrip.watch.miband;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;

import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.R;
import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.Pref;

import java.util.Date;

import static com.thatguysservice.huami_xdrip.services.BroadcastService.CMD_LOCAL_REFRESH;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.INTENT_FUNCTION_KEY;

public class MiBandEntry {
    public static final String PREF_MIBAND_ENABLED = "miband_enabled";
    public static final String PREF_MIBAND_MAC = "miband_data_mac";
    public static final String PREF_MIBAND_ENABLE_DEVICE = "miband_enable_device";
    public static final String PREF_MIBAND_ACTIVE_DEVICE = "miband_active_device";
    public static final String PREF_MIBAND_AUTH_KEY = "miband_data_authkey";
    public static final String PREF_MIBAND_SEND_READINGS = "miband_send_readings";
    public static final String PREF_VIBRATE_ON_READINGS = "miband_vibrate_on_readings";
    public static final String PREF_SEND_ALARMS = "miband_send_alarms";
    public static final String PREF_SEND_ALARMS_OTHER = "miband_send_alarms_other";
    public static final String PREF_ENABLE_WEB_SERVER = "miband_enable_web_server";
    public static final String PREF_MIBAND_SETTINGS = "miband_settings";
    public static final String PREF_MIBAND_PREFERENCES = "miband_preferences";
    public static final String PREF_MIBAND_UPDATE_BG = "update_miband_bg";
    public static final String PREF_MIBAND_NIGHTMODE_ENABLED = "miband_nightmode_enabled";
    public static final String PREF_MIBAND_NIGHTMODE_START = "miband_nightmode_start";
    public static final String PREF_MIBAND_NIGHTMODE_END = "miband_nightmode_end";
    public static final String PREF_MIBAND_NIGHTMODE_INTERVAL = "miband_nightmode_interval";
    public static final String PREF_MIBAND_GRAPH_HOURS = "miband_graph_hours";
    public static final String PREF_MIBAND_GRAPH_LIMIT = "miband_graph_limit";
    public static final String PREF_MIBAND_TREATMENT_ENBALE = "miband_graph_treatment_enable";
    public static final String PREF_MIBAND_DISABLE_HIGH_MTU = "debug_miband_disable_high_mtu";
    public static final String PREF_MIBAND_RSSI_TRESHOLD = "advanced_rssi_threshold";
    public static final String PREF_MIBAND_USE_CUSTOM_WATHCFACE = "debug_miband_use_custom_watchface";
    public static final String PREF_MIBAND_COLLECT_HEARTRATE = "miband_collect_heartrate";
    public static final String PREF_MIBAND_COLLECT_STEPS = "miband_collect_steps";
    public static final String PREF_MIBAND_DATE_CAREGORY = "miband_date_settings_category";
    public static final String PREF_MIBAND_US_DATE_FORMAT = "miband_us_date_format";

    protected static final int MINIMUM_RSSI = 90;

    public static final int NIGHT_MODE_INTERVAL_STEP = 5;
    public static Preference.OnPreferenceChangeListener sBindMibandPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            try {
                String key = preference.getKey();
                if (key.equals(MiBandEntry.PREF_MIBAND_NIGHTMODE_INTERVAL)) {
                    setNightModeInterval((int) value);
                    final String minutes = HuamiXdrip.gs(R.string.unit_minutes);
                    final String title_text = HuamiXdrip.gs(R.string.title_miband_interval_in_nightmode);

                    int nightModeInterval = MiBandEntry.getNightModeInterval();
                    if (nightModeInterval == MiBandEntry.NIGHT_MODE_INTERVAL_STEP)
                        preference.setTitle(String.format("%s (%s)", title_text, "live"));
                    else
                        preference.setTitle(String.format("%s (%d %s)", title_text, nightModeInterval, minutes));
                } else if (key.equals(MiBandEntry.PREF_MIBAND_GRAPH_LIMIT)) {
                    final int ivalue = (int) value;
                    setGraphLimit(ivalue);
                    final String title_text = HuamiXdrip.gs(R.string.title_miband_miband_graph_limit);

                    boolean isgMgDl = MiBand.usingMgDl();
                    final String unit = Helper.unit(isgMgDl);
                    double dVal;
                    if (isgMgDl) {
                        dVal = ivalue * Constants.MMOLL_TO_MGDL;
                    } else {
                        dVal = ivalue; // no conversion needed
                    }
                    preference.setTitle(String.format("%s (%d %s)", title_text, (int) Math.round(dVal), unit));
                }
            } catch (Exception e) {
                //
            }
            return true;
        }
    };

    public static boolean isDeviceEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_MIBAND_ENABLE_DEVICE);
    }

    public static int getActiveDeviceIndex() {
        return Integer.parseInt(Pref.getString(PREF_MIBAND_ACTIVE_DEVICE, "0"));
    }

    public static void setActiveDeviceIndex(int index) {
        Pref.setString(PREF_MIBAND_ACTIVE_DEVICE, String.valueOf(index));
    }

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse(PREF_MIBAND_ENABLED);
    }

    public static boolean isWebServerEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_ENABLE_WEB_SERVER);
    }

    public static boolean areAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_SEND_ALARMS);
    }

    public static boolean areOtherAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_SEND_ALARMS_OTHER);
    }

    public static boolean isVibrateOnReadings() {
        return Pref.getBooleanDefaultFalse(PREF_VIBRATE_ON_READINGS);
    }

    public static boolean isNeedSendReading() {
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_MIBAND_SEND_READINGS);
    }

    public static boolean isNeedSendReadingAsNotification() {
        return !MiBandType.supportGraph(MiBand.getMibandType());
    }

    public static boolean isNightModeEnabled() {
        if (MiBand.getMibandType() == MiBandType.MI_BAND2) return false;
        return Pref.getBooleanDefaultFalse(PREF_MIBAND_NIGHTMODE_ENABLED);
    }

    public static Date getNightModeStart() {
        return new Date(Pref.getLong(PREF_MIBAND_NIGHTMODE_START, 0));
    }

    public static Date getNightModeEnd() {
        return new Date(Pref.getLong(PREF_MIBAND_NIGHTMODE_END, 0));
    }

    public static int getNightModeInterval() {
        return (Pref.getInt(PREF_MIBAND_NIGHTMODE_INTERVAL, 0) + 1) * NIGHT_MODE_INTERVAL_STEP;
    }

    public static void setNightModeInterval(int val) {
        Pref.setInt(PREF_MIBAND_NIGHTMODE_INTERVAL, val);
    }

    public static boolean isTreatmentEnabled() {
        return Pref.getBooleanDefaultFalse(PREF_MIBAND_TREATMENT_ENBALE);
    }

    public static int getGraphHours() {
        return Pref.getStringToInt(PREF_MIBAND_GRAPH_HOURS, 4);
    }

    public static int getGraphLimit() {
        return 16; //Pref.getInt(PREF_MIBAND_GRAPH_LIMIT, 16);
    }

    public static int getRSSITreshold() {
        return -Pref.getStringToInt(PREF_MIBAND_RSSI_TRESHOLD, MINIMUM_RSSI);
    }

    public static void setGraphLimit(int val) {
        Pref.setInt(PREF_MIBAND_GRAPH_LIMIT, val);
    }

    public static boolean isNeedToDisableHightMTU() {
        return Pref.getBooleanDefaultFalse(PREF_MIBAND_DISABLE_HIGH_MTU);
    }

    public static boolean setCustomWatchfaceUse(boolean val) {
        return Pref.setBoolean(PREF_MIBAND_USE_CUSTOM_WATHCFACE, val);
    }

    public static boolean isNeedToUseCustomWatchface() {
        return Pref.getBooleanDefaultFalse(PREF_MIBAND_USE_CUSTOM_WATHCFACE);
    }

    public static boolean isNeedToCollectHR() {
        return Pref.getBooleanDefaultFalse(PREF_MIBAND_COLLECT_HEARTRATE);
    }

    public static boolean isNeedToCollectSteps() {
        return Pref.getBooleanDefaultFalse(PREF_MIBAND_COLLECT_STEPS);
    }

    public static boolean isUS_DateFormat() {
        return Pref.getBooleanDefaultFalse(PREF_MIBAND_US_DATE_FORMAT);
    }

    public static void refresh() {
        Inevitable.task("miband-preference-changed", 1000, () -> Helper.startService(MiBandService.class, INTENT_FUNCTION_KEY, CMD_LOCAL_REFRESH));
    }

    public static void sendToService(String function, Bundle bundle) {
        if (isEnabled()) {
            Intent serviceIntent = new Intent(HuamiXdrip.getAppContext(), MiBandService.class);
            serviceIntent.putExtra(INTENT_FUNCTION_KEY, function);
            serviceIntent.putExtras(bundle);
            HuamiXdrip.getAppContext().startService(serviceIntent);
        }
    }

}
