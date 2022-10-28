package com.thatguysservice.huami_xdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Info implements Parcelable {
    public String name = "";

    @SerializedName("description")
    public String watchfaceDescription = "";

    @SerializedName("author")
    public String author = "";

    @SerializedName("watch_type")
    public String watchType = "";

    public String version = "";

    protected Info(Parcel in) {
        name = in.readString();
        watchfaceDescription = in.readString();
        author = in.readString();
        watchType = in.readString();
        version = in.readString();
    }

    public Info() {

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(watchfaceDescription);
        dest.writeString(author);
        dest.writeString(watchType);
        dest.writeString(version);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Info> CREATOR = new Creator<Info>() {
        @Override
        public Info createFromParcel(Parcel in) {
            return new Info(in);
        }

        @Override
        public Info[] newArray(int size) {
            return new Info[size];
        }
    };
}
