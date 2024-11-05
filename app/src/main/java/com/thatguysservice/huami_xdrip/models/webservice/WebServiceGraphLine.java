package com.thatguysservice.huami_xdrip.models.webservice;

import com.eveningoutpost.dexdrip.services.broadcastservice.models.GraphLine;
import com.eveningoutpost.dexdrip.services.broadcastservice.models.GraphPoint;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class WebServiceGraphLine {
    @Getter
    private List<Object[]> points;
    @Getter
    @Setter
    private String color;
    private String name;

    public WebServiceGraphLine(String name, GraphLine line, boolean doMgdl) {
        points = new ArrayList<>();
        Object pointVal[];
        for (GraphPoint point : line.getValues()) {
            pointVal = new Object[2];
            pointVal[0] = (long) point.getX();
            if (doMgdl) {
                pointVal[1] = (long) point.getY();
            } else {
                pointVal[1] = (float) (Math.floor(point.getY() * 10) / 10);
            }
            points.add(pointVal);
        }
        this.name = name;
        color = String.format("0x%06X", (0xFFFFFF & line.getColor()));
    }
}
