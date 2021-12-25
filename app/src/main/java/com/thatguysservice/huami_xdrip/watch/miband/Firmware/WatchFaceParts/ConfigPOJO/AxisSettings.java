package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import android.graphics.Color;

public class AxisSettings {
    public boolean has_lines = true;
    public boolean is_inside = true;
    public int text_size = 10;
    public float text_angle;
    private String line_color = "#FFFFFF";
    private String text_color = "#FFFFFF";

    public Integer getLineColor() {
        if (line_color == null) {
            return Color.parseColor("#FFFFFF");
        }
        return Color.parseColor(line_color);
    }

    public Integer getTextColor() {
        if (text_color == null) {
            return Color.parseColor("#FFFFFF");
        }
        return Color.parseColor(text_color);
    }
}
