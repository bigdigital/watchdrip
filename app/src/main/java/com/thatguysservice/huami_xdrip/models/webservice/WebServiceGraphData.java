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
        boolean doMgdl = graphComponent.doMgdl;
        addLine("high", graphComponent.getHighValues(), doMgdl);
        addLine("inRange", graphComponent.getInRangeValues(), doMgdl);
        addLine("low", graphComponent.getLowValues(), doMgdl);
        addLine("predict", graphComponent.getPredictedBgValues(), doMgdl);
        addLine("lineLow", graphComponent.getLowLineValues(), doMgdl);
        addLine("lineHigh", graphComponent.getHighLineValues(), doMgdl);
        addLine("treatment", graphComponent.getTreatmentValues(), doMgdl);
        start = graphComponent.getStart();
        end = graphComponent.getEnd();
        fuzzer = graphComponent.getFuzzer();
    }

    private void addLine(String name, GraphLine line, boolean doMgdl) {
        if (line.getValues().size() != 0) {
            lines.add(new WebServiceGraphLine(name, line, doMgdl));
        }
    }
}
