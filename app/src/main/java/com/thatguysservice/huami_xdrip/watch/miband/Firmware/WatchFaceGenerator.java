package com.thatguysservice.huami_xdrip.watch.miband.Firmware;

import android.Manifest;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.UtilityModels.BgGraphBuilder;
import com.thatguysservice.huami_xdrip.UtilityModels.BgGraphCompontens;
import com.thatguysservice.huami_xdrip.models.BgData;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.WatchfaceConfig;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.DisplayData;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Header.Header;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Image.ImageBipPalette;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Image.ImageInterface;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Image.ImageMiBandPalette;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Image.ImageRGB;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Image.ImageRGB_GTS2Mini;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Image.ImageTransparentRGB;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Parameter;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Utils.QuickLZ;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandType;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import lombok.NonNull;
import pub.devrel.easypermissions.EasyPermissions;

import static com.thatguysservice.huami_xdrip.models.Helper.threadSleep;
import static com.thatguysservice.huami_xdrip.utils.FileUtils.getExternalDir;
import static com.thatguysservice.huami_xdrip.utils.FileUtils.makeSureDirectoryExists;
import static com.thatguysservice.huami_xdrip.watch.miband.Firmware.operations.FirmwareOperationsNew.fromUint32;

public class WatchFaceGenerator {
    public final static int VERGE2_HEADERLEN = 40;
    private static final boolean debug = true; //need only for debug to save resulting image and firmware
    private static final String TAG = WatchFaceGenerator.class.getSimpleName();
    private static final int offsetTableItemLength = 4;
    private static Bitmap graphImage;
    private static boolean drawMutex;
    private BufferedInputStreamPos fwFileStream;
    private Bitmap mainWatchfaceImage;
    private AssetManager assetManager;
    private Bitmap imageMask = null;

    private MiBandType bandType;
    private WatchfaceConfig watchfaceConfig;
    private ArrayList<byte[]> resources;
    private int resourcesTableOffset;

    public WatchFaceGenerator(AssetManager assetManager, MiBandType bandType) throws Exception {
        this.assetManager = assetManager;
        this.bandType = bandType;
        if (!MiBandType.supportGraph(bandType)) {
            throw new Exception("Not supported device");
        }
        InputStream mainImageStream = null;
        InputStream maskImageStream = null;
        InputStream configFileStream = null;
        InputStream firmwareFileStream = null;

        boolean customFilesFound = false;

        String filePrefix = "";
        if (bandType == MiBandType.MI_BAND4) {
            filePrefix = "miband4";
        } else if (bandType == MiBandType.MI_BAND5 || bandType == MiBandType.AMAZFIT5) {
            filePrefix = "miband5";
        } else if (bandType == MiBandType.MI_BAND6) {
            filePrefix = "miband6";
        } else if (bandType == MiBandType.AMAZFITGTR || bandType == MiBandType.AMAZFITGTR_LITE) {
            filePrefix = "amazfit_gtr";
        } else if (bandType == MiBandType.AMAZFITGTR_42) {
            filePrefix = "amazfit_gtr42";
        } else if (MiBandType.isBip(bandType)) {
            filePrefix = "bip";
        } else if (MiBandType.isBipS(bandType)) {
            filePrefix = "bip_s";
        } else if ((bandType == MiBandType.AMAZFITGTR2 || bandType == MiBandType.AMAZFITGTR2E)) {
            filePrefix = "amazfit_gtr2";
        } else if ((bandType == MiBandType.AMAZFITGTS2 || bandType == MiBandType.AMAZFITGTS2E)) {
            filePrefix = "amazfit_gts2";
        } else if (bandType == MiBandType.AMAZFITGTS2_MINI) {
            filePrefix = "amazfit_gts2_mini";
        } else if (bandType == MiBandType.AMAZFIT_TREX_PRO) {
            filePrefix = "amazfit_trex_pro";
        }
        if (MiBandEntry.isNeedToUseCustomWatchface()) {
            final String dir = getExternalDir();
            final File imageFile = new File(dir + "/my_image.png");
            final File configFile = new File(dir + "/config.json");
            final File maskFile = new File(dir + "/my_mask.png");
            final File wfFile = new File(dir + "/my_watchface.bin");
            if (configFile.exists()) {
                configFileStream = new FileInputStream(configFile);
            }
            if (maskFile.exists()) {
                maskImageStream = new FileInputStream(maskFile);
            }
            if (imageFile.exists() && wfFile.exists()) {
                customFilesFound = true;
                mainImageStream = new FileInputStream(imageFile);
                firmwareFileStream = new FileInputStream(wfFile);
            }
        }
        String assetWatcfaceDir = "miband_watchface_parts/" + filePrefix + "/";

        if (configFileStream == null) {
            configFileStream = assetManager.open(assetWatcfaceDir + "config.json");
        }
        if (maskImageStream == null) {
            try {
                maskImageStream = assetManager.open(assetWatcfaceDir + "mask.png");
            } catch (IOException e) {
            }
        }

        if (!customFilesFound) {
            String firmwareFileName = assetWatcfaceDir + "watchface";
            if (MiBandType.supportDateFormat(bandType) && !MiBandEntry.isUS_DateFormat()) {
                firmwareFileName += "_eu";
            }
            firmwareFileName += ".bin";
            firmwareFileStream = assetManager.open(firmwareFileName);
            mainImageStream = assetManager.open(assetWatcfaceDir + "canvas.png");
        }

        Gson gson = new Gson();
        // String temp = gson.toJson(new WatchfaceConfig()); //temp for tests

        BufferedReader br = new BufferedReader(new InputStreamReader(configFileStream));
        GsonBuilder builder = new GsonBuilder();
        gson = builder.create();
        watchfaceConfig = gson.fromJson(br, WatchfaceConfig.class);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        mainWatchfaceImage = BitmapFactory.decodeStream(mainImageStream);
        mainImageStream.close();

        fwFileStream = new BufferedInputStreamPos(firmwareFileStream, firmwareFileStream.available());
        fwFileStream.mark(firmwareFileStream.available());

        if (maskImageStream != null) {
            imageMask = BitmapFactory.decodeStream(maskImageStream);
            maskImageStream.close();
        }
    }

    private void parseWatchfaceFile() throws IOException {
        fwFileStream.reset();
        int fileSize = fwFileStream.available();
        UserError.Log.d(TAG, "Reading header");
        Header header = Header.getHeader(bandType);
        header = header.readFrom(fwFileStream);
        UserError.Log.d(TAG, "Header was read:");
        UserError.Log.d(TAG, String.format("Signature: %1$s, ParametersSize: %2$d isValid: %3$s", header.getSignature(), header.getParametersSize(), header.isValid()));
        if (!header.isValid())
            throw new RuntimeException("Wrong watchface format");
        UserError.Log.d(TAG, "Reading parameter offsets...");
        //catch wrong parameter size
        if (header.getParametersSize() > 10000) {
            throw new RuntimeException("Parameter size too big, check watchface");
        }
        if (header.getParametersSize() <= 0) {
            throw new RuntimeException("Parameter size negative, check watchface");
        }
        byte[] bytes = new byte[header.getParametersSize()];
        fwFileStream.read(bytes, 0, bytes.length);
        InputStream parameterStream = new ByteArrayInputStream(bytes);
        Parameter mainParam = Parameter.readFrom(parameterStream, 0);
        UserError.Log.d(TAG, "Parameters descriptor was read");
        int parametersTableLength = (int) mainParam.getChildren().get(0).getValue();
        int imagesCount = (int) mainParam.getChildren().get(1).getValue() - header.getStartImageIndex();
        UserError.Log.d(TAG, "parametersTableLength: " + parametersTableLength + ", imagesCount: " + imagesCount);
        //bytes = new byte[parametersTableLength];
        //fwFileStream.read(bytes, 0, bytes.length);
        fwFileStream.skip(parametersTableLength);

        resourcesTableOffset = fwFileStream.getPos();
        UserError.Log.d(TAG, "Reading images table offsets...");
        bytes = new byte[imagesCount * offsetTableItemLength];
        fwFileStream.read(bytes, 0, bytes.length);
        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        ArrayList<Integer> imageOffsets = new ArrayList<>();
        for (int i = 0; i < imagesCount; i++) {
            imageOffsets.add(b.getInt());
        }
        int resourcesOffset = fwFileStream.getPos();
        UserError.Log.d(TAG, "Image offsets were read");
        resources = new ArrayList<>();

        UserError.Log.d(TAG, "Reading resources");
        for (int i = 0; i < imageOffsets.size(); i++) {
            int offset = imageOffsets.get(i) + resourcesOffset;
            int nextOffset = (i + 1 < imageOffsets.size()) ? imageOffsets.get(i + 1) + resourcesOffset : fileSize;
            int length = nextOffset - offset;
            //UserError.Log.d(TAG, "Resource " + i + " offset: " + offset + ", length: " + length);
            if (fwFileStream.getPos() != offset) {
                int bytesGap = offset - fwFileStream.getPos();
                UserError.Log.d(TAG, "Found " + bytesGap + " bytes gap before resource number " + i);
                fwFileStream.skip(bytesGap);
            }
            byte[] data = new byte[length];
            fwFileStream.read(data, 0, length);
            resources.add(data);
        }
    }

    private void writeImages(ByteArrayOutputStream stream) throws IOException {
        UserError.Log.d(TAG, "Writing resources offsets table");
        int num_padding = 4 - resourcesTableOffset % 4;
        int offset = num_padding;
        for (int i = 0; i < resources.size(); i++) {
            stream.write(fromUint32(offset));
            offset += resources.get(i).length;
        }
        UserError.Log.d(TAG, "Writing padding...");

        byte[] buffer = new byte[num_padding];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) 0xff;
        }
        stream.write(buffer);

        UserError.Log.d(TAG, "Writing resources");
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) 0xff;
        }

        for (byte[] res : resources) {
            stream.write(res);
        }
    }

    public byte[] genWatchFace(Bundle bundle, BgData bgData) throws IOException {
        Bitmap mainScreen;
        //send firmware without modification, uncomment when need to test only a watchface uploading process
       /* if (true) {
            return FirmwareOperationsNew.readAll(fwFileStream, 10000000);
        }*/


        boolean isAllowedToWrite = debug && EasyPermissions.hasPermissions(HuamiXdrip.getAppContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        parseWatchfaceFile();
        if (!bgData.isNoBgData()) {
            DisplayData.Builder displayDataBuilder = null;
            displayDataBuilder = DisplayData.newBuilder(bundle, bgData, watchfaceConfig, assetManager );
            displayDataBuilder.setShowTreatment(MiBandEntry.isTreatmentEnabled());
            displayDataBuilder.setGraphLimit(MiBandEntry.getGraphLimit());
            DisplayData data = displayDataBuilder.build();
            mainScreen = drawBitmap(data);
        } else {
            DisplayData displayData = new DisplayData(bundle, bgData, watchfaceConfig, assetManager);
            mainScreen = drawNoDataBitmap(displayData);
        }
        UserError.Log.d(TAG, "Encoding main picture");
        ByteArrayOutputStream imageByteArrayOutput = new ByteArrayOutputStream();
        ImageInterface encodedImage;
        if (MiBandType.isBip(bandType) || MiBandType.isBipS(bandType)) {
            encodedImage = new ImageBipPalette(imageByteArrayOutput);
        } else if (MiBandType.AMAZFITGTS2_MINI == bandType) {
            encodedImage = new ImageRGB_GTS2Mini(imageByteArrayOutput);
        } else if (MiBandType.AMAZFIT_TREX_PRO == bandType) {
            encodedImage = new ImageTransparentRGB(imageByteArrayOutput);
        } else if (MiBandType.isVerge1(bandType)) {
            encodedImage = new ImageRGB(imageByteArrayOutput);
        } else if (MiBandType.isVerge2(bandType)) {
            encodedImage = new ImageTransparentRGB(imageByteArrayOutput);
        } else {
            encodedImage = new ImageMiBandPalette(imageByteArrayOutput);
        }
        encodedImage.setImageMask(imageMask);
        Bitmap resultImage = encodedImage.prepareImage(mainScreen);
        encodedImage.write(resultImage);
        UserError.Log.d(TAG, "Encoded image size: " + imageByteArrayOutput.size() + " bytes");

        int newImageSize = imageByteArrayOutput.size();

        //replace main image in resources
        UserError.Log.d(TAG, "Replace resource " + watchfaceConfig.resourceToReplace);
        resources.set(watchfaceConfig.resourceToReplace, imageByteArrayOutput.toByteArray());

        ByteArrayOutputStream firmwareWriteStream = new ByteArrayOutputStream(newImageSize + resourcesTableOffset);

        UserError.Log.d(TAG, "Copying original header with params ");
        byte[] bytes = new byte[resourcesTableOffset];

        //fwFileStream.mark(0);
        fwFileStream.reset();
        fwFileStream.read(bytes, 0, bytes.length);
        firmwareWriteStream.write(bytes, 0, bytes.length);

        writeImages(firmwareWriteStream);

        fwFileStream.close();

        //compress watchface if supported
        if (MiBandType.enableCompression(bandType)) {
            if (isAllowedToWrite) {
                final String dir = getExternalDir();
                byte[] firmwareData = firmwareWriteStream.toByteArray();
                try (FileOutputStream out = new FileOutputStream(dir + "/watchface_uncomp.bin")) {
                    out.write(firmwareData);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            UserError.Log.d(TAG, "Compressing watchface");
            int compressSize = 0;
            int decompressSize = 0;
            int uncFileSize = firmwareWriteStream.size();
            ByteArrayInputStream firmwareReadStream = new ByteArrayInputStream(firmwareWriteStream.toByteArray());
            firmwareWriteStream.reset();
            if ( (MiBandType.AMAZFIT_TREX_PRO == bandType) || MiBandType.isVerge2(bandType)) {
                byte[] header = new byte[VERGE2_HEADERLEN];
                firmwareReadStream.read(header, 0, header.length);
                //copy header chunk unmodified
                firmwareWriteStream.write(header, 0, header.length);
                decompressSize += header.length;
            }
            bytes = new byte[0x1000];
            int read;
            while (true) {
                if (uncFileSize <= decompressSize) {
                    break;
                }
                if (uncFileSize < compressSize + QuickLZ.DEFAULT_HEADERLEN) {
                    break;
                }
                read = firmwareReadStream.read(bytes, 0, bytes.length);
                if (read == bytes.length) {
                    byte[] compressed = QuickLZ.compress(bytes, 3);
                    firmwareWriteStream.write(compressed, 0, compressed.length);
                    compressSize += compressed.length;
                } else {
                    //copy last chunk unmodified
                    firmwareWriteStream.write(bytes, 0, read);
                    compressSize += (uncFileSize - decompressSize);
                }
                decompressSize += read;
            }
            int afterSize = firmwareWriteStream.size();
            UserError.Log.d(TAG, "Compressing finished. Watchface size before: " + uncFileSize + " after: " + afterSize);
        }

        UserError.Log.d(TAG, "Watchface file ready");

        byte[] firmwareData = firmwareWriteStream.toByteArray();

        if (isAllowedToWrite) {
            final String dir = getExternalDir();
            makeSureDirectoryExists(dir);
            try (FileOutputStream out = new FileOutputStream(dir + "/canvas.png")) {
                resultImage.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (FileOutputStream out = new FileOutputStream(dir + "/watchface.bin")) {
                out.write(firmwareData);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mainScreen.recycle();
        resultImage.recycle();
        return firmwareData;
    }

    private Bitmap drawBitmap(DisplayData data) {
        Bitmap resultBitmap = mainWatchfaceImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);

        WatchfaceConfig config = data.getConfig();
        //draw graph
        if (config.graph != null) {
            drawMutex = true;
            Helper.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BgGraphBuilder bgGraph = new BgGraphBuilder(HuamiXdrip.getAppContext())
                                .setBgGraphCompontens(new BgGraphCompontens(data.getBundle(), HuamiXdrip.getAppContext()))
                                .setWidthPx(config.graph.width + 16)
                                .setHeightPx(config.graph.height)
                                .setBackgroundColor(config.graph.getBgColor());
                        bgGraph.setGraphSettings(config.graph);
                        bgGraph.setGraphLimit(data.getGraphLimit());

                        bgGraph.showTreatmentLine(data.isShowTreatment());
                        graphImage = bgGraph.build();
                    } catch (Exception e) {
                        UserError.Log.e(TAG, "Exception while generating: " + e);
                        e.printStackTrace();
                    } finally {
                        drawMutex = false;
                    }
                }
            });
            while (drawMutex)
                threadSleep(100);
            //strip left and right fields
            if (graphImage != null) {
                Bitmap resizedGraphImage = Bitmap.createBitmap(graphImage, 8, 0, config.graph.width, graphImage.getHeight());
                canvas.save();
                canvas.rotate(config.graph.position.rotate, config.graph.position.x, config.graph.position.y);
                canvas.drawBitmap(resizedGraphImage, config.graph.position.x, config.graph.position.y, null);
                canvas.restore();
            }
        }
        //draw predicted data
        data.drawFormattedTextOnCanvas(canvas,  data.getPredictIoB(), config.predictIOB);
        data.drawFormattedTextOnCanvas(canvas, data.getPredictWpb(), config.predictWPB);
        //draw pump
        data.drawFormattedTextOnCanvas(canvas, data.getPumpIoB(), config.pumpIOB);
        data.drawFormattedTextOnCanvas(canvas, data.getPumpReservoir(), config.pumpReservoir);
        data.drawFormattedTextOnCanvas(canvas, data.getPumpBattery(), config.pumpBattery);

        //draw arrow
        canvas.save();
        canvas.rotate(config.arrowPosition.rotate, config.arrowPosition.x, config.arrowPosition.y);
        canvas.drawBitmap(data.getArrowBitmap(), config.arrowPosition.x, config.arrowPosition.y, null);
        canvas.restore();

        //draw bgValueText
        Paint paint = data.getTextPaint(config.bgValue.textSettings);
        if (data.isBgHigh()) paint.setColor(config.bgValue.getColorHigh());
        if (data.isBgLow()) paint.setColor(config.bgValue.getColorLow());
        paint.setStrikeThruText(data.isBgIsStale());

        data.drawFormattedTextOnCanvas(canvas, data.getBgValue(), config.bgValue, paint);

        //draw unitized delta
        data.drawFormattedTextOnCanvas(canvas, data.getUnitized_delta(), config.deltaText);
        //draw delta time text if specified
        if (config.deltaTimeText != null) {
            data.drawFormattedTextOnCanvas(canvas, data.getUnitized_delta(), config.deltaTimeText);
        }

        //draw treatment
        data.drawFormattedTextOnCanvas(canvas, data.getTreatment(), config.treatmentText);
        //draw treatment time text if specified
        if (config.treatmentTimeText != null) {
            data.drawFormattedTextOnCanvas(canvas, data.getTreatment(), config.treatmentTimeText);
        }

        //draw battery level text if specified
        if (config.batteryLevel != null) {
            data.drawFormattedTextOnCanvas(canvas, data.getBatteryLevel(), config.batteryLevel);
        }

        //draw custom text
      /*  if (config.customText != null) {
            data.drawFormattedTextOnCanvas(canvas, data.getBatteryLevel(), config.batteryLevel);
        }*/

        //draw ext status data
        data.drawFormattedTextOnCanvas(canvas, data.getExtStatusLine(), config.extStatusLineText);

        return resultBitmap;
    }

    private Bitmap drawNoDataBitmap(DisplayData data) {
        Bitmap resultBitmap = mainWatchfaceImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);

        WatchfaceConfig config = data.getConfig();

        //draw no readings text
        data.drawFormattedTextOnCanvas(canvas, data.getNoReadings(), config.noReadingsText);
        //draw timestamp
        if (config.treatmentTimeText != null) {
            data.drawFormattedTextOnCanvas(canvas, data.getNoReadings(), config.noReadingsTimeText);
        }
        return resultBitmap;
    }
}

class BufferedInputStreamPos extends BufferedInputStream {

    public BufferedInputStreamPos(@NonNull InputStream in) {
        super(in);
    }

    public BufferedInputStreamPos(@NonNull InputStream in, int size) {
        super(in, size);
    }

    public int getPos() {
        return this.pos;
    }
}
