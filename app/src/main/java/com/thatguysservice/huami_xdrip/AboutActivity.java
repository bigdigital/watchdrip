package com.thatguysservice.huami_xdrip;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);


        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        TextView about_version = findViewById(R.id.about_version);
        TextView about_build = findViewById(R.id.about_build_timestamp);
        String versionName = BuildConfig.VERSION_NAME;
        long versionTimestamp = BuildConfig.buildTimestamp;
        about_version.setText(String.format(getString(R.string.about_version), versionName));

        Date date = new Date(versionTimestamp);
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        about_build.setText(sd.format(date));
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish(); //this method close current activity and return to previous
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
