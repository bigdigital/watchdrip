package com.thatguysservice.huami_xdrip;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.models.PersistantDeviceInfo;
import com.thatguysservice.huami_xdrip.models.PersistantDevices;
import com.thatguysservice.huami_xdrip.models.Pref;
import com.thatguysservice.huami_xdrip.models.PropertiesUpdate;
import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.repository.BgDataRepository;
import com.thatguysservice.huami_xdrip.watch.miband.MiBand;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;

import static com.thatguysservice.huami_xdrip.models.Constants.REQUEST_ID_BLUETOOTH_PERMISSIONS;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.bgForce;
import static com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry.PREF_ENABLE_WEB_SERVER;

public class SettingsFragment extends PreferenceFragmentCompat {

    private void updateAdvancedMenuVisibility(){
        boolean xiaomiService = Pref.getBooleanDefaultFalse(MiBandEntry.PREF_ENABLE_XIAOMI_SERVICE);
        boolean serviceEnabled = Pref.getBooleanDefaultFalse(MiBandEntry.PREF_MIBAND_ENABLED);
        boolean deviceEnabled = Pref.getBooleanDefaultFalse(MiBandEntry.PREF_MIBAND_ENABLE_DEVICE);
        boolean enabled = serviceEnabled && (deviceEnabled || xiaomiService);
        if (advancedPreferenseMenu != null) {
            advancedPreferenseMenu.setEnabled(enabled);
        }
    }

    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.startsWith("miband")) {
                UserError.Log.d("miband", "Preference key: " + key);
                if (!key.equals(MiBandEntry.PREF_MIBAND_ENABLED)) {
                    MiBandEntry.refresh();
                }
            }
            if (key.startsWith("miband_enable")) {
                updateAdvancedMenuVisibility();
            }

        }
    };
    BgDataRepository bgDataRepository;
    private TwoStatePreference servicePref;
    private TwoStatePreference deviceEnabledPref;
    private TwoStatePreference webServerEnabledPref;
    private TwoStatePreference xiaomiServerEnabledPref;
    private ListPreference activeDevicePref;

    private Preference advancedPreferenseMenu;

    protected void setListPreferenceData() {
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

    protected void updateListPreferenceVisibility() {
        PersistantDevices devices = MiBand.getDevices();
        int size = devices.getDevices().size();
        if (size <= 1) {
            activeDevicePref.setVisible(false);
            return;
        }
        activeDevicePref.setVisible(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bgDataRepository = BgDataRepository.getInstance();
        super.onViewCreated(view, savedInstanceState);
    }

    /// Called from onCreate - this puts up a dialog explaining we need permissions, and goes to the correct Activity

    public void setDevicePref(Boolean state) {
        deviceEnabledPref.callChangeListener(state);
        deviceEnabledPref.setChecked(state);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.advanced_preferences, rootKey);
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        servicePref = findPreference(MiBandEntry.PREF_MIBAND_ENABLED);
        deviceEnabledPref = findPreference(MiBandEntry.PREF_MIBAND_ENABLE_DEVICE);
        webServerEnabledPref = findPreference(PREF_ENABLE_WEB_SERVER);
        xiaomiServerEnabledPref = findPreference(MiBandEntry.PREF_ENABLE_XIAOMI_SERVICE);
        advancedPreferenseMenu = findPreference("advanced_pref");
        servicePref.setOnPreferenceChangeListener((preference, newValue) -> prefEnableCallback(preference, (Boolean) newValue));
        deviceEnabledPref.setOnPreferenceChangeListener((preference, newValue) -> prefEnableCallback(preference, (Boolean) newValue));
        webServerEnabledPref.setOnPreferenceChangeListener((preference, newValue) -> prefEnableCallback(preference, (Boolean) newValue));
        xiaomiServerEnabledPref.setOnPreferenceChangeListener((preference, newValue) -> prefEnableCallback(preference, (Boolean) newValue));



        activeDevicePref = findPreference(MiBandEntry.PREF_MIBAND_ACTIVE_DEVICE);
        activeDevicePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
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
            }
        });
        setListPreferenceData();

        updateAdvancedMenuVisibility();
        updateListPreferenceVisibility();
    }
    private boolean prefEnableCallback(Preference preference, Boolean newValue) {
        if (preference == deviceEnabledPref && (Boolean) newValue) {
            boolean result = checkAndRequestBTPermissions();
            if (!result) {
                return false;
            }
        }
        Inevitable.task("start_on_pref_changed", 500, new Runnable() {
            @Override
            public void run() {
                bgForce();
            }
        });
        bgDataRepository.setNewServiceStatus((Boolean) newValue);
        return true;
    }

    private boolean checkAndRequestBTPermissions() {

        // Map each permission to its corresponding string resource ID
        Map<String, Integer> permissionMap = new HashMap<>();

        permissionMap.put(android.Manifest.permission.ACCESS_FINE_LOCATION, R.string.permission_location_info);
        permissionMap.put(android.Manifest.permission.READ_EXTERNAL_STORAGE, R.string.permissions_file_system_read);


        FragmentActivity context = this.getActivity();
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!EasyPermissions.hasPermissions(context, Manifest.permission.BLUETOOTH_CONNECT)) {
                listPermissionsNeeded.add(android.Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (!EasyPermissions.hasPermissions(context, Manifest.permission.BLUETOOTH_SCAN)) {
                listPermissionsNeeded.add(android.Manifest.permission.BLUETOOTH_SCAN);
            }

            permissionMap.put(android.Manifest.permission.BLUETOOTH_CONNECT, R.string.permissions_bt_connect);
            permissionMap.put(android.Manifest.permission.BLUETOOTH_SCAN, R.string.permissions_bt_scan);
        }

        // Location needs to be enabled for Bluetooth discovery on Marshmallow.
        if (!EasyPermissions.hasPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            listPermissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!EasyPermissions.hasPermissions(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    listPermissionsNeeded.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
                }
            }
        }
        if (!EasyPermissions.hasPermissions(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            listPermissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
           /* In order to be able to set ringer mode to silent
           the permission to access notifications is needed above Android M
           ACCESS_NOTIFICATION_POLICY is also needed in the manifest */
        if (!((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).isNotificationPolicyAccessGranted()) {
            // Put up a dialog explaining why we need permissions (Polite, but also Play Store policy)
            // When accepted, we open the Activity for Notification access
            DialogFragment dialog = new NotifyPolicyPermissionsDialogFragment();
            dialog.show(getActivity().getSupportFragmentManager(), "PermissionsDialogFragment");
        }
        // Android 10 check additional permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!EasyPermissions.hasPermissions(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                listPermissionsNeeded.add(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                permissionMap.put(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION, R.string.permissions_background_location_info);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            // Generate the dialog content based on the permissions needed
            StringBuilder dialogContent = new StringBuilder();
            for (String permission : listPermissionsNeeded) {
                Integer stringResId = permissionMap.get(permission);
                if (stringResId != null) {
                    dialogContent.append(context.getString(stringResId)).append("\n\n");
                }
            }

            // Trim the final content to remove any trailing space
            String rationaleText = context.getString(R.string.permission_dialog_start) + dialogContent.toString().trim();

            EasyPermissions.requestPermissions(context, rationaleText, REQUEST_ID_BLUETOOTH_PERMISSIONS, listPermissionsNeeded.toArray(new String[0]));

            return false;
        }
        return true;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BgDataRepository dataRepository = BgDataRepository.getInstance();

        // Create the observer which updates the UI.
        final Observer<PropertiesUpdate> prefObserver = new Observer<PropertiesUpdate>() {
            @Override
            public void onChanged(@Nullable final PropertiesUpdate prop) {
                try {
                    String propKey = prop.getKey();
                    EditTextPreference pref = findPreference(propKey);
                    pref.setText(prop.getValue());
                } catch (Exception e) {

                }
            }
        };
        dataRepository.getPropLiveData().observe(this, prefObserver);


        // Create the observer which updates the UI.
        final Observer<Integer> activeDevicePropObserver = new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable final Integer newActiveIndex) {
                MiBandEntry.setActiveDeviceIndex(newActiveIndex);
                setListPreferenceData();
                updateListPreferenceVisibility();
            }
        };
        dataRepository.getActiveDeviceChangeData().observe(this, activeDevicePropObserver);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(prefListener);
        if (MiBandEntry.isDeviceEnabled() && !checkAndRequestBTPermissions()) {
            setDevicePref(false);
        }
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(prefListener);
        super.onPause();
    }

    public static class NotifyPolicyPermissionsDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            Context context = getContext();
            builder.setMessage(context.getString(R.string.permission_notification_policy_access,
                            getContext().getString(R.string.app_name),
                            getContext().getString(R.string.ok)))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                            }
                        }
                    });
            return builder.create();
        }
    }
}
