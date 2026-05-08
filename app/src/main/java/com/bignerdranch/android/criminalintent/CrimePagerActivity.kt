package com.bignerdranch.android.criminalintent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import java.util.UUID

class CrimePagerActivity : AppCompatActivity(), CrimeFragment.Callbacks {

    private lateinit var viewPager: ViewPager
    private lateinit var crimes: List<Crime>
    private lateinit var jumpToFirstButton: Button
    private lateinit var jumpToLastButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crime_pager)

        val crimeId = intent.getSerializableExtra(EXTRA_CRIME_ID) as UUID

        viewPager = findViewById(R.id.crime_view_pager)
        jumpToFirstButton = findViewById(R.id.jump_to_first_button)
        jumpToLastButton = findViewById(R.id.jump_to_last_button)

        crimes = CrimeLab.get(this).getCrimes()

        val fragmentManager = supportFragmentManager
        viewPager.adapter = object : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                val crime = crimes[position]
                return CrimeFragment.newInstance(crime.id)
            }

            override fun getCount(): Int {
                return crimes.size
            }
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                updateButtons(position)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        jumpToFirstButton.setOnClickListener {
            viewPager.currentItem = 0
        }

        jumpToLastButton.setOnClickListener {
            viewPager.currentItem = crimes.size - 1
        }

        for (i in crimes.indices) {
            if (crimes[i].id == crimeId) {
                viewPager.currentItem = i
                updateButtons(i)
                break
            }
        }
    }

    override fun onCrimeUpdated(crime: Crime) {
        // Optional: Implement if pager needs to react to crime updates
    }

    private fun updateButtons(position: Int) {
        jumpToFirstButton.isEnabled = position > 0
        jumpToLastButton.isEnabled = position < crimes.size - 1
    }

    companion object {
        private const val EXTRA_CRIME_ID = "com.bignerdranch.android.criminalintent.crime_id"

        fun newIntent(packageContext: Context, crimeId: UUID): Intent {
            val intent = Intent(packageContext, CrimePagerActivity::class.java)
            intent.putExtra(EXTRA_CRIME_ID, crimeId)
            return intent
        }
    }
}
