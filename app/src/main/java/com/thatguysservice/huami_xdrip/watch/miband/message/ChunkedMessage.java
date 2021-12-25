package com.thatguysservice.huami_xdrip.watch.miband.message;

import com.thatguysservice.huami_xdrip.watch.miband.Const;

import java.util.UUID;

public class ChunkedMessage extends BaseMessage {
    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_CHUNKEDTRANSFER;
    }
}
