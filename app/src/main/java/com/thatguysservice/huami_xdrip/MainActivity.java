package com.thatguysservice.huami_xdrip;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.thatguysservice.huami_xdrip.databinding.ActivityMainBinding;
import com.thatguysservice.huami_xdrip.models.BgData;
import com.thatguysservice.huami_xdrip.models.Constants;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.PersistantDeviceInfo;
import com.thatguysservice.huami_xdrip.models.PersistantDevices;
import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.repository.BgDataRepository;
import com.thatguysservice.huami_xdrip.services.BroadcastService;
import com.thatguysservice.huami_xdrip.watch.miband.MiBand;
import com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.thatguysservice.huami_xdrip.HuamiXdrip.gs;
import static com.thatguysservice.huami_xdrip.models.Constants.REQUEST_ID_BLUETOOTH_PERMISSIONS;
import static com.thatguysservice.huami_xdrip.models.Constants.REQUEST_ID_READ_WRITE_PERMISSIONS;
import static com.thatguysservice.huami_xdrip.watch.miband.MiBandEntry.isLoggingEnabled;

public class MainActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, EasyPermissions.PermissionCallbacks {
    private final String TAG = this.getClass().getSimpleName();
    Runnable updater;
    ActivityMainBinding binding;
    private BgData bgData;
    private Handler timerHandler;
    private MainActivity mActivity;

    private PersistantDevices devices;
    private MenuItem removeDeviceItem;
    private MenuItem addDeviceItem;
    private MenuItem renameDeviceItem;
    private BgDataRepository bgDataRepository;
    private MenuItem viewLog;

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If you don't have res/menu, just create a directory named "menu" inside res
        getMenuInflater().inflate(R.menu.main_menu, menu);

        removeDeviceItem = menu.findItem(R.id.action_remove_device);
        addDeviceItem = menu.findItem(R.id.action_add_device);
        renameDeviceItem = menu.findItem(R.id.action_rename_device);
        viewLog = menu.findItem(R.id.action_view_log);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        handleMenusVisibility();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        FragmentManager fm;
        PersistantDeviceInfo device;
        switch (item.getItemId()) {
            case R.id.action_view_log:
                intent = new Intent(this, SendFeedBackActiviy.class);
                startActivity(intent);
                return true;
            case R.id.action_wf_store:
                intent = new Intent(this, WatchStoreActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_view_about:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_get_statistic:
                Helper.startService(BroadcastService.class, BroadcastService.INTENT_FUNCTION_KEY, BroadcastService.CMD_STAT_INFO);
                return true;
            case R.id.action_add_device:
                fm = getSupportFragmentManager();
                EditDeviceNameDialogFragment addFragment = EditDeviceNameDialogFragment.newInstance("Device name");
                addFragment.setDialogResultListener(new EditDeviceNameDialogFragment.OnDialogResultListener() {
                    @Override
                    public void onDialogResult(String deviceName) {
                        if (!deviceName.isEmpty()) {
                            PersistantDeviceInfo device = new PersistantDeviceInfo(deviceName, "", "");
                            devices.addDevice(device);
                            int newActiveIndex = devices.count() - 1;
                            MiBand.setMacPref(device.getMacAddress(), bgDataRepository);
                            MiBand.setAuthKeyPref(device.getAuthKey(), bgDataRepository);
                            bgDataRepository.updateActiveDeviceData(newActiveIndex);
                            handleMenusVisibility();
                        }
                    }
                });
                addFragment.show(fm, "EditDeviceNameFragment");
                return true;
            case R.id.action_remove_device:
                Helper.show_ok_dialog(this, gs(R.string.action_remove_device), gs(R.string.confirm_remove_message), new Runnable() {
                    @Override
                    public void run() {
                        devices.removeDevice(MiBandEntry.getActiveDeviceIndex());
                        int newActiveIndex = devices.count() - 1;
                        PersistantDeviceInfo deviceInner = devices.getDeviceByIndex(newActiveIndex);
                        MiBand.setMacPref(deviceInner.getMacAddress(), bgDataRepository);
                        MiBand.setAuthKeyPref(deviceInner.getAuthKey(), bgDataRepository);
                        handleMenusVisibility();
                        bgDataRepository.updateActiveDeviceData(newActiveIndex);
                    }
                }, false);

                return true;
            case R.id.action_rename_device:
                fm = getSupportFragmentManager();
                int deviceEntryIndex = MiBandEntry.getActiveDeviceIndex();
                device = devices.getDeviceByIndex(deviceEntryIndex);
                if (device == null) break;
                EditDeviceNameDialogFragment editFragment = EditDeviceNameDialogFragment.newInstance(device.getName());
                editFragment.setDialogResultListener(new EditDeviceNameDialogFragment.OnDialogResultListener() {
                    @Override
                    public void onDialogResult(String deviceName) {
                        if (!deviceName.isEmpty()) {
                            device.setName(deviceName);
                            devices.updateDevice(device, deviceEntryIndex);
                            handleMenusVisibility();
                            bgDataRepository.updateActiveDeviceData(deviceEntryIndex);
                        }
                    }
                });
                editFragment.show(fm, "EditDeviceNameFragment");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleMenusVisibility() {

        viewLog.setVisible(isLoggingEnabled());

        if (removeDeviceItem == null || addDeviceItem == null) return;

        if (MiBandEntry.isDeviceEnabled() && devices != null) {
            boolean res = (devices.count() > 1) ? true : false;
            removeDeviceItem.setVisible(res);
            renameDeviceItem.setVisible(res);
            addDeviceItem.setVisible(true);
        } else {
            removeDeviceItem.setVisible(false);
            renameDeviceItem.setVisible(false);
            addDeviceItem.setVisible(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        mActivity = this;
        super.onCreate(savedInstanceState);
        devices = MiBand.getDevices();
        if (devices.count() == 0) {
            //add first device
            PersistantDeviceInfo device = new PersistantDeviceInfo("Default Device", MiBand.getMacPref(), MiBand.getAuthKeyPref());
            devices.addDevice(device);
        }

        //setContentView(R.layout.activity_main);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                    .replace(R.id.settings_fragment, new SettingsFragment())
                    .commit();
        }
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());

        handleFragmentBackButton(toolbar);

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                handleFragmentBackButton(toolbar);
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        binding.setFabVisibility(BroadcastService.shouldServiceRun());
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateMiBandBG(mActivity);
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
        bgDataRepository = BgDataRepository.getInstance();

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
                binding.setFabVisibility(MiBandEntry.isWebServerEnabled() || MiBandEntry.isDeviceEnabled());
                handleMenusVisibility();
            }
        };

        bgDataRepository.getServiceStatus().observe(this, serviceStateObserver);
    }

    private void handleFragmentBackButton(Toolbar toolbar) {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);// show back button
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        } else {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    public void updateMiBandBG(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(HuamiXdrip.getAppContext().getResources().getString(R.string.miband_bg_dialog_title));
        builder.setPositiveButton(HuamiXdrip.getAppContext().getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                BroadcastService.initialStartIfEnabled();
            }
        });

        builder.setNegativeButton(HuamiXdrip.getAppContext().getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            // Do something after user returned from app settings screen, like showing a Toast.
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
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
       // devices = MiBand.getDevices();
        super.onResume();
        timerHandler.post(updater);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        HuamiXdrip.getAppContext().stopService(new Intent(HuamiXdrip.getAppContext(), BroadcastService.class));
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.settings_fragment, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

    @AfterPermissionGranted(REQUEST_ID_READ_WRITE_PERMISSIONS)
    private void readWritePermissionsGranted() {
        Log.d(TAG, "readWritePermissionsGranted");
        SettingsAdvancedFragment settingsFragment = (SettingsAdvancedFragment) getSupportFragmentManager().findFragmentById(R.id.settings_fragment);
        settingsFragment.setWatchfacePref(true);
    }

    @AfterPermissionGranted(REQUEST_ID_BLUETOOTH_PERMISSIONS)
    private void bluetoothPermissionsGranted() {
        Log.d(TAG, "bluetoothPermissionsGranted");
        SettingsFragment settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.settings_fragment);
        settingsFragment.setDevicePref(true);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsGranted " + perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsDenied " + perms);
        // Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(mActivity).setRationale(R.string.permission_dialog_enable_permissions_in_settings).build().show();
        }
    }
}
