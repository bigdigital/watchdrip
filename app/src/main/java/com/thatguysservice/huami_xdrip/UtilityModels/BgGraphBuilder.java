package com.thatguysservice.huami_xdrip.UtilityModels;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.view.View;

import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.JoH;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.AxisSettings;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.GraphSettings;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.LineSettings;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BgGraphBuilder {
    private Context mContext;

    protected static final String TAG = "BgGraphBuilder";
    protected int width;
    protected int height;
    protected BgGraphCompontens bgGraphCompontens;
    protected LineChartView chart;

    protected boolean showLowLine = false;
    protected boolean showHighLine = false;
    protected boolean showAxes = false;
    protected boolean showTreatment = false;
    protected int graphLimit = 16;
    private GraphSettings graphSettings;
    protected boolean showFiltered = false;
    protected int backgroundColor = Color.TRANSPARENT;

    public static double unitized(double value, boolean doMgdl) {
        if (doMgdl) {
            return value;
        } else {
            return mmolConvert(value);
        }
    }

    public static double mmolConvert(double mgdl) {
        return mgdl * Constants.MGDL_TO_MMOLL;
    }

    public static String unitized_string(double value, boolean doMgdl) {
        final DecimalFormat df = new DecimalFormat("#");
        if (value >= 400) {
            return "HIGH";
        } else if (value >= 40) {
            if (doMgdl) {
                df.setMaximumFractionDigits(0);
                return df.format(value);
            } else {
                df.setMaximumFractionDigits(1);
                //next line ensures mmol/l value is XX.x always.  Required by PebbleWatchSync, and probably not a bad idea.
                df.setMinimumFractionDigits(1);
                return df.format(mmolConvert(value));
            }
        } else if (value > 12) {
            return "LOW";
        } else {
            switch ((int) value) {
                case 0:
                    return "??0";
                case 1:
                    return "?SN";
                case 2:
                    return "??2";
                case 3:
                    return "?NA";
                case 5:
                    return "?NC";
                case 6:
                    return "?CD";
                case 9:
                    return "?AD";
                case 12:
                    return "?RF";
                default:
                    return "???";
            }
        }
    }

    public static String unitizedDeltaStringRaw(boolean showUnit, boolean highGranularity, double value, boolean doMgdl) {
        if (Math.abs(value) > 100) {
            // a delta > 100 will not happen with real BG values -> problematic sensor data
            return "ERR";
        }

        // TODO: allow localization from os settings once pebble doesn't require english locale
        DecimalFormat df = new DecimalFormat("#", new DecimalFormatSymbols(Locale.ENGLISH));
        String delta_sign = "";
        if (value > 0) {
            delta_sign = "+";
        }
        if (doMgdl) {

            if (highGranularity) {
                df.setMaximumFractionDigits(1);
            } else {
                df.setMaximumFractionDigits(0);
            }

            return delta_sign + df.format(unitized(value, doMgdl)) + (showUnit ? " mg/dl" : "");
        } else {
            // only show 2 decimal places on mmol/l delta when less than 0.1 mmol/l
            if (highGranularity && (Math.abs(value) < (Constants.MMOLL_TO_MGDL * 0.1))) {
                df.setMaximumFractionDigits(2);
            } else {
                df.setMaximumFractionDigits(1);
            }

            df.setMinimumFractionDigits(1);
            df.setMinimumIntegerDigits(1);
            return delta_sign + df.format(unitized(value, doMgdl)) + (showUnit ? " mmol/l" : "");
        }
    }

    public BgGraphBuilder showTreatmentLine(boolean show) {
        this.showTreatment = show;
        return this;
    }

    public BgGraphBuilder setGraphLimit(int graphLimit) {
        this.graphLimit = graphLimit;
        return this;
    }

    public BgGraphBuilder setGraphSettings(GraphSettings graphSettings) {
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
            //axis.setTiltAngle(settings.text_angle); todo check this
        }
        return axis;
    }

    public BgGraphBuilder showHighLine(boolean show) {
        this.showHighLine = show;
        return this;
    }

    public BgGraphBuilder showHighLine() {
        return showHighLine(true);
    }

    public BgGraphBuilder showLowLine(boolean show) {
        this.showLowLine = show;
        return this;
    }

    public BgGraphBuilder showLowLine() {
        return showLowLine(true);
    }

    public BgGraphBuilder showAxes(boolean show) {
        this.showAxes = show;
        return this;
    }

    public BgGraphBuilder showAxes() {
        return showAxes(true);
    }

    public BgGraphBuilder setWidth(float width) {
        this.width = convertDpToPixel(width);
        return this;
    }

    public BgGraphBuilder setHeight(float height) {
        this.height = convertDpToPixel(height);
        return this;
    }

    public BgGraphBuilder setWidthPx(int width) {
        this.width = width;
        return this;
    }

    public BgGraphBuilder setHeightPx(int height) {
        this.height = height;
        return this;
    }

    public BgGraphBuilder setBackgroundColor(int color)
    {
        this.backgroundColor = color;
        return this;
    }

    public BgGraphBuilder(Context context) {
        mContext = context;
        chart = new LineChartView(mContext);
    }

    public BgGraphBuilder setBgGraphCompontens(BgGraphCompontens bgGraphCompontens) {
        this.bgGraphCompontens = bgGraphCompontens;
        return this;
    }

    /**
     * Draw the view into a bitmap.
     */
    protected Bitmap getViewBitmap(View v) {
        v.clearFocus();
        v.setPressed(false);

        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);

        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(Color.TRANSPARENT);

        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null) {
            android.util.Log.e(TAG, "failed getViewBitmap(" + JoH.backTrace() + ")", new RuntimeException());

            v.destroyDrawingCache(); // duplicate of below, flow could be reordered better
            v.setWillNotCacheDrawing(willNotCache);
            v.setDrawingCacheBackgroundColor(color);
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);

        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);

        return bitmap;
    }

    public Bitmap build() {
        List<Line> lines = new ArrayList<>();

        lines = applyLineSettings( bgGraphCompontens.lowLine().setFilled(false), lines, graphSettings.low_line );
        lines = applyLineSettings( bgGraphCompontens.highLine(), lines, graphSettings.high_line );

        lines = applyLineSettings(bgGraphCompontens.inRangeValuesLine().setPointRadius(1), lines, graphSettings.in_range_line );
        lines = applyLineSettings(bgGraphCompontens.lowValuesLine().setPointRadius(1), lines, graphSettings.low_val_line );
        lines = applyLineSettings(bgGraphCompontens.highValuesLine().setPointRadius(1), lines, graphSettings.high_val_line );

        lines = applyLineSettings(bgGraphCompontens.predictedBg(), lines, graphSettings.predictive_line ); // predictive
        lines = applyLineSettings(bgGraphCompontens.cobValues(), lines, graphSettings.cob_vals_line, true ); //cobValues

        lines = applyLineSettings(bgGraphCompontens.polyBg(), lines, graphSettings.poly_predictive_line); //poly predict

      /*  if (showFiltered) {
            for (Line line : bgGraphBuilder.filteredLines()) {
                line.setHasPoints(false);
                lines.add(line);
            }
        }*/

        if (showTreatment) {
            lines = applyLineSettings(bgGraphCompontens.iobValues(), lines, graphSettings.iob_line ); //insulin on board
            lines = applyLineSettings(bgGraphCompontens.bolusValues(), lines, graphSettings.bolus_line); //poly predict*/
        }

        LineChartData lineData = new LineChartData(lines);

        if (graphSettings.y_axis != null) {
            Axis yaxis = bgGraphCompontens.yAxis();
            yaxis = applyAxisSettings(yaxis, graphSettings.y_axis);
            lineData.setAxisYLeft(yaxis);
        }
        if (graphSettings.x_axis != null) {
            Axis xaxis = bgGraphCompontens.chartXAxis();
            xaxis = applyAxisSettings(xaxis, graphSettings.x_axis);
            lineData.setAxisXBottom(xaxis);
        }

        chart.setBackgroundColor(backgroundColor);
        chart.setLineChartData(lineData);
        Viewport viewport = chart.getMaximumViewport();
        viewport.left = bgGraphCompontens.getStart();
        viewport.right = bgGraphCompontens.getEnd();
        viewport.bottom = 0;
        viewport.top = (float) (bgGraphCompontens.doMgdl ? graphLimit * Constants.MMOLL_TO_MGDL : graphLimit);
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

    protected Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        if (bm==null) return null;
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    public static int convertDpToPixel(float dp){
        Resources resources = HuamiXdrip.getAppContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return (int) (dp * (metrics.densityDpi / 160f));
    }
}
