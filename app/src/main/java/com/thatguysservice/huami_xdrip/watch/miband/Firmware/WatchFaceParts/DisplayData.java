package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;

import com.thatguysservice.huami_xdrip.UtilityModels.BgGraphBuilder;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.Position;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.SimpleText;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.TextSettings;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.WatchfaceConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.thatguysservice.huami_xdrip.models.Helper.hourMinuteString;
import static com.thatguysservice.huami_xdrip.utils.FileUtils.getExternalDir;

public class DisplayData {
    private Double bgDeltaMgdl;
    private ValueTime unitizedDelta;
    private Double bgValueMgdl;
    private String bgValueUnitized;
    private Bitmap arrowBitmap;
    private boolean bgIsStale = false;
    private String pluginSource = "";
    private boolean isBgHigh = false;
    private boolean isBgLow = false;
    private boolean doMgdl = true;
    private ValueTime treatment;

    private int graphLimit = 16;
    private boolean showTreatment = false;
    private String pumpIoB = "";
    private String pumpReservoir = "";
    private String pumpBattery = "";
    private Bundle bundle;
    private WatchfaceConfig config;
    private int batteryLevel;

    public DisplayData(Bundle intent, WatchfaceConfig config) {
        bundle = intent;
        this.config = config;
    }

    public static Builder newBuilder(Bundle intent, AssetManager assetManager, WatchfaceConfig config) {
        return new DisplayData(intent, config).new Builder(assetManager);
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
        return new ValueTime(config.noReadingsText.textPattern, hourMinuteString(Helper.tsl()), false);
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
        return isBgHigh;
    }

    public boolean isBgLow() {
        return isBgLow;
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

    //handle text alignment
    public void drawTextOnCanvas(Canvas canvas, String text, Position position, Paint paint) {
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
                paint.setTextAlign(align);
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
        paint.setTextAlign(align);
    }

    public Paint getTextPaint(TextSettings text) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        if (text.fontFamily != null && text.textStyle != null) {
            paint.setTypeface(Typeface.create(text.fontFamily, TextStyle.valueOf(text.textStyle.toUpperCase()).getValue()));
        } else if (text.fontFamily == null && text.textStyle != null) {
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, TextStyle.valueOf(text.textStyle.toUpperCase()).getValue()));
        } else if (text.fontFamily != null && text.textStyle == null) {
            paint.setTypeface(Typeface.create(text.fontFamily, Typeface.NORMAL));
        }

        //Typeface tf = Typeface.createFromFile("font.ttf");

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
        if (val.value != null && !val.value.isEmpty()) {
            text = text.replace("$value", val.value);
            handled = true;
        }
        if (val.timestamp != null && !val.timestamp.isEmpty()) {
            text = text.replace("$time", val.timestamp);
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
        private AssetManager assetManager;

        public Builder(AssetManager assetManager) {
            this.assetManager = assetManager;
        }


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
            if (pumpReservoir.isEmpty()){
                DisplayData.this.pumpReservoir = pumpReservoir;
            }
            pumpReservoir = pumpReservoir.replace(",", ".");
            pumpReservoir = pumpReservoir + "u".replace(".0u", "u");
            DisplayData.this.pumpReservoir = pumpReservoir;
            return this;
        }

        public Builder setIoB(String iob) {
            if (iob.isEmpty()){
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
            bgValueMgdl = bundle.getDouble("bg.valueMgdl", -1000);
            if (bgValueMgdl == -1000) {
                throw new IllegalArgumentException("data info not provided");
            }

            doMgdl = bundle.getBoolean("doMgdl", true);
            arrowImageName = getEmptyIfNull(bundle.getString("bg.deltaName")) + ".png";
            //fill bg
            bgValueUnitized = com.thatguysservice.huami_xdrip.UtilityModels.BgGraphBuilder.unitized_string(bgValueMgdl, doMgdl).replace(',', '.');
            isBgHigh = bundle.getBoolean("bg.isHigh", false);
            isBgLow = bundle.getBoolean("bg.isLow", false);
            long timeStampVal = bundle.getLong("bg.timeStamp", -1);
            bgIsStale = bundle.getBoolean("bg.isStale", false);
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
            String timeStampText = "";
            if (timeStampVal > Constants.DAY_IN_MS) {
                timeStampText = hourMinuteString(timeStampVal);
            } else {
                isOld = true;
                timeStampText = Helper.niceTimeScalar(Helper.msSince(timeStampVal));
            }
            bgDeltaMgdl = bundle.getDouble("bg.deltaValueMgdl", 0);
            String unitized_delta = BgGraphBuilder.unitizedDeltaStringRaw(false, true, bgDeltaMgdl, doMgdl);
            unitizedDelta = new ValueTime(unitized_delta, timeStampText, isOld);

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

            setIoB(getDouble(pumpIob, 2));
            setPumpReservoir(getDouble(pumpReservoir, 1));
            setPumpBattery(getDouble(pumpBattery, 0));

            double insulin = bundle.getDouble("treatment.insulin", -1);
            double carbs = bundle.getDouble("treatment.carbs", -1);
            long timeStamp = bundle.getLong("treatment.timeStamp", -1);

            timeStampText = "";
            isOld = false;
            String treatmentText = "";
            if (insulin > 0) {
                treatmentText = treatmentText + (Helper.qs(insulin, 2) + "u").replace(".0u", "u");
            }
            //if (carbs > 0) {
            //    treatmentText = treatmentText + (JoH.qs(treatment.carbs, 1) + "g").replace(".0g", "g");
            // }

            if (treatmentText.length() > 0) {
                if (timeStamp > Constants.HOUR_IN_MS * 6) {
                    isOld = true;
                    timeStampText = Helper.niceTimeScalar(Helper.msSince(timeStamp));
                } else if (timeStamp > Constants.HOUR_IN_MS) {
                    timeStampText = hourMinuteString(timeStamp);
                } else {
                    treatmentText = "";
                    timeStampText = "";
                }
            }

            treatment = new ValueTime(treatmentText, timeStampText, isOld);

            return DisplayData.this;
        }
    }

    private class ValueTime {
        private String value;
        private String timestamp = null;
        private boolean isOld = false;

        ValueTime(String value) {
            this.value = value;
        }

        ValueTime(String value, String timestamp, boolean isOld) {
            this.value = value;
            this.timestamp = timestamp;
            this.isOld = isOld;
        }

        public String getValue() {
            return value;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public boolean isOld() {
            return isOld;
        }
    }
}
