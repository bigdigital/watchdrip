package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Header;

public class HeaderAmazfitVerge2 extends Header {
    private static final String headerSignature = "UIHH\2\0";
    private static final int headerSize = 88;
    private static final int paramOffset = 80;
    private final int startImageIndex = 1;

    @Override
    public int getParamOffset() {
        return paramOffset;
    }

    @Override
    public int getHeaderSize() {
        return headerSize;
    }

    @Override
    public String getSignature() {
        return headerSignature;
    }

    @Override
    public int getStartImageIndex() {
        return startImageIndex;
    }
}
