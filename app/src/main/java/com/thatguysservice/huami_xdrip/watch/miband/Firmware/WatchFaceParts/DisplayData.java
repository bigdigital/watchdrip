package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;

import com.thatguysservice.huami_xdrip.models.BgData;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.Position;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.SimpleText;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.TextSettings;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.WatchfaceConfig;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.thatguysservice.huami_xdrip.models.Helper.hourMinuteString;
import static com.thatguysservice.huami_xdrip.utils.FileUtils.getExternalDir;

public class DisplayData {
    private ValueTime unitizedDelta;
    private String bgValueUnitized;
    private Bitmap arrowBitmap;
    private boolean bgIsStale = false;
    private String pluginSource = "";
    private ValueTime treatment;

    private int graphLimit = 16;
    private boolean showTreatment = false;
    private String predictIoB = "";
    private String predictWpB = "";
    private String pumpIoB = "";
    private String pumpReservoir = "";
    private String pumpBattery = "";
    private Bundle bundle;
    private WatchfaceConfig config;
    private int batteryLevel;
    private BgData bgData;

    private AssetManager assetManager;

    public DisplayData(Bundle intent, BgData bgData, WatchfaceConfig config, AssetManager assetManager) {
        this.bundle = intent;
        this.bgData = bgData;
        this.config = config;
        this.assetManager = assetManager;
    }

    public static Builder newBuilder(Bundle intent, BgData bgData, WatchfaceConfig config, AssetManager assetManager) {
        return new DisplayData(intent, bgData, config, assetManager).new Builder();
    }

    public BgData getBgData() {
        return bgData;
    }

    public ValueTime getPumpReservoir() {
        return new ValueTime(pumpReservoir);
    }

    public ValueTime getPumpBattery() {
        return new ValueTime(pumpBattery);
    }

    public Bundle getBundle() {
        return bundle;
    }

    public ValueTime getBgValue() {
        return new ValueTime(bgValueUnitized);
    }

    public String getBgValueString() {
        return bgValueUnitized;
    }

    public ValueTime getPredictIoB() {
        return new ValueTime(predictIoB);
    }

    public ValueTime getPredictWpb() {
        return new ValueTime(predictWpB);
    }

    public ValueTime getPumpIoB() {
        return new ValueTime(pumpIoB);
    }

    public ValueTime getUnitized_delta() {
        return unitizedDelta;
    }

    public ValueTime getBatteryLevel() {
        return new ValueTime(String.valueOf(batteryLevel));
    }

    public ValueTime getNoReadings() {
        return new ValueTime(config.noReadingsText.textPattern, Helper.tsl(), false);
    }

    public ValueTime getTreatment() {
        return treatment;
    }

    public Bitmap getArrowBitmap() {
        return arrowBitmap;
    }

    public boolean isBgIsStale() {
        return bgIsStale;
    }

    public boolean isBgHigh() {
        return bgData.isBgHigh();
    }

    public boolean isBgLow() {
        return bgData.isBgLow();
    }

    public int getGraphLimit() {
        return graphLimit;
    }

    public boolean isShowTreatment() {
        return showTreatment;
    }

    public WatchfaceConfig getConfig() {
        return config;
    }

    public void drawFormattedTextOnCanvas(Canvas canvas, ValueTime val, SimpleText simpleText) {
        drawTextOnCanvas(canvas, getFormattedText(val, simpleText), simpleText.position, getTextPaint(simpleText.textSettings));
    }

    public void drawFormattedTextOnCanvas(Canvas canvas, ValueTime val, SimpleText simpleText, Paint paint) {
        drawTextOnCanvas(canvas, getFormattedText(val, simpleText), simpleText.position, paint);
    }

    //handle text alignment
    private void drawTextOnCanvas(Canvas canvas, String text, Position position, Paint paint) {
        if (text.isEmpty()) {
            return;
        }
        Paint.Align align = paint.getTextAlign();
        paint.setTextAlign(Paint.Align.LEFT);

        Rect bounds = new Rect();
        float xOffset = 0;
        switch (align) {
            case LEFT:
                canvas.drawText(text, position.x, position.y, paint);
                return;
            case CENTER:
                paint.getTextBounds(text, 0, text.length(), bounds);
                xOffset = (canvas.getWidth() / 2f - bounds.width() / 2f - bounds.left) + position.x;
                break;
            case RIGHT:
                paint.getTextBounds(text, 0, text.length(), bounds);
                xOffset = canvas.getWidth() - bounds.width() - position.x;
                break;
        }
        canvas.save();
        canvas.translate(xOffset, 0);
        canvas.rotate(position.rotate, 0, position.y);
        canvas.drawText(text, 0, position.y, paint);
        canvas.restore();
    }

    public Paint getTextPaint(TextSettings text) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        Typeface tf = null;
        if (text.fontFamily != null) {
            if (MiBandEntry.isNeedToUseCustomWatchface()) {
                final String dir = getExternalDir();
                final File fontFile = new File(dir + "/fonts/" + text.fontFamily);
                if (fontFile.exists()) {
                    try {
                        tf = Typeface.createFromFile(fontFile);
                    } catch (Exception e) {

                    }
                }
            }
            if (tf == null) {
                try {
                    String assetFontsDir = "fonts/";
                    tf = Typeface.createFromAsset(assetManager, assetFontsDir + text.fontFamily + ".ttf");
                } catch (Exception e) {

                }
            }
        }

        if (tf == null) {
            if (text.fontFamily != null && text.textStyle != null) {
                tf = Typeface.create(text.fontFamily, TextStyle.valueOf(text.textStyle.toUpperCase()).getValue());
            } else if (text.fontFamily == null && text.textStyle != null) {
                tf = Typeface.create(Typeface.DEFAULT, TextStyle.valueOf(text.textStyle.toUpperCase()).getValue());
            } else if (text.fontFamily != null && text.textStyle == null) {
                tf = paint.setTypeface(Typeface.create(text.fontFamily, Typeface.NORMAL));
            }
        }

        if (tf != null) {
            paint.setTypeface(tf);
        }

        if (text.textAlign != null) {
            paint.setTextAlign(Paint.Align.valueOf(text.textAlign.toUpperCase()));
        }

        paint.setColor(text.getColor());

        if (text.textScale != null) {
            paint.setTextScaleX(text.textScale);
        }
        if (text.fontSize != null) {
            paint.setTextSize(text.fontSize);
        }

        if (text.strokeWidth != null) {
            paint.setStrokeWidth(text.strokeWidth);
        }
        return paint;
    }

    public String getFormattedText(ValueTime val, SimpleText simpleText) {
        if (simpleText == null) {
            return "";
        } else if (!val.isOld() && simpleText.textPattern == null) {
            return "";
        } else if (val.isOld() && simpleText.outdatedTextPattern == null) {
            return "";
        }

        String text;
        if (val.isOld()) {
            text = simpleText.outdatedTextPattern;
        } else {
            text = simpleText.textPattern;
        }
        boolean handled = false;
        if (val.getValue() != null && !val.getValue().isEmpty()) {
            text = text.replaceAll("\\$value\\b", val.getValue());
            handled = true;
        }
        if (val.getTimestamp() != null ) {
            text = text.replaceAll("\\$time\\b", val.getTimestampText(false));
            text = text.replaceAll("\\$time_short\\b", val.getTimestampText(true));
            handled = true;
        }
        if (!handled) {
            return "";
        }
        return text;
    }

    public enum TextStyle {
        NORMAL(0),
        BOLD(1),
        ITALIC(2),
        BOLD_ITALIC(3);

        private final int value;

        TextStyle(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public class Builder {

        public Builder setShowTreatment(boolean showTreatment) {
            DisplayData.this.showTreatment = showTreatment;

            return this;
        }

        public Builder setGraphLimit(int graphLimit) {
            DisplayData.this.graphLimit = graphLimit;

            return this;
        }

        public Builder setBatteryLevel(int batteryLevel) {
            DisplayData.this.batteryLevel = batteryLevel;

            return this;
        }

        public Builder setPumpBattery(String pumpBattery) {
            DisplayData.this.pumpBattery = pumpBattery;
            return this;
        }

        public Builder setPumpReservoir(String pumpReservoir) {
            pumpReservoir = getEmptyIfNull(pumpReservoir);
            if (pumpReservoir.isEmpty()) {
                DisplayData.this.pumpReservoir = pumpReservoir;
            }
            pumpReservoir = pumpReservoir.replace(",", ".");
            pumpReservoir = pumpReservoir + "u".replace(".0u", "u");
            DisplayData.this.pumpReservoir = pumpReservoir;
            return this;
        }

        public Builder setPredictIoB(String iob) {
            iob = getEmptyIfNull(iob);
            if (iob.isEmpty()) {
                DisplayData.this.predictIoB = iob;
                return this;
            }
            iob = iob.replace(",", ".");
            iob = iob + "u".replace(".0u", "u");
            DisplayData.this.predictIoB = iob;
            return this;
        }

        public Builder setPredictWpB(String wpb) {
            wpb = getEmptyIfNull(wpb);
            if (wpb.isEmpty()) {
                DisplayData.this.predictWpB = wpb;
                return this;
            }
            wpb = wpb.replace(",", ".");
            wpb = wpb.replace("\u224F", "");
            wpb = wpb.replace("\u26A0", "!");
            DisplayData.this.predictWpB = wpb;
            return this;
        }

        public Builder setPumpIob(String iob) {
            iob = getEmptyIfNull(iob);
            if (iob.isEmpty()) {
                DisplayData.this.pumpIoB = iob;
                return this;
            }
            iob = iob.replace(",", ".");
            iob = iob + "u".replace(".0u", "u");
            DisplayData.this.pumpIoB = iob;
            return this;
        }

        private String getEmptyIfNull(String s) {
            return s == null ? "" : s;
        }

        private String getDouble(double v, int digits) {
            return v > -1 ? Helper.qs(v, digits) : "";
        }

        public DisplayData build() throws IllegalArgumentException, IOException {
            String arrowImageName;
            arrowImageName = getEmptyIfNull(bgData.getDeltaName()) + ".png";
            //fill bg
            bgValueUnitized = bgData.unitizedBgValue();
            long timeStampVal = bgData.getTimeStamp();
            bgIsStale = bgData.isStale();
            pluginSource = getEmptyIfNull(bundle.getString("bg.plugin"));

            InputStream arrowStream = null;
            if (config.useCustomArrows) {
                final String dir = getExternalDir();
                final File imageFile = new File(dir + "/arrows/" + arrowImageName);
                if (imageFile.exists()) {
                    arrowStream = new FileInputStream(imageFile);
                }
            }
            if (arrowStream == null) {
                arrowStream = assetManager.open("miband_watchface_parts/arrows/" + arrowImageName);
            }
            arrowBitmap = BitmapFactory.decodeStream(arrowStream);
            arrowStream.close();


            //fill delta
            boolean isOld = false;
            long since = Helper.msSince(timeStampVal);
            Long timeStamp;
            if (since < Constants.DAY_IN_MS) {
                timeStamp = timeStampVal;
            } else {
                isOld = true;
                timeStamp = timeStampVal;
            }
            unitizedDelta = new ValueTime(bgData.unitizedDelta(), timeStamp, isOld);

            setPredictIoB(bundle.getString("predict.IOB"));
            setPredictWpB(bundle.getString("predict.BWP"));

            String pumpJSON = bundle.getString("pumpJSON");
            JSONObject json = null;
            double pumpReservoir = -1;
            double pumpIob = -1;
            double pumpBattery = -1;
            try {
                json = new JSONObject(pumpJSON);
            } catch (JSONException e) {
            }
            try {
                pumpReservoir = json.getDouble("reservoir");
            } catch (JSONException e) {
            }
            try {
                pumpIob = json.getDouble("bolusiob");
            } catch (JSONException e) {
            }
            try {
                pumpBattery = (json.getDouble("battery"));
            } catch (JSONException e) {
            }

            setPumpIob(getDouble(pumpIob, 2));
            setPumpReservoir(String.valueOf(pumpReservoir));
            setPumpBattery(String.valueOf(pumpBattery));

            setBatteryLevel(bundle.getInt("phoneBattery"));

            double insulin = bundle.getDouble("treatment.insulin", -1);
            double carbs = bundle.getDouble("treatment.carbs", -1);
            timeStampVal = bundle.getLong("treatment.timeStamp", -1);

            timeStamp = null;
            isOld = false;
            String treatmentText = "";
            if (insulin > 0) {
                treatmentText = treatmentText + (Helper.qs(insulin, 2) + "u").replace(".0u", "u");
            } else if (carbs > 0) {
                treatmentText = treatmentText + (Helper.qs(carbs, 1) + "g").replace(".0g", "g");
            }

            if (treatmentText.length() > 0) {
                since = Helper.msSince(timeStampVal);
                if (since < Constants.HOUR_IN_MS * 6) {
                    timeStamp = timeStampVal;
                } else if (since < Constants.HOUR_IN_MS * 12) {
                    isOld = true;
                    timeStamp = timeStampVal;
                } else {
                    treatmentText = "";
                }
            }

            treatment = new ValueTime(treatmentText, timeStamp, isOld);

            return DisplayData.this;
        }
    }

    private class ValueTime {
        private String value;
        private Long timestamp = null;
        private boolean isOld = false;

        ValueTime(String value) {
            this.value = value;
        }

        ValueTime(String value, Long timestamp, boolean isOld) {
            this.value = value;
            this.timestamp = timestamp;
            this.isOld = isOld;
        }

        public String getValue() {
            return value;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public boolean isOld() {
            return isOld;
        }

        public String getTimestampText(boolean isShortText){
           String timeStampText = "";
           if (timestamp != null){
                if (isOld()){
                    long since = Helper.msSince(getTimestamp());
                    if (isShortText) {
                        timeStampText = Helper.niceTimeScalarShortText(since);
                    }
                    else{
                        timeStampText = Helper.niceTimeScalar(since);
                    }
                }else{
                    timeStampText = hourMinuteString(getTimestamp());
                }
           }

           return timeStampText;
        }
    }
}
