package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Image;

import android.graphics.Bitmap;


import com.thatguysservice.huami_xdrip.models.UserError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperationsNew.fromUint8;

public class ImageTransparentRGB extends ImageRGB {

    public ImageTransparentRGB(ByteArrayOutputStream stream) {
        super(stream);
    }

    public ImageTransparentRGB(ByteArrayOutputStream stream, Bitmap image) {
        super(stream, image);
    }


    @Override
    public void write(Bitmap image) throws IOException {
        Bitmap.Config config = image.getConfig();
        if (config != ARGB_8888) {
            throw new RuntimeException("Image should have ARGB_8888 format, stopping execution");
        }
        initImage(image);
        bitsPerPixel = 32;
        transparency = 1;
        paletteColors = 24;
        rowLengthInBytes = (int) Math.ceil((width * bitsPerPixel / 8.0));
        writer.write(Signature);
        writeHeader();
        writeImage();
    }


    @Override
    void writeImage() {
        if (d)
            UserError.Log.d(TAG, "Writing image");
        int pixel, r, g, b, a;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixel = image.getPixel(x, y);
                a = (pixel >> 24) & 0xff;
                r = (pixel >> 16) & 0xff;
                g = (pixel >> 8) & 0xff;
                b = (pixel) & 0xff;
                writer.write(fromUint8(b));
                writer.write(fromUint8(g));
                writer.write(fromUint8(r));
                writer.write(fromUint8(a));
            }
        }
    }
}
