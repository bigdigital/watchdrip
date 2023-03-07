package com.thatguysservice.huami_xdrip.models.webservice;

import com.eveningoutpost.dexdrip.services.broadcastservice.models.GraphLine;
import com.eveningoutpost.dexdrip.services.broadcastservice.models.GraphPoint;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class WebServiceGraphLine {
    @Getter
    private List<long[]> points;
    @Getter
    @Setter
    private String color;
    private String name;

    public WebServiceGraphLine(String name , GraphLine line) {
        points = new ArrayList<>();
        long pointVal[];
        for (GraphPoint point : line.getValues()) {
            pointVal = new long[2];
            pointVal[0] = (long)point.getX();
            pointVal[1] = (long)point.getY();
            points.add(pointVal);
        }
        this.name = name;
        color = String.format("0x%06X", (0xFFFFFF & line.getColor()));
    }
}
