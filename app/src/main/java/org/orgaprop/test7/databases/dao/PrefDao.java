package org.orgaprop.test7.databases.dao;

import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.orgaprop.test7.databases.PrefDatabase;
import org.orgaprop.test7.models.Pref;

@Dao
public interface PrefDao {

    @Query("SELECT * FROM "+PrefDatabase.PREF_TABLE_NAME+" WHERE "+PrefDatabase.PREF_COL_PARAM_NAME+" = :param") LiveData<Pref> getPrefFromParam(String param);
    @Query("SELECT * FROM "+PrefDatabase.PREF_TABLE_NAME+" WHERE "+PrefDatabase.PREF_COL_ID_NAME+" = :paramId") LiveData<Pref> getPrefFromId(long paramId);
    @Query("SELECT * FROM "+PrefDatabase.PREF_TABLE_NAME+" WHERE "+PrefDatabase.PREF_COL_PARAM_NAME+" = :param") Cursor getPrefFromParamWithCursor(String param);
    @Query("SELECT * FROM "+PrefDatabase.PREF_TABLE_NAME+" WHERE "+PrefDatabase.PREF_COL_ID_NAME+" = :paramId") Cursor getPrefFromIdWithCursor(long paramId);

    @Insert
    long insertPref(Pref pref);

    @Update
    int updatePref(Pref pref);

    @Query("DELETE FROM "+PrefDatabase.PREF_TABLE_NAME+" WHERE "+PrefDatabase.PREF_COL_PARAM_NAME+" = :param") int deletePref(String param);
    @Query("DELETE FROM "+PrefDatabase.PREF_TABLE_NAME+" WHERE "+PrefDatabase.PREF_COL_ID_NAME+" = :paramId") int deletePref(long paramId);

}
