package com.example.criminalintent;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ContactPickerDialogFragment extends DialogFragment {
    private static final String ARG_REQUEST_KEY = "request_key";
    private static final String ARG_RESULT_KEY = "result_key";
    private static final String ARG_PHONE_RESULT_KEY = "phone_result_key";
    private static final int VIEW_TYPE_SECTION = 0;
    private static final int VIEW_TYPE_CONTACT = 1;

    private final List<ContactEntry> mAllContacts = new ArrayList<>();
    private final List<ContactRow> mVisibleRows = new ArrayList<>();

    private ContactAdapter mAdapter;
    private AppCompatEditText mSearchField;
    private View mSearchContainer;
    private TextView mSummaryView;
    private TextView mEmptyView;
    private RecyclerView mRecyclerView;
    private MaterialButton mCreateContactButton;
    private boolean mSearchVisible;

    public static ContactPickerDialogFragment newInstance(
            String requestKey,
            String resultKey,
            String phoneResultKey
    ) {
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_KEY, requestKey);
        args.putString(ARG_RESULT_KEY, resultKey);
        args.putString(ARG_PHONE_RESULT_KEY, phoneResultKey);

        ContactPickerDialogFragment fragment = new ContactPickerDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_contact_picker, null, false);
        bindViews(view);
        reloadContacts();

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(view);
        dialog.setCanceledOnTouchOutside(true);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }

        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }

        int horizontalMargin = dpToPx(20);
        int maxWidth = getResources().getDimensionPixelSize(R.dimen.contact_picker_dialog_max_width);
        int maxHeight = getResources().getDimensionPixelSize(R.dimen.contact_picker_dialog_max_height);
        int screenWidth = requireContext().getResources().getDisplayMetrics().widthPixels;
        int screenHeight = requireContext().getResources().getDisplayMetrics().heightPixels;
        int width = Math.min(screenWidth - (horizontalMargin * 2), maxWidth);
        int height = Math.min((int) (screenHeight * 0.84f), maxHeight);

        window.setLayout(width, height);
        window.setGravity(Gravity.CENTER);
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadContacts();
    }

    private void bindViews(View view) {
        ImageButton closeButton = view.findViewById(R.id.contact_picker_close);
        ImageButton searchButton = view.findViewById(R.id.contact_picker_search);
        mSearchContainer = view.findViewById(R.id.contact_picker_search_container);
        mSearchField = view.findViewById(R.id.contact_picker_search_field);
        mSummaryView = view.findViewById(R.id.contact_picker_summary);
        mEmptyView = view.findViewById(R.id.contact_picker_empty);
        mRecyclerView = view.findViewById(R.id.contact_picker_recycler_view);
        mCreateContactButton = view.findViewById(R.id.contact_picker_create_contact);

        closeButton.setOnClickListener(v -> dismiss());
        searchButton.setOnClickListener(v -> toggleSearch());

        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mAdapter = new ContactAdapter();
        mRecyclerView.setAdapter(mAdapter);

        if (canAddContact()) {
            mCreateContactButton.setOnClickListener(v -> launchAddContact());
        } else {
            mCreateContactButton.setVisibility(View.GONE);
        }
    }

    private void toggleSearch() {
        mSearchVisible = !mSearchVisible;
        mSearchContainer.setVisibility(mSearchVisible ? View.VISIBLE : View.GONE);
        if (!mSearchVisible) {
            mSearchField.setText("");
            mSearchField.clearFocus();
            applyFilter("");
            return;
        }

        mSearchField.requestFocus();
        int selection = mSearchField.getText() == null ? 0 : mSearchField.getText().length();
        mSearchField.setSelection(selection);
    }

    private void reloadContacts() {
        mAllContacts.clear();
        mAllContacts.addAll(loadContacts());
        String query = mSearchField == null || mSearchField.getText() == null
                ? ""
                : mSearchField.getText().toString();
        applyFilter(query);
    }

    private List<ContactEntry> loadContacts() {
        List<ContactEntry> contacts = new ArrayList<>();
        Set<Long> seenContactIds = new java.util.HashSet<>();
        Cursor cursor = requireContext()
                .getContentResolver()
                .query(
                        ContactsContract.Contacts.CONTENT_URI,
                        new String[]{
                                ContactsContract.Contacts._ID,
                                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                                ContactsContract.Contacts.DISPLAY_NAME,
                                ContactsContract.Contacts.HAS_PHONE_NUMBER
                        },
                        null,
                        null,
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " COLLATE LOCALIZED ASC"
                );

        try {
            if (cursor == null) {
                return new ArrayList<>();
            }

            int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            int primaryNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
            int fallbackNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            int hasPhoneNumberIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
            while (cursor.moveToNext()) {
                if (idIndex < 0) {
                    continue;
                }

                long contactId = cursor.getLong(idIndex);
                if (!seenContactIds.add(contactId)) {
                    continue;
                }

                String contactName = primaryNameIndex >= 0 ? cursor.getString(primaryNameIndex) : null;
                if (contactName == null || contactName.trim().isEmpty()) {
                    contactName = fallbackNameIndex >= 0 ? cursor.getString(fallbackNameIndex) : null;
                }

                if (contactName == null) {
                    continue;
                }

                String trimmedName = contactName.trim();
                if (!trimmedName.isEmpty()) {
                    boolean hasPhoneNumber = hasPhoneNumberIndex >= 0 && cursor.getInt(hasPhoneNumberIndex) > 0;
                    contacts.add(new ContactEntry(
                            trimmedName,
                            hasPhoneNumber ? loadPhoneNumber(contactId) : null
                    ));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return contacts;
    }

    private String loadPhoneNumber(long contactId) {
        Cursor cursor = requireContext()
                .getContentResolver()
                .query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{String.valueOf(contactId)},
                        ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY + " DESC, "
                                + ContactsContract.CommonDataKinds.Phone.IS_PRIMARY + " DESC, "
                                + ContactsContract.CommonDataKinds.Phone._ID + " ASC"
                );

        try {
            if (cursor == null) {
                return null;
            }

            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            while (cursor.moveToNext()) {
                String phoneNumber = numberIndex >= 0 ? cursor.getString(numberIndex) : null;
                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    return phoneNumber.trim();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    private void applyFilter(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        List<ContactEntry> filteredContacts = new ArrayList<>();
        for (ContactEntry contact : mAllContacts) {
            if (normalizedQuery.isEmpty()
                    || contact.name.toLowerCase(Locale.getDefault()).contains(normalizedQuery)) {
                filteredContacts.add(contact);
            }
        }

        rebuildRows(filteredContacts);
        updateSummary(filteredContacts.size(), normalizedQuery.isEmpty());
        updateEmptyState(filteredContacts.isEmpty(), normalizedQuery.isEmpty());
        mAdapter.notifyDataSetChanged();
    }

    private void rebuildRows(List<ContactEntry> contacts) {
        mVisibleRows.clear();
        String currentSection = null;
        for (ContactEntry contact : contacts) {
            if (!contact.section.equals(currentSection)) {
                currentSection = contact.section;
                mVisibleRows.add(ContactRow.section(currentSection));
            }
            mVisibleRows.add(ContactRow.contact(contact));
        }
    }

    private void updateSummary(int visibleCount, boolean showingAllContacts) {
        if (showingAllContacts) {
            mSummaryView.setText(getString(R.string.crime_contact_picker_summary, visibleCount));
        } else {
            mSummaryView.setText(getString(R.string.crime_contact_picker_results, visibleCount));
        }
    }

    private void updateEmptyState(boolean isEmpty, boolean showingAllContacts) {
        mEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        mEmptyView.setText(showingAllContacts
                ? R.string.crime_contact_picker_empty
                : R.string.crime_contact_picker_empty_search);
    }

    private boolean canAddContact() {
        Intent addContactIntent = createAddContactIntent();
        return addContactIntent.resolveActivity(requireContext().getPackageManager()) != null;
    }

    private void launchAddContact() {
        Intent addContactIntent = createAddContactIntent();
        if (addContactIntent.resolveActivity(requireContext().getPackageManager()) == null) {
            Toast.makeText(
                    requireContext(),
                    R.string.crime_add_contact_unavailable,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        startActivity(addContactIntent);
    }

    private Intent createAddContactIntent() {
        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
        return intent;
    }

    private void sendResult(ContactEntry contact) {
        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }

        String requestKey = arguments.getString(ARG_REQUEST_KEY);
        String resultKey = arguments.getString(ARG_RESULT_KEY);
        String phoneResultKey = arguments.getString(ARG_PHONE_RESULT_KEY);
        if (requestKey == null || resultKey == null || phoneResultKey == null) {
            return;
        }

        Bundle result = new Bundle();
        result.putString(resultKey, contact.name);
        result.putString(phoneResultKey, contact.phoneNumber);
        getParentFragmentManager().setFragmentResult(requestKey, result);
        dismiss();
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static final class ContactEntry {
        private final String name;
        private final String phoneNumber;
        private final String section;
        private final String initial;

        private ContactEntry(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.section = resolveSection(name);
            this.initial = this.section;
        }

        private static String resolveSection(String name) {
            String trimmedName = name == null ? "" : name.trim();
            if (trimmedName.isEmpty()) {
                return "#";
            }

            char leadingCharacter = Character.toUpperCase(trimmedName.charAt(0));
            return Character.isLetter(leadingCharacter) ? String.valueOf(leadingCharacter) : "#";
        }
    }

    private static final class ContactRow {
        private final int viewType;
        private final String sectionTitle;
        private final ContactEntry contact;

        private ContactRow(int viewType, String sectionTitle, ContactEntry contact) {
            this.viewType = viewType;
            this.sectionTitle = sectionTitle;
            this.contact = contact;
        }

        private static ContactRow section(String sectionTitle) {
            return new ContactRow(VIEW_TYPE_SECTION, sectionTitle, null);
        }

        private static ContactRow contact(ContactEntry contact) {
            return new ContactRow(VIEW_TYPE_CONTACT, null, contact);
        }
    }

    private final class ContactAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemViewType(int position) {
            return mVisibleRows.get(position).viewType;
        }

        @Override
        public int getItemCount() {
            return mVisibleRows.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_SECTION) {
                View view = inflater.inflate(R.layout.list_item_contact_picker_header, parent, false);
                return new SectionHeaderHolder(view);
            }

            View view = inflater.inflate(R.layout.list_item_contact_picker_contact, parent, false);
            return new ContactHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ContactRow row = mVisibleRows.get(position);
            if (holder instanceof SectionHeaderHolder) {
                ((SectionHeaderHolder) holder).bind(row.sectionTitle);
            } else if (holder instanceof ContactHolder && row.contact != null) {
                ((ContactHolder) holder).bind(row.contact);
            }
        }
    }

    private static final class SectionHeaderHolder extends RecyclerView.ViewHolder {
        private final TextView sectionView;

        private SectionHeaderHolder(View itemView) {
            super(itemView);
            sectionView = itemView.findViewById(R.id.contact_picker_section_text);
        }

        private void bind(String sectionTitle) {
            sectionView.setText(sectionTitle);
        }
    }

    private final class ContactHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView avatarView;
        private final TextView nameView;
        private ContactEntry contact;

        private ContactHolder(View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.contact_picker_contact_avatar);
            nameView = itemView.findViewById(R.id.contact_picker_contact_name);
            itemView.setOnClickListener(this);
        }

        private void bind(ContactEntry contact) {
            this.contact = contact;
            avatarView.setText(contact.initial);
            nameView.setText(contact.name);
        }

        @Override
        public void onClick(View v) {
            if (contact != null) {
                sendResult(contact);
            }
        }
    }
}
