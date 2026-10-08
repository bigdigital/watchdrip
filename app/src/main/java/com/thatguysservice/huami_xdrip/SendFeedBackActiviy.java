package com.thatguysservice.huami_xdrip;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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

        final boolean collectLogs = (log_data == null || log_data.isEmpty())
                && ((CheckBox) findViewById(R.id.sendLogsCheckBox)).isChecked();

        final OkHttpClient client = new OkHttpClient();

        client.setConnectTimeout(10, TimeUnit.SECONDS);
        client.setReadTimeout(30, TimeUnit.SECONDS);
        client.setWriteTimeout(30, TimeUnit.SECONDS);

        PersistentStore.setString(FEEDBACK_CONTACT, contact.getText().toString());
        Helper.static_toast_short("Sending...");

        try {
            final String contactText = contact.getText().toString();
            final String issueText = yourtext.getText().toString();
            final String deviceHeader = Helper.getDeviceDetails() + "\n Version " + Helper.getVersionDetails() + "\n"
                    + "Device: " + MiBand.getMibandType() + "\n Device version: " + MiBand.getVersion() + "\n"
                    + "===\n\n";
            new Thread(() -> {
                try {
                    // Reading thousands of log rows is slow, so keep it off the UI thread
                    if (collectLogs) {
                        log_data = collectLogData();
                    }
                    final String data = deviceHeader + issueText + " \n\n===\nType: " + type_of_message + "\nLog data:\n\n" + log_data + "\n\n\nSent: " + Helper.dateTimeText(Helper.tsl());
                    final RequestBody formBody = new FormEncodingBuilder()
                            .add("contact", contactText)
                            .add("body", data)
                            .add("type", type_of_message)
                            .build();
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
                } catch (Throwable e) {
                    Log.e(TAG, "Got exception in execute: " + e.toString());
                    Helper.static_toast_short("Error sending feedback: " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            Helper.static_toast_short(e.getMessage());
            Log.e(TAG, "General exception: " + e.toString());
        }
    }

    private final ActivityResultLauncher<String> saveLogLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/plain"), uri -> {
                if (uri == null) {
                    return; // cancelled by the user
                }
                new Thread(() -> {
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                        final String text = Helper.getDeviceDetails() + "\n Version " + Helper.getVersionDetails() + "\n"
                                + "Device: " + MiBand.getMibandType() + "\n Device version: " + MiBand.getVersion() + "\n"
                                + "===\n\n" + collectLogData();
                        out.write(text.getBytes(StandardCharsets.UTF_8));
                        Helper.static_toast_long(getString(R.string.log_saved));
                    } catch (Throwable e) {
                        Log.e(TAG, "Cannot save log: " + e);
                        Helper.static_toast_long(getString(R.string.log_save_error) + ": " + e.getMessage());
                    }
                }).start();
            });

    public void saveLogToFile(View view) {
        saveLogLauncher.launch("watchdrip_log_" + Helper.tsl() + ".txt");
    }

    private String collectLogData() {
        final Integer[] severities = {3, 2, 1, 5, 6};
        final StringBuilder tmp = new StringBuilder(20000);
        for (UserError item : UserError.bySeverity(severities)) {
            tmp.append(item.toString());
            tmp.append("\n");
            if (tmp.length() > 200000) {
                Helper.static_toast(this, "Could not package up all logs, using most recent", Toast.LENGTH_LONG);
                break;
            }
        }
        return tmp.toString();
    }
}
