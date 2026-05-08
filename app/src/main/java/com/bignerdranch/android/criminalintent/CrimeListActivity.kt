package com.bignerdranch.android.criminalintent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import java.util.UUID

class CrimeListActivity : SingleFragmentActivity(), CrimeListFragment.Callbacks, CrimeFragment.Callbacks {

    override fun createFragment(): Fragment {
        return CrimeListFragment()
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_masterdetail
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // If we are in two-pane mode and no detail is showing, show the empty message
        if (findViewById<View>(R.id.detail_fragment_container) != null) {
            val detailFragment = supportFragmentManager.findFragmentById(R.id.detail_fragment_container)
            if (detailFragment == null) {
                val emptyFragment = EmptyDetailFragment()
                supportFragmentManager.beginTransaction()
                    .add(R.id.detail_fragment_container, emptyFragment)
                    .commit()
            }
        }
    }

    override fun onCrimeSelected(crime: Crime) {
        if (findViewById<View>(R.id.detail_fragment_container) == null) {
            val intent = CrimePagerActivity.newIntent(this, crime.id)
            startActivity(intent)
        } else {
            val newDetail = CrimeFragment.newInstance(crime.id)
            supportFragmentManager.beginTransaction()
                .replace(R.id.detail_fragment_container, newDetail)
                .commit()
        }
    }

    override fun onCrimeUpdated(crime: Crime) {
        val listFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as CrimeListFragment
        listFragment.updateUI()
    }

    companion object {
        const val EXTRA_CRIME_ID = "com.bignerdranch.android.criminalintent.crime_id"

        fun newIntent(packageContext: Context?, crimeId: UUID?): Intent {
            val intent = Intent(packageContext, CrimeListActivity::class.java)
            intent.putExtra(EXTRA_CRIME_ID, crimeId)
            return intent
        }
    }
}
