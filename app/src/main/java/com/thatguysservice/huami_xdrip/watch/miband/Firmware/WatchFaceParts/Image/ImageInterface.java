package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Image;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class ImageInterface {
    static final boolean d = true;
    static final String TAG = WatchFaceGenerator.class.getSimpleName();
    ByteArrayOutputStream writer;
    int bitsPerPixel;
    int height;
    int width;
    Bitmap image;
    int rowLengthInBytes;
    int transparency;
    int paletteColors;
    Bitmap imgMask = null;

    ImageInterface(ByteArrayOutputStream stream) {
        this.writer = stream;
    }

    ImageInterface(ByteArrayOutputStream stream, Bitmap image) {
        this.writer = stream;
        initImage(image);
    }

    void initImage(Bitmap image) {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
    }

    public abstract Bitmap prepareImage(Bitmap src) throws IOException;

    public abstract void write(Bitmap image) throws IOException;

    abstract void writeHeader() throws IOException;

    abstract void writeImage() throws IOException;

    Bitmap applyMask(Bitmap src) {
        if (imgMask != null) {
            Bitmap resultBitmap = src.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(resultBitmap);
            canvas.drawBitmap(imgMask, 0f, 0f, null);
            return resultBitmap;
        }
        return src; //TODO add circle mask
    }

    public void setImageMask(Bitmap imgMask) {
        this.imgMask = imgMask;
    }
}
