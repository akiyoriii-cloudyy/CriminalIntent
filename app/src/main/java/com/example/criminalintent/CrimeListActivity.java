package com.example.criminalintent;

import android.content.Intent;
import android.view.View;
import androidx.fragment.app.Fragment;

public class CrimeListActivity extends SingleFragmentActivity
        implements CrimeListFragment.Callbacks, CrimeFragment.Callbacks {
    @Override
    public Fragment createFragment() {
        return new CrimeListFragment();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_masterdetail;
    }

    @Override
    public void onCrimeSelected(Crime crime) {
        if (findViewById(R.id.detail_fragment_container) == null) {
            Intent intent = CrimePagerActivity.newIntent(this, crime.getId());
            startActivity(intent);
            return;
        }

        Fragment newDetail = CrimeFragment.newInstance(crime.getId());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.detail_fragment_container, newDetail)
                .setReorderingAllowed(true)
                .commit();
    }

    @Override
    public void onCrimeDeleted(Crime crime) {
        CrimeListFragment listFragment = (CrimeListFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (listFragment != null) {
            listFragment.updateUI();
        }

        View detailContainer = findViewById(R.id.detail_fragment_container);
        if (detailContainer == null) {
            return;
        }

        Fragment detailFragment = getSupportFragmentManager().findFragmentById(R.id.detail_fragment_container);
        if (!(detailFragment instanceof CrimeFragment)) {
            return;
        }

        CrimeFragment crimeFragment = (CrimeFragment) detailFragment;
        if (crime.getId().equals(crimeFragment.getCrimeId())) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(detailFragment)
                    .commit();
        }
    }

    @Override
    public void onCrimeUpdated(Crime crime) {
        CrimeListFragment listFragment = (CrimeListFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (listFragment != null) {
            listFragment.updateUI();
        }
    }
}
