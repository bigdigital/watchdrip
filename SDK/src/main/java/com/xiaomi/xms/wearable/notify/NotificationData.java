package com.xiaomi.xms.wearable.notify;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author user
 */
public class NotificationData implements Parcelable {
    public static final Creator<NotificationData> CREATOR = new Creator<NotificationData>() {
        @Override
        public NotificationData createFromParcel(Parcel in) {
            return new NotificationData(in);
        }

        @Override
        public NotificationData[] newArray(int size) {
            return new NotificationData[size];
        }
    };
    public final String message;
    public final String title;

    protected NotificationData(Parcel in) {
        message = in.readString();
        title = in.readString();
    }

    public NotificationData(String message, String title) {
        this.message = message;
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(message);
        dest.writeString(title);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
