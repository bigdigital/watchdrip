package com.thatguysservice.huami_xdrip.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.thatguysservice.huami_xdrip.models.BgData;
import com.thatguysservice.huami_xdrip.models.PropertiesUpdate;

public class BgDataRepository {
    private static BgDataRepository instance;
    private MutableLiveData<BgData> bgLiveData = new MutableLiveData<>();
    private MutableLiveData<String> watchConnectionStateLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> serviceStatusLiveData = new MutableLiveData<>();
    private MutableLiveData<PropertiesUpdate> propLiveData = new MutableLiveData<>();

    private BgDataRepository() {
    }

    public static BgDataRepository getInstance() {
        if (instance == null) {
            instance = new BgDataRepository();
        }
        return instance;
    }

    public MutableLiveData<PropertiesUpdate> getPropLiveData() {
        return propLiveData;
    }

    public MutableLiveData<Boolean> getServiceStatus() {
        return serviceStatusLiveData;
    }

    public void setNewServiceStatus(Boolean serviceStatus) {
        serviceStatusLiveData.postValue(serviceStatus);
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

    public void updatePropData(PropertiesUpdate data) {
        propLiveData.postValue(data);
    }

    public void setNewConnectionState(String status) {
        watchConnectionStateLiveData.postValue(status);
    }
}
