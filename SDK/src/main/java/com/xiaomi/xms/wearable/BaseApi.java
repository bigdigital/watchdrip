package com.xiaomi.xms.wearable;

import android.content.Context;

/**
 * @author user
 */
public class BaseApi {
    public final ApiClient apiClient;

    public BaseApi(Context context) {
        this.apiClient = ApiClient.getInstance(context);
    }

    public interface Callback<T> {
        void onSuccess(T obj);

        void onFailure(Status status);
    }

    public interface Result<T> {
        void onResult(T obj);
    }
}
