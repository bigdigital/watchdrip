package com.thatguysservice.huami_xdrip.models;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;

public class StatisticInfo extends BaseObservable {
    Bundle bundle;
    private String avg;
    private String a1c_dcct;
    private String a1c_ifcc;
    private String in;
    private String high;
    private String low;
    private String stdev;
    private String gvi;
    private String carbs;
    private String insulin;
    private String royce_ratio;
    private String capture_percentage;
    private String capture_realtime_capture_percentage;
    private String accuracy;
    private String steps;
    public StatisticInfo(Bundle bundle) {
        this.bundle = bundle;
        avg = getData("status.avg");
        a1c_dcct = getData("status.a1c_dcct");
        a1c_ifcc = getData("status.a1c_ifcc");
        in = getData("status.in");
        high = getData("status.high");
        low = getData("status.low");
        stdev = getData("status.stdev");
        gvi = getData("status.gvi");
        carbs = getData("status.carbs");
        insulin = getData("status.insulin");
        royce_ratio = getData("status.royce_ratio");
        capture_percentage = getData("status.capture_percentage");
        capture_realtime_capture_percentage = getData("status.capture_realtime_capture_percentage");
        accuracy = getData("status.accuracy");
        steps = getData("status.steps");

        this.bundle = null;
    }

    @NonNull
    @Override
    public String toString() {
            StringBuilder sb = new StringBuilder();
            append(sb, avg);
            append(sb, a1c_dcct);
            append(sb, a1c_ifcc);
            append(sb, in);
            append(sb, high);
            append(sb, low);
            append(sb, stdev);
            append(sb, gvi);
            append(sb, carbs);
            append(sb, insulin);
            append(sb, royce_ratio);
            append(sb, capture_percentage);
            append(sb, capture_realtime_capture_percentage);
            append(sb, accuracy);
            append(sb, steps);
        return sb.toString();
    }

    private static void append(final StringBuilder sb, final String value) {
        if (sb.length() != 0) sb.append(' ');
        sb.append(value);
    }

    public String getAvg() {
        return avg;
    }

    public String getA1c_dcct() {
        return a1c_dcct;
    }

    public String getA1c_ifcc() {
        return a1c_ifcc;
    }

    public String getIn() {
        return in;
    }

    public String getHigh() {
        return high;
    }

    public String getLow() {
        return low;
    }

    public String getStdev() {
        return stdev;
    }

    public String getGvi() {
        return gvi;
    }

    public String getCarbs() {
        return carbs;
    }

    public String getInsulin() {
        return insulin;
    }

    public String getRoyce_ratio() {
        return royce_ratio;
    }

    public String getCapture_percentage() {
        return capture_percentage;
    }

    public String getCapture_realtime_capture_percentage() {
        return capture_realtime_capture_percentage;
    }

    public String getAccuracy() {
        return accuracy;
    }

    public String getSteps() {
        return steps;
    }

    private String getData(String s) {
        String res = bundle.getString(s);
        return res == null ? "" : res;
    }
}
