package org.orgaprop.test7.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Looper;
import android.util.Log;

import org.orgaprop.test7.databases.PrefDatabase;
import org.orgaprop.test7.models.Contact;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class Contacts {

//************ PRIVATE VARIABLES

    public static final String TAG = "Contacts";

    private Context mContext;

//************ STATIC VARIABLES



//************ CONSTRUCTORS

    public Contacts(Context context) {
        mContext = context;
    }

//************ SETTERS

    public void setContact(String address) {
        Executors.newSingleThreadExecutor().execute(() -> {
            ContentValues values = new ContentValues();

            values.put("address", address);

            PrefDatabase.getInstance(mContext).mContactDao().insertContact(Contact.fromContentValues(values));
        });
    }

//************ GETTERS

    public String getContact(String contact) {
        String result = "";

        try (Cursor cursor = PrefDatabase.getInstance(mContext).mContactDao().getContactWithCursor(contact)) {
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(PrefDatabase.CONTACT_COL_ADR_NUM);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching contact: " + e.getMessage(), e);
        }

        return result;
    }
    public String getAllContacts() {
        StringBuilder result = new StringBuilder();

        try (Cursor cursor = PrefDatabase.getInstance(mContext).mContactDao().getAllContactsWithCursor()) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    if (result.length() > 0) {
                        result.append(";");
                    }
                    result.append(cursor.getString(PrefDatabase.CONTACT_COL_ADR_NUM));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching all contacts: " + e.getMessage(), e);
        }

        return result.toString();
    }
    public List<String> getListContacts() {
        List<String> result = new ArrayList<>();

        try (Cursor cursor = PrefDatabase.getInstance(mContext).mContactDao().getAllContactsWithCursor()) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    result.add(cursor.getString(PrefDatabase.CONTACT_COL_ADR_NUM));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching contact list: " + e.getMessage(), e);
        }

        return result;
    }
    public Cursor getAllContactsWithCursor() {
        return PrefDatabase.getInstance(mContext).mContactDao().getAllContactsWithCursor();
    }

}
