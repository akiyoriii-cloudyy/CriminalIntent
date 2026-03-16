package com.example.criminalintent.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CrimeBaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 4;
    private static final String DATABASE_NAME = "crimeBase.db";

    public CrimeBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        ensureCrimeSchema(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ensureCrimeSchema(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ensureCrimeSchema(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        ensureCrimeSchema(db);
    }

    private void ensureCrimeSchema(SQLiteDatabase db) {
        db.execSQL("create table if not exists " + CrimeDbSchema.CrimeTable.NAME + "(" +
                " _id integer primary key autoincrement, " +
                CrimeDbSchema.CrimeTable.Cols.UUID + " text, " +
                CrimeDbSchema.CrimeTable.Cols.TITLE + " text, " +
                CrimeDbSchema.CrimeTable.Cols.DATE + " integer, " +
                CrimeDbSchema.CrimeTable.Cols.SOLVED + " integer, " +
                CrimeDbSchema.CrimeTable.Cols.SUSPECT + " text, " +
                CrimeDbSchema.CrimeTable.Cols.SUSPECT_PHONE_NUMBER + " text, " +
                CrimeDbSchema.CrimeTable.Cols.REQUIRES_POLICE + " integer default 0" +
                ")"
        );

        addColumnIfMissing(db, CrimeDbSchema.CrimeTable.Cols.SUSPECT, "text");
        addColumnIfMissing(db, CrimeDbSchema.CrimeTable.Cols.SUSPECT_PHONE_NUMBER, "text");
        addColumnIfMissing(db, CrimeDbSchema.CrimeTable.Cols.REQUIRES_POLICE, "integer default 0");
    }

    private void addColumnIfMissing(SQLiteDatabase db, String columnName, String columnDefinition) {
        if (hasColumn(db, CrimeDbSchema.CrimeTable.NAME, columnName)) {
            return;
        }

        db.execSQL(
                "alter table " + CrimeDbSchema.CrimeTable.NAME + " add column " + columnName + " " + columnDefinition
        );
    }

    private boolean hasColumn(SQLiteDatabase db, String tableName, String columnName) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
        try {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (columnName.equals(cursor.getString(nameIndex))) {
                    return true;
                }
            }
            return false;
        } finally {
            cursor.close();
        }
    }
}
