package com.thatguysservice.huami_xdrip.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class WatchfaceViewModel extends ViewModel {
    private String[] names;

    private final MutableLiveData<String> selected = new MutableLiveData<String>();

    public void select(String item) {
        selected.setValue(item);
    }

    public LiveData getSelected() {
        return selected;
    }
}
