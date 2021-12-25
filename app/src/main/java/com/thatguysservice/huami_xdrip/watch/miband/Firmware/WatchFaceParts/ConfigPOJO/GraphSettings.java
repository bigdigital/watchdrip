package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import android.graphics.Color;

import com.google.gson.annotations.SerializedName;

public class GraphSettings {
    public Position position = new Position();
    public int width = 120;
    public int height = 80;
    public AxisSettings x_axis;
    public AxisSettings y_axis;
    public LineSettings low_line;
    public LineSettings high_line;
    public LineSettings in_range_line;
    public LineSettings low_val_line;
    public LineSettings high_val_line;
    public LineSettings predictive_line;
    public LineSettings bolus_line;

    @SerializedName("bg_color")
    private String bgColor = "#FFFFFF";

    public int getBgColor() {
        if (bgColor == null) {
            return Color.parseColor("#FFFFFF");
        }
        return Color.parseColor(bgColor);
    }
}
