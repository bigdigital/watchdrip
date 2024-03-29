package com.thatguysservice.huami_xdrip;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.PersistentStore;
import com.thatguysservice.huami_xdrip.models.database.UserError;
import com.thatguysservice.huami_xdrip.watch.miband.MiBand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SendFeedBackActiviy extends AppCompatActivity {
    private static final String FEEDBACK_CONTACT = "feedback-contact";
    private static final String FEEDBACK_SERVER_DIRECTORY = "/debug-logs.php";
    private final String TAG = this.getClass().getSimpleName();
    EditText contact;
    private String type_of_message = "Unknown";
    private String send_url;
    private String log_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_feedback);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        send_url = getString(R.string.wserviceurl) + FEEDBACK_SERVER_DIRECTORY;
        contact = findViewById(R.id.contactTextField);
        contact.setText(PersistentStore.getString(FEEDBACK_CONTACT));
        Intent intent = getIntent();
        type_of_message = "feedback";
        if (intent != null) {
            final Bundle bundle = intent.getExtras();
            if (bundle != null) {
                final String str2 = bundle.getString("generic_text");
                if (str2 != null) {
                    log_data = str2;
                    type_of_message = "Log";
                }
            }
        }
    }

 /*   @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish(); //this method close current activity and return to previous
                return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

    public void sendFeedback(View myview) {
        final EditText contact = findViewById(R.id.contactTextField);
        final EditText yourtext = findViewById(R.id.issueTextField);

        List<Integer> severitiesList = new ArrayList<>();
        // if (highCheckboxView.isChecked())
        severitiesList.add(3);
        // if (mediumCheckboxView.isChecked())
        severitiesList.add(2);
        // if (lowCheckboxView.isChecked())
        severitiesList.add(1);
        //if (userEventLowCheckboxView.isChecked())
        severitiesList.add(5);
        // if (userEventHighCheckboxView.isChecked())
        severitiesList.add(6);
        List<UserError> errors = UserError.bySeverity(severitiesList.toArray(new Integer[severitiesList.size()]));

        if (log_data == null || log_data.isEmpty()) {
            CheckBox logCheckbox = findViewById(R.id.sendLogsCheckBox);
            if (logCheckbox.isChecked()) {
                StringBuilder tmp = new StringBuilder(20000);
                for (UserError item : errors) {
                    tmp.append(item.toString());
                    tmp.append("\n");
                    if (tmp.length() > 200000) {
                        Helper.static_toast(this, "Could not package up all logs, using most recent", Toast.LENGTH_LONG);
                        break;
                    }
                }
                log_data = tmp.toString();
            }
        }

        final OkHttpClient client = new OkHttpClient();

        client.setConnectTimeout(10, TimeUnit.SECONDS);
        client.setReadTimeout(30, TimeUnit.SECONDS);
        client.setWriteTimeout(30, TimeUnit.SECONDS);

        PersistentStore.setString(FEEDBACK_CONTACT, contact.getText().toString());
        Helper.static_toast_short("Sending...");

        try {
            String data = Helper.getDeviceDetails() + "\n Version " + Helper.getVersionDetails() + "\n";
            data = data + "Device: " + MiBand.getMibandType() + "\n Device version: " + MiBand.getVersion() + "\n";
            data = data + "===\n\n";
            data = data + yourtext.getText().toString() + " \n\n===\nType: " + type_of_message + "\nLog data:\n\n" + log_data + "\n\n\nSent: " + Helper.dateTimeText(Helper.tsl());
            final RequestBody formBody = new FormEncodingBuilder()
                    .add("contact", contact.getText().toString())
                    .add("body", data)
                    .add("type", type_of_message)
                    .build();
            new Thread(() -> {
                try {
                    final Request request = new Request.Builder()
                            .url(send_url)
                            .post(formBody)
                            .build();
                    Log.i(TAG, "Sending feedback request");
                    final Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        Helper.static_toast_long(response.body().string());
                        log_data = "";
                        //Home.toaststatic("Feedback sent successfully");
                        finish();
                    } else {
                        Helper.static_toast_short("Error sending feedback: " + response.message().toString());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Got exception in execute: " + e.toString());
                    Helper.static_toast_short("Network connection error");
                }
            }).start();
        } catch (Exception e) {
            Helper.static_toast_short(e.getMessage());
            Log.e(TAG, "General exception: " + e.toString());
        }
    }
}
