package com.thatguysservice.huami_xdrip;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.thatguysservice.huami_xdrip.models.Constants.REQUEST_ID_READ_WRITE_PERMISSIONS;

public class SettingsAdvancedFragment extends PreferenceFragmentCompat {
    TwoStatePreference customWatcfacePref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.advanced_preferences, rootKey);
        customWatcfacePref = findPreference(MiBandEntry.PREF_MIBAND_USE_CUSTOM_WATHCFACE);
        customWatcfacePref.setOnPreferenceChangeListener((preference, newValue) -> {
            if ((Boolean) newValue) {
                boolean result = checkAndRequestFilePermissions();
                if (!result) {
                    return false;
                }
            }
            return true;
        });
    }

    private boolean checkAndRequestFilePermissions() {
        FragmentActivity context = this.getActivity();
        List<String> listPermissionsNeeded = new ArrayList<>();

        //check read permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!EasyPermissions.hasPermissions(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                listPermissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        //checkWritePermissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!EasyPermissions.hasPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            String rationaleText = context.getString(R.string.permission_dialog_start) + context.getString(R.string.permissions_file_system);
            EasyPermissions.requestPermissions(context, rationaleText, REQUEST_ID_READ_WRITE_PERMISSIONS, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]));

            return false;
        }
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setWatchfacePref(boolean checked) {
        customWatcfacePref.setChecked(checked);
    }


    @Override
    public void onResume() {
        super.onResume();

        if (MiBandEntry.isNeedToUseCustomWatchface() && !checkAndRequestFilePermissions()){
            setWatchfacePref(false);
        }
    }

}
