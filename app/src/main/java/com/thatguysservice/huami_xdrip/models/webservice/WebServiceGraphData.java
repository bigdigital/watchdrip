package com.thatguysservice.huami_xdrip.models.webservice;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.services.broadcastservice.models.GraphLine;
import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.UtilityModels.BgGraphCompontens;

import java.util.ArrayList;
import java.util.List;

public class WebServiceGraphData {
    private List<WebServiceGraphLine> lines = null;
    private long start;
    private long end;
    private long fuzzer;

    public WebServiceGraphData(Bundle bundle) {
        lines = new ArrayList<>();
        BgGraphCompontens graphComponent = new BgGraphCompontens(bundle, HuamiXdrip.getAppContext());
        addLine("high", graphComponent.getHighValues());
        addLine("inRange", graphComponent.getInRangeValues());
        addLine("low", graphComponent.getLowValues());
        addLine("predict", graphComponent.getPredictedBgValues());
        addLine("lineLow", graphComponent.getLowLineValues());
        addLine("lineHigh", graphComponent.getHighLineValues());
        addLine("treatment", graphComponent.getTreatmentValues());
        start = graphComponent.getStart();
        end = graphComponent.getEnd();
        fuzzer = graphComponent.getFuzzer();
    }

    private void addLine(String name, GraphLine line){
        if (line.getValues().size() != 0 ) {
            lines.add(new WebServiceGraphLine(name, line));
        }
    }
}
