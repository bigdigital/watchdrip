package com.thatguysservice.huami_xdrip;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
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
import androidx.fragment.app.DialogFragment;
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
import com.thatguysservice.huami_xdrip.models.UserError;
import com.thatguysservice.huami_xdrip.repository.BgDataRepository;
import com.thatguysservice.huami_xdrip.services.BroadcastService;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import static com.thatguysservice.huami_xdrip.models.Constants.REQUEST_ID_BLUETOOTH_PERMISSIONS;
import static com.thatguysservice.huami_xdrip.models.Constants.REQUEST_ID_READ_WRITE_PERMISSIONS;

public class MainActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, EasyPermissions.PermissionCallbacks {
    private final String TAG = this.getClass().getSimpleName();
    Runnable updater;
    ActivityMainBinding binding;
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


 /*   public void onRequestPermissionsResulta(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "Permission callback called-------");
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {

                Map<String, Integer> perms = new HashMap<>();
                // Initialize the map with both permissions
                perms.put(Manifest.permission.SEND_SMS, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "sms & location services permission granted");
                        // process the normal flow
                        //else any one or both the permissions are not granted
                    } else {
                        Log.d(TAG, "Some permissions are not granted ask again ");
                        //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
//                        // shouldShowRequestPermissionRationale will return true
                        //show the dialog or snackbar saying its necessary and try again otherwise proceed with setup.
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                            showDialogOK("SMS and Location Services Permission required for this app",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    checkAndRequestPermissions();
                                                    break;
                                                case DialogInterface.BUTTON_NEGATIVE:
                                                    // proceed with logic by disabling the related features or quit the app.
                                                    break;
                                            }
                                        }
                                    });
                        }
                        //permission is denied (and never ask again is  checked)
                        //shouldShowRequestPermissionRationale will return false
                        else {
                            Toast.makeText(this, "Go to settings and enable permissions", Toast.LENGTH_LONG)
                                    .show();
                            //                            //proceed with logic by disabling the related features or quit the app.
                        }
                    }
                }
            }
        }

    }
*/


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
        settingsFragment.setServicePref(true);
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
