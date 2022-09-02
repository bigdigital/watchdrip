package com.thatguysservice.huami_xdrip;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import com.thatguysservice.huami_xdrip.utils.time.TimePreference;
import com.thatguysservice.huami_xdrip.utils.time.TimePreferenceDialogFragmentCompat;
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
        if (customWatcfacePref != null) {
            customWatcfacePref.setOnPreferenceChangeListener((preference, newValue) -> {
                if ((Boolean) newValue) {
                    boolean result = checkAndRequestFilePermissions();
                    return result;
                }
                return true;
            });
        }

        Preference miband_nightmode_interval = findPreference(MiBandEntry.PREF_MIBAND_NIGHTMODE_INTERVAL);
        if (miband_nightmode_interval != null) {
            miband_nightmode_interval.setOnPreferenceChangeListener(MiBandEntry.sBindMibandPreferenceChangeListener);
            MiBandEntry.sBindMibandPreferenceChangeListener.onPreferenceChange(miband_nightmode_interval,
                    PreferenceManager
                            .getDefaultSharedPreferences(miband_nightmode_interval.getContext())
                            .getInt(miband_nightmode_interval.getKey(), -1));
        }

        EditTextPreference editTextPreference = getPreferenceManager().findPreference(MiBandEntry.PREF_MIBAND_RSSI_TRESHOLD);
        editTextPreference.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            }
        });

        editTextPreference = getPreferenceManager().findPreference(MiBandEntry.PREF_MIBAND_GRAPH_HOURS);
        editTextPreference.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            }
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

        if (MiBandEntry.isNeedToUseCustomWatchface() && !checkAndRequestFilePermissions()) {
            setWatchfacePref(false);
        }
    }


    @Override
    public void onDisplayPreferenceDialog(Preference preference) {

        if (preference instanceof TimePreference) {
            DialogFragment dialogFragment = new TimePreferenceDialogFragmentCompat();
            Bundle bundle = new Bundle(1);
            bundle.putString("key", preference.getKey());
            dialogFragment.setArguments(bundle);
            dialogFragment.setTargetFragment(this, 0);
            if (getFragmentManager() != null) {
                dialogFragment.show(getFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
            }
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}
