package com.thatguysservice.huami_xdrip.watch.miband;

import com.thatguysservice.huami_xdrip.models.PersistentStore;
import com.thatguysservice.huami_xdrip.models.Pref;

import static com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry.PREF_MIBAND_AUTH_KEY;
import static com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry.PREF_MIBAND_MAC;

/**
 * bigdigital
 * <p>
 * huami devices main logic class
 */

public class MiBand {

    private static final String PREF_MIBAND_AUTH_MAC = "miband_auth_mac";
    private static final String PREF_MIBAND_PERSISTANT_AUTH_KEY = "miband_persist_authkey";
    private static final String PREF_MIBAND_MODEL = "miband_model_";
    private static final String PREF_MIBAND_VERSION = "miband_version_";

    public static MiBandType getMibandType() {
        return MiBandType.fromString(getModel());
    }

    public static boolean isAuthenticated() {
        return  MiBand.getPersistentAuthMac().isEmpty() ? false : true;
    }

    public static String getMacPref() {
        return Pref.getString(PREF_MIBAND_MAC, "");
    }

    static void setMacPref(final String mac) {
        Pref.setString(PREF_MIBAND_MAC, mac);
    }

    public static String getAuthKeyPref() {
        return Pref.getString(PREF_MIBAND_AUTH_KEY, "");
    }

    public static void setAuthKeyPref(final String key) {
        Pref.setString(PREF_MIBAND_AUTH_KEY, key.toLowerCase());
    }

    public static String getPersistentAuthMac() {
        return PersistentStore.getString(PREF_MIBAND_AUTH_MAC);
    }

    public static void setPersistentAuthMac(final String mac) {
        if (mac.isEmpty()) {
            String authMac = getPersistentAuthMac();
            setVersion("", authMac);
            setModel("", authMac);
            setPersistentAuthKey("", authMac);
            PersistentStore.removeItem(PREF_MIBAND_AUTH_MAC);
            return;
        }
        PersistentStore.setString(PREF_MIBAND_AUTH_MAC, mac);
    }


    public static String getPersistentAuthKey() {
        final String mac = getPersistentAuthMac();
        if (!mac.isEmpty()) {
            return PersistentStore.getString(PREF_MIBAND_PERSISTANT_AUTH_KEY + mac);
        }
        return "";
    }

    public static void setPersistentAuthKey(final String key, String mac) {
        if (key.isEmpty()) {
            PersistentStore.removeItem(PREF_MIBAND_PERSISTANT_AUTH_KEY + mac);
            return;
        }
        if (!mac.isEmpty()) {
            PersistentStore.setString(PREF_MIBAND_PERSISTANT_AUTH_KEY + mac, key.toLowerCase());
        }
    }

    public static String getModel() {
        final String mac = getMacPref();
        if (!mac.isEmpty()) {
            return PersistentStore.getString(PREF_MIBAND_MODEL + mac);
        }
        return "";
    }

    public static void setModel(final String model, String mac) {
        if (mac.isEmpty()) mac = getMacPref();
        if (model.isEmpty()) {
            PersistentStore.removeItem(PREF_MIBAND_MODEL + mac);
            return;
        }
        if (!mac.isEmpty()) {
            PersistentStore.setString(PREF_MIBAND_MODEL + mac, model);
        }
    }

    public static String getVersion() {
        final String mac = getMacPref();
        if (!mac.isEmpty()) {
            return PersistentStore.getString(PREF_MIBAND_VERSION + mac);
        }
        return "";
    }

    public static void setVersion(final String version, String mac) {
        if (mac.isEmpty()) mac = getMacPref();
        if (version.isEmpty()) {
            PersistentStore.removeItem(PREF_MIBAND_MODEL + mac);
            return;
        }
        if (!mac.isEmpty()) {
            PersistentStore.setString(PREF_MIBAND_VERSION + mac, version.trim());
        }
    }
}
