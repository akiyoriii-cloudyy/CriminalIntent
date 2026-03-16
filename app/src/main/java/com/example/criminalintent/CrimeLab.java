package com.example.criminalintent;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.criminalintent.database.CrimeBaseHelper;
import com.example.criminalintent.database.CrimeCursorWrapper;
import com.example.criminalintent.database.CrimeDbSchema.CrimeTable;
import java.io.File;
import java.util.List;
import java.util.UUID;

public class CrimeLab {
    private static CrimeLab sCrimeLab;
    private final Context mContext;
    private final SQLiteDatabase mDatabase;

    private CrimeLab(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new CrimeBaseHelper(mContext).getWritableDatabase();
    }

    // Uninstall app / clear storage once to rebuild DB

    public static CrimeLab get(Context context) {
        if (sCrimeLab == null) {
            sCrimeLab = new CrimeLab(context);
        }
        return sCrimeLab;
    }

    public List<Crime> getCrimes() {
        List<Crime> crimes = new java.util.ArrayList<>();
        CrimeCursorWrapper cursor = queryCrimes(null, null);

        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                crimes.add(cursor.getCrime());
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }

        return crimes;
    }

    public Crime getCrime(UUID id) {
        CrimeCursorWrapper cursor = queryCrimes(
                CrimeTable.Cols.UUID + " = ?",
                new String[] { id.toString() }
        );

        try {
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            return cursor.getCrime();
        } finally {
            cursor.close();
        }
    }

    public void addCrime(Crime crime) {
        if (getCrime(crime.getId()) != null) {
            updateCrime(crime);
            return;
        }

        ContentValues values = getContentValues(crime);
        mDatabase.insert(CrimeTable.NAME, null, values);
    }

    public void updateCrime(Crime crime) {
        String uuidString = crime.getId().toString();
        ContentValues values = getContentValues(crime);

        mDatabase.update(CrimeTable.NAME, values,
                CrimeTable.Cols.UUID + " = ?",
                new String[] { uuidString });
    }

    public void deleteCrime(Crime crime) {
        String uuidString = crime.getId().toString();
        mDatabase.delete(
                CrimeTable.NAME,
                CrimeTable.Cols.UUID + " = ?",
                new String[] { uuidString }
        );

        File photoFile = getPhotoFile(crime);
        if (photoFile.exists()) {
            photoFile.delete();
        }
    }

    public void saveCrime(Crime crime) {
        updateCrime(crime);
    }

    public File getPhotoFile(Crime crime) {
        return new File(mContext.getFilesDir(), crime.getPhotoFilename());
    }

    private CrimeCursorWrapper queryCrimes(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                CrimeTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );
        return new CrimeCursorWrapper(cursor);
    }

    private static ContentValues getContentValues(Crime crime) {
        ContentValues values = new ContentValues();
        values.put(CrimeTable.Cols.UUID, crime.getId().toString());
        values.put(CrimeTable.Cols.TITLE, crime.getTitle());
        values.put(CrimeTable.Cols.DATE, crime.getDate().getTime());
        values.put(CrimeTable.Cols.SOLVED, crime.isSolved() ? 1 : 0);
        values.put(CrimeTable.Cols.SUSPECT, crime.getSuspect());
        values.put(CrimeTable.Cols.SUSPECT_PHONE_NUMBER, crime.getSuspectPhoneNumber());
        values.put(CrimeTable.Cols.REQUIRES_POLICE, crime.isRequiresPolice() ? 1 : 0);
        return values;
    }
}
