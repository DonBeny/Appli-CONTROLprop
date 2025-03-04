package org.orgaprop.test7.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.orgaprop.test7.databases.PrefDatabase;
import org.orgaprop.test7.models.Pref;

import java.util.concurrent.Executors;

public class Prefs {

//************ PRIVATE VARIABLES

    private final Context mContext;

//************ STATIC VARIABLES

    public static final String TAG = "Prefs";

//************ CONSTRUCTORS

    public Prefs(Context context) {
        mContext = context;
    }

//************ SETTERS

    public void setMbr(String idMbr) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            ContentValues values = new ContentValues();

            values.put("id", Long.parseLong(PrefDatabase.PREF_ROW_ID_MBR_NUM));
            values.put("param", PrefDatabase.PREF_ROW_ID_MBR);
            values.put("value", idMbr);

            PrefDatabase.getInstance(mContext).mPrefDao().updatePref(Pref.fromContentValues(values));

            Looper.loop();
        });
    }
    public void setAdrMac(String adrMac) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            ContentValues values = new ContentValues();

            values.put("id", Long.parseLong(PrefDatabase.PREF_ROW_ADR_MAC_NUM));
            values.put("param", PrefDatabase.PREF_ROW_ADR_MAC);
            values.put("value", adrMac);

            PrefDatabase.getInstance(mContext).mPrefDao().updatePref(Pref.fromContentValues(values));

            Looper.loop();
        });
    }
    public void setAgency(String agency) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            ContentValues values = new ContentValues();

            values.put("id", Long.parseLong(PrefDatabase.PREF_ROW_AGENCY_NUM));
            values.put("param", PrefDatabase.PREF_ROW_AGENCY);
            values.put("value", agency);

            PrefDatabase.getInstance(mContext).mPrefDao().updatePref(Pref.fromContentValues(values));

            Looper.loop();
        });
    }
    public void setGroup(String group) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            ContentValues values = new ContentValues();

            values.put("id", Long.parseLong(PrefDatabase.PREF_ROW_GROUP_NUM));
            values.put("param", PrefDatabase.PREF_ROW_GROUP);
            values.put("value", group);

            PrefDatabase.getInstance(mContext).mPrefDao().updatePref(Pref.fromContentValues(values));

            Looper.loop();
        });
    }
    public void setResidence(String residence) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            ContentValues values = new ContentValues();

            values.put("id", Long.parseLong(PrefDatabase.PREF_ROW_RESIDENCE_NUM));
            values.put("param", PrefDatabase.PREF_ROW_RESIDENCE);
            values.put("value", residence);

            PrefDatabase.getInstance(mContext).mPrefDao().updatePref(Pref.fromContentValues(values));

            Looper.loop();
        });
    }

//************ GETTERS

    public void getMbr(Callback<String> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            String result = "new";
            Cursor cursor = PrefDatabase.getInstance(mContext).mPrefDao().getPrefFromParamWithCursor(PrefDatabase.PREF_ROW_ID_MBR);

            if( cursor != null && cursor.moveToFirst() ) {
                result = cursor.getString(2);
                cursor.close();
            }

            String finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult));
        });
    }
    public void getAdrMac(Callback<String> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            String result = "new";
            Cursor cursor = PrefDatabase.getInstance(mContext).mPrefDao().getPrefFromParamWithCursor(PrefDatabase.PREF_ROW_ADR_MAC);

            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(2);
                cursor.close();
            }

            String finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult));
        });
    }
    public void getAgency(Callback<String> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            String result = "";
            Cursor cursor = PrefDatabase.getInstance(mContext).mPrefDao().getPrefFromParamWithCursor(PrefDatabase.PREF_ROW_AGENCY);

            if( cursor != null && cursor.moveToFirst() ) {
                result = cursor.getString(2);
                cursor.close();
            }

            String finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult));
        });
    }
    public void getGroup(Callback<String> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            String result = "";
            Cursor cursor = PrefDatabase.getInstance(mContext).mPrefDao().getPrefFromParamWithCursor(PrefDatabase.PREF_ROW_GROUP);

            if( cursor != null && cursor.moveToFirst() ) {
                result = cursor.getString(2);
                cursor.close();
            }

            String finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult));
        });
    }
    public void getResidence(Callback<String> callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            String result = "";
            Cursor cursor = PrefDatabase.getInstance(mContext).mPrefDao().getPrefFromParamWithCursor(PrefDatabase.PREF_ROW_RESIDENCE);

            if( cursor != null && cursor.moveToFirst() ) {
                result = cursor.getString(2);
                cursor.close();
            }

            String finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(finalResult));
        });
    }

//************** INTERFACES

    public interface Callback<T> {
        void onResult(T result);
    }

}
