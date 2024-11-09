package com.xiaomi.xms.wearable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author user
 */
public class Status implements Parcelable {
    public static final Creator<Status> CREATOR = new Creator<Status>() {
        @Override
        public Status createFromParcel(Parcel in) {
            return new Status(in);
        }

        @Override
        public Status[] newArray(int size) {
            return new Status[size];
        }
    };
    public static final Status RESULT_APP_NOT_INSTALLED = new Status(-8);
    public static final Status RESULT_CANCELLED = new Status(-1);
    public static final Status RESULT_DISCONNECTED = new Status(-4);
    public static final Status RESULT_INTERRUPTED = new Status(-3);
    public static final Status RESULT_PACKAGE_NOT_INSTALLED = new Status(-5);
    public static final Status RESULT_PERMISSION_DENIED = new Status(-7);
    public static final Status RESULT_SIGNATURE_VERIFY_FAILED = new Status(-6);
    public static final Status RESULT_SUCCESS = new Status(0);
    public static final Status RESULT_TIMEOUT = new Status(-2);
    private final int code;

    protected Status(Parcel in) {
        this.code = in.readInt();
    }

    private Status(int v) {
        this.code = v;
    }

    public int getCode() {
        return this.code;
    }

    public boolean isSuccess() {
        return this.code == Status.RESULT_SUCCESS.code;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(code);
    }
}
