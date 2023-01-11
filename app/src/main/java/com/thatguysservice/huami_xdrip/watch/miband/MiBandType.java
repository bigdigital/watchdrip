package com.thatguysservice.huami_xdrip.watch.miband;

import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.utils.Version;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.AuthOperations;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.AuthOperations2021;

public enum MiBandType {
    MI_BAND2(Const.MIBAND_NAME_2),
    MI_BAND3(Const.MIBAND_NAME_3),
    MI_BAND3_1(Const.MIBAND_NAME_3_1),
    MI_BAND4(Const.MIBAND_NAME_4),
    MI_BAND5(Const.MIBAND_NAME_5),
    MI_BAND6(Const.MIBAND_NAME_6),
    AMAZFIT5(Const.AMAZFIT5_NAME),
    AMAZFITGTR(Const.AMAZFITGTR_NAME),
    AMAZFITGTR_42(Const.AMAZFITGTR_42_NAME),
    AMAZFITGTS(Const.AMAZFITGTS_NAME),
    AMAZFITGTS2_MINI(Const.AMAZFITGTS2_MINI_NAME),
    AMAZFITGTR_LITE(Const.AMAZFITGTR_LITE_NAME),
    AMAZFITGTS2E(Const.AMAZFITGTS2E_NAME),
    AMAZFITGTR2E(Const.AMAZFITGTR2E_NAME),
    AMAZFITGTR2(Const.AMAZFITGTR2_NAME),
    AMAZFITGTS2(Const.AMAZFITGTS2_NAME),
    AMAZFIT_TREX(Const.AMAZFIT_TREX),
    AMAZFIT_TREX_PRO(Const.AMAZFIT_TREX_PRO),
    ZEPP_E(Const.ZEPP_E_NAME),
    AMAZFITBIP(Const.AMAZFITBIP_NAME),
    AMAZFITBIP_LITE(Const.AMAZFITBIP_LITE_NAME),
    AMAZFITBIPS(Const.AMAZFITBIPS_NAME),
    AMAZFITBIPS_LITE(Const.AMAZFITBIPS_LITE_NAME),
    AMAZFITBIPU(Const.AMAZFITBIPU_NAME),
    UNKNOWN("");

    private final String text;

    /**
     * @param text
     */
    MiBandType(final String text) {
        this.text = text;
    }

    public static MiBandType fromString(String text) {
        for (MiBandType b : MiBandType.values()) {
            if (b.text.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return UNKNOWN;
    }

    public static boolean supportDateFormat(MiBandType bandType) {
        return !MiBandType.isVerge(bandType);
    }

    public static boolean supportGraph(MiBandType bandType) {
        return bandType == MiBandType.MI_BAND4 ||
                bandType == MiBandType.MI_BAND5 ||
                bandType == MiBandType.MI_BAND6 ||
                bandType == MiBandType.AMAZFIT5 ||
                MiBandType.isVerge(bandType) ||
                MiBandType.isBip(bandType) ||
                MiBandType.isBipS(bandType) ||
                bandType == MiBandType.AMAZFITBIPU;
    }

    public static boolean supportNightMode(MiBandType bandType) {
        return bandType == MiBandType.MI_BAND3 ||
                bandType == MiBandType.MI_BAND3_1 ||
                bandType == MiBandType.MI_BAND4 ||
                bandType == MiBandType.MI_BAND5 ||
                bandType == MiBandType.MI_BAND6 ||
                bandType == MiBandType.AMAZFIT5 ||
                MiBandType.isVerge(bandType) ||
                MiBandType.isBip(bandType) ||
                MiBandType.isBipS(bandType) ||
                bandType == MiBandType.AMAZFITBIPU;
    }

    public static boolean useAlternativeAuthFlag(MiBandType bandType) {
        return bandType == MiBandType.MI_BAND3 ||
                bandType == MiBandType.MI_BAND3_1 ||
                bandType == MiBandType.MI_BAND4 ||
                bandType == MiBandType.MI_BAND5 ||
                bandType == MiBandType.MI_BAND6 ||
                bandType == MiBandType.AMAZFIT5 ||
                MiBandType.isVerge(bandType) ||
                bandType == MiBandType.AMAZFITBIP_LITE ||
                MiBandType.isBipS(bandType) ||
                bandType == MiBandType.AMAZFITBIPU;
    }

    public static boolean useAlternativeCryptFlag(MiBandType bandType) {
        return bandType == MiBandType.MI_BAND4 ||
                bandType == MiBandType.MI_BAND5 ||
                bandType == MiBandType.MI_BAND6 ||
                bandType == MiBandType.AMAZFIT5 ||
                MiBandType.isVerge(bandType) ||
                bandType == MiBandType.AMAZFITBIP_LITE ||
                MiBandType.isBipS(bandType) ||
                bandType == MiBandType.AMAZFITBIPU;
    }

    public static boolean supportPairingKey(MiBandType bandType) {
        return bandType == MiBandType.MI_BAND4 ||
                bandType == MiBandType.MI_BAND5 ||
                bandType == MiBandType.MI_BAND6 ||
                bandType == MiBandType.AMAZFIT5 ||
                MiBandType.isVerge(bandType) ||
                MiBandType.isBip(bandType) ||
                MiBandType.isBipS(bandType) ||
                bandType == MiBandType.AMAZFITBIPU;
    }

    public static boolean isVerge(MiBandType bandType) {
        return MiBandType.isVerge1(bandType) || MiBandType.isVerge2(bandType);
    }

    public static boolean enableCompression(MiBandType bandType) {
        return bandType != MiBandType.AMAZFITGTS2_MINI && MiBandType.isVerge(bandType);
    }

    public static boolean isVerge1(MiBandType bandType) {
        return bandType == MiBandType.AMAZFITGTR ||
                bandType == MiBandType.AMAZFITGTR_42 ||
                bandType == MiBandType.AMAZFITGTS ||
                bandType == MiBandType.AMAZFIT_TREX_PRO ||
                bandType == MiBandType.AMAZFITGTS2_MINI ||
                bandType == MiBandType.AMAZFITGTR_LITE ||
                bandType == MiBandType.ZEPP_E;
    }

    public static boolean isVerge2(MiBandType bandType) {
        return bandType == MiBandType.AMAZFITGTR2 ||
                bandType == MiBandType.AMAZFITGTR2E ||
                bandType == MiBandType.AMAZFITGTS2 ||
                bandType == MiBandType.AMAZFITGTS2E;
    }

    public static boolean isBip(MiBandType bandType) {
        return bandType == MiBandType.AMAZFITBIP ||
                bandType == MiBandType.AMAZFITBIP_LITE;
    }

    public static boolean isBipS(MiBandType bandType) {
        return bandType == MiBandType.AMAZFITBIPS ||
                bandType == MiBandType.AMAZFITBIPS_LITE;
    }

    public static AuthOperations getAuthOperations(MiBandType bandType, MiBandService service) {
        String versionString = MiBand.getVersion();
        try {
            Version version = new Version(versionString);
            if (bandType == MiBandType.MI_BAND6) {
                if ((version.compareTo(new Version("1.0.4.1")) >= 0)) {
                    return new AuthOperations2021(bandType, service);
                }
            }
        } catch (IllegalArgumentException e) {
            UserError.Log.e("MiBandService", e + "versionString : " + versionString );
        }
        return new AuthOperations(bandType, service);
    }


    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return text;
    }

    public static String getModelPrefix(MiBandType bandType){
        String filePrefix = "";
        if (bandType == MiBandType.MI_BAND4) {
            filePrefix = "miband4";
        } else if (bandType == MiBandType.MI_BAND5 || bandType == MiBandType.AMAZFIT5) {
            filePrefix = "miband5";
        } else if (bandType == MiBandType.MI_BAND6) {
            filePrefix = "miband6";
        } else if (bandType == MiBandType.AMAZFITGTR || bandType == MiBandType.AMAZFITGTR_LITE) {
            filePrefix = "amazfit_gtr";
        } else if (bandType == MiBandType.AMAZFITGTR_42) {
            filePrefix = "amazfit_gtr42";
        } else if (MiBandType.isBip(bandType)) {
            filePrefix = "bip";
        } else if (MiBandType.isBipS(bandType)) {
            filePrefix = "bip_s";
        } else if ((bandType == MiBandType.AMAZFITGTR2 || bandType == MiBandType.AMAZFITGTR2E)) {
            filePrefix = "amazfit_gtr2";
        } else if ((bandType == MiBandType.AMAZFITGTS2 || bandType == MiBandType.AMAZFITGTS2E)) {
            filePrefix = "amazfit_gts2";
        } else if (bandType == MiBandType.AMAZFITGTS2_MINI) {
            filePrefix = "amazfit_gts2_mini";
        } else if (bandType == MiBandType.AMAZFIT_TREX_PRO) {
            filePrefix = "amazfit_trex_pro";
        }
        return filePrefix;
    }
}
