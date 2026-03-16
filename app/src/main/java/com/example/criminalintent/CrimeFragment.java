package com.example.criminalintent;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;
import java.io.File;
import java.util.Date;
import java.util.UUID;

public class CrimeFragment extends Fragment {

    private static final String ARG_CRIME_ID = "crime_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final String DIALOG_PHOTO = "DialogPhoto";
    private static final String REQUEST_KEY_DATE_PREFIX = "request_key_date_";
    private static final String RESULT_KEY_DATE_PREFIX = "result_key_date_";
    private static final String REQUEST_KEY_PHOTO_DELETE_PREFIX = "request_key_photo_delete_";

    public interface Callbacks {
        void onCrimeUpdated(Crime crime);
        void onCrimeDeleted(Crime crime);
    }

    private Crime mCrime;
    private String mDateRequestKey;
    private String mDateResultKey;
    private String mPhotoDeleteRequestKey;
    private EditText titleField;
    private Button mDateButton;
    private CheckBox solvedCheckBox;
    private CheckBox mRequiresPoliceCheckBox;
    private Button saveButton;
    private Button mPhotoButton;
    private Button mSuspectButton;
    private Button mReportButton;
    private ImageView mPhotoView;
    private File mPhotoFile;
    private File mPendingPhotoFile;
    private ViewTreeObserver.OnGlobalLayoutListener mPhotoLayoutListener;
    private Callbacks mCallbacks;
    private ActivityResultLauncher<String> requestContactsPermissionLauncher;
    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private ActivityResultLauncher<Intent> pickContactLauncher;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        UUID crimeId = SerializationCompat.getSerializable(getArguments(), ARG_CRIME_ID, UUID.class);
        mCrime = crimeId != null ? CrimeLab.get(requireContext()).getCrime(crimeId) : null;
        if (mCrime == null) {
            mCrime = crimeId != null ? new Crime(crimeId) : new Crime();
            CrimeLab.get(requireContext()).addCrime(mCrime);
        }
        mPhotoFile = CrimeLab.get(requireContext()).getPhotoFile(mCrime);

        mDateRequestKey = REQUEST_KEY_DATE_PREFIX + mCrime.getId();
        mDateResultKey = RESULT_KEY_DATE_PREFIX + mCrime.getId();
        mPhotoDeleteRequestKey = REQUEST_KEY_PHOTO_DELETE_PREFIX + mCrime.getId();

        getParentFragmentManager().setFragmentResultListener(
                mDateRequestKey,
                this,
                (requestKey, result) -> {
                    Date date = SerializationCompat.getSerializable(result, mDateResultKey, Date.class);
                    if (date != null) {
                        mCrime.setDate(date);
                        updateCrime();
                        updateDate();
                    }
                }
        );
        getParentFragmentManager().setFragmentResultListener(
                mPhotoDeleteRequestKey,
                this,
                (requestKey, result) -> {
                    if (result.getBoolean(CrimePhotoDialogFragment.RESULT_DELETE_PHOTO, false)) {
                        deletePhoto();
                    }
                }
        );

        requestContactsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchContactPicker();
                    } else {
                        Toast.makeText(
                                requireContext(),
                                R.string.crime_contacts_permission_required,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                didTakePhoto -> {
                    if (didTakePhoto) {
                        commitPendingPhoto();
                        updateCrime();
                    } else {
                        discardPendingPhoto();
                    }
                    updatePhotoView();
                }
        );

        pickContactLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }

                    handleContactPicked(result.getData());
                }
        );
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_delete_crime) {
            Crime deletedCrime = mCrime;
            CrimeLab.get(requireContext()).deleteCrime(mCrime);
            mCallbacks.onCrimeDeleted(deletedCrime);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crime, container, false);

        titleField = view.findViewById(R.id.crime_title);
        mDateButton = view.findViewById(R.id.crime_date);
        solvedCheckBox = view.findViewById(R.id.crime_solved);
        mRequiresPoliceCheckBox = view.findViewById(R.id.crime_requires_police);
        saveButton = view.findViewById(R.id.crime_save);
        mPhotoButton = view.findViewById(R.id.crime_camera);
        mSuspectButton = view.findViewById(R.id.crime_suspect);
        mReportButton = view.findViewById(R.id.crime_report);
        mPhotoView = view.findViewById(R.id.crime_photo);

        titleField.setText(mCrime.getTitle());
        titleField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCrime.setTitle(s != null ? s.toString() : "");
                updateCrime();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        solvedCheckBox.setChecked(mCrime.isSolved());
        solvedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mCrime.setSolved(isChecked);
            updateCrime();
        });

        mRequiresPoliceCheckBox.setChecked(mCrime.isRequiresPolice());
        mRequiresPoliceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mCrime.setRequiresPolice(isChecked);
            updateCrime();
        });

        mDateButton.setEnabled(true);
        updateDate();
        mDateButton.setOnClickListener(v -> {
            DatePickerFragment.newInstance(
                    mCrime.getDate(),
                    mDateRequestKey,
                    mDateResultKey
            ).show(getParentFragmentManager(), DIALOG_DATE);
        });

        saveButton.setOnClickListener(v -> {
            updateCrime();
            if (requireActivity().findViewById(R.id.detail_fragment_container) == null) {
                requireActivity().finish();
            }
        });

        mReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShareCompat.IntentBuilder
                        .from(requireActivity())
                        .setType("text/plain")
                        .setSubject(getString(R.string.crime_report_subject))
                        .setText(getCrimeReport())
                        .setChooserTitle(getString(R.string.send_report))
                        .startChooser();
            }
        });

        final Intent pickContact = new Intent(
                Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI
        );
        final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        PackageManager pm = requireActivity().getPackageManager();
        if (pm.resolveActivity(pickContact, PackageManager.MATCH_DEFAULT_ONLY) == null) {
            mSuspectButton.setEnabled(false);
        } else {
            mSuspectButton.setEnabled(true);
        }
        mPhotoButton.setEnabled(pm.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY) != null);

        updateSuspectButton();
        observePhotoViewLayout();

        mSuspectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestContactsPermissionIfNeeded();
            }
        });

        mPhotoButton.setOnClickListener(v -> launchCamera());
        mPhotoView.setOnClickListener(v -> showPhotoDialog());

        return view;
    }

    private String getCrimeReport() {
        String title = mCrime.getTitle() == null || mCrime.getTitle().trim().isEmpty()
                ? getString(R.string.crime_default_title)
                : mCrime.getTitle();

        String solvedString = mCrime.isSolved()
                ? getString(R.string.crime_report_solved)
                : getString(R.string.crime_report_unsolved);

        String dateString = android.text.format.DateFormat
                .format("EEE, MMM dd", mCrime.getDate())
                .toString();

        String suspect = mCrime.getSuspect() == null
                ? getString(R.string.crime_report_no_suspect)
                : getString(R.string.crime_report_suspect, mCrime.getSuspect());

        return getString(R.string.crime_report,
                title,
                dateString,
                solvedString,
                suspect);
    }

    @Override
    public void onPause() {
        super.onPause();
        CrimeLab.get(requireContext()).updateCrime(mCrime);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onDestroyView() {
        removePhotoLayoutListener();
        super.onDestroyView();
    }

    private void updateDate() {
        CharSequence dateText = android.text.format.DateFormat.format(
                "EEE, MMM dd yyyy  |  hh:mm a",
                mCrime.getDate()
        );
        mDateButton.setText(dateText);
    }

    private void handleContactPicked(Intent data) {
        Uri contactUri = data.getData();
        if (contactUri == null) {
            return;
        }

        String[] queryFields = new String[] {
                ContactsContract.Contacts.DISPLAY_NAME
        };

        Cursor cursor = requireActivity()
                .getContentResolver()
                .query(contactUri, queryFields, null, null, null);

        try {
            if (cursor == null || cursor.getCount() == 0) {
                return;
            }

            cursor.moveToFirst();
            String suspect = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            );
            mCrime.setSuspect(suspect);
            mCrime.setSuspectPhoneNumber(null);
            updateCrime();
            updateSuspectButton();

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void launchCamera() {
        if (mPhotoFile == null) {
            return;
        }

        discardPendingPhoto();
        mPendingPhotoFile = new File(mPhotoFile.getParentFile(), "PENDING_" + mPhotoFile.getName());

        Uri photoUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                mPendingPhotoFile
        );
        takePhotoLauncher.launch(photoUri);
    }

    private void requestContactsPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED) {
            launchContactPicker();
        } else {
            requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void launchContactPicker() {
        if (!hasAvailableContacts()) {
            Toast.makeText(requireContext(), R.string.crime_no_contacts_available, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent pickContact = new Intent(
                Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI
        );
        pickContactLauncher.launch(pickContact);
    }

    private void observePhotoViewLayout() {
        if (mPhotoView == null) {
            return;
        }

        if (mPhotoView.getWidth() > 0 && mPhotoView.getHeight() > 0) {
            updatePhotoView();
            return;
        }

        removePhotoLayoutListener();
        mPhotoLayoutListener = () -> {
            if (mPhotoView.getWidth() > 0 && mPhotoView.getHeight() > 0) {
                updatePhotoView();
                removePhotoLayoutListener();
            }
        };
        mPhotoView.getViewTreeObserver().addOnGlobalLayoutListener(mPhotoLayoutListener);
    }

    private void removePhotoLayoutListener() {
        if (mPhotoView == null || mPhotoLayoutListener == null) {
            return;
        }

        ViewTreeObserver observer = mPhotoView.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeOnGlobalLayoutListener(mPhotoLayoutListener);
        }
        mPhotoLayoutListener = null;
    }

    private void updatePhotoView() {
        if (mPhotoView == null) {
            return;
        }

        if (!hasPhoto()) {
            mPhotoView.setPadding(
                    getResources().getDimensionPixelSize(R.dimen.crime_photo_placeholder_padding),
                    getResources().getDimensionPixelSize(R.dimen.crime_photo_placeholder_padding),
                    getResources().getDimensionPixelSize(R.dimen.crime_photo_placeholder_padding),
                    getResources().getDimensionPixelSize(R.dimen.crime_photo_placeholder_padding)
            );
            mPhotoView.setScaleType(ImageView.ScaleType.CENTER);
            mPhotoView.setImageResource(R.drawable.ic_camera);
            mPhotoView.setContentDescription(getString(R.string.crime_photo_placeholder_description));
            mPhotoView.setEnabled(false);
            mPhotoView.setAlpha(0.72f);
            return;
        }

        int photoViewWidth = mPhotoView.getWidth();
        int photoViewHeight = mPhotoView.getHeight();
        if (photoViewWidth <= 0 || photoViewHeight <= 0) {
            return;
        }

        Bitmap bitmap = PictureUtils.getScaledBitmap(
                mPhotoFile.getPath(),
                photoViewWidth,
                photoViewHeight
        );
        if (bitmap == null) {
            return;
        }

        mPhotoView.setPadding(0, 0, 0, 0);
        mPhotoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mPhotoView.setImageBitmap(bitmap);
        mPhotoView.setContentDescription(
                getString(R.string.crime_photo_thumbnail_description, getCrimeTitleForDisplay())
        );
        mPhotoView.setEnabled(true);
        mPhotoView.setAlpha(1f);
    }

    private boolean hasPhoto() {
        return mPhotoFile != null && mPhotoFile.exists() && mPhotoFile.length() > 0;
    }

    private void commitPendingPhoto() {
        if (mPendingPhotoFile == null || !mPendingPhotoFile.exists()) {
            mPendingPhotoFile = null;
            return;
        }

        if (mPhotoFile.exists()) {
            mPhotoFile.delete();
        }

        if (!mPendingPhotoFile.renameTo(mPhotoFile)) {
            Toast.makeText(requireContext(), R.string.crime_photo_save_failed, Toast.LENGTH_SHORT).show();
        }

        mPendingPhotoFile = null;
    }

    private void discardPendingPhoto() {
        if (mPendingPhotoFile != null && mPendingPhotoFile.exists()) {
            mPendingPhotoFile.delete();
        }
        mPendingPhotoFile = null;
    }

    private void showPhotoDialog() {
        if (!hasPhoto()) {
            return;
        }

        CrimePhotoDialogFragment
                .newInstance(mPhotoFile.getAbsolutePath(), mPhotoDeleteRequestKey)
                .show(getParentFragmentManager(), DIALOG_PHOTO);
    }

    private void deletePhoto() {
        discardPendingPhoto();
        if (mPhotoFile == null || !mPhotoFile.exists()) {
            updatePhotoView();
            return;
        }

        if (!mPhotoFile.delete()) {
            Toast.makeText(requireContext(), R.string.crime_photo_delete_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        updateCrime();
        updatePhotoView();
    }

    private String getCrimeTitleForDisplay() {
        String title = mCrime.getTitle();
        if (title == null || title.trim().isEmpty()) {
            return getString(R.string.crime_default_title);
        }

        return title;
    }

    private void updateCrime() {
        CrimeLab.get(requireContext()).updateCrime(mCrime);
        if (mCallbacks != null) {
            mCallbacks.onCrimeUpdated(mCrime);
        }
    }

    private boolean hasAvailableContacts() {
        Cursor contactCursor = requireActivity()
                .getContentResolver()
                .query(
                        ContactsContract.Contacts.CONTENT_URI,
                        new String[] { ContactsContract.Contacts._ID },
                        null,
                        null,
                        null
                );

        try {
            return contactCursor != null && contactCursor.moveToFirst();
        } finally {
            if (contactCursor != null) {
                contactCursor.close();
            }
        }
    }

    private void updateSuspectButton() {
        if (mCrime.getSuspect() == null || mCrime.getSuspect().trim().isEmpty()) {
            mSuspectButton.setText(R.string.crime_suspect_text);
        } else {
            mSuspectButton.setText(mCrime.getSuspect());
        }
    }

    public UUID getCrimeId() {
        return mCrime.getId();
    }

    public static CrimeFragment newInstance(UUID crimeId) {
        CrimeFragment fragment = new CrimeFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);
        fragment.setArguments(args);
        return fragment;
    }
}
