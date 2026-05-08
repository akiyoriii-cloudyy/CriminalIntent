package com.bignerdranch.android.criminalintent

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import java.io.File

private const val ARG_PHOTO_FILE = "photo_file"

class PhotoDetailFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val photoFile = arguments?.getSerializable(ARG_PHOTO_FILE) as File
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_photo_detail, null)
        val imageView = view.findViewById<ImageView>(R.id.photo_detail)

        if (photoFile.exists()) {
            val bitmap = PictureUtils.getScaledBitmap(photoFile.path, requireActivity())
            imageView.setImageBitmap(bitmap)
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    companion object {
        fun newInstance(photoFile: File): PhotoDetailFragment {
            val args = Bundle().apply {
                putSerializable(ARG_PHOTO_FILE, photoFile)
            }
            return PhotoDetailFragment().apply {
                arguments = args
            }
        }
    }
}
