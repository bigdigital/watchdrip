package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Image;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Utils.PnnQuantizer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperationsNew.fromUint16;

public class ImageMiBandPalette extends ImageInterface {

    static byte[] Signature = {(byte) 'B', (byte) 'M', (byte) 'd', 0};
    static int MAX_PALETTE_COLORS = 256;
    ArrayList<Integer> palette = new ArrayList<>();

    public ImageMiBandPalette(ByteArrayOutputStream stream) {
        super(stream);
    }

    public ImageMiBandPalette(ByteArrayOutputStream stream, Bitmap image) {
        super(stream, image);
    }

    public ArrayList<Integer> getPalette() {
        return palette;
    }

    @Override
    public Bitmap prepareImage(Bitmap src) throws IOException {
        Bitmap maskedImg = applyMask(src);
        return quantinizeImage(maskedImg);
    }

    @Override
    public void write(Bitmap image) throws IOException {
        initImage(image);
        extractPalette();
        rowLengthInBytes = (int) Math.ceil((width * bitsPerPixel / 8.0));
        writer.write(Signature);
        writeHeader();
        writePalette();
        writeImage();
    }

    Bitmap quantinizeImage(Bitmap src) throws IOException {
        PnnQuantizer pnnQuantizer = new PnnQuantizer(src);
        return pnnQuantizer.convert(MAX_PALETTE_COLORS, false);
    }

    void extractPalette() {
        if (d)
            UserError.Log.e(TAG, "Extracting palette");

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = image.getPixel(x, y);
                if (palette.contains(color)) continue;

                if (Color.alpha(color) < 0x80 && transparency == 0) {
                    palette.add(0, color);
                    transparency = 1;
                } else {
                    palette.add(color);
                }
            }
        }
        Collections.sort(palette);
        paletteColors = palette.size();
        bitsPerPixel = (int) Math.ceil(Math.log(paletteColors) / Math.log(2));
        if (d)
            UserError.Log.e(TAG, String.format("Extracted: %d palette colors, bitsPerPixel:%d", paletteColors, bitsPerPixel));

        if (palette.size() > MAX_PALETTE_COLORS)
            throw new RuntimeException("Too many colors for palette mode, stopping execution");

        //expand palette if needed
        boolean isNeedToexpandColorPallete = false;
        if (bitsPerPixel < 1) {
            bitsPerPixel = 1;
            isNeedToexpandColorPallete = true;
        }
        if (bitsPerPixel < 2) {
            bitsPerPixel = 2;
            isNeedToexpandColorPallete = true;
        } else if (bitsPerPixel < 4) {
            bitsPerPixel = 4;
            isNeedToexpandColorPallete = true;
        } else if (bitsPerPixel > 4 && bitsPerPixel < 8) {
            bitsPerPixel = 8;
            isNeedToexpandColorPallete = true;
        }
        if (isNeedToexpandColorPallete) {
            int colorsShouldBe = (int) Math.pow(2, bitsPerPixel);
            int lastColor = palette.get(palette.size() - 1);
            while (palette.size() < colorsShouldBe) {
                palette.add(lastColor);
            }
            paletteColors = palette.size();
            bitsPerPixel = (int) Math.ceil(Math.log(paletteColors) / Math.log(2));
            if (d)
                UserError.Log.e(TAG, String.format("palette was expanded to: %d palette colors", paletteColors));
        }
    }

    @Override
    void writeHeader() throws IOException {
        if (d) {
            UserError.Log.d(TAG, "Writing image header");
            UserError.Log.d(TAG, String.format("Width: %d, Height: %d, RowLength: %d", width, height, rowLengthInBytes));
            UserError.Log.d(TAG, String.format("BPP: %d, PaletteColors: %d, Transaparency: %d", bitsPerPixel, paletteColors, transparency));
        }
        writer.write(fromUint16(width));
        writer.write(fromUint16(height));
        writer.write(fromUint16(rowLengthInBytes));
        writer.write(fromUint16(bitsPerPixel));
        writer.write(fromUint16(paletteColors));
        writer.write(fromUint16(transparency));
    }

    @Override
    void writeImage() throws IOException {
        if (d)
            UserError.Log.d(TAG, "Writing image");

        HashMap<Integer, Integer> paletteHash = new HashMap<>();
        int i = 0;
        for (int color : palette) {
            paletteHash.put(color, i);
            i++;
        }

        for (int y = 0; y < height; y++) {
            ByteArrayOutputStream memoryStream = new ByteArrayOutputStream(rowLengthInBytes);
            ImageBitWriter bitWriter = new ImageBitWriter(memoryStream);
            Integer paletteIndex;
            for (int x = 0; x < width; x++) {
                int color = image.getPixel(x, y);
                if (Color.alpha(color) < 0x80 && transparency == 1) {
                    bitWriter.writeBits(0, bitsPerPixel);
                } else {
                    paletteIndex = paletteHash.get(color);
                    bitWriter.writeBits(paletteIndex, bitsPerPixel);
                }
            }
            bitWriter.flush();
            writer.write(memoryStream.toByteArray());
        }
    }

    void writePalette() throws IOException {
        if (d)
            UserError.Log.e(TAG, "Writing palette");
        for (int color : palette) {
            writer.write(Color.red(color));
            writer.write(Color.green(color));
            writer.write(Color.blue(color));
            writer.write((byte) 0); // always 0 maybe padding
        }
    }
}
