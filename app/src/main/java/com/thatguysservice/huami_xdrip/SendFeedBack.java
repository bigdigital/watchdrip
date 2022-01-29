package com.thatguysservice.huami_xdrip;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.thatguysservice.huami_xdrip.models.Helper;
import com.thatguysservice.huami_xdrip.models.PersistentStore;

import java.util.concurrent.TimeUnit;

public class SendFeedBack extends AppCompatActivity {
    private static final String TAG = "SendFeedBack";
    private static final String FEEDBACK_CONTACT = "feedback-contact";
    private static final String FEEDBACK_SERVER_DIRECTORY = "/xdrip/debug-logs.php";
    EditText contact;
    private String type_of_message = "Unknown";
    private String send_url;
    private String log_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_feed_back);

        send_url = getString(R.string.wserviceurl) + FEEDBACK_SERVER_DIRECTORY;
        contact = findViewById(R.id.contactTextField);
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

    public void sendFeedback(View myview) {
        final EditText contact = findViewById(R.id.contactTextField);
        final EditText yourtext = findViewById(R.id.issueTextField);
        final OkHttpClient client = new OkHttpClient();

        client.setConnectTimeout(10, TimeUnit.SECONDS);
        client.setReadTimeout(30, TimeUnit.SECONDS);
        client.setWriteTimeout(30, TimeUnit.SECONDS);

        PersistentStore.setString(FEEDBACK_CONTACT, contact.getText().toString());
        Helper.static_toast_short("Sending...");

        try {
            final RequestBody formBody = new FormEncodingBuilder()
                    .add("contact", contact.getText().toString())
                    .add("body", Helper.getDeviceDetails() + "\n" + Helper.getVersionDetails() + "\n===\n\n" + yourtext.getText().toString() + " \n\n===\nType: " + type_of_message + "\nLog data:\n\n" + log_data + "\n\n\nSent: " + Helper.dateTimeText(Helper.tsl()))
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
