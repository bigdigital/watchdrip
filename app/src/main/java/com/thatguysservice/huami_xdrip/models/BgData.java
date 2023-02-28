package com.thatguysservice.huami_xdrip.models;

import android.os.Bundle;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import com.thatguysservice.huami_xdrip.UtilityModels.BgGraphBuilder;

import static com.thatguysservice.huami_xdrip.UtilityModels.BgGraphBuilder.TREND_ARROW_VALUES.getSlope;
import static com.thatguysservice.huami_xdrip.UtilityModels.BgGraphBuilder.TREND_ARROW_VALUES.getTrend;

public class BgData extends BaseObservable {
    private final String deltaName;
    private final boolean isBgHigh;
    private final boolean isBgLow;
    private boolean noBgData = false;
    private final double valueMgdl;
    private final double deltaMgdl;
    private final long timeStamp;
    private final boolean isStale;
    private final boolean doMgdl;
    public BgData(Bundle bundle) {
        valueMgdl = bundle.getDouble("bg.valueMgdl", -1000);
        if (valueMgdl == -1000) {
            noBgData = true;
        }
        deltaMgdl = bundle.getDouble("bg.deltaValueMgdl", 0);

        timeStamp = bundle.getLong("bg.timeStamp", -1);
        isStale = bundle.getBoolean("bg.isStale", false);
        doMgdl = bundle.getBoolean("doMgdl", true);
        deltaName = bundle.getString("bg.deltaName");

        isBgHigh = bundle.getBoolean("bg.isHigh", false);
        isBgLow = bundle.getBoolean("bg.isLow", false);
    }

    public boolean isNoBgData() {
        return noBgData;
    }

    public boolean isBgHigh() {
        return isBgHigh;
    }

    public boolean isBgLow() {
        return isBgLow;
    }

    public boolean isDoMgdl() {
        return doMgdl;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public boolean isStale() {
        return isStale;
    }

    public double getValueMgdl() {
        return valueMgdl;
    }

    public double getDeltaMgdl() {
        return deltaMgdl;
    }
 //   @Bindable
    public String unitizedDelta() {
        return BgGraphBuilder.unitizedDeltaStringRaw(false, true, deltaMgdl, doMgdl);
    }
   // @Bindable
    public String unitizedBgValue() {
        return BgGraphBuilder.unitized_string(valueMgdl, doMgdl).replace(',', '.');
    }
  //  @Bindable
    public String getSlopeArrow(){
        try {
            double slope = getSlope(deltaName);
            return getTrend(slope).Symbol();
        }catch (IllegalArgumentException e){
            return BgGraphBuilder.TREND_ARROW_VALUES.NOT_COMPUTABLE.Symbol();
        }
    }

    public String getDeltaName() {
        return deltaName;
    }
}
