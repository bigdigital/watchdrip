package com.thatguysservice.huami_xdrip;

import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.thatguysservice.huami_xdrip.models.Pref;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

/**
 * Settings shared by every integration: graph range, treatments and how often data is pushed.
 */
public class SettingsDataFragment extends BaseSettingsFragment {

    @Override
    protected int getScreenTitle() {
        return R.string.screen_data;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_data, rootKey);
        numericInput(MiBandEntry.PREF_MIBAND_GRAPH_HOURS);
        numericInput(MiBandEntry.PREF_MIBAND_ALL_DAY_INTERVAL);

        // These listeners also keep the titles showing the current value
        final Preference nightModeInterval = findPreference(MiBandEntry.PREF_MIBAND_NIGHTMODE_INTERVAL);
        nightModeInterval.setOnPreferenceChangeListener(MiBandEntry.sBindMibandPreferenceChangeListener);
        MiBandEntry.sBindMibandPreferenceChangeListener.onPreferenceChange(nightModeInterval,
                Pref.getInt(MiBandEntry.PREF_MIBAND_NIGHTMODE_INTERVAL, -1));

        final EditTextPreference allDayInterval = findPreference(MiBandEntry.PREF_MIBAND_ALL_DAY_INTERVAL);
        allDayInterval.setOnPreferenceChangeListener(MiBandEntry.sBindMibandPreferenceChangeListener);
        MiBandEntry.sBindMibandPreferenceChangeListener.onPreferenceChange(allDayInterval,
                Integer.toString(MiBandEntry.getDayModeInterval()));
    }
}
