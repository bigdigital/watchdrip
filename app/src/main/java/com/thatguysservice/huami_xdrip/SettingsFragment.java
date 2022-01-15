package com.thatguysservice.huami_xdrip;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceFragmentCompat;

import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.services.BroadcastService;
import com.thatguysservice.huami_xdrip.utils.bt.LocationHelper;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        findPreference(MiBandEntry.PREF_MIBAND_UPDATE_BG).setOnPreferenceClickListener(preference -> {
            updateMiBandBG(preference.getContext());
            return true;
        });

        findPreference(MiBandEntry.PREF_MIBAND_ENABLED).setOnPreferenceChangeListener((preference, newValue) -> {
            if ((Boolean) newValue) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (boolean) newValue) {
                    LocationHelper.requestLocationForBluetooth((Activity) preference.getContext());
                }
                checkReadPermission(this.getActivity());
            }
            return true;
        });
    }

    private void updateMiBandBG(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(HuamiXdrip.getAppContext().getResources().getString(R.string.miband_bg_dialog_title));
        builder.setPositiveButton(HuamiXdrip.getAppContext().getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                BroadcastService.initialStartIfEnabled();
            }
        });

        builder.setNegativeButton(HuamiXdrip.getAppContext().getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public static void checkReadPermission(final Activity activity) {

        // TODO call log permission - especially for Android 9+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (HuamiXdrip.getAppContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        Constants.GET_PHONE_READ_PERMISSION);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(MiBandEntry.prefListener);

    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(MiBandEntry.prefListener);
        super.onPause();
    }
}
