package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import android.graphics.Color;

import com.google.gson.annotations.SerializedName;

public class TextSettings {

    @SerializedName("font_size")
    public Float fontSize = 10f;
    @SerializedName("text_scale")
    public Float textScale = 1f;

    @SerializedName("font_family")
    public String fontFamily = "sans-serif";

    @SerializedName("text_style")
    public String textStyle = "normal";

    @SerializedName("stroke_width")
    public Float strokeWidth = 1f;
    @SerializedName("text_align")
    public String textAlign = "left";
    private String color = "#FFFFFF";

    public int getColor() {
        if (color == null) {
            return Color.parseColor("#FFFFFF");
        }
        return Color.parseColor(color);
    }
}
