package com.example.criminalintent;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.DialogFragment;
import java.io.File;

public class CrimePhotoDialogFragment extends DialogFragment {
    private static final String ARG_PHOTO_PATH = "photo_path";
    private static final String ARG_DELETE_REQUEST_KEY = "delete_request_key";
    public static final String RESULT_DELETE_PHOTO = "result_delete_photo";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_crime_photo, null);
        AppCompatImageView photoView = view.findViewById(R.id.crime_photo_dialog_image);

        String photoPath = getArguments() != null ? getArguments().getString(ARG_PHOTO_PATH) : null;
        bindPhoto(photoView, photoPath);

        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.crime_photo_dialog_title)
                .setView(view)
                .setNegativeButton(R.string.crime_delete_photo, (dialog, which) -> {
                    String requestKey = getArguments() != null
                            ? getArguments().getString(ARG_DELETE_REQUEST_KEY)
                            : null;
                    if (requestKey == null || requestKey.trim().isEmpty()) {
                        return;
                    }

                    Bundle result = new Bundle();
                    result.putBoolean(RESULT_DELETE_PHOTO, true);
                    getParentFragmentManager().setFragmentResult(requestKey, result);
                })
                .setPositiveButton(R.string.crime_close_photo, null)
                .create();
    }

    public static CrimePhotoDialogFragment newInstance(String photoPath, String deleteRequestKey) {
        Bundle args = new Bundle();
        args.putString(ARG_PHOTO_PATH, photoPath);
        args.putString(ARG_DELETE_REQUEST_KEY, deleteRequestKey);

        CrimePhotoDialogFragment fragment = new CrimePhotoDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private void bindPhoto(AppCompatImageView photoView, String photoPath) {
        File photoFile = photoPath == null ? null : new File(photoPath);
        if (photoFile == null || !photoFile.exists() || photoFile.length() == 0) {
            photoView.setScaleType(ImageView.ScaleType.CENTER);
            photoView.setImageResource(R.drawable.ic_camera);
            photoView.setContentDescription(getString(R.string.crime_photo_placeholder_description));
            return;
        }

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int destWidth = Math.round(displayMetrics.widthPixels * 0.9f);
        int destHeight = Math.round(displayMetrics.heightPixels * 0.75f);
        Bitmap bitmap = PictureUtils.getScaledBitmap(photoFile.getPath(), destWidth, destHeight);
        if (bitmap == null) {
            photoView.setScaleType(ImageView.ScaleType.CENTER);
            photoView.setImageResource(R.drawable.ic_camera);
            photoView.setContentDescription(getString(R.string.crime_photo_placeholder_description));
            return;
        }

        photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        photoView.setImageBitmap(bitmap);
        photoView.setContentDescription(getString(R.string.crime_photo_dialog_title));
    }
}
