package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import android.graphics.Color;

public class LineSettings {
    public boolean display = true;
    public Integer point_radius;
    public Integer text_size;
    public Boolean has_lines;
    public ValueShape shape;
    public Boolean has_labels;
    private String color;

    public Integer getColor() {
        if (color == null) {
            return null;
        }
        return Color.parseColor(color);
    }

    public enum ValueShape {
        CIRCLE, SQUARE, DIAMOND
    }
}


