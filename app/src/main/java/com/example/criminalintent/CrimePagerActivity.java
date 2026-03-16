package com.example.criminalintent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import java.util.UUID;

public class CrimePagerActivity extends AppCompatActivity implements CrimeFragment.Callbacks {

    private static final String EXTRA_CRIME_ID =
            "com.bignerdranch.android.criminalintent.crime_id";

    private ViewPager mViewPager;
    private MaterialButton mJumpToFirstButton;
    private MaterialButton mJumpToLastButton;
    private List<Crime> mCrimes;

    public static Intent newIntent(Context packageContext, UUID crimeId) {
        Intent intent = new Intent(packageContext, CrimePagerActivity.class);
        intent.putExtra(EXTRA_CRIME_ID, crimeId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crime_pager);

        MaterialToolbar toolbar = findViewById(R.id.pager_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        UUID crimeId = SerializationCompat.getSerializableExtra(getIntent(), EXTRA_CRIME_ID, UUID.class);

        mJumpToFirstButton = findViewById(R.id.jump_to_first);
        mJumpToLastButton = findViewById(R.id.jump_to_last);
        mViewPager = (ViewPager) findViewById(R.id.crime_view_pager);
        mCrimes = CrimeLab.get(this).getCrimes();
        FragmentManager fragmentManager = getSupportFragmentManager();

        mViewPager.setAdapter(new FragmentStatePagerAdapter(
                fragmentManager,
                FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            @Override
            public Fragment getItem(int position) {
                Crime crime = mCrimes.get(position);
                return CrimeFragment.newInstance(crime.getId());
            }

            @Override
            public int getCount() {
                return mCrimes.size();
            }
        });

        mJumpToFirstButton.setOnClickListener(v -> mViewPager.setCurrentItem(0, false));
        mJumpToLastButton.setOnClickListener(v -> {
            if (!mCrimes.isEmpty()) {
                mViewPager.setCurrentItem(mCrimes.size() - 1, false);
            }
        });
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateJumpButtons(position);
            }
        });

        if (crimeId != null) {
            for (int i = 0; i < mCrimes.size(); i++) {
                if (mCrimes.get(i).getId().equals(crimeId)) {
                    mViewPager.setCurrentItem(i, false);
                    break;
                }
            }
        }

        updateJumpButtons(mViewPager.getCurrentItem());
    }

    @Override
    public Intent getParentActivityIntent() {
        Intent intent = super.getParentActivityIntent();
        if (intent == null) {
            return null;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    private void updateJumpButtons(int position) {
        int lastIndex = mCrimes.size() - 1;
        boolean hasCrimes = lastIndex >= 0;

        mJumpToFirstButton.setEnabled(hasCrimes && position > 0);
        mJumpToLastButton.setEnabled(hasCrimes && position < lastIndex);
    }

    @Override
    public void onCrimeUpdated(Crime crime) {
    }

    @Override
    public void onCrimeDeleted(Crime crime) {
        finish();
    }
}
