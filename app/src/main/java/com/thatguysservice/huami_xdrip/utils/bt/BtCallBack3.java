package com.thatguysservice.huami_xdrip.utils.bt;

// interface for providing bluetooth status callbacks

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

public interface BtCallBack3 {

    void btCallback3(String mac, String status, String name, Bundle bundle, BluetoothDevice device);

}
