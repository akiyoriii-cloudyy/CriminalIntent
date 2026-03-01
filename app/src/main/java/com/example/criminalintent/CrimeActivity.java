package com.example.criminalintent;

import android.content.Intent;
import java.util.UUID;
import androidx.fragment.app.Fragment;

public class CrimeActivity extends SingleFragmentActivity {
    
    @SuppressWarnings("SpellCheckingInspection")
    private static final String EXTRA_CRIME_ID = "com.bignerdranch.android.criminalintent.crime_id";

    @Override
    public Fragment createFragment() {
        UUID crimeId = getIntent().getSerializableExtra(EXTRA_CRIME_ID, UUID.class);
        return CrimeFragment.newInstance(crimeId);
    }
}
