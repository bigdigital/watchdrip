package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Image;

import android.graphics.Bitmap;


import com.thatguysservice.huami_xdrip.models.UserError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperationsNew.fromUint32;
import static com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperationsNew.fromUint8;

public class ImageRGB extends ImageInterface {

    static byte[] Signature = {(byte) 'B', (byte) 'M', (byte) 0xff, (byte) 0xff};

    public ImageRGB(ByteArrayOutputStream stream) {
        super(stream);
    }

    public ImageRGB(ByteArrayOutputStream stream, Bitmap image) {
        super(stream, image);
    }

    @Override
    public Bitmap prepareImage(Bitmap src) throws IOException {
        return applyMask(src);
    }

    @Override
    public void write(Bitmap image) throws IOException {
        Bitmap.Config config = image.getConfig();
        if (config != ARGB_8888) {
            throw new RuntimeException("Image should have ARGB_8888 format, stopping execution");
        }
        initImage(image);
        bitsPerPixel = 24;
        transparency = 0;
        paletteColors = 24;
        rowLengthInBytes = (int) Math.ceil((width * bitsPerPixel / 8.0));
        writer.write(Signature);
        writeHeader();
        writeImage();
    }

    @Override
    void writeHeader() throws IOException {
        if (d) {
            UserError.Log.d(TAG, "Writing image header");
            UserError.Log.d(TAG, String.format("Width: %d, Height: %d, RowLength: %d", width, height, rowLengthInBytes));
            UserError.Log.d(TAG, String.format("BPP: %d, PaletteColors: %d, Transaparency: %d", bitsPerPixel, paletteColors, transparency));
        }
        writer.write(fromUint32(width));
        writer.write(fromUint32(height));
        writer.write(fromUint32(bitsPerPixel));
        writer.write(fromUint32(paletteColors));
        writer.write(fromUint32(transparency));
    }

    @Override
    void writeImage() {
        if (d)
            UserError.Log.d(TAG, "Writing image");
        int pixel, r, g, b;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixel = image.getPixel(x, y);
                //int alfa = (pixel >> 24) & 0xff;
                r = (pixel >> 16) & 0xff;
                g = (pixel >> 8) & 0xff;
                b = (pixel) & 0xff;
                writer.write(fromUint8(b));
                writer.write(fromUint8(g));
                writer.write(fromUint8(r));
            }
        }
    }
}
