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
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.models.BgData;
import com.thatguysservice.huami_xdrip.models.PropertiesUpdate;
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

    /// Called from onCreate - this puts up a dialog explaining we need permissions, and goes to the correct Activity

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
           /* In order to be able to set ringer mode to silent
           the permission to access notifications is needed above Android M
           ACCESS_NOTIFICATION_POLICY is also needed in the manifest */
            if (!((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).isNotificationPolicyAccessGranted()) {
                // Put up a dialog explaining why we need permissions (Polite, but also Play Store policy)
                // When accepted, we open the Activity for Notification access
                DialogFragment dialog = new NotifyPolicyPermissionsDialogFragment();
                dialog.show(getActivity().getSupportFragmentManager(), "PermissionsDialogFragment");
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BgDataRepository dataRepository = BgDataRepository.getInstance();

        // Create the observer which updates the UI.
        final Observer<PropertiesUpdate> prefObserver = new Observer<PropertiesUpdate>() {
            @Override
            public void onChanged(@Nullable final PropertiesUpdate prop) {
                try {
                    EditTextPreference pref = findPreference(prop.getKey());
                    pref.setText(prop.getValue());
                }catch (Exception e){

                }
            }
        };
        dataRepository.getPropLiveData().observe(this, prefObserver);

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
