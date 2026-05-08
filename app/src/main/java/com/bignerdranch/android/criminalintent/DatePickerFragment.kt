package com.bignerdranch.android.criminalintent

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

private const val ARG_DATE = "date"

class DatePickerFragment : DialogFragment() {

    companion object {
        const val EXTRA_DATE = "com.bignerdranch.android.criminalintent.date"

        fun newInstance(date: Date): DatePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }

            return DatePickerFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.dialog_date, container, false)

        val date = arguments?.getSerializable(ARG_DATE) as Date
        val calendar = Calendar.getInstance()
        calendar.time = date
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = v.findViewById<DatePicker>(R.id.dialog_date_picker)
        datePicker.init(year, month, day, null)

        val okButton = v.findViewById<Button>(R.id.dialog_date_ok_button)
        okButton.setOnClickListener {
            val year = datePicker.year
            val month = datePicker.month
            val day = datePicker.dayOfMonth
            val date = GregorianCalendar(year, month, day).time
            sendResult(Activity.RESULT_OK, date)
        }

        return v
    }

    private fun sendResult(resultCode: Int, date: Date) {
        val intent = Intent().apply {
            putExtra(EXTRA_DATE, date)
        }

        if (targetFragment != null) {
            targetFragment?.onActivityResult(targetRequestCode, resultCode, intent)
            dismiss()
        } else {
            activity?.setResult(resultCode, intent)
            activity?.finish()
        }
    }
}
