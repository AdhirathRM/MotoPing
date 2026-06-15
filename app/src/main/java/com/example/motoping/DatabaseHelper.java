package com.example.motoping;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "VehicleTracker.db";
    private static final int DATABASE_VERSION = 2; // INCREMENTED FOR COLOR UPGRADE

    private static final String TABLE_VEHICLES = "vehicles";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_INSURANCE = "insurance_expiry";
    private static final String COLUMN_SERVICE = "service_due";
    private static final String COLUMN_PUC = "puc_due";
    private static final String COLUMN_RC = "rc_expiry";
    private static final String COLUMN_COLOR = "color_hex"; // NEW COLUMN

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_VEHICLES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_INSURANCE + " TEXT, " +
                COLUMN_SERVICE + " TEXT, " +
                COLUMN_PUC + " TEXT, " +
                COLUMN_RC + " TEXT, " +
                COLUMN_COLOR + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VEHICLES);
        onCreate(db);
    }

    // NEW METHOD: Takes the color
    public void addVehicleWithColor(String name, String insurance, String service, String puc, String rc, String colorHex) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_INSURANCE, insurance);
        values.put(COLUMN_SERVICE, service);
        values.put(COLUMN_PUC, puc);
        values.put(COLUMN_RC, rc);
        values.put(COLUMN_COLOR, colorHex);

        db.insert(TABLE_VEHICLES, null, values);
        db.close();
    }

    // OLD METHOD: Kept so your app doesn't crash right now. Defaults to Red.
    public void addVehicle(String name, String insurance, String service, String puc, String rc) {
        addVehicleWithColor(name, insurance, service, puc, rc, "#EF5350");
    }

    public List<Vehicle> getAllVehicles() {
        List<Vehicle> vehicleList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_VEHICLES;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Vehicle vehicle = new Vehicle(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getString(6) // The color string
                );
                vehicleList.add(vehicle);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return vehicleList;
    }

    public void deleteVehicle(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_VEHICLES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // NEW METHOD: Updates the color too
    public void updateVehicleWithColor(int id, String name, String insurance, String service, String puc, String rc, String colorHex) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_INSURANCE, insurance);
        values.put(COLUMN_SERVICE, service);
        values.put(COLUMN_PUC, puc);
        values.put(COLUMN_RC, rc);
        values.put(COLUMN_COLOR, colorHex);

        db.update(TABLE_VEHICLES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // OLD METHOD: Kept for safety
    public void updateVehicle(int id, String name, String insurance, String service, String puc, String rc) {
        updateVehicleWithColor(id, name, insurance, service, puc, rc, "#EF5350");
    }
}