package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import android.graphics.Color;

import com.google.gson.annotations.SerializedName;


public class BgValueText extends SimpleText {
    @SerializedName("color_high")
    private String colorHigh = "#FFFFFF";
    @SerializedName("color_low")
    private String colorLow = "#FFFFFF";

    public int getColorHigh() {
        return Color.parseColor(colorHigh);
    }

    public int getColorLow() {
        return Color.parseColor(colorLow);
    }
}
