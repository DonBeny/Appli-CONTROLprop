package org.orgaprop.test7.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Looper;

import org.orgaprop.test7.databases.PrefDatabase;
import org.orgaprop.test7.models.Storage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class Storages {

//************ PRIVATE VARIABLES

    private final Context mContext;

//************ STATIC VARIABLES

    public static final String TAG = "Storages";

//************ CONSTRUCTORS

    public Storages(Context context) {
        mContext = context;
    }

//************ SETTERS

    public void addStorage(ContentValues values) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Looper.prepare();

            PrefDatabase.getInstance(mContext).mStorageDao().insertStorage(Storage.fromContentValues(values));

            Looper.loop();
        });
    }

//************ GETTERS

    public Storage getStorage(String residId) {
        Storage result = new Storage();
        Cursor cursor = PrefDatabase.getInstance(mContext).mStorageDao().getStorageRsdWithCursor(Integer.parseInt(residId));

        if( cursor != null && cursor.moveToFirst() ) {
            result = makStorage(cursor);
        }

        return result;
    }
    public List<Storage> getAllStorages() {
        List<Storage> result = new ArrayList<>();
        Cursor cursor = PrefDatabase.getInstance(mContext).mStorageDao().getAllStorageWithCursor();

        if( cursor != null && cursor.moveToFirst() ) {

            do {
                result.add(makStorage(cursor));
            } while (cursor.moveToNext());

            cursor.close();
        }

        return result;
    }
    public Cursor getAllStoragesWithCursor() {
        return PrefDatabase.getInstance(mContext).mStorageDao().getAllStorageWithCursor();
    }

//************ UTILS

    public static Storage makStorage(Cursor cursor) {
        Storage r = new Storage();

        r.setId(cursor.getLong(PrefDatabase.STORAGE_COL_ID_NUM));
        r.setResid(cursor.getInt(PrefDatabase.STORAGE_COL_RESID_NUM));
        r.setDate(cursor.getInt(PrefDatabase.STORAGE_COL_DATE_NUM));
        r.setTypeCtrl(cursor.getString(PrefDatabase.STORAGE_COL_TYPE_CTRL_NUM));
        r.setCtrl_type(cursor.getString(PrefDatabase.STORAGE_COL_CTRL_TYPE_NUM));
        r.setCtrl_ctrl(cursor.getString(PrefDatabase.STORAGE_COL_CTRL_CTRL_NUM));
        r.setCtrl_sig1(cursor.getString(PrefDatabase.STORAGE_COL_CTRL_SIG1_NUM));
        r.setCtrl_sig2(cursor.getString(PrefDatabase.STORAGE_COL_CTRL_SIG2_NUM));
        r.setCtrl_sig(cursor.getString(PrefDatabase.STORAGE_COL_CTRL_SIG_NUM));
        r.setPlan_end(cursor.getInt(PrefDatabase.STORAGE_COL_PLAN_END_NUM));
        r.setPlan_content(cursor.getString(PrefDatabase.STORAGE_COL_PLAN_CONTENT_NUM));
        r.setPlan_validate(cursor.getInt(PrefDatabase.STORAGE_COL_PLAN_VALIDATE_NUM) == 1);
        r.setSend_dest(cursor.getString(PrefDatabase.STORAGE_COL_SEND_DEST_NUM));
        r.setSend_idPlan(cursor.getInt(PrefDatabase.STORAGE_COL_SEND_ID_PLAN_NUM));
        r.setSend_dateCtrl(cursor.getInt(PrefDatabase.STORAGE_COL_SEND_DATE_CTRL_NUM));
        r.setSend_typeCtrl(cursor.getString(PrefDatabase.STORAGE_COL_SEND_TYPE_CTRL_NUM));
        r.setSend_src(cursor.getString(PrefDatabase.STORAGE_COL_SEND_SRC_NUM));

        return r;
    }

}
