package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Header;

import com.thatguysservice.huami_xdrip.watch.miband.MiBandType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class Header{
    private final int startImageIndex = 0;
    private int parametersSize;
    private String signature;

    public abstract int getParamOffset();

    public abstract int getHeaderSize();

    public abstract String getSignature();

    public boolean isValid() {
        return (signature.equals(getSignature()));
    }

    public int getStartImageIndex() {
        return startImageIndex;
    }

    public int getParametersSize() {
        return parametersSize;
    }

    public Header readFrom(InputStream stream) throws IOException {
        ByteBuffer b;
        byte[] bytes = new byte[getHeaderSize()];
        stream.read(bytes, 0, bytes.length);

        int signatureLen = getSignature().length();
        b = ByteBuffer.allocate(signatureLen);
        b.put(bytes, 0, signatureLen);
        signature = new String(b.array());

        b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        b.put(bytes, getParamOffset(), 4);
        b.rewind();
        parametersSize = b.getInt();
        return this;
    }

    public static Header getHeader(MiBandType bandType) throws IllegalArgumentException {
        Header header = null;
        if (bandType == MiBandType.MI_BAND4 || MiBandType.isBip(bandType) || MiBandType.isBipS(bandType)) {
            header = new HeaderMiBand4();
        } else if (bandType == MiBandType.MI_BAND5 || bandType == MiBandType.AMAZFIT5 || bandType == MiBandType.MI_BAND6) {
            header = new HeaderMiBand5();
        } else if (bandType == MiBandType.AMAZFITGTS2_MINI) {
            header = new HeaderAmazfitGTS2Mini();
        } else if (bandType == MiBandType.AMAZFIT_TREX_PRO) {
            header = new HeaderAmazfitVerge2();
        } else if (MiBandType.isVerge1(bandType)) {
            header = new HeaderAmazfitVerge();
        } else if (MiBandType.isVerge2(bandType)) {
            header = new HeaderAmazfitVerge2();
        } else {
            throw new IllegalArgumentException("Not supported header");
        }
        return header;
    }
}
