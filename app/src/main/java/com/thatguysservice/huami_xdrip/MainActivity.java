package com.thatguysservice.huami_xdrip;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SearchEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.thatguysservice.huami_xdrip.databinding.ActivityMainBinding;
import com.thatguysservice.huami_xdrip.models.BgData;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.repository.BgDataRepository;
import com.thatguysservice.huami_xdrip.services.BroadcastService;

public class MainActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();
    Runnable updater;
    ActivityMainBinding binding;
    private BroadcastReceiver newDataReceiver;
    private BgData bgData;
    private Handler timerHandler;
    private MainActivity mActivity;

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If you don't have res/menu, just create a directory named "menu" inside res
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_view_log:
                intent = new Intent(this, SendFeedBackActiviy.class);
                startActivity(intent);
                return true;
            case R.id.action_view_about:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        mActivity = this;
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_fragment, new SettingsFragment())
                    .commit();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());


        FloatingActionButton fab = findViewById(R.id.fab);
        binding.setFabVisibility(BroadcastService.shouldServiceRun());
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsFragment fragment = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settings_fragment);
                assert fragment != null;
                fragment.updateMiBandBG(mActivity);
               /* Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
            }
        });


        timerHandler = new Handler();
        updater = new Runnable() {
            @Override
            public void run() {
                refreshBGLine();
                timerHandler.postDelayed(updater, Constants.MINUTE_IN_MS);
            }
        };

        // BgDataModel model = new ViewModelProvider(this).get(BgDataModel.class);
        BgDataRepository bgDataRepository = BgDataRepository.getInstance();

        // Create the observer which updates the UI.
        final Observer<BgData> bgDataObserver = new Observer<BgData>() {
            @Override
            public void onChanged(@Nullable final BgData bg) {
                // Update the UI, in this case, a TextView.
                bgData = bg;
                timerHandler.post(updater);
            }
        };

        bgDataRepository.getBgData().observe(this, bgDataObserver);

        // Create the observer which updates the UI.
        final Observer<String> connectionStatusObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String status) {
                binding.setConnectionState(status);
            }
        };

        bgDataRepository.getStatusData().observe(this, connectionStatusObserver);


        // Create the observer which updates the UI.
        final Observer<Boolean> serviceStateObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable final Boolean status) {
                binding.setFabVisibility(status);
            }
        };

        bgDataRepository.getServiceStatus().observe(this, serviceStateObserver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.GET_EXTERNAL_STORAGE_WRITE_PERMISSION) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            } else {
                Helper.static_toast_long("Application need permission to write watchface");
            }
        }
    }

    @Override
    public boolean onPictureInPictureRequested() {
        return super.onPictureInPictureRequested();
    }

    @Override
    public boolean onSearchRequested(@Nullable SearchEvent searchEvent) {
        return super.onSearchRequested(searchEvent);
    }

    @Override
    public boolean onSearchRequested() {
        return super.onSearchRequested();
    }

    public String getMinutesAgo(long msSince, boolean includeWords) {
        final int minutes = ((int) (msSince / Constants.MINUTE_IN_MS));
        return Integer.toString(minutes) + (includeWords ? (((minutes == 1) ? HuamiXdrip.getAppContext().getString(R.string.space_minute_ago) : HuamiXdrip.getAppContext().getString(R.string.space_minutes_ago))) : "");
    }

    private void refreshBGLine() {
        if (bgData == null) return;
        try {
            TextView bgDataView = findViewById(R.id.bgDataTextView);
            bgDataView.setText(bgData.unitizedBgValue());
            if (bgData.isStale()) {
                bgDataView.setPaintFlags(bgDataView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                bgDataView.setPaintFlags(bgDataView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }

            TextView bgDataArrowView = findViewById(R.id.bgDataArrowTextView);
            bgDataArrowView.setText(bgData.getSlopeArrow());

            TextView bgDataTimeView = findViewById(R.id.bgDataTimeTextView);
            long msSince = Helper.msSince(bgData.getTimeStamp());
            bgDataTimeView.setText(getMinutesAgo(msSince, true));
            Log.d(TAG, "Refresh bg Line: " + bgData.unitizedBgValue());
        } catch (Exception e) {
            UserError.Log.e(TAG, "refreshBGLine exception: " + e);
        }
    }

    @Override
    public void onPause() {

        Log.d(TAG, "onPause");
        super.onPause();
        timerHandler.removeCallbacks(updater);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        timerHandler.post(updater);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        HuamiXdrip.getAppContext().stopService(new Intent(HuamiXdrip.getAppContext(), BroadcastService.class));
        super.onDestroy();
    }
}
