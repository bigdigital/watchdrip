package com.thatguysservice.huami_xdrip;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;

import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.thatguysservice.huami_xdrip.UtilityModels.Inevitable;
import com.thatguysservice.huami_xdrip.models.Pref;
import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.repository.BgDataRepository;
import com.thatguysservice.huami_xdrip.services.BroadcastService;
import com.thatguysservice.huami_xdrip.utils.time.TimePreference;
import com.thatguysservice.huami_xdrip.utils.time.TimePreferenceDialogFragmentCompat;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.EasyPermissions;

import static com.thatguysservice.huami_xdrip.models.Constants.REQUEST_ID_BLUETOOTH_PERMISSIONS;
import static com.thatguysservice.huami_xdrip.services.BroadcastService.bgForce;

/**
 * Shared behaviour of all settings screens
 */
public abstract class BaseSettingsFragment extends PreferenceFragmentCompat {

    private final List<String> waitingForBluetooth = new ArrayList<>();

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        if (key == null) return;
        if (key.startsWith("miband")) {
            UserError.Log.d("miband", "Preference key: " + key);
            if (!key.equals(MiBandEntry.PREF_MIBAND_ENABLED)) {
                MiBandEntry.refresh();
            }
        }
        if (key.equals(MiBandEntry.PREF_TELINK_DEVICES)) {
            Inevitable.task("telink_mac_changed", 500, BroadcastService::bgForce);
        }
        onPreferenceChanged(key);
    };

    /**
     * Toolbar title for this screen, 0 for the app name
     */
    @StringRes
    protected abstract int getScreenTitle();

    protected void onPreferenceChanged(String key) {
    }

    /**
     * Wires an integration's on/off switch
     */
    protected void bindEnableSwitch(String key, boolean needsBluetooth) {
        final Preference pref = findPreference(key);
        if (pref == null) return;
        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            final boolean enabled = (Boolean) newValue;
            if (needsBluetooth && enabled && !checkAndRequestBTPermissions()) {
                waitingForBluetooth.add(key);
                return false;
            }
            onEnableChanged(enabled);
            return true;
        });
    }

    protected void onEnableChanged(boolean enabled) {
        Inevitable.task("start_on_pref_changed", 500, () -> bgForce());
        BgDataRepository.getInstance().setNewServiceStatus(enabled);
    }

    // Turns a Bluetooth integration off until its permissions are granted
    protected void disableUntilBluetoothGranted(String key) {
        waitingForBluetooth.add(key);
        final TwoStatePreference pref = findPreference(key);
        if (pref != null) {
            pref.setChecked(false);
        } else {
            Pref.setBoolean(key, false);
        }
        onEnableChanged(false);
    }

    // Called by MainActivity once the Bluetooth permissions requested here were granted
    public void onBluetoothPermissionsGranted() {
        for (String key : waitingForBluetooth) {
            final TwoStatePreference pref = findPreference(key);
            if (pref != null) {
                pref.setChecked(true);
            } else {
                Pref.setBoolean(key, true);
            }
        }
        if (!waitingForBluetooth.isEmpty()) {
            onEnableChanged(true);
        }
        waitingForBluetooth.clear();
    }

    protected void numericInput(String key) {
        final EditTextPreference pref = findPreference(key);
        if (pref != null) {
            pref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(prefListener);
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            final CollapsingToolbarLayout toolbar = activity.findViewById(R.id.toolbar_layout);
            if (toolbar != null) {
                toolbar.setTitle(getScreenTitle() == 0 ? activity.getTitle() : getString(getScreenTitle()));
            }
        }
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(prefListener);
        super.onPause();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof TimePreference) {
            DialogFragment dialogFragment = new TimePreferenceDialogFragmentCompat();
            Bundle bundle = new Bundle(1);
            bundle.putString("key", preference.getKey());
            dialogFragment.setArguments(bundle);
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    protected boolean checkAndRequestBTPermissions() {

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

    public static class NotifyPolicyPermissionsDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            Context context = getContext();
            builder.setMessage(context.getString(R.string.permission_notification_policy_access,
                            getContext().getString(R.string.app_name),
                            getContext().getString(R.string.ok)))
                    .setPositiveButton(R.string.ok, (dialog, id) -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                        }
                    });
            return builder.create();
        }
    }
}
