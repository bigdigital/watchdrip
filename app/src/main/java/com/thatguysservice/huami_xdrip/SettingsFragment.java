package com.thatguysservice.huami_xdrip;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.thatguysservice.huami_xdrip.models.PersistantDeviceInfo;
import com.thatguysservice.huami_xdrip.repository.BgDataRepository;
import com.thatguysservice.huami_xdrip.services.TelinkService;
import com.thatguysservice.huami_xdrip.watch.miband.MiBand;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

/**
 * Main settings screen: the service switch and one row per integration, each opening its own screen.
 */
public class SettingsFragment extends BaseSettingsFragment {

    // Store the defaults of screens the user may never open, services rely on some of them
    private static final int[] SCREENS = {
            R.xml.pref_miband, R.xml.pref_zepp, R.xml.pref_xiaomi, R.xml.pref_garmin,
            R.xml.pref_telink, R.xml.pref_data, R.xml.pref_troubleshooting
    };

    @Override
    protected int getScreenTitle() {
        return 0;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        for (int screen : SCREENS) {
            // readAgain = true only fills in values that are missing, it never overwrites
            PreferenceManager.setDefaultValues(requireContext(), screen, true);
        }
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        bindEnableSwitch(MiBandEntry.PREF_MIBAND_ENABLED, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Devices can be added, renamed or switched from the toolbar menu while this screen is shown
        BgDataRepository.getInstance().getActiveDeviceChangeData().observe(this, index -> updateSummaries());
    }

    @Override
    protected void onPreferenceChanged(String key) {
        updateSummaries();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Bluetooth integrations without their permissions are switched off until granted again
        if ((MiBandEntry.isDeviceEnabled() || TelinkService.shouldServiceRun()) && !checkAndRequestBTPermissions()) {
            if (MiBandEntry.isDeviceEnabled()) disableUntilBluetoothGranted(MiBandEntry.PREF_MIBAND_ENABLE_DEVICE);
            if (TelinkService.shouldServiceRun()) disableUntilBluetoothGranted(MiBandEntry.PREF_ENABLE_TELINK_SERVICE);
        }
        updateSummaries();
    }

    @Override
    public void onBluetoothPermissionsGranted() {
        super.onBluetoothPermissionsGranted();
        updateSummaries();
    }

    private void updateSummaries() {
        String activeDevice = null;
        if (MiBandEntry.isDeviceEnabled()) {
            final PersistantDeviceInfo device = MiBand.getDevices().getDeviceByIndex(MiBandEntry.getActiveDeviceIndex());
            if (device != null) activeDevice = device.getName();
        }
        setStatus("screen_miband", MiBandEntry.isDeviceEnabled(), activeDevice);
        setStatus("screen_zepp", MiBandEntry.isWebServerEnabled(), null);
        setStatus("screen_xiaomi", MiBandEntry.isXiaomiServiceEnabled(), null);
        setStatus("screen_garmin", MiBandEntry.isGarminServiceEnabled(), null);

        String thermometers = null;
        if (TelinkService.shouldServiceRun()) {
            final int count = TelinkService.getMacs().size();
            thermometers = count == 0
                    ? getString(R.string.status_telink_no_mac)
                    : getResources().getQuantityString(R.plurals.status_telink_count, count, count);
        }
        setStatus("screen_telink", TelinkService.shouldServiceRun(), thermometers);
    }

    private void setStatus(String key, boolean enabled, String detail) {
        final Preference pref = findPreference(key);
        if (pref == null) return;
        if (!enabled) {
            pref.setSummary(R.string.status_off);
        } else if (detail == null) {
            pref.setSummary(R.string.status_on);
        } else {
            pref.setSummary(getString(R.string.status_on_detail, detail));
        }
    }
}
