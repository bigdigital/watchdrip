package com.thatguysservice.huami_xdrip;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.services.BroadcastService;
import com.thatguysservice.huami_xdrip.services.TelinkService;
import com.thatguysservice.huami_xdrip.services.telink.TelinkThermometers;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

/**
 * Telink thermometers running the pvvx custom firmware. Thermometers are added, renamed and
 * removed from the toolbar menu like the watches; all of them show the data.
 */
public class SettingsTelinkFragment extends BaseSettingsFragment {
    private ListPreference selectedPref;
    private EditTextPreference macPref;

    @Override
    protected int getScreenTitle() {
        return R.string.screen_telink;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_telink, rootKey);
        bindEnableSwitch(MiBandEntry.PREF_ENABLE_TELINK_SERVICE, true);

        selectedPref = findPreference("telink_selected_list");
        selectedPref.setOnPreferenceChangeListener((preference, newValue) -> {
            MiBandEntry.setTelinkSelected(Integer.parseInt((String) newValue));
            updateThermometers();
            return false; // shown by updateThermometers()
        });

        macPref = findPreference("telink_selected_mac");
        macPref.setOnPreferenceChangeListener((preference, newValue) -> {
            final String text = ((String) newValue).trim();
            final String mac = text.isEmpty() ? "" : TelinkService.normalizeMac(text);
            if (!mac.isEmpty() && !TelinkService.parseMacs(mac).contains(mac)) {
                Toast.makeText(getContext(), R.string.telink_invalid_mac, Toast.LENGTH_SHORT).show();
                return false;
            }
            final TelinkThermometers thermometers = TelinkThermometers.load();
            thermometers.setMac(selectedIndex(thermometers), mac);
            thermometers.save();
            updateThermometers();
            return false; // stored in the list, not as a preference
        });

        // The new value is passed along: the preference is only stored after this listener returns
        findPreference(MiBandEntry.PREF_TELINK_ALARM_BEEP).setOnPreferenceChangeListener((preference, newValue) -> {
            if (TelinkService.shouldServiceRun()) {
                Helper.startService(TelinkService.class, BroadcastService.INTENT_FUNCTION_KEY, TelinkService.CMD_ALARM_BEEP_CHANGED,
                        TelinkService.EXTRA_ALARM_BEEP, String.valueOf(newValue));
            }
            return true;
        });

        findPreference("telink_snooze").setOnPreferenceClickListener(preference -> {
            if (TelinkService.shouldServiceRun()) {
                Helper.startService(TelinkService.class, BroadcastService.INTENT_FUNCTION_KEY, TelinkService.CMD_SNOOZE,
                        TelinkService.EXTRA_SNOOZE_ON, String.valueOf(!TelinkService.isSnoozed()));
            }
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateThermometers();
        updateSnoozeSummary();
    }

    @Override
    protected void onPreferenceChanged(String key) {
        if (MiBandEntry.PREF_TELINK_SNOOZE_UNTIL.equals(key)) {
            updateSnoozeSummary();
        }
    }

    private static int selectedIndex(TelinkThermometers thermometers) {
        return Math.max(0, Math.min(MiBandEntry.getTelinkSelected(), thermometers.count() - 1));
    }

    private void updateThermometers() {
        final TelinkThermometers thermometers = TelinkThermometers.load();
        final int count = thermometers.count();
        final int selected = selectedIndex(thermometers);
        final CharSequence[] names = new CharSequence[count];
        final CharSequence[] values = new CharSequence[count];
        for (int i = 0; i < count; i++) {
            names[i] = thermometers.get(i).getName();
            values[i] = String.valueOf(i);
        }
        selectedPref.setEntries(names);
        selectedPref.setEntryValues(values);
        selectedPref.setVisible(count > 1);
        if (count > 0) {
            selectedPref.setValue(String.valueOf(selected));
            selectedPref.setTitle(getString(R.string.title_telink_selected) + " (" + names[selected] + ")");
        }

        final TelinkThermometers.Thermometer thermometer = thermometers.get(selected);
        macPref.setEnabled(thermometer != null && MiBandEntry.isTelinkServiceEnabled());
        if (thermometer == null) {
            macPref.setText("");
            macPref.setSummary(R.string.summary_telink_no_thermometer);
        } else {
            macPref.setText(thermometer.getMac());
            macPref.setTitle(count > 1 ? getString(R.string.title_telink_mac) + " (" + thermometer.getName() + ")" : getString(R.string.title_telink_mac));
            macPref.setSummary(thermometer.getMac().isEmpty() ? getString(R.string.summary_telink_mac_not_set) : thermometer.getMac());
        }
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.telink_menu, menu);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        // the watch device entries don't apply here
        for (int id : new int[]{R.id.action_add_device, R.id.action_rename_device, R.id.action_remove_device}) {
            final MenuItem item = menu.findItem(id);
            if (item != null) item.setVisible(false);
        }
        final boolean any = TelinkThermometers.load().count() > 0;
        menu.findItem(R.id.action_rename_thermometer).setVisible(any);
        menu.findItem(R.id.action_remove_thermometer).setVisible(any);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        final TelinkThermometers thermometers = TelinkThermometers.load();
        final int selected = selectedIndex(thermometers);
        if (id == R.id.action_add_thermometer) {
            askName(getString(R.string.telink_default_name, thermometers.count() + 1), name -> {
                final TelinkThermometers list = TelinkThermometers.load();
                final int index = list.add(name);
                if (index >= 0) {
                    list.save();
                    MiBandEntry.setTelinkSelected(index);
                    updateThermometers();
                }
            });
            return true;
        } else if (id == R.id.action_rename_thermometer && thermometers.get(selected) != null) {
            askName(thermometers.get(selected).getName(), name -> {
                final TelinkThermometers list = TelinkThermometers.load();
                list.rename(selected, name);
                list.save();
                updateThermometers();
            });
            return true;
        } else if (id == R.id.action_remove_thermometer && thermometers.get(selected) != null) {
            Helper.show_ok_dialog(getActivity(), getString(R.string.action_remove_thermometer),
                    getString(R.string.confirm_remove_thermometer, thermometers.get(selected).getName()), () -> {
                        final TelinkThermometers list = TelinkThermometers.load();
                        list.remove(selected);
                        list.save();
                        MiBandEntry.setTelinkSelected(Math.max(0, selected - 1));
                        updateThermometers();
                    }, false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private interface NameListener {
        void onName(String name);
    }

    // The watches' name dialog
    private void askName(String initial, NameListener listener) {
        final EditDeviceNameDialogFragment dialog = EditDeviceNameDialogFragment.newInstance(initial);
        dialog.setDialogResultListener(result -> {
            final String name = result.trim();
            if (!name.isEmpty()) listener.onName(name);
        });
        dialog.show(getParentFragmentManager(), "EditThermometerNameFragment");
    }

    private void updateSnoozeSummary() {
        final Preference snooze = findPreference("telink_snooze");
        if (TelinkService.isSnoozed()) {
            snooze.setSummary(getString(R.string.summary_telink_snooze_on, Helper.hourMinuteString(MiBandEntry.getTelinkSnoozeUntil())));
        } else {
            snooze.setSummary(R.string.summary_telink_snooze_off);
        }
    }
}
