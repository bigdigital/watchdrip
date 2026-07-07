package com.thatguysservice.huami_xdrip.models.webservice;

import com.thatguysservice.huami_xdrip.models.BgData;

public class WebServiceBgInfo {
    public String tir;
    public String tirLow;
    public String tirHigh;

    public String val;
    public String delta;
    public String trend;
    public boolean isHigh;
    public boolean isLow;
    public long time;
    public boolean isStale;

    public WebServiceBgInfo(BgData bgData,String lowRange,String inRange,String highRange) {
      this.tir = inRange;
      this.tirLow = lowRange;
      this.tirHigh = highRange;
      this.val = bgData.unitizedBgValue();
      this.delta = bgData.unitizedDelta();
      this.trend = bgData.getDeltaName();
      this.isHigh = bgData.isBgHigh();
      this.isLow = bgData.isBgLow();
      this.time = bgData.getTimeStamp();
      this.isStale = bgData.isStale();
    }
}
