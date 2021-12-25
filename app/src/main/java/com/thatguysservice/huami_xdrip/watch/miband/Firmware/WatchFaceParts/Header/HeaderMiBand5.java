package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Header;

public class HeaderMiBand5 extends Header {
    private static final String headerSignature = "UIHH\1\0";
    private static final int headerSize = 87;
    private static final int paramOffset = 83;

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
