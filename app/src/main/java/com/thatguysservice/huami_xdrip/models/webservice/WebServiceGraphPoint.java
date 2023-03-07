package com.thatguysservice.huami_xdrip.models.webservice;

import lombok.Getter;
import lombok.Setter;

public class WebServiceGraphPoint {
    @Getter
    @Setter
    private long x;

    @Getter
    @Setter
    private long y;

    public WebServiceGraphPoint(long x, long y) {
        this.x = x;
        this.y = y;
    }
}
