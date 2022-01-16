package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Utils;

import android.content.Context;
import android.graphics.Bitmap;
public class BgMibandSparklineBuilder {}
/*import com.eveningoutpost.dexdrip.UtilityModels.BgSparklineBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.AxisSettings;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.GraphSettings;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.LineSettings;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.Viewport;

public class BgMibandSparklineBuilder extends BgSparklineBuilder {
    protected boolean showTreatment = false;
    protected int graphLimit = 16;
    private GraphSettings graphSettings;

    public BgMibandSparklineBuilder(Context context) {
        super(context);
    }


    public BgSparklineBuilder showTreatmentLine(boolean show) {
        this.showTreatment = show;
        return this;
    }

    public BgSparklineBuilder setGraphLimit(int graphLimit) {
        this.graphLimit = graphLimit;
        return this;
    }

    public BgSparklineBuilder setGraphSettings(GraphSettings graphSettings) {
        this.graphSettings = graphSettings;
        return this;
    }

    private List<Line> applyLineSettings(Line line,List<Line> lines, LineSettings settings, boolean onlyInitialized){
        if (onlyInitialized && settings == null){
            return lines;
        }
        return applyLineSettings(line, lines, settings);
    }

    private List<Line> applyLineSettings(Line line,List<Line> lines, LineSettings settings) {
        if (settings != null){
            if (!settings.display) {
                return lines;
            }
            Integer color = settings.getColor();
            if (color != null) {
                line.setColor(color);
            }
            if (settings.text_size != null) {
                line.setStrokeWidth(settings.text_size);
            }
            if (settings.point_radius != null) {
                line.setPointRadius(settings.point_radius);
            }
            if (settings.has_labels != null){
                line.setHasLabels(settings.has_labels);
            }
            if (settings.shape != null){
                line.setShape(settings.shape);
            }
            if (settings.has_lines != null){
                line.setHasLines(settings.has_lines);
            }
            if (settings.has_point != null){
                line.setHasPoints(settings.has_point);
            }
            if (settings.is_filled != null){
                line.setFilled(settings.is_filled);
            }
            if (settings.area_transparency != null){
                line.setAreaTransparency(settings.area_transparency);
            }
            if (settings.line_thickness != null){
                line.setStrokeWidth(settings.line_thickness);
            }
        }
        lines.add(line);
        return lines;
    }


    private Axis applyAxisSettings(Axis axis, AxisSettings settings) {
        if (settings != null){
            axis.setHasLines(settings.has_lines);
            axis.setLineColor(settings.getLineColor());
            axis.setTextSize(settings.text_size);
            axis.setTextColor(settings.getTextColor());
            axis.setInside(settings.is_inside);
            axis.setTiltAngle(settings.text_angle);
        }
        return axis;
    }

    public Bitmap build() {
        List<Line> lines = new ArrayList<>();
        bgGraphBuilder.defaultLines(false); // simple mode

        lines = applyLineSettings( bgGraphBuilder.lowLine().setFilled(false), lines, graphSettings.low_line );
        lines = applyLineSettings( bgGraphBuilder.highLine(), lines, graphSettings.high_line );

        lines = applyLineSettings(bgGraphBuilder.inRangeValuesLine().setPointRadius(1), lines, graphSettings.in_range_line );
        lines = applyLineSettings(bgGraphBuilder.lowValuesLine().setPointRadius(1), lines, graphSettings.low_val_line );
        lines = applyLineSettings(bgGraphBuilder.highValuesLine().setPointRadius(1), lines, graphSettings.high_val_line );

        Line[] treatments = bgGraphBuilder.treatmentValuesLine();

        lines = applyLineSettings(treatments[4], lines, graphSettings.annotations_line, true ); //annotations
        lines = applyLineSettings(treatments[5], lines, graphSettings.predictive_line ); // predictive
        lines = applyLineSettings(treatments[6], lines, graphSettings.cob_vals_line, true ); //cobValues

        treatments[7].setHasLines(true);
        treatments[7].setHasPoints(true);

        lines = applyLineSettings(treatments[7], lines, graphSettings.poly_predictive_line); //poly predict

        if (showFiltered) {
            for (Line line : bgGraphBuilder.filteredLines()) {
                line.setHasPoints(false);
                lines.add(line);
            }
        }

        if (showTreatment) {
            treatments[2].setStrokeWidth(1); //bolus line
            treatments[2].setHasPoints(false);
            lines = applyLineSettings(treatments[2], lines, graphSettings.iob_line ); //insulin on board
            treatments[1].setFilled(false); //bolus dots
            treatments[1].setHasLabels(false);
            treatments[1].setPointRadius(2);
            lines = applyLineSettings(treatments[1], lines, graphSettings.bolus_line); //poly predict
        }

        LineChartData lineData = new LineChartData(lines);

        if (graphSettings.y_axis != null) {
            Axis yaxis = bgGraphBuilder.yAxis();
            yaxis = applyAxisSettings(yaxis, graphSettings.y_axis);
            lineData.setAxisYLeft(yaxis);
        }
        if (graphSettings.x_axis != null) {
            Axis xaxis = bgGraphBuilder.chartXAxis();
            xaxis = applyAxisSettings(xaxis, graphSettings.x_axis);
            lineData.setAxisXBottom(xaxis);
        }

        chart.setBackgroundColor(backgroundColor);
        chart.setLineChartData(lineData);
        Viewport viewport = chart.getMaximumViewport();
        viewport.left = start;
        viewport.right = end;
        viewport.bottom = 0;
        viewport.top = (float) (bgGraphBuilder.doMgdl ? graphLimit * Constants.MMOLL_TO_MGDL : graphLimit);
        chart.setViewportCalculationEnabled(false);
        chart.setInteractive(false);
        chart.setCurrentViewport(viewport);
        chart.setPadding(0, 0, 0, 0);
        chart.setLeft(0);
        chart.setTop(0);
        chart.setRight(width * 2);
        chart.setBottom(height * 2);
        return getResizedBitmap(getViewBitmap(chart), width, height);
    }
}
*/
