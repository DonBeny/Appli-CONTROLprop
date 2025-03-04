package org.orgaprop.test7.databases.dao;

import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import org.orgaprop.test7.databases.PrefDatabase;
import org.orgaprop.test7.models.Storage;
import org.orgaprop.test7.services.MyDataProvider;

@Dao
public interface StorageDao {

    @Query("SELECT * FROM "+PrefDatabase.STORAGE_TABLE_NAME) LiveData<Storage> getAllStorage();
    @Query("SELECT * FROM "+PrefDatabase.STORAGE_TABLE_NAME+" WHERE "+PrefDatabase.STORAGE_COL_ID+" = :storageId") LiveData<Storage> getStorage(int storageId);
    @Query("SELECT * FROM "+PrefDatabase.STORAGE_TABLE_NAME+" WHERE "+PrefDatabase.STORAGE_COL_RESID+" = :storageRsd") LiveData<Storage> getStorageRsd(int storageRsd);
    @Query("SELECT * FROM "+PrefDatabase.STORAGE_TABLE_NAME) Cursor getAllStorageWithCursor();
    @Query("SELECT * FROM "+PrefDatabase.STORAGE_TABLE_NAME+" WHERE "+PrefDatabase.STORAGE_COL_ID+" = :storageId") Cursor getStorageWithCursor(int storageId);
    @Query("SELECT * FROM "+PrefDatabase.STORAGE_TABLE_NAME+" WHERE "+PrefDatabase.STORAGE_COL_RESID+" = :storageRsd") Cursor getStorageRsdWithCursor(int storageRsd);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertStorage(Storage storage);

    @Update
    int updateStorage(Storage storage);

    @Query("DELETE FROM "+PrefDatabase.STORAGE_TABLE_NAME+" WHERE "+PrefDatabase.STORAGE_COL_ID+" = :storageId") int deleteStorageById(long storageId);
    @Query("DELETE FROM "+PrefDatabase.STORAGE_TABLE_NAME+" WHERE "+PrefDatabase.STORAGE_COL_RESID+" = :storageRsd") int deleteStorageByRsd(long storageRsd);
    @Query("DELETE FROM "+PrefDatabase.STORAGE_TABLE_NAME) int deleteAllStorage();

}
