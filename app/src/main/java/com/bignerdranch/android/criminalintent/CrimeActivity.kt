package com.bignerdranch.android.criminalintent

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import java.util.UUID

class CrimeActivity : SingleFragmentActivity() {

    override fun createFragment(): Fragment {
        val crimeId = intent.getSerializableExtra(EXTRA_CRIME_ID) as UUID
        return CrimeFragment.newInstance(crimeId)
    }

    companion object {
        private const val EXTRA_CRIME_ID = "com.bignerdranch.android.criminalintent.crime_id"

        fun newIntent(packageContext: Context, crimeId: UUID): Intent {
            return Intent(packageContext, CrimeActivity::class.java).apply {
                putExtra(EXTRA_CRIME_ID, crimeId)
            }
        }
    }
}