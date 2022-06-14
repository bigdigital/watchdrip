package com.thatguysservice.huami_xdrip.watch.miband;

// jamorham

import android.os.Bundle;

import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.R;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.repository.BgDataRepository;
import com.thatguysservice.huami_xdrip.utils.bt.BtCallBack2;
import com.thatguysservice.huami_xdrip.utils.bt.ScanMeister;

public class FindNearby implements BtCallBack2 {

    private static final String TAG = "MiBand Scan";
    private static ScanMeister scanMeister;
    private BgDataRepository bgDataRepository;

    public synchronized void scan(BgDataRepository bgDataRepository) {
        this.bgDataRepository = bgDataRepository;
        if (scanMeister == null) {
            scanMeister = new ScanMeister();
        } else {
            scanMeister.stop();
        }
        bgDataRepository.setNewConnectionState( HuamiXdrip.gs(R.string.miband_search_text));
        Helper.static_toast_long(HuamiXdrip.getAppContext().getString(R.string.miband_search_text));
        for (MiBandType b : MiBandType.values()) {
            if (!b.toString().isEmpty()) {
                scanMeister.setName(b.toString());
            }
        }
        scanMeister.addCallBack2(this, TAG).scan();
    }

    @Override
    public void btCallback2(String mac, String status, String name, Bundle bundle) {
        switch (status) {
            case ScanMeister.SCAN_FOUND_CALLBACK:
                MiBand.setMacPref(mac, bgDataRepository);
                MiBand.setModel(name, mac);
                String text = String.format(HuamiXdrip.getAppContext().getString(R.string.miband_search_found_text), name, mac);
                Helper.static_toast_long(text);
                Helper.static_toast_long(text);
                break;
            case ScanMeister.SCAN_FAILED_CALLBACK:
            case ScanMeister.SCAN_TIMEOUT_CALLBACK:
                Helper.static_toast_long(HuamiXdrip.getAppContext().getString(R.string.miband_search_failed_text));
                break;
        }
    }
}
