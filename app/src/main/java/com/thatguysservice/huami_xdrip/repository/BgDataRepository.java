package com.thatguysservice.huami_xdrip.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.thatguysservice.huami_xdrip.models.BgData;

public class BgDataRepository {
    private static BgDataRepository instance;
    private MutableLiveData<BgData> bgLiveData = new MutableLiveData<>();
    private MutableLiveData<String> watchConnectionStateLiveData = new MutableLiveData<>();

    private BgDataRepository() {
    }

    public static BgDataRepository getInstance() {
        if (instance == null) {
            instance = new BgDataRepository();
        }
        return instance;
    }

    public LiveData<BgData> getBgData() {
        return bgLiveData;
    }

    public LiveData<String> getStatusData() {
        return watchConnectionStateLiveData;
    }

    public void setNewBgData(BgData bgData) {
        bgLiveData.postValue(bgData);
    }

    public void setNewConnectionState(String status) {
        watchConnectionStateLiveData.postValue(status);
    }
}
