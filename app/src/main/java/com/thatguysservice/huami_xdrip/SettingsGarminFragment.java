package com.thatguysservice.huami_xdrip;

import android.os.Bundle;

import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

/**
 * Garmin watches, via the ConnectIQ app.
 */
public class SettingsGarminFragment extends BaseSettingsFragment {

    @Override
    protected int getScreenTitle() {
        return R.string.screen_garmin;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_garmin, rootKey);
        bindEnableSwitch(MiBandEntry.PREF_ENABLE_GARMIN_SERVICE, false);
    }
}
