package com.thatguysservice.huami_xdrip.utils.time;

import android.content.Context;
import android.view.View;
import android.widget.TimePicker;

import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class TimePreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat  implements DialogPreference.TargetFragment{
    private TimePicker picker = null;

    @Override
    protected View onCreateDialogView(Context context) {
        picker = new TimePicker(context);
        return picker;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        TimePreference pref = (TimePreference) getPreference();
        Calendar calendar = pref.getCalendar();
        picker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
        picker.setCurrentMinute(calendar.get(Calendar.MINUTE));
    }


    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            TimePreference preference = (TimePreference) getPreference();

            Calendar calendar = new GregorianCalendar();
            calendar.set(Calendar.HOUR_OF_DAY, picker.getCurrentHour());
            calendar.set(Calendar.MINUTE, picker.getCurrentMinute());
            if (preference.callChangeListener(calendar.getTimeInMillis())) {
                preference.setCalendar(calendar);
                preference.persistCalendarValue();
                preference.updateSummary();
            }
        }
    }

    @Nullable
    @Override
    public Preference findPreference(CharSequence key) {
        return getPreference();
    }
}
