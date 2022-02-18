package com.thatguysservice.huami_xdrip;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.repository.BgDataRepository;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.thatguysservice.huami_xdrip.models.Constants.REQUEST_ID_BLUETOOTH_PERMISSIONS;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.bgForce;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.startsWith("miband")) {
                UserError.Log.d("miband", "Preference key: " + key);
                if (!key.equals(MiBandEntry.PREF_MIBAND_ENABLED)) {
                    MiBandEntry.refresh();
                }
            }
        }
    };
    BgDataRepository bgDataRepository;
    TwoStatePreference servicePref;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bgDataRepository = BgDataRepository.getInstance();
        super.onViewCreated(view, savedInstanceState);
    }

    public void setServicePref(Boolean state) {
        servicePref.callChangeListener(state);
        servicePref.setChecked(state);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        servicePref = findPreference(MiBandEntry.PREF_MIBAND_ENABLED);
        servicePref.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((Boolean) newValue) {
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
        });
    }

    private boolean checkAndRequestBTPermissions() {
        FragmentActivity context = this.getActivity();
        List<String> listPermissionsNeeded = new ArrayList<>();

        // Location needs to be enabled for Bluetooth discovery on Marshmallow.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!EasyPermissions.hasPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                listPermissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (!EasyPermissions.hasPermissions(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                listPermissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } else {
            // Android 10 check additional permissions
            if (Build.VERSION.SDK_INT >= 29) {
                if (!EasyPermissions.hasPermissions(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    listPermissionsNeeded.add(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                }
                if (!EasyPermissions.hasPermissions(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    listPermissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {

            String dialogContent = "";
            if (listPermissionsNeeded.contains(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                dialogContent = dialogContent + context.getString(R.string.permission_location_info);
            }
            if (listPermissionsNeeded.contains(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                dialogContent = dialogContent + context.getString(R.string.permissions_background_location_info);
            }
            if (listPermissionsNeeded.contains(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                dialogContent = dialogContent + context.getString(R.string.permissions_file_system_read);
            }

            String rationaleText = context.getString(R.string.permission_dialog_start) + dialogContent;

            EasyPermissions.requestPermissions(context, rationaleText, REQUEST_ID_BLUETOOTH_PERMISSIONS, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]));

            return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(prefListener);
        if (MiBandEntry.isEnabled() && !checkAndRequestBTPermissions()){
            setServicePref(false);
        }
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(prefListener);
        super.onPause();
    }
}
