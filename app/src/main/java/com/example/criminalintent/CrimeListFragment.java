package com.example.criminalintent;

import android.content.Context;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.text.DateFormat;
import java.util.List;

public class CrimeListFragment extends Fragment {

    private static final String SAVED_SUBTITLE_VISIBLE = "subtitle";
    private RecyclerView mCrimeRecyclerView;
    private CrimeAdapter mAdapter;
    private boolean mSubtitleVisible;
    private Callbacks mCallbacks;

    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_POLICE = 1;

    public interface Callbacks {
        void onCrimeSelected(Crime crime);
        void onCrimeDeleted(Crime crime);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

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
                    CrimeLab crimeLab = CrimeLab.get(requireActivity());
                    MenuItem newCrimeItem = menu.findItem(R.id.new_crime);
                    if (newCrimeItem != null) {
                        newCrimeItem.setVisible(crimeLab.canAddMoreCrimes());
                    }

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
                        CrimeLab crimeLab = CrimeLab.get(requireActivity());
                        if (!crimeLab.canAddMoreCrimes()) {
                            requireActivity().invalidateOptionsMenu();
                            return true;
                        }

                        Crime crime = new Crime();
                        crimeLab.addCrime(crime);
                        updateUI();
                        mCallbacks.onCrimeSelected(crime);
                        return true;
                    } else if (menuItem.getItemId() == R.id.show_subtitle) {
                        mSubtitleVisible = !mSubtitleVisible;
                        updateSubtitle();
                        requireActivity().invalidateOptionsMenu();
                        return true;
                    } else if (menuItem.getItemId() == R.id.change_language) {
                        showLanguageSelectionDialog();
                        return true;
                    }
                    return false;
                }
            },
            getViewLifecycleOwner(),
            Lifecycle.State.RESUMED
        );
    }

    private void showLanguageSelectionDialog() {
        String[] languages = {"English", "Español"};
        String[] langTags = {"en", "es"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Language")
                .setItems(languages, (dialog, which) -> {
                    LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(langTags[which]);
                    AppCompatDelegate.setApplicationLocales(appLocales);
                })
                .show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crime_list, container, false);

        if (savedInstanceState != null) {
            mSubtitleVisible = savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE);
        }

        mCrimeRecyclerView = view.findViewById(R.id.crime_recycler_view);
        mCrimeRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        attachSwipeToDismiss();

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

    public void updateUI() {
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        List<Crime> crimes = crimeLab.getCrimes();

        if (mAdapter == null) {
            mAdapter = new CrimeAdapter(crimes);
            mCrimeRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.setCrimes(crimes);
            mAdapter.notifyDataSetChanged();
        }
        updateSubtitle();
        requireActivity().invalidateOptionsMenu();
    }

    private void attachSwipeToDismiss() {
        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(
                            RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            RecyclerView.ViewHolder target
                    ) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getBindingAdapterPosition();
                        if (position == RecyclerView.NO_POSITION || mAdapter == null) {
                            if (mAdapter != null) {
                                mAdapter.notifyDataSetChanged();
                            }
                            return;
                        }

                        Crime crime = mAdapter.getCrime(position);
                        CrimeLab.get(requireContext()).deleteCrime(crime);
                        mCallbacks.onCrimeDeleted(crime);
                        updateUI();
                    }
                };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(mCrimeRecyclerView);
    }

    private void updateSubtitle() {
        int crimeCount = CrimeLab.get(getActivity()).getCrimeCount();
        String subtitle = getString(R.string.subtitle_format, crimeCount);
        if (!mSubtitleVisible) {
            subtitle = null;
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setSubtitle(subtitle);
        }
    }

    private String getDisplayTitle(Crime crime) {
        String title = crime.getTitle();
        return title == null || title.trim().isEmpty()
                ? getString(R.string.crime_default_title)
                : title;
    }

    private CharSequence formatCrimeDate(Crime crime) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        return dateFormat.format(crime.getDate());
    }

    private void bindStatusBadge(TextView statusTextView, Crime crime) {
        if (crime.isSolved()) {
            statusTextView.setText(R.string.crime_status_solved);
            statusTextView.setBackgroundResource(R.drawable.bg_badge_solved);
        } else {
            statusTextView.setText(R.string.crime_status_open);
            statusTextView.setBackgroundResource(R.drawable.bg_badge_open);
        }
    }

    private void bindCaseAppearance(
            MaterialCardView cardView,
            View caseStrip,
            Crime crime,
            int defaultStrokeColorRes,
            int defaultStripColorRes
    ) {
        int strokeColorRes = crime.isSolved() ? R.color.crime_accent_green : defaultStrokeColorRes;
        int stripColorRes = crime.isSolved() ? R.color.crime_accent_green : defaultStripColorRes;

        cardView.setStrokeColor(ContextCompat.getColor(cardView.getContext(), strokeColorRes));
        caseStrip.setBackgroundColor(ContextCompat.getColor(caseStrip.getContext(), stripColorRes));
    }

    private void bindSolvedIndicator(ImageView solvedImageView, Crime crime) {
        if (crime.isSolved()) {
            solvedImageView.setVisibility(View.VISIBLE);
            solvedImageView.setImageResource(R.drawable.ic_solved);
        } else {
            solvedImageView.setVisibility(View.GONE);
        }
    }

    private class CrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Crime mCrime;
        private final MaterialCardView cardView;
        private final View caseStrip;
        private final TextView titleTextView;
        private final TextView dateTextView;
        private final TextView statusTextView;
        private final ImageView solvedImageView;

        public CrimeHolder(View view) {
            super(view);
            cardView = (MaterialCardView) itemView;
            caseStrip = itemView.findViewById(R.id.case_strip);
            titleTextView = itemView.findViewById(R.id.crime_title);
            dateTextView = itemView.findViewById(R.id.crime_date);
            statusTextView = itemView.findViewById(R.id.crime_status_badge);
            solvedImageView = itemView.findViewById(R.id.crime_solved);
            itemView.setOnClickListener(this);
        }

        public void bind(Crime crime) {
            mCrime = crime;
            titleTextView.setText(getDisplayTitle(crime));
            dateTextView.setText(formatCrimeDate(crime));
            bindCaseAppearance(cardView, caseStrip, crime, R.color.crime_stroke, R.color.crime_accent_amber);
            bindStatusBadge(statusTextView, crime);
            bindSolvedIndicator(solvedImageView, crime);
        }

        @Override
        public void onClick(View v) {
            mCallbacks.onCrimeSelected(mCrime);
        }
    }

    private class CrimePoliceHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Crime mCrime;
        private final MaterialCardView cardView;
        private final View caseStrip;
        private final TextView titleTextView;
        private final TextView dateTextView;
        private final TextView statusTextView;
        private final Button contactPoliceButton;
        private final ImageView solvedImageView;

        public CrimePoliceHolder(View view) {
            super(view);
            cardView = (MaterialCardView) itemView;
            caseStrip = itemView.findViewById(R.id.case_strip);
            titleTextView = itemView.findViewById(R.id.crime_title);
            dateTextView = itemView.findViewById(R.id.crime_date);
            statusTextView = itemView.findViewById(R.id.crime_status_badge);
            contactPoliceButton = itemView.findViewById(R.id.contact_police_button);
            solvedImageView = itemView.findViewById(R.id.crime_solved);
            itemView.setOnClickListener(this);
        }

        public void bind(Crime crime) {
            mCrime = crime;
            String displayTitle = getDisplayTitle(crime);
            titleTextView.setText(displayTitle);
            dateTextView.setText(formatCrimeDate(crime));
            bindCaseAppearance(cardView, caseStrip, crime, R.color.crime_accent_red, R.color.crime_accent_red);
            bindStatusBadge(statusTextView, crime);
            bindSolvedIndicator(solvedImageView, crime);

            contactPoliceButton.setOnClickListener(v -> 
                Toast.makeText(
                    getContext(),
                    getString(R.string.contact_police_message, displayTitle),
                    Toast.LENGTH_SHORT
                ).show()
            );
        }

        @Override
        public void onClick(View v) {
            mCallbacks.onCrimeSelected(mCrime);
        }
    }

    private class CrimeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<Crime> crimes;

        public CrimeAdapter(List<Crime> crimes) {
            this.crimes = crimes;
        }

        public void setCrimes(List<Crime> crimes) {
            this.crimes = crimes;
        }

        public Crime getCrime(int position) {
            return crimes.get(position);
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
