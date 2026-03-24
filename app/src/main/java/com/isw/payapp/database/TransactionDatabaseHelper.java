package com.isw.payapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.isw.payapp.model.Receipt;

import java.util.ArrayList;
import java.util.List;

public class TransactionDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "TransactionDBHelper";
    private static final String DATABASE_NAME = "transactions.db";
    private static final int DATABASE_VERSION = 1;

    // Table name
    public static final String TABLE_TRANSACTIONS = "transactions";

    // Column names
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TRANSACTION_DATE = "transaction_date";
    public static final String COLUMN_CARD_NUMBER = "card_number";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_CURRENCY = "currency";
    public static final String COLUMN_MERCHANT = "merchant";
    public static final String COLUMN_BANK = "bank";
    public static final String COLUMN_TERMINAL_ID = "terminal_id";
    public static final String COLUMN_TRANSACTION_TYPE = "transaction_type";
    public static final String COLUMN_ENTRY_MODE = "entry_mode";
    public static final String COLUMN_AID = "aid";
    public static final String COLUMN_ATC = "atc";
    public static final String COLUMN_TVR = "tvr";
    public static final String COLUMN_RESPONSE = "response";
    public static final String COLUMN_STAN = "stan";
    public static final String COLUMN_AUTH_ID = "auth_id";
    public static final String COLUMN_REFERENCE_NUMBER = "reference_number";
    public static final String COLUMN_CARDHOLDER_NAME = "cardholder_name";
    public static final String COLUMN_FULL_DATA = "full_data";
    public static final String COLUMN_TELLER = "teller_name"; // JSON of full receipt

    // Create table SQL statement
    private static final String CREATE_TABLE_TRANSACTIONS =
            "CREATE TABLE " + TABLE_TRANSACTIONS + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_TRANSACTION_DATE + " TEXT NOT NULL," +
                    COLUMN_CARD_NUMBER + " TEXT NOT NULL," +
                    COLUMN_AMOUNT + " TEXT," +
                    COLUMN_CURRENCY + " TEXT," +
                    COLUMN_MERCHANT + " TEXT," +
                    COLUMN_BANK + " TEXT," +
                    COLUMN_TERMINAL_ID + " TEXT," +
                    COLUMN_TRANSACTION_TYPE + " TEXT," +
                    COLUMN_ENTRY_MODE + " TEXT," +
                    COLUMN_AID + " TEXT," +
                    COLUMN_ATC + " TEXT," +
                    COLUMN_TVR + " TEXT," +
                    COLUMN_RESPONSE + " TEXT," +
                    COLUMN_STAN + " TEXT," +
                    COLUMN_AUTH_ID + " TEXT," +
                    COLUMN_REFERENCE_NUMBER + " TEXT," +
                    COLUMN_CARDHOLDER_NAME + " TEXT DEFAULT 'Card Holder'," +
                    COLUMN_FULL_DATA + " TEXT," +
                    COLUMN_TELLER +" TEXT"+
                    ");";

    // Index for faster queries by date
    private static final String CREATE_INDEX_DATE =
            "CREATE INDEX idx_transaction_date ON " + TABLE_TRANSACTIONS + "(" + COLUMN_TRANSACTION_DATE + ");";

    public TransactionDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TRANSACTIONS);
        db.execSQL(CREATE_INDEX_DATE);
        Log.d(TAG, "Database tables created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        onCreate(db);
    }

    /**
     * Save a transaction receipt to the database
     */
    public long saveTransaction(Receipt receipt) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_TRANSACTION_DATE, receipt.getDateTime());
        values.put(COLUMN_CARD_NUMBER, receipt.getCardNumber());
        values.put(COLUMN_AMOUNT, receipt.getAmount());
        values.put(COLUMN_CURRENCY, receipt.getCurrency());
        values.put(COLUMN_MERCHANT, receipt.getMerchant());
        values.put(COLUMN_BANK, receipt.getBank());
        values.put(COLUMN_TERMINAL_ID, receipt.getTerminalId());
        values.put(COLUMN_TRANSACTION_TYPE, receipt.getTransactionType());
        values.put(COLUMN_ENTRY_MODE, receipt.getEntryMode());
        values.put(COLUMN_AID, receipt.getAid());
        values.put(COLUMN_ATC, receipt.getAtc());
        values.put(COLUMN_TVR, receipt.getTvr());
        values.put(COLUMN_RESPONSE, receipt.getResponse());
        values.put(COLUMN_STAN, receipt.getStan());
        values.put(COLUMN_AUTH_ID, receipt.getAuthId());
        values.put(COLUMN_REFERENCE_NUMBER, receipt.getReferenceNumber());

        // For cardholder name, you might need to get this from elsewhere
        // Defaulting to "Card Holder" for now
        values.put(COLUMN_CARDHOLDER_NAME, receipt.getCardHolderName());
        values.put(COLUMN_TELLER, receipt.getTeller());

        // Store full receipt data as JSON (optional)
        // values.put(COLUMN_FULL_DATA, receipt.toJson());

        long id = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();

        Log.d(TAG, "Transaction saved with ID: " + id);
        return id;
    }

    /**
     * Get all transactions, most recent first
     */
    public List<Receipt> getAllTransactions() {
        List<Receipt> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_TRANSACTIONS +
                " ORDER BY " + COLUMN_TRANSACTION_DATE + " DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Receipt receipt = new Receipt();
                receipt.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                receipt.setDateTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_DATE)));
                receipt.setCardNumber(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_NUMBER)));
                receipt.setAmount(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                receipt.setCurrency(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)));
                receipt.setMerchant(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MERCHANT)));
                receipt.setBank(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BANK)));
                receipt.setTerminalId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_ID)));
                receipt.setTransactionType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_TYPE)));
                receipt.setEntryMode(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENTRY_MODE)));
                receipt.setAid(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AID)));
                receipt.setAtc(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ATC)));
                receipt.setTvr(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TVR)));
                receipt.setResponse(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RESPONSE)));
                receipt.setStan(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STAN)));
                receipt.setAuthId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTH_ID)));
                receipt.setReferenceNumber(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REFERENCE_NUMBER)));
                receipt.setTeller(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TELLER)));
                receipt.setCardHolderName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARDHOLDER_NAME)));
                transactions.add(receipt);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return transactions;
    }

    /**
     * Get transaction by ID
     */
    public Receipt getTransactionById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Receipt receipt = null;

        Cursor cursor = db.query(TABLE_TRANSACTIONS, null,
                COLUMN_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null);

        if (cursor.moveToFirst()) {
            receipt = new Receipt();
            receipt.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            receipt.setDateTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_DATE)));
            receipt.setCardNumber(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_NUMBER)));
            receipt.setAmount(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
            receipt.setCurrency(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)));
            receipt.setMerchant(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MERCHANT)));
            receipt.setBank(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BANK)));
            receipt.setTerminalId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_ID)));
            receipt.setTransactionType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_TYPE)));
            receipt.setEntryMode(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENTRY_MODE)));
            receipt.setAid(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AID)));
            receipt.setAtc(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ATC)));
            receipt.setTvr(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TVR)));
            receipt.setResponse(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RESPONSE)));
            receipt.setStan(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STAN)));
            receipt.setAuthId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTH_ID)));
            receipt.setReferenceNumber(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REFERENCE_NUMBER)));
            receipt.setTeller(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TELLER)));
            receipt.setCardHolderName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARDHOLDER_NAME)));
        }

        cursor.close();
        db.close();

        return receipt;
    }

    /**
     * Delete transaction by ID
     */
    public boolean deleteTransaction(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_TRANSACTIONS, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return result > 0;
    }

    /**
     * Get transactions for today only
     */
    public List<Receipt> getTodayTransactions() {
        List<Receipt> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Get today's date in format yyyy-MM-dd
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());

        String query = "SELECT * FROM " + TABLE_TRANSACTIONS +
                " WHERE " + COLUMN_TRANSACTION_DATE + " LIKE '" + today + "%'" +
                " ORDER BY " + COLUMN_TRANSACTION_DATE + " DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Receipt receipt = new Receipt();
                receipt.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                receipt.setDateTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_DATE)));
                receipt.setCardNumber(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_NUMBER)));
                receipt.setAmount(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT)));
                receipt.setCurrency(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)));
                receipt.setMerchant(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MERCHANT)));
                receipt.setBank(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BANK)));
                receipt.setTerminalId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TERMINAL_ID)));
                receipt.setTransactionType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSACTION_TYPE)));

                transactions.add(receipt);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return transactions;
    }

    /**
     * Get total number of transactions
     */
    public int getTransactionCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_TRANSACTIONS;
        Cursor cursor = db.rawQuery(query, null);

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }

    /**
     * Clear all transactions
     */
    public void clearAllTransactions() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, null, null);
        db.close();
    }
}