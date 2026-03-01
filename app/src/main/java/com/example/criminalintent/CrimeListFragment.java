package com.example.criminalintent;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CrimeListFragment extends Fragment {

    private static final String SAVED_SUBTITLE_VISIBLE = "subtitle";
    private RecyclerView mCrimeRecyclerView;
    private CrimeAdapter mAdapter;
    private boolean mSubtitleVisible;

    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_POLICE = 1;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(
            new MenuProvider() {
                @Override
                public void onCreateMenu(Menu menu, MenuInflater menuInflater) {
                    menuInflater.inflate(R.menu.fragment_crime_list, menu);
                }

                @Override
                public void onPrepareMenu(Menu menu) {
                    MenuItem subtitleItem = menu.findItem(R.id.show_subtitle);
                    if (subtitleItem != null) {
                        subtitleItem.setTitle(mSubtitleVisible
                                ? R.string.hide_subtitle
                                : R.string.show_subtitle);
                    }
                }

                @Override
                public boolean onMenuItemSelected(MenuItem menuItem) {
                    if (menuItem.getItemId() == R.id.new_crime) {
                        Crime crime = new Crime();
                        CrimeLab.get(requireActivity()).addCrime(crime);
                        startActivity(CrimePagerActivity.newIntent(requireContext(), crime.getId()));
                        return true;
                    } else if (menuItem.getItemId() == R.id.show_subtitle) {
                        mSubtitleVisible = !mSubtitleVisible;
                        updateSubtitle();
                        requireActivity().invalidateOptionsMenu();
                        return true;
                    }
                    return false;
                }
            },
            getViewLifecycleOwner(),
            Lifecycle.State.RESUMED
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crime_list, container, false);

        if (savedInstanceState != null) {
            mSubtitleVisible = savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE);
        }

        mCrimeRecyclerView = view.findViewById(R.id.crime_recycler_view);
        mCrimeRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        updateUI();

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_SUBTITLE_VISIBLE, mSubtitleVisible);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        List<Crime> crimes = crimeLab.getCrimes();

        if (mAdapter == null) {
            mAdapter = new CrimeAdapter(crimes);
            mCrimeRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
        updateSubtitle();
    }

    private void updateSubtitle() {
        int crimeCount = CrimeLab.get(getActivity()).getCrimes().size();
        String subtitle = getString(R.string.subtitle_format, crimeCount);
        if (!mSubtitleVisible) {
            subtitle = null;
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setSubtitle(subtitle);
        }
    }

    private class CrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Crime mCrime;
        private final TextView titleTextView;
        private final TextView dateTextView;
        private final ImageView solvedImageView;

        public CrimeHolder(View view) {
            super(view);
            titleTextView = itemView.findViewById(R.id.crime_title);
            dateTextView = itemView.findViewById(R.id.crime_date);
            solvedImageView = itemView.findViewById(R.id.crime_solved);
            itemView.setOnClickListener(this);
        }

        public void bind(Crime crime) {
            mCrime = crime;
            titleTextView.setText(crime.getTitle());
            dateTextView.setText(crime.getDate().toString());

            if (crime.isSolved()) {
                solvedImageView.setVisibility(View.VISIBLE);
                solvedImageView.setImageResource(R.drawable.handcuffs);
            } else {
                solvedImageView.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View v) {
            Intent intent = CrimePagerActivity.newIntent(getActivity(), mCrime.getId());
            startActivity(intent);
        }
    }

    private class CrimePoliceHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Crime mCrime;
        private final TextView titleTextView;
        private final TextView dateTextView;
        private final Button contactPoliceButton;
        private final ImageView solvedImageView;

        public CrimePoliceHolder(View view) {
            super(view);
            titleTextView = itemView.findViewById(R.id.crime_title);
            dateTextView = itemView.findViewById(R.id.crime_date);
            contactPoliceButton = itemView.findViewById(R.id.contact_police_button);
            solvedImageView = itemView.findViewById(R.id.crime_solved);
            itemView.setOnClickListener(this);
        }

        public void bind(Crime crime) {
            mCrime = crime;
            titleTextView.setText(crime.getTitle());
            dateTextView.setText(crime.getDate().toString());

            if (crime.isSolved()) {
                solvedImageView.setVisibility(View.VISIBLE);
                solvedImageView.setImageResource(R.drawable.handcuffs);
            } else {
                solvedImageView.setVisibility(View.GONE);
            }

            contactPoliceButton.setOnClickListener(v -> 
                Toast.makeText(
                    getContext(),
                    "Contacting police for " + crime.getTitle(),
                    Toast.LENGTH_SHORT
                ).show()
            );
        }

        @Override
        public void onClick(View v) {
            Intent intent = CrimePagerActivity.newIntent(getActivity(), mCrime.getId());
            startActivity(intent);
        }
    }

    private class CrimeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<Crime> crimes;

        public CrimeAdapter(List<Crime> crimes) {
            this.crimes = crimes;
        }

        @Override
        public int getItemCount() {
            return crimes.size();
        }

        @Override
        public int getItemViewType(int position) {
            return crimes.get(position).isRequiresPolice() ? VIEW_TYPE_POLICE : VIEW_TYPE_NORMAL;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_POLICE) {
                View view = layoutInflater.inflate(R.layout.list_item_crime_police, parent, false);
                return new CrimePoliceHolder(view);
            } else {
                View view = layoutInflater.inflate(R.layout.list_item_crime, parent, false);
                return new CrimeHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Crime crime = crimes.get(position);
            if (holder instanceof CrimePoliceHolder) {
                ((CrimePoliceHolder) holder).bind(crime);
            } else if (holder instanceof CrimeHolder) {
                ((CrimeHolder) holder).bind(crime);
            }
        }
    }
}
