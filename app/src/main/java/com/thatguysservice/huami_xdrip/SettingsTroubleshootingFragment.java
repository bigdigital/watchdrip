package com.thatguysservice.huami_xdrip;

import android.content.Intent;
import android.os.Bundle;

/**
 * Error log switch and the feedback screen.
 */
public class SettingsTroubleshootingFragment extends BaseSettingsFragment {

    @Override
    protected int getScreenTitle() {
        return R.string.screen_troubleshooting;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_troubleshooting, rootKey);
        findPreference("troubleshooting_feedback").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireContext(), SendFeedBackActiviy.class));
            return true;
        });
    }

    @Override
    protected void onPreferenceChanged(String key) {
        // The feedback entry of the toolbar menu follows the logging switch
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }
}
