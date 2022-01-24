package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.thatguysservice.huami_xdrip.models.UserError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperations.fromUint16;
import static com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperations.fromUint32;
import static com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperations.fromUint8;

public class ImageRGB_GTS2Mini extends ImageInterface {

    static byte[] Signature = {(byte) 'B', (byte) 'M', (byte) 0x65, (byte) 0x00};
    protected ByteArrayOutputStream tempStream;

    public ImageRGB_GTS2Mini(ByteArrayOutputStream stream) {
        super(stream);
    }
    public ImageRGB_GTS2Mini(ByteArrayOutputStream stream, Bitmap image) {
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
        getImageData();
        bitsPerPixel = 16;
        rowLengthInBytes = (int) Math.ceil((width * bitsPerPixel / 8.0));
        writer.write(Signature);
        writeHeader();
        writeImage();
    }

    @Override
    void writeHeader() throws IOException {
        int dataSize = tempStream.size();
        if (d) {
            UserError.Log.d(TAG, "Writing image header");
            UserError.Log.d(TAG, String.format("Width: %d, Height: %d, RowLength: %d", width, height, rowLengthInBytes));
            UserError.Log.d(TAG, String.format("BPP: %d, DataSize: %d", bitsPerPixel, dataSize));
        }
        writer.write(fromUint16(width+1));
        writer.write(fromUint16(height));
        writer.write(fromUint16(rowLengthInBytes));
        writer.write(fromUint16(bitsPerPixel));
        writer.write(fromUint32(dataSize));
    }

    void getImageData() throws IOException {
        UserError.Log.d(TAG, "Prepare image data");

        Bitmap nonTransparentImage = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        nonTransparentImage.eraseColor(Color.BLACK);
        Canvas canvas = new Canvas(nonTransparentImage);
        canvas.drawBitmap(image, 0, 0, null);

        tempStream = new ByteArrayOutputStream();
        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        int startX = 0;
        int pixelWidth = 0;
        int pixel, a, r, g, b, temp_b, temp_g, temp_g2, temp_r;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixel = image.getPixel(x, y);
                //skip full transparent pixels
                a = (pixel >> 24) & 0xff;
                if (a != 0 ) {
                    if (temp.size() == 0) {
                        startX = x;
                    }
                    pixel = nonTransparentImage.getPixel(x, y);
                    //a = (pixel >> 24) & 0xff;
                    r = (pixel >> 16) & 0xff;
                    g = (pixel >> 8) & 0xff;
                    b = (pixel) & 0xff;

                    temp_b = ((b >> 3) & 0x1f);
                    temp_g = (((g >> 2) & 0x7) << 5);
                    temp_g2 = ((g >> 5) & 0x07);
                    temp_r = (((r >> 3) & 0x1f) << 3);

                    temp.write(fromUint8(temp_b | temp_g)); //write first byte
                    temp.write(fromUint8(temp_g2 | temp_r)); //write second byte
                    pixelWidth++;
                } else {
                    if (temp.size() > 0) {
                       // UserError.Log.d(TAG, String.format("Y: %d, startX: %d, pixelWidth: %d, bytes: %d", y, startX, pixelWidth, temp.size()));
                        tempStream.write(fromUint16(y));
                        tempStream.write(fromUint16(startX));
                        tempStream.write(fromUint16(pixelWidth));
                        tempStream.write(temp.toByteArray());
                    }
                    temp.reset();
                    startX = 0;
                    pixelWidth = 0;
                }
            }
            if (temp.size() > 0) {
                //UserError.Log.d(TAG, "New Row");
                //UserError.Log.d(TAG, String.format("Y: %d, startX: %d, pixelWidth: %d, bytes: %d", y, startX, pixelWidth, temp.size()));
                tempStream.write(fromUint16(y));
                tempStream.write(fromUint16(startX));
                tempStream.write(fromUint16(pixelWidth));
                tempStream.write(temp.toByteArray());
            }
            temp.reset();
            startX = 0;
            pixelWidth = 0;
        }
        temp.flush();
        nonTransparentImage.recycle();
    }

    @Override
    void writeImage() throws IOException {
        if (d)
            UserError.Log.d(TAG, "Writing image");
        writer.write(tempStream.toByteArray());
        tempStream.close();
    }
}
