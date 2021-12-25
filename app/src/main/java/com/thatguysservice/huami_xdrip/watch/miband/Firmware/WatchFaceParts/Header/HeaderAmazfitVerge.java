package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Header;

public class HeaderAmazfitVerge extends Header {
    private static final String headerSignature = "HMDIAL\0";
    private static final int headerSize = 64;
    private static final int paramOffset = 56;

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
}
