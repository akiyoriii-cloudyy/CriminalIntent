package com.bignerdranch.android.criminalintent.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bignerdranch.android.criminalintent.database.CrimeDbSchema.CrimeTable

private const val VERSION = 2
private const val DATABASE_NAME = "crimeBase.db"

class CrimeBaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("create table " + CrimeTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                CrimeTable.Cols.UUID + ", " +
                CrimeTable.Cols.TITLE + ", " +
                CrimeTable.Cols.DATE + ", " +
                CrimeTable.Cols.SOLVED + ", " +
                CrimeTable.Cols.SUSPECT + ", " +
                CrimeTable.Cols.REQUIRES_POLICE +
                ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("alter table ${CrimeTable.NAME} add column ${CrimeTable.Cols.REQUIRES_POLICE} integer default 0")
        }
    }
}
