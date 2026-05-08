package com.bignerdranch.android.criminalintent

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import java.util.Date

class DatePickerActivity : SingleFragmentActivity() {

    override fun createFragment(): Fragment {
        val date = intent.getSerializableExtra(DatePickerFragment.EXTRA_DATE) as Date
        return DatePickerFragment.newInstance(date)
    }

    companion object {
        fun newIntent(packageContext: Context, date: Date): Intent {
            val intent = Intent(packageContext, DatePickerActivity::class.java)
            intent.putExtra(DatePickerFragment.EXTRA_DATE, date)
            return intent
        }
    }
}
