package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.Header;

public class HeaderMiBand4 extends Header {
    private static final String headerSignature = "HMDIAL\0";
    private static final int headerSize = 40;
    private static final int paramOffset = 36;

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
