package com.thatguysservice.huami_xdrip.models.watchfaces;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO.Info;

public class WatchFaceInfo implements Parcelable {
    public String file;
    public String preview;
    @SerializedName("preview_animated")
    public String previewAnimated;
    public Info info;

    protected WatchFaceInfo(Parcel in) {
        file = in.readString();
        preview = in.readString();
        previewAnimated = in.readString();
        info = in.readParcelable(Info.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(file);
        dest.writeString(preview);
        dest.writeString(previewAnimated);
        dest.writeParcelable(info, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WatchFaceInfo> CREATOR = new Creator<WatchFaceInfo>() {
        @Override
        public WatchFaceInfo createFromParcel(Parcel in) {
            return new WatchFaceInfo(in);
        }

        @Override
        public WatchFaceInfo[] newArray(int size) {
            return new WatchFaceInfo[size];
        }
    };
}
