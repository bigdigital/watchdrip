package com.thatguysservice.huami_xdrip.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.thatguysservice.huami_xdrip.R;
import com.thatguysservice.huami_xdrip.models.database.UserError;

import java.text.DateFormat;
import java.util.Date;

public class ErrorListAdapter extends ArrayAdapter<UserError> {

    public ErrorListAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.item_error, null);
        }
        LinearLayout row = view.findViewById(R.id.errorRow);
        TextView shortText = view.findViewById(R.id.errorShort);
        TextView longText = view.findViewById(R.id.errorLong);
        TextView timestamp = view.findViewById(R.id.errorTimestamp);

        UserError error = getItem(position);

        row.setBackgroundColor(backgroundFor(error.severity));
        shortText.setText(error.shortError);
        longText.setText(error.message);
        timestamp.setText(dateformatter(error.timestamp));
        return view;
    }

    private int backgroundFor(int severity) {
        switch (severity) {
            case 1:
                return Color.rgb(255, 204, 102);
            case 2:
                return Color.rgb(255, 153, 102);
        }
        return Color.rgb(255, 102, 102);
    }

    private String dateformatter(double timestamp) {
        Date date = new Date((long) timestamp);
        DateFormat format = DateFormat.getDateTimeInstance();
        return format.format(date);
    }
}
