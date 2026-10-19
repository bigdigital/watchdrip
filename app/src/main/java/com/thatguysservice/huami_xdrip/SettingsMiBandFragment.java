package com.thatguysservice.huami_xdrip;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.TwoStatePreference;

import com.thatguysservice.huami_xdrip.models.PersistantDeviceInfo;
import com.thatguysservice.huami_xdrip.models.PersistantDevices;
import com.thatguysservice.huami_xdrip.models.PropertiesUpdate;
import com.thatguysservice.huami_xdrip.repository.BgDataRepository;
import com.thatguysservice.huami_xdrip.watch.miband.MiBand;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.thatguysservice.huami_xdrip.models.Constants.REQUEST_ID_READ_WRITE_PERMISSIONS;

/**
 * Mi Band / Amazfit watches connected directly over Bluetooth.
 */
public class SettingsMiBandFragment extends BaseSettingsFragment {
    private ListPreference activeDevicePref;
    private TwoStatePreference customWatchfacePref;
    private BgDataRepository bgDataRepository;

    @Override
    protected int getScreenTitle() {
        return R.string.screen_miband;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_miband, rootKey);
        bgDataRepository = BgDataRepository.getInstance();
        bindEnableSwitch(MiBandEntry.PREF_MIBAND_ENABLE_DEVICE, true);
        numericInput(MiBandEntry.PREF_MIBAND_RSSI_TRESHOLD);

        customWatchfacePref = findPreference(MiBandEntry.PREF_MIBAND_USE_CUSTOM_WATHCFACE);
        customWatchfacePref.setOnPreferenceChangeListener((preference, newValue) ->
                !(Boolean) newValue || checkAndRequestFilePermissions());

        activeDevicePref = findPreference(MiBandEntry.PREF_MIBAND_ACTIVE_DEVICE);
        activeDevicePref.setOnPreferenceChangeListener((preference, newValue) -> {
            PersistantDevices devices = MiBand.getDevices();
            int oldIndex = Integer.parseInt(((ListPreference) preference).getValue());
            PersistantDeviceInfo device = devices.getDeviceByIndex(oldIndex);
            device.setAuthKey(MiBand.getAuthKeyPref());
            device.setMacAddress(MiBand.getMacPref());
            devices.updateDevice(device, oldIndex);

            int newIndex = Integer.parseInt((String) newValue);
            device = devices.getDeviceByIndex(newIndex);
            if (device == null) return false;
            MiBandEntry.setActiveDeviceIndex(newIndex);
            setListPreferenceData();
            MiBand.setMacPref(device.getMacAddress(), bgDataRepository);
            MiBand.setAuthKeyPref(device.getAuthKey(), bgDataRepository);
            return true;
        });
        setListPreferenceData();
        updateListPreferenceVisibility();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BgDataRepository dataRepository = BgDataRepository.getInstance();

        // MAC and auth key change when another device is selected from the toolbar menu
        final Observer<PropertiesUpdate> prefObserver = prop -> {
            try {
                EditTextPreference pref = findPreference(prop.getKey());
                pref.setText(prop.getValue());
            } catch (Exception e) {
                // not a preference of this screen
            }
        };
        dataRepository.getPropLiveData().observe(this, prefObserver);

        final Observer<Integer> activeDeviceObserver = newActiveIndex -> {
            MiBandEntry.setActiveDeviceIndex(newActiveIndex);
            setListPreferenceData();
            updateListPreferenceVisibility();
        };
        dataRepository.getActiveDeviceChangeData().observe(this, activeDeviceObserver);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (MiBandEntry.isNeedToUseCustomWatchface() && !checkAndRequestFilePermissions()) {
            setWatchfacePref(false);
        }
    }

    private void setListPreferenceData() {
        PersistantDevices devices = MiBand.getDevices();
        int size = devices.getDevices().size();
        if (size == 0) {
            return;
        }
        final CharSequence[] entries = new CharSequence[size];
        final CharSequence[] entryValues = new CharSequence[size];
        int i = 0;
        for (PersistantDeviceInfo device : devices.getDevices()) {
            entries[i] = device.getName();
            entryValues[i] = String.valueOf(i);
            i++;
        }
        activeDevicePref.setEntries(entries);
        activeDevicePref.setEntryValues(entryValues);
        activeDevicePref.setDefaultValue(entryValues[0]);

        int deviceEntryIndex = MiBandEntry.getActiveDeviceIndex();
        activeDevicePref.setValue(String.valueOf(deviceEntryIndex));
        PersistantDeviceInfo device = devices.getDeviceByIndex(deviceEntryIndex);
        if (device != null) {
            activeDevicePref.setTitle(getString(R.string.title_active_device) + " (" + device.getName() + ")");
        }
    }

    private void updateListPreferenceVisibility() {
        activeDevicePref.setVisible(MiBand.getDevices().getDevices().size() > 1);
    }

    public void setWatchfacePref(boolean checked) {
        customWatchfacePref.setChecked(checked);
    }

    private boolean checkAndRequestFilePermissions() {
        FragmentActivity context = this.getActivity();
        List<String> listPermissionsNeeded = new ArrayList<>();

        //check read permission
        if (!EasyPermissions.hasPermissions(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        //checkWritePermissions
        if (!EasyPermissions.hasPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            String rationaleText = context.getString(R.string.permission_dialog_start) + context.getString(R.string.permissions_file_system);
            EasyPermissions.requestPermissions(context, rationaleText, REQUEST_ID_READ_WRITE_PERMISSIONS, listPermissionsNeeded.toArray(new String[0]));

            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                    startActivity(intent);
                } catch (Exception ex) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
                return false;
            }
        }
        return true;
    }
}
