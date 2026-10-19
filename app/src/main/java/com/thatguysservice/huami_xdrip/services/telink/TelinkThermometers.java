package com.thatguysservice.huami_xdrip.services.telink;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.thatguysservice.huami_xdrip.models.Pref;
import com.thatguysservice.huami_xdrip.services.TelinkService;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Named thermometers, stored as JSON like the watches (PersistantDevices). All of them get the
 * data, the selected one is only the one the settings screen edits.
 */
public class TelinkThermometers {
    private static final int MAX_THERMOMETERS = 10;

    public static class Thermometer {
        private String name;
        private String mac;

        public Thermometer(String name, String mac) {
            this.name = name;
            this.mac = mac;
        }

        public String getName() {
            return name;
        }

        public String getMac() {
            return mac == null ? "" : mac;
        }
    }

    private List<Thermometer> thermometers = new ArrayList<>();

    public static TelinkThermometers load() {
        final String json = Pref.getString(MiBandEntry.PREF_TELINK_DEVICES, "");
        TelinkThermometers loaded = null;
        if (!json.isEmpty()) {
            try {
                loaded = new Gson().fromJson(json, TelinkThermometers.class);
            } catch (JsonSyntaxException e) {
                // start over
            }
        }
        if (loaded == null) {
            loaded = new TelinkThermometers();
        }
        if (loaded.thermometers == null) {
            loaded.thermometers = new ArrayList<>();
        }
        return loaded;
    }

    public void save() {
        Pref.setString(MiBandEntry.PREF_TELINK_DEVICES, new Gson().toJson(this));
    }

    public int count() {
        return thermometers.size();
    }

    public Thermometer get(int index) {
        return index >= 0 && index < count() ? thermometers.get(index) : null;
    }

    public List<Thermometer> getAll() {
        return thermometers;
    }

    // Index of the new thermometer, -1 when the list is full
    public int add(String name) {
        if (count() >= MAX_THERMOMETERS) return -1;
        thermometers.add(new Thermometer(name, ""));
        return count() - 1;
    }

    public void rename(int index, String name) {
        if (get(index) != null) thermometers.get(index).name = name;
    }

    public void setMac(int index, String mac) {
        if (get(index) != null) thermometers.get(index).mac = mac;
    }

    public void remove(int index) {
        if (get(index) != null) thermometers.remove(index);
    }

    // Valid, distinct addresses of all thermometers
    public List<String> macs() {
        final StringBuilder all = new StringBuilder();
        for (Thermometer thermometer : thermometers) {
            all.append(thermometer.getMac()).append(',');
        }
        return TelinkService.parseMacs(all.toString());
    }
}
