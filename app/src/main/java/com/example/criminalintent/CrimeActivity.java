package com.example.criminalintent;

import android.content.Context;
import android.content.Intent;
import androidx.fragment.app.Fragment;
import java.util.UUID;

public class CrimeActivity extends SingleFragmentActivity implements CrimeFragment.Callbacks {
    
    @SuppressWarnings("SpellCheckingInspection")
    private static final String EXTRA_CRIME_ID = "com.bignerdranch.android.criminalintent.crime_id";

    public static Intent newIntent(Context packageContext, UUID crimeId) {
        Intent intent = new Intent(packageContext, CrimeActivity.class);
        intent.putExtra(EXTRA_CRIME_ID, crimeId);
        return intent;
    }

    @Override
    public Fragment createFragment() {
        UUID crimeId = SerializationCompat.getSerializableExtra(getIntent(), EXTRA_CRIME_ID, UUID.class);
        if (crimeId == null) {
            Crime crime = new Crime();
            CrimeLab.get(this).addCrime(crime);
            crimeId = crime.getId();
        }
        return CrimeFragment.newInstance(crimeId);
    }

    @Override
    public void onCrimeUpdated(Crime crime) {
    }

    @Override
    public void onCrimeDeleted(Crime crime) {
        finish();
    }
}
