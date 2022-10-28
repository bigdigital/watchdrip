package com.thatguysservice.huami_xdrip;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.thatguysservice.huami_xdrip.models.watchfaces.MainInfo;
import com.thatguysservice.huami_xdrip.models.watchfaces.ModelInfo;
import com.thatguysservice.huami_xdrip.models.watchfaces.WatchFaceInfo;
import com.thatguysservice.huami_xdrip.watch.miband.MiBand;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandType;

import java.util.List;

public class WatchStoreActivity extends AppCompatActivity {
    public static final String RESULT_SUCCESS = "done";
    private static final String DATA_DIR = "/wf/data.json";

    public static final String DATA_FILES_DIR = "/wf/files/";
    private final String TAG = this.getClass().getSimpleName();
    private String url;
    private MainInfo watchfacesInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_store);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                    .replace(R.id.watchStoreFragmentCont, new WatchStoreGridFragment())
                    .commit();
        }

        url = getString(R.string.wserviceurl) + DATA_DIR;

        updateRemoteWathcfaces();
    }

    public void updateRemoteWathcfaces() {
        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                parseJsonData(s);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, "Got exception while downloading JSON: " + volleyError.toString());
                sendResultToFragment("Could not connect to the server, check your connection");
            }
        });

        RequestQueue rQueue = Volley.newRequestQueue(this);
        rQueue.add(request);
    }

    void parseJsonData(String jsonString) {
        try {
            Gson gson = new Gson();
            GsonBuilder builder = new GsonBuilder();
            gson = builder.create();
            watchfacesInfo = gson.fromJson(jsonString, MainInfo.class);
            List<WatchFaceInfo> wf = getWatchfaces();
            if (wf == null) {
                sendResultToFragment("There no watcfaces for your watch model");
            } else {
                sendResultToFragment(RESULT_SUCCESS);
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Got exception while parsing JSON: " + e.toString());
            sendResultToFragment("Some error occurred during parsing json");
        }
    }

    private void sendResultToFragment(String msg) {
        Bundle result = new Bundle();
        result.putString("msg", msg);
        getSupportFragmentManager().setFragmentResult("server_responce_result", result);
    }

    public List<WatchFaceInfo> getWatchfaces() {
        if (watchfacesInfo == null) {
            return null;
        }
        MiBandType mibandType = MiBand.getMibandType();
        String modelPrefix = MiBandType.getModelPrefix(mibandType);
        try {
            for (ModelInfo modelInfo : watchfacesInfo.models) {
                if (modelInfo.name.equals(modelPrefix)) {
                    return modelInfo.watcfaces;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception while parsing data from server: " + e.toString());
        }
        return null;
    }
}
