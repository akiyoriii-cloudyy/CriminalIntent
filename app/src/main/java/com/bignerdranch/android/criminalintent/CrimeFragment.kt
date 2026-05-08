package com.bignerdranch.android.criminalintent

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeBinding
import java.io.File
import java.util.Date
import java.util.UUID

private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_TIME = "DialogTime"
private const val DIALOG_PHOTO = "DialogPhoto"
private const val REQUEST_DATE = 0
private const val REQUEST_TIME = 1
private const val REQUEST_CONTACT = 2
private const val REQUEST_CONTACT_PERMISSION = 3
private const val REQUEST_PHOTO = 4
private const val DATE_FORMAT = "EEE, MMM dd"

class CrimeFragment : Fragment() {

    /**
     * Required interface for hosting activities.
     */
    interface Callbacks {
        fun onCrimeUpdated(crime: Crime)
    }

    private var callbacks: Callbacks? = null

    private var _binding: FragmentCrimeBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private lateinit var crime: Crime
    private lateinit var photoFile: File

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crimeId = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crime = CrimeLab.get(requireContext()).getCrime(crimeId) ?: Crime()
        photoFile = CrimeLab.get(requireContext()).getPhotoFile(crime)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.crimeTitle.setText(crime.title)
        binding.crimeTitle.doOnTextChanged { text, _, _, _ ->
            crime.title = text.toString()
            updateCrime()
        }

        updateDate()
        binding.crimeDate.setOnClickListener {
            val dialog = DatePickerFragment.newInstance(crime.date)
            dialog.setTargetFragment(this, REQUEST_DATE)
            dialog.show(parentFragmentManager, DIALOG_DATE)
        }

        binding.crimeTime.setOnClickListener {
            val dialog = TimePickerFragment.newInstance(crime.date)
            dialog.setTargetFragment(this, REQUEST_TIME)
            dialog.show(parentFragmentManager, DIALOG_TIME)
        }

        binding.crimeSolved.apply {
            isChecked = crime.isSolved
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
                updateCrime()
            }
        }

        binding.crimeRequiresPolice.apply {
            isChecked = crime.requiresPolice
            setOnCheckedChangeListener { _, isChecked ->
                crime.requiresPolice = isChecked
                updateCrime()
            }
        }

        binding.crimeReport.setOnClickListener {
            ShareCompat.IntentBuilder(requireActivity())
                .setType("text/plain")
                .setText(getCrimeReport())
                .setSubject(getString(R.string.crime_report_subject))
                .setChooserTitle(getString(R.string.send_report))
                .startChooser()
        }

        val pickContactIntent =
            Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

        binding.crimeSuspect.setOnClickListener {
            startActivityForResult(pickContactIntent, REQUEST_CONTACT)
        }

        val packageManager: PackageManager = requireActivity().packageManager
        if (packageManager.resolveActivity(pickContactIntent,
                PackageManager.MATCH_DEFAULT_ONLY) == null) {
            binding.crimeSuspect.isEnabled = false
        }

        binding.crimeCall.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_CONTACT_PERMISSION)
            } else {
                dialSuspect()
            }
        }

        val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val canTakePhoto = photoFile != null &&
                captureImage.resolveActivity(packageManager) != null

        binding.crimeCamera.isEnabled = canTakePhoto

        binding.crimeCamera.setOnClickListener {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "com.bignerdranch.android.criminalintent.fileprovider",
                photoFile
            )
            captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri)

            val cameraActivities = requireActivity().packageManager.queryIntentActivities(
                captureImage, PackageManager.MATCH_DEFAULT_ONLY
            )

            for (activity in cameraActivities) {
                requireActivity().grantUriPermission(
                    activity.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }

            startActivityForResult(captureImage, REQUEST_PHOTO)
        }

        binding.crimePhoto.setOnClickListener {
            if (photoFile.exists()) {
                val dialog = PhotoDetailFragment.newInstance(photoFile)
                dialog.show(parentFragmentManager, DIALOG_PHOTO)
            }
        }

        val observer = binding.crimePhoto.viewTreeObserver
        observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (binding.crimePhoto.viewTreeObserver.isAlive) {
                    binding.crimePhoto.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
                updatePhotoView()
            }
        })

        updateSuspectButton()
    }

    private fun dialSuspect() {
        val contactUri = ContactsContract.Contacts.CONTENT_URI
        val queryFields = arrayOf(ContactsContract.Contacts._ID)
        val selection = "${ContactsContract.Contacts.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(crime.suspect)

        val cursor = requireActivity().contentResolver
            .query(contactUri, queryFields, selection, selectionArgs, null)

        cursor?.use {
            if (it.count == 0) return
            it.moveToFirst()
            val contactId = it.getString(0)

            val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val phoneQueryFields = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val phoneSelection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
            val phoneSelectionArgs = arrayOf(contactId)

            val phoneCursor = requireActivity().contentResolver
                .query(phoneUri, phoneQueryFields, phoneSelection, phoneSelectionArgs, null)

            phoneCursor?.use { pc ->
                if (pc.count == 0) return
                pc.moveToFirst()
                val number = pc.getString(0)

                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$number")
                }
                startActivity(dialIntent)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CONTACT_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dialSuspect()
            }
        }
    }

    private fun updateSuspectButton() {
        if (crime.suspect != null) {
            binding.crimeSuspect.text = crime.suspect
            binding.crimeCall.isEnabled = true
        } else {
            binding.crimeCall.isEnabled = false
        }
    }

    override fun onPause() {
        super.onPause()
        CrimeLab.get(requireContext()).updateCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_crime -> {
                CrimeLab.get(requireContext()).deleteCrime(crime)
                activity?.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        when (requestCode) {
            REQUEST_DATE -> {
                val date = data?.getSerializableExtra(DatePickerFragment.EXTRA_DATE) as Date
                crime.date = date
                updateCrime()
                updateDate()
            }
            REQUEST_TIME -> {
                val date = data?.getSerializableExtra(TimePickerFragment.EXTRA_TIME) as Date
                crime.date = date
                updateCrime()
                updateDate()
            }
            REQUEST_CONTACT -> {
                data?.data?.let { contactUri ->
                    val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                    val cursor = requireActivity().contentResolver
                        .query(contactUri, queryFields, null, null, null)
                    cursor?.use {
                        if (it.count == 0) return
                        it.moveToFirst()
                        val suspect = it.getString(0)
                        crime.suspect = suspect
                        updateCrime()
                        updateSuspectButton()
                    }
                }
            }
            REQUEST_PHOTO -> {
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.bignerdranch.android.criminalintent.fileprovider",
                    photoFile
                )
                requireActivity().revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updateCrime()
                updatePhotoView()
            }
        }
    }

    private fun updateCrime() {
        CrimeLab.get(requireContext()).updateCrime(crime)
        callbacks?.onCrimeUpdated(crime)
    }

    private fun updateDate() {
        binding.crimeDate.text = DateFormat.format(DATE_FORMAT, crime.date).toString()
        binding.crimeTime.text = DateFormat.getTimeFormat(requireContext()).format(crime.date)
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        val suspect = if (crime.suspect == null) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(
            R.string.crime_report,
            crime.title, dateString, solvedString, suspect
        )
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val width = binding.crimePhoto.width
            val height = binding.crimePhoto.height
            val bitmap = if (width > 0 && height > 0) {
                PictureUtils.getScaledBitmap(photoFile.path, width, height)
            } else {
                PictureUtils.getScaledBitmap(photoFile.path, requireActivity())
            }
            binding.crimePhoto.setImageBitmap(bitmap)
        } else {
            binding.crimePhoto.setImageDrawable(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}
