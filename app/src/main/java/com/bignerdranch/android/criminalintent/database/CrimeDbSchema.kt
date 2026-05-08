package com.bignerdranch.android.criminalintent.database

class CrimeDbSchema {
    object CrimeTable {
        const val NAME = "crimes"

        object Cols {
            const val UUID = "uuid"
            const val TITLE = "title"
            const val DATE = "date"
            const val SOLVED = "solved"
            const val SUSPECT = "suspect"
            const val REQUIRES_POLICE = "requires_police"
        }
    }
}
