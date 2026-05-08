package com.bignerdranch.android.criminalintent

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.util.Calendar
import java.util.Date

private const val ARG_TIME = "time"

class TimePickerFragment : DialogFragment() {

    companion object {
        const val EXTRA_TIME = "com.bignerdranch.android.criminalintent.time"

        fun newInstance(time: Date): TimePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_TIME, time)
            }

            return TimePickerFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val date = arguments?.getSerializable(ARG_TIME) as Date
        val calendar = Calendar.getInstance()
        calendar.time = date
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val v = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_time, null)

        val timePicker = v.findViewById<TimePicker>(R.id.dialog_time_picker)
        timePicker.hour = hour
        timePicker.minute = minute

        return AlertDialog.Builder(requireContext())
            .setView(v)
            .setTitle(R.string.time_picker_title)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val hour = timePicker.hour
                val minute = timePicker.minute
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                val time = calendar.time
                sendResult(Activity.RESULT_OK, time)
            }
            .create()
    }

    private fun sendResult(resultCode: Int, time: Date) {
        targetFragment?.let { fragment ->
            val intent = Intent().apply {
                putExtra(EXTRA_TIME, time)
            }
            fragment.onActivityResult(targetRequestCode, resultCode, intent)
        }
    }
}
