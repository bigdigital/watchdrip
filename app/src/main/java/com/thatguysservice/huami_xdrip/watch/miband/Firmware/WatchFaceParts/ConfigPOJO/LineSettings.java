package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import android.graphics.Color;

public class LineSettings {
    public boolean display = true;
    public Integer point_radius;
    public Integer line_thickness;
    public Integer text_size;
    public Boolean has_lines;
    public Boolean has_point;
    //public ValueShape shape; //todo fix
    public Boolean has_labels;
    public Boolean is_filled;
    public Integer area_transparency;
    private String color;


    public Integer getColor() {
        if (color == null) {
            return null;
        }
        return Color.parseColor(color);
    } 
}


