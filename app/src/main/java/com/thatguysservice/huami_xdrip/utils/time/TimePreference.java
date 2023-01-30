package com.thatguysservice.huami_xdrip.utils.time;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import com.thatguysservice.huami_xdrip.R;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TimePreference extends DialogPreference {
    private Calendar calendar;

    public TimePreference(Context ctxt) {
        this(ctxt, null);
    }

    public TimePreference(Context ctxt, AttributeSet attrs) {
        this(ctxt, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public TimePreference(Context ctxt, AttributeSet attrs, int defStyle) {
        super(ctxt, attrs, defStyle);

        setPositiveButtonText(R.string.set_value);
        setNegativeButtonText(R.string.close_value);
        calendar = new GregorianCalendar();
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public Calendar setCalendar(Calendar calendar) {
        return this.calendar = calendar;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            if (defaultValue == null) {
                calendar.setTimeInMillis(getPersistedLong(System.currentTimeMillis()));
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(getPersistedString((String) defaultValue)));
                calendar.set(Calendar.MINUTE, 0);
            }
        } else {
            if (defaultValue == null) {
                calendar.setTimeInMillis(System.currentTimeMillis());
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(getPersistedString((String) defaultValue)));
                calendar.set(Calendar.MINUTE, 0);
            }
            persistCalendarValue();
        }
        updateSummary();
    }

    @Override
    public CharSequence getSummary() {
        if (calendar == null) {
            return null;
        }
        return DateFormat.getTimeFormat(getContext()).format(new Date(calendar.getTimeInMillis()));
    }

    public void updateSummary() {
        setSummary(getSummary());
    }

    public void persistCalendarValue() {
        persistLong(calendar.getTimeInMillis());
    }
}
