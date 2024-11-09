package com.xiaomi.xms.wearable.auth;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * @author user
 */
public class Permission implements Parcelable {
    public static final Creator<Permission> CREATOR = new Creator<Permission>() {
        @Override
        public Permission createFromParcel(Parcel in) {
            return new Permission(in);
        }

        @Override
        public Permission[] newArray(int size) {
            return new Permission[size];
        }
    };
    /**
     * 设备数据权限
     * 具体作用未知
     */
    public static final Permission DEVICE_MANAGER = new Permission("data_manager");
    /**
     * 发送通知事件
     */
    public static final Permission NOTIFY = new Permission("notify");
    private final String name;

    protected Permission(Parcel in) {
        this.name = in.readString();
    }

    private Permission(String s) {
        this.name = s;
    }

    public String getName() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
    }
}
