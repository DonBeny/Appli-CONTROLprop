package org.orgaprop.test7.services;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.orgaprop.test7.databases.PrefDatabase;
import org.orgaprop.test7.models.Contact;
import org.orgaprop.test7.models.Pref;
import org.orgaprop.test7.models.Storage;

public class MyDataProvider extends ContentProvider {

    public static final String TAG = "MyDataProvider";

    public static final String AUTHORITY = "fr.benysoftware.ControlProp.PrefProvider";

    public static final String PREF_TABLE_NAME = Pref.class.getSimpleName();
    public static final String CONTACT_TABLE_NAME = Contact.class.getSimpleName();
    public static final String STORAGE_TABLE_NAME = Storage.class.getSimpleName();

    public static final String STRING_URI_PREF = "content://" + AUTHORITY + "/" + PREF_TABLE_NAME;
    public static final String STRING_URI_CONTACT = "content://" + AUTHORITY + "/" + CONTACT_TABLE_NAME;
    public static final String STRING_URI_STORAGE = "content://" + AUTHORITY + "/" + STORAGE_TABLE_NAME;

    public static final Uri URI_PREF = Uri.parse(STRING_URI_PREF);
    public static final Uri URI_CONTACT = Uri.parse(STRING_URI_CONTACT);
    public static final Uri URI_STORAGE = Uri.parse(STRING_URI_STORAGE);

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sort) {
        if (getContext() != null) {
            Cursor cursor = null;

            try {
                if (projection == null && selection == null && selectionArgs == null) {
                    if (URI_PREF.equals(uri)) {
                        long paramId = ContentUris.parseId(uri);
                        cursor = PrefDatabase.getInstance(getContext()).mPrefDao().getPrefFromIdWithCursor(paramId);
                    } else if (URI_CONTACT.equals(uri)) {
                        cursor = PrefDatabase.getInstance(getContext()).mContactDao().getAllContactsWithCursor();
                    } else if (URI_STORAGE.equals(uri)) {
                        cursor = PrefDatabase.getInstance(getContext()).mStorageDao().getAllStorageWithCursor();
                    }
                } else if (selection != null && selectionArgs != null) {
                    if (selection.equals("param")) {
                        cursor = PrefDatabase.getInstance(getContext()).mPrefDao().getPrefFromParamWithCursor(selectionArgs[0]);
                    } else if (selection.equals("address")) {
                        cursor = PrefDatabase.getInstance(getContext()).mContactDao().getContactWithCursor(selectionArgs[0]);
                    } else if (selection.equals("storage")) {
                        cursor = PrefDatabase.getInstance(getContext()).mStorageDao().getStorageWithCursor(Integer.parseInt(selectionArgs[0]));
                    }
                }

                if (cursor != null) {
                    cursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return cursor;
                }

            } catch (Exception e) {
                Log.e(TAG, "Query error: " + e.getMessage(), e);
            }
        }

        throw new IllegalArgumentException("Failed to query row for uri " + uri);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        if (URI_PREF.equals(uri)) {
            return "vnd.android.cursor.item/" + AUTHORITY + "." + PREF_TABLE_NAME;
        } else if (URI_CONTACT.equals(uri)) {
            return "vnd.android.cursor.item/" + AUTHORITY + "." + CONTACT_TABLE_NAME;
        } else if (URI_STORAGE.equals(uri)) {
            return "vnd.android.cursor.item/" + AUTHORITY + "." + STORAGE_TABLE_NAME;
        }

        throw new IllegalArgumentException("Unknown URI: " + uri);
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String where, @Nullable String[] whereArgs) {
        return 0;
    }
}

