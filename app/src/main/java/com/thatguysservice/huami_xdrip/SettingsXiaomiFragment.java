package com.thatguysservice.huami_xdrip;

import android.os.Bundle;

import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

/**
 * Xiaomi / HyperOS watches, via the Xiaomi Wear app.
 */
public class SettingsXiaomiFragment extends BaseSettingsFragment {

    @Override
    protected int getScreenTitle() {
        return R.string.screen_xiaomi;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_xiaomi, rootKey);
        bindEnableSwitch(MiBandEntry.PREF_ENABLE_XIAOMI_SERVICE, false);
    }
}
