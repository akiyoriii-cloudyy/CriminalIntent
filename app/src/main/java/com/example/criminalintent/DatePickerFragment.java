package com.example.criminalintent;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DatePickerFragment extends DialogFragment {

    private static final String ARG_DATE = "date";
    private static final String ARG_REQUEST_KEY = "request_key";
    private static final String ARG_RESULT_KEY = "result_key";
    private DatePicker mDatePicker;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Date date = SerializationCompat.getSerializable(getArguments(), ARG_DATE, Date.class);
        if (date == null) {
            date = new Date();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_date, null);
        mDatePicker = (DatePicker) view.findViewById(R.id.dialog_date_picker);
        mDatePicker.init(year, month, day, null);

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(R.string.date_picker_title)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int pickedYear = mDatePicker.getYear();
                    int pickedMonth = mDatePicker.getMonth();
                    int pickedDay = mDatePicker.getDayOfMonth();
                    Date pickedDate = new GregorianCalendar(pickedYear, pickedMonth, pickedDay).getTime();
                    sendResult(pickedDate);
                })
                .create();
    }

    public static DatePickerFragment newInstance(Date date, String requestKey, String resultKey) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATE, date);
        args.putString(ARG_REQUEST_KEY, requestKey);
        args.putString(ARG_RESULT_KEY, resultKey);
         DatePickerFragment fragment = new DatePickerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void sendResult(Date date) {
        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }

        String requestKey = arguments.getString(ARG_REQUEST_KEY);
        String resultKey = arguments.getString(ARG_RESULT_KEY);
        if (requestKey == null || resultKey == null) {
            return;
        }

        Bundle result = new Bundle();
        result.putSerializable(resultKey, date);
        getParentFragmentManager().setFragmentResult(requestKey, result);
    }
}
