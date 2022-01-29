package com.thatguysservice.huami_xdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.thatguysservice.huami_xdrip.UtilityModels.Intents;
import com.thatguysservice.huami_xdrip.models.UserError;

public class PreferenceActivity extends AppCompatActivity {
    private final static String TAG = PreferenceActivity.class.getSimpleName();
    private static PreferenceActivity mActivity;
    private static String nexttoast;
    private static boolean activityVisible;
    private BroadcastReceiver newDataReceiver;

    public static void toastStatic(String msg) {
        nexttoast = msg;
        staticRefresh();
    }

    public static void staticRefresh() {
        if (activityVisible) {
            Intent updateIntent = new Intent(Intents.ACTION_UPDATE_VIEW);
            mActivity.sendBroadcast(updateIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        activityVisible = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If you don't have res/menu, just create a directory named "menu" inside res
       /* getMenuInflater().inflate(R.menu.mymenu, menu);*/
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onPause() {
        activityVisible = false;
        super.onPause();

        if (newDataReceiver != null) {
            try {
                unregisterReceiver(newDataReceiver);
            } catch (IllegalArgumentException e) {
                UserError.Log.e(TAG, "newDataReceiver not registered", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        newDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (nexttoast != null) {
                    toast(nexttoast);
                    nexttoast = null;
                }

            }
        };
        registerReceiver(newDataReceiver, new IntentFilter(Intents.ACTION_UPDATE_VIEW));
    }

    public void toast(final String msg) {
        try {
            runOnUiThread(() -> Toast.makeText(mActivity, msg, Toast.LENGTH_LONG).show());
            Log.d(TAG, "toast: " + msg);
        } catch (Exception e) {
            Log.d(TAG, "Couldn't display toast: " + msg + " / " + e.toString());
        }
    }

}
