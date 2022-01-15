package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.JoH;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.Position;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.SimpleText;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.TextSettings;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.WatchfaceConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.thatguysservice.huami_xdrip.models.JoH.hourMinuteString;

public class DisplayData {
    private ValueTime unitizedDelta;
    private String bgValue;
    private Bitmap arrowBitmap;
    private boolean bgStrikeThrough = false;
    private boolean isBgHigh = false;
    private boolean isBgLow = false;

    private int graphHours = 6;
    private int graphLimit = 16;
    private boolean showTreatment = false;
    private String iob = "";

    private WatchfaceConfig config;
    private int batteryLevel;

    public DisplayData(WatchfaceConfig config) {
        this.config = config;
    }


    public static Builder newBuilder(AssetManager assetManager, WatchfaceConfig config) {
        return new DisplayData(config).new Builder(assetManager);
    }

    public ValueTime getBgValue() {
        return new ValueTime(bgValue);
    }

    public ValueTime getIob() {
        return new ValueTime(iob);
    }

    public ValueTime getUnitized_delta() {
        return unitizedDelta;
    }

    public ValueTime getBatteryLevel(){
        return new ValueTime(String.valueOf(batteryLevel));
    }

    public ValueTime getNoReadings() {
        return new ValueTime(config.noReadingsText.textPattern, hourMinuteString(JoH.tsl()), false);
    }

    public ValueTime getTreatment() {
        /*Treatments treatment = Treatments.last();
        String treatmentText = "";
        String timeStamp = "";
        boolean isOld = false;

        if (treatment != null && treatment.hasContent() && !treatment.noteOnly()) {
            if (treatment.insulin > 0) {
                treatmentText = treatmentText + (JoH.qs(treatment.insulin, 2) + "u").replace(".0u", "u");
            }
            //if (treatment.carbs > 0) {
            //    treatmentText = treatmentText + (JoH.qs(treatment.carbs, 1) + "g").replace(".0g", "g");
           // }

            if (treatmentText.length() > 0) {
                if (treatment.timestamp > Constants.DAY_IN_MS) {
                    timeStamp = hourMinuteString(treatment.timestamp);
                } else {
                    isOld = true;
                    timeStamp = JoH.niceTimeScalar(JoH.msSince(treatment.timestamp));
                }
            }
        }*/
        String treatmentText = "100";
        String timeStamp = "100";
        boolean isOld = false;

        return new ValueTime(treatmentText, timeStamp, isOld);
    }

    public Bitmap getArrowBitmap() {
        return arrowBitmap;
    }

    public boolean isBgStrikeThrough() {
        return bgStrikeThrough;
    }

    public boolean isBgHigh() {
        return isBgHigh;
    }

    public boolean isBgLow() {
        return isBgLow;
    }

    public int getGraphHours() {
        return graphHours;
    }
    public int getGraphLimit() { return graphLimit; }

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
        canvas.rotate(position.rotate,0, position.y);
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

        public Builder(  AssetManager assetManager) {
            this.assetManager = assetManager;
        }

        public Builder setGraphHours(int graphHours) {
            DisplayData.this.graphHours = graphHours;
            return this;
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

        public Builder setIoB(String iob) {
            DisplayData.this.iob = iob.replace(",", ".");

            return this;
        }

        public DisplayData build() throws IllegalArgumentException, IOException {
            String arrowImageName;
            long timeStampVal = -1;
            /*if (dg != null) {
                arrowImageName = dg.delta_name + ".png";
                //fill bg
                bgValue = dg.unitized.replace(',', '.');
                bgStrikeThrough = dg.isStale();
                isBgHigh = dg.isHigh();
                isBgLow = dg.isLow();
                timeStampVal = dg.timestamp;
            } else if (bgReading != null) {
                arrowImageName = bgReading.getDg_deltaName() + ".png";
                //fill bg
                bgValue = bgReading.displayValue(null).replace(',', '.');
                bgStrikeThrough = bgReading.isStale();
                timeStampVal = bgReading.getEpochTimestamp();
            } else {
                throw new IllegalArgumentException("data info not provided");
            }
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
*/

            //fill delta
            boolean isOld = false;
            String timeStamp = "";
            if (timeStampVal > Constants.DAY_IN_MS) {
                timeStamp = hourMinuteString(timeStampVal);
            } else {
                isOld = true;
                timeStamp = JoH.niceTimeScalar(JoH.msSince(timeStampVal));
            }
            unitizedDelta = new ValueTime("tett", timeStamp, isOld);

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
