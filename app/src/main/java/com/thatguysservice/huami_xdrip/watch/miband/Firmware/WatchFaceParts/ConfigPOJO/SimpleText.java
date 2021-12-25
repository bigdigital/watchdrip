package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import com.google.gson.annotations.SerializedName;

public class SimpleText {
    @SerializedName("text_settings")
    public TextSettings textSettings = new TextSettings();

    public Position position = new Position();

    @SerializedName("text_pattern")
    public String textPattern = "$value at $time";

    @SerializedName("outdated_text_pattern")
    public String outdatedTextPattern = "$value $time ago";
}
