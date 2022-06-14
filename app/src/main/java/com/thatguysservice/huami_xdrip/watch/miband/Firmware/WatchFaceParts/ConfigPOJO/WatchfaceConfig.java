package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import com.google.gson.annotations.SerializedName;

public class WatchfaceConfig {
    @SerializedName("info")
    public Info info = new Info();

    @SerializedName("use_custom_arrows")
    public Boolean useCustomArrows = false;

    @SerializedName("graph")
    public GraphSettings graph;

    @SerializedName("arrow_position")
    public Position arrowPosition = new Position();

    @SerializedName("bg_value_text")
    public BgValueText bgValue = new BgValueText();

    @SerializedName("iob_text")
    public SimpleText iob = new SimpleText();

    @SerializedName("pump_reservoir_text")
    public SimpleText pumpReservoir = new SimpleText();

    @SerializedName("pump_battery_text")
    public SimpleText pumpBattery = new SimpleText();

    @SerializedName("delta_text")
    public SimpleText deltaText = new SimpleText();

    @SerializedName("delta_time_text")
    public SimpleText deltaTimeText = new SimpleText();

    @SerializedName("treatment_text")
    public SimpleText treatmentText = new SimpleText();

    @SerializedName("treatment_time_text")
    public SimpleText treatmentTimeText = new SimpleText();

    @SerializedName("no_readings_text")
    public SimpleText noReadingsText = new SimpleText();

    @SerializedName("no_readings_time_text")
    public SimpleText noReadingsTimeText = new SimpleText();

    @SerializedName("resource_to_replace")
    public int resourceToReplace = 0;

    @SerializedName("battery_level")
    public SimpleText batteryLevel;
}
