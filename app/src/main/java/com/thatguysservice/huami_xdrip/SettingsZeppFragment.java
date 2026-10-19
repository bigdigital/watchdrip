package com.thatguysservice.huami_xdrip;

import android.os.Bundle;

import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

/**
 * Zepp OS watches, served by the built-in web server.
 */
public class SettingsZeppFragment extends BaseSettingsFragment {

    @Override
    protected int getScreenTitle() {
        return R.string.screen_zepp;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_zepp, rootKey);
        bindEnableSwitch(MiBandEntry.PREF_ENABLE_WEB_SERVER, false);
    }
}
