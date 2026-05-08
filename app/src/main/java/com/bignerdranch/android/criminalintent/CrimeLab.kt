package com.bignerdranch.android.criminalintent

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.bignerdranch.android.criminalintent.database.CrimeBaseHelper
import com.bignerdranch.android.criminalintent.database.CrimeCursorWrapper
import com.bignerdranch.android.criminalintent.database.CrimeDbSchema.CrimeTable
import java.io.File
import java.util.UUID

class CrimeLab private constructor(context: Context) {

    private val mContext: Context = context.applicationContext
    private val mDatabase: SQLiteDatabase = CrimeBaseHelper(mContext).writableDatabase

    fun getCrimes(): List<Crime> {
        val crimes = mutableListOf<Crime>()
        val cursor = queryCrimes(null, null)

        cursor.use {
            if (it.count == 0) return emptyList()
            it.moveToFirst()
            while (!it.isAfterLast) {
                crimes.add(it.getCrime())
                it.moveToNext()
            }
        }

        return crimes
    }

    fun getCrime(id: UUID): Crime? {
        val cursor = queryCrimes(
            "${CrimeTable.Cols.UUID} = ?",
            arrayOf(id.toString())
        )

        cursor.use {
            if (it.count == 0) {
                return null
            }

            it.moveToFirst()
            return it.getCrime()
        }
    }

    fun addCrime(crime: Crime) {
        val values = getContentValues(crime)
        mDatabase.insert(CrimeTable.NAME, null, values)
    }

    fun updateCrime(crime: Crime) {
        val uuidString = crime.id.toString()
        val values = getContentValues(crime)

        mDatabase.update(
            CrimeTable.NAME,
            values,
            "${CrimeTable.Cols.UUID} = ?",
            arrayOf(uuidString)
        )
    }

    fun deleteCrime(crime: Crime) {
        val uuidString = crime.id.toString()
        mDatabase.delete(
            CrimeTable.NAME,
            "${CrimeTable.Cols.UUID} = ?",
            arrayOf(uuidString)
        )
    }

    fun getPhotoFile(crime: Crime): File {
        val filesDir = mContext.filesDir
        return File(filesDir, crime.photoFileName)
    }

    private fun queryCrimes(whereClause: String?, whereArgs: Array<String>?): CrimeCursorWrapper {
        val cursor = mDatabase.query(
            CrimeTable.NAME,
            null, // columns - null selects all columns
            whereClause,
            whereArgs,
            null, // groupBy
            null, // having
            null  // orderBy
        )
        return CrimeCursorWrapper(cursor)
    }

    companion object {
        private var INSTANCE: CrimeLab? = null

        fun get(context: Context): CrimeLab {
            return INSTANCE ?: CrimeLab(context).also {
                INSTANCE = it
            }
        }

        private fun getContentValues(crime: Crime): ContentValues {
            val values = ContentValues()
            values.put(CrimeTable.Cols.UUID, crime.id.toString())
            values.put(CrimeTable.Cols.TITLE, crime.title)
            values.put(CrimeTable.Cols.DATE, crime.date.time)
            values.put(CrimeTable.Cols.SOLVED, if (crime.isSolved) 1 else 0)
            values.put(CrimeTable.Cols.SUSPECT, crime.suspect)
            values.put(CrimeTable.Cols.REQUIRES_POLICE, if (crime.requiresPolice) 1 else 0)
            return values
        }
    }
}
