package com.bignerdranch.android.criminalintent

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CrimeListViewModel : ViewModel() {

    private val _crimes = MutableLiveData<List<Crime>>()
    val crimes: LiveData<List<Crime>> = _crimes

    fun loadCrimes(context: Context) {
        _crimes.value = CrimeLab.get(context).getCrimes()
    }
}
