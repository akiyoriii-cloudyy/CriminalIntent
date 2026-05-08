package com.bignerdranch.android.criminalintent

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.criminalintent.databinding.FragmentCrimeListBinding
import com.bignerdranch.android.criminalintent.databinding.ListItemCrimeBinding
import com.bignerdranch.android.criminalintent.databinding.ListItemCrimePoliceBinding

private const val VIEW_TYPE_NORMAL = 0
private const val VIEW_TYPE_POLICE = 1
private const val SAVED_SUBTITLE_VISIBLE = "subtitle"
private const val MAX_CRIMES = 10
private const val DATE_FORMAT = "EEE, MMM dd, yyyy"

class CrimeListFragment : Fragment() {

    /**
     * Required interface for hosting activities.
     */
    interface Callbacks {
        fun onCrimeSelected(crime: Crime)
    }

    private var callbacks: Callbacks? = null
    private var _binding: FragmentCrimeListBinding? = null
    private val binding get() = _binding!!

    private var adapter: CrimeAdapter? = null
    private var isSubtitleVisible: Boolean = false

    private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProvider(this)[CrimeListViewModel::class.java]
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrimeListBinding.inflate(inflater, container, false)

        binding.crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        
        binding.addCrimeButton.setOnClickListener {
            createNewCrime()
        }

        if (savedInstanceState != null) {
            isSubtitleVisible = savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE)
        }

        setupItemTouchHelper()
        
        return binding.root
    }

    private fun setupItemTouchHelper() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                adapter?.crimes?.get(position)?.let { crime ->
                    CrimeLab.get(requireContext()).deleteCrime(crime)
                    crimeListViewModel.loadCrimes(requireContext())
                }
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.crimeRecyclerView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeListViewModel.crimes.observe(viewLifecycleOwner) { crimes ->
            crimes?.let {
                updateUI(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        crimeListViewModel.loadCrimes(requireContext())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVED_SUBTITLE_VISIBLE, isSubtitleVisible)
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)

        val subtitleItem = menu.findItem(R.id.show_subtitle)
        if (isSubtitleVisible) {
            subtitleItem.setTitle(R.string.hide_subtitle)
        } else {
            subtitleItem.setTitle(R.string.show_subtitle)
        }

        val newCrimeItem = menu.findItem(R.id.new_crime)
        val crimeCount = crimeListViewModel.crimes.value?.size ?: 0
        newCrimeItem.isVisible = crimeCount < MAX_CRIMES
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_crime -> {
                createNewCrime()
                true
            }
            R.id.show_subtitle -> {
                isSubtitleVisible = !isSubtitleVisible
                activity?.invalidateOptionsMenu()
                updateSubtitle()
                true
            }
            R.id.change_language -> {
                showLanguageDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.english), getString(R.string.spanish))
        val languageCodes = arrayOf("en", "es")

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.change_language)
            .setItems(languages) { _, which ->
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCodes[which])
                AppCompatDelegate.setApplicationLocales(appLocale)
            }
            .show()
    }

    private fun createNewCrime() {
        val crimeCount = crimeListViewModel.crimes.value?.size ?: 0
        if (crimeCount >= MAX_CRIMES) {
            return
        }
        val crime = Crime()
        CrimeLab.get(requireContext()).addCrime(crime)
        crimeListViewModel.loadCrimes(requireContext())
        callbacks?.onCrimeSelected(crime)
    }

    private fun updateSubtitle() {
        val crimeCount = crimeListViewModel.crimes.value?.size ?: 0
        val subtitle = if (isSubtitleVisible) {
            resources.getQuantityString(R.plurals.subtitle_plural, crimeCount, crimeCount)
        } else {
            null
        }

        val activity = activity as AppCompatActivity
        activity.supportActionBar?.subtitle = subtitle
    }

    fun updateUI() {
        crimeListViewModel.loadCrimes(requireContext())
    }

    private fun updateUI(crimes: List<Crime>) {
        val isEmpty = crimes.isEmpty()
        binding.crimeRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE

        if (adapter == null) {
            adapter = CrimeAdapter(crimes)
            binding.crimeRecyclerView.adapter = adapter
        } else {
            adapter?.crimes = crimes
            adapter?.notifyDataSetChanged()
        }

        activity?.invalidateOptionsMenu()
        updateSubtitle()
    }

    private abstract inner class BaseCrimeHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        protected lateinit var crime: Crime
        
        init {
            itemView.setOnClickListener(this)
        }

        abstract fun bind(crime: Crime)

        override fun onClick(v: View) {
            callbacks?.onCrimeSelected(crime)
        }

        protected fun applySolvedStyling(titleView: android.widget.TextView, dateView: android.widget.TextView, isSolved: Boolean) {
            val color = if (isSolved) {
                ContextCompat.getColor(itemView.context, R.color.green)
            } else {
                ContextCompat.getColor(itemView.context, R.color.black)
            }
            titleView.setTextColor(color)
            dateView.setTextColor(color)
        }
    }

    private inner class CrimeHolder(private val itemBinding: ListItemCrimeBinding) : BaseCrimeHolder(itemBinding.root) {
        override fun bind(crime: Crime) {
            this.crime = crime
            itemBinding.crimeTitle.text = crime.title
            itemBinding.crimeDate.text = DateFormat.format(DATE_FORMAT, crime.date)
            itemBinding.crimeSolved.visibility = if (crime.isSolved) View.VISIBLE else View.GONE
            applySolvedStyling(itemBinding.crimeTitle, itemBinding.crimeDate, crime.isSolved)
        }
    }

    private inner class PoliceCrimeHolder(private val itemBinding: ListItemCrimePoliceBinding) : BaseCrimeHolder(itemBinding.root) {
        override fun bind(crime: Crime) {
            this.crime = crime
            itemBinding.crimeTitle.text = crime.title
            itemBinding.crimeDate.text = DateFormat.format(DATE_FORMAT, crime.date)
            itemBinding.crimeSolved.visibility = if (crime.isSolved) View.VISIBLE else View.GONE
            itemBinding.contactPoliceButton.setOnClickListener {
                Toast.makeText(context, "Police contacted for ${crime.title}", Toast.LENGTH_SHORT).show()
            }
            applySolvedStyling(itemBinding.crimeTitle, itemBinding.crimeDate, crime.isSolved)
        }
    }

    private inner class CrimeAdapter(var crimes: List<Crime>) : RecyclerView.Adapter<BaseCrimeHolder>() {

        override fun getItemViewType(position: Int): Int {
            return if (crimes[position].requiresPolice) VIEW_TYPE_POLICE else VIEW_TYPE_NORMAL
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseCrimeHolder {
            val layoutInflater = LayoutInflater.from(activity)
            return if (viewType == VIEW_TYPE_POLICE) {
                PoliceCrimeHolder(ListItemCrimePoliceBinding.inflate(layoutInflater, parent, false))
            } else {
                CrimeHolder(ListItemCrimeBinding.inflate(layoutInflater, parent, false))
            }
        }

        override fun onBindViewHolder(holder: BaseCrimeHolder, position: Int) {
            holder.bind(crimes[position])
        }

        override fun getItemCount() = crimes.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): CrimeListFragment {
            return CrimeListFragment()
        }
    }
}
