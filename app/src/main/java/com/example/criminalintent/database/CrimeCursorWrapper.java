package com.example.criminalintent.database;

import android.database.Cursor;
import android.database.CursorWrapper;
import java.util.Date;
import java.util.UUID;

public class CrimeCursorWrapper extends CursorWrapper {
    public CrimeCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public com.example.criminalintent.Crime getCrime() {
        String uuidString = getString(getColumnIndexOrThrow(CrimeDbSchema.CrimeTable.Cols.UUID));
        String title = getString(getColumnIndexOrThrow(CrimeDbSchema.CrimeTable.Cols.TITLE));
        long date = getLong(getColumnIndexOrThrow(CrimeDbSchema.CrimeTable.Cols.DATE));
        int isSolved = getInt(getColumnIndexOrThrow(CrimeDbSchema.CrimeTable.Cols.SOLVED));
        int suspectIndex = getColumnIndex(CrimeDbSchema.CrimeTable.Cols.SUSPECT);
        int suspectPhoneNumberIndex = getColumnIndex(CrimeDbSchema.CrimeTable.Cols.SUSPECT_PHONE_NUMBER);
        int requiresPoliceIndex = getColumnIndex(CrimeDbSchema.CrimeTable.Cols.REQUIRES_POLICE);
        String suspect = suspectIndex >= 0 ? getString(suspectIndex) : null;
        String suspectPhoneNumber = suspectPhoneNumberIndex >= 0 ? getString(suspectPhoneNumberIndex) : null;
        boolean requiresPolice = requiresPoliceIndex >= 0 && getInt(requiresPoliceIndex) != 0;

        com.example.criminalintent.Crime crime =
                new com.example.criminalintent.Crime(UUID.fromString(uuidString));
        crime.setTitle(title);
        crime.setDate(new Date(date));
        crime.setSolved(isSolved != 0);
        crime.setSuspect(suspect);
        crime.setSuspectPhoneNumber(suspectPhoneNumber);
        crime.setRequiresPolice(requiresPolice);

        return crime;
    }
}
