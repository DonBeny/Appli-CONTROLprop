package org.orgaprop.test7.databases.dao;

import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.orgaprop.test7.databases.PrefDatabase;
import org.orgaprop.test7.models.Contact;

@Dao
public interface ContactDao {

    @Query("SELECT * FROM "+PrefDatabase.CONTACT_TABLE_NAME) LiveData<Contact> getAllContacts();
    @Query("SELECT * FROM "+PrefDatabase.CONTACT_TABLE_NAME+" WHERE "+ PrefDatabase.CONTACT_COL_ADR+" LIKE '%'+:txt+'%'") LiveData<Contact> getContact(String txt);
    @Query("SELECT * FROM "+PrefDatabase.CONTACT_TABLE_NAME) Cursor getAllContactsWithCursor();
    @Query("SELECT * FROM "+PrefDatabase.CONTACT_TABLE_NAME+" WHERE "+ PrefDatabase.CONTACT_COL_ADR+" LIKE '%'+:txt+'%'") Cursor getContactWithCursor(String txt);

    @Insert
    long insertContact(Contact address);

    @Update
    int updateContact(Contact address);

    @Query("DELETE FROM "+PrefDatabase.CONTACT_TABLE_NAME+" WHERE "+PrefDatabase.CONTACT_COL_ID+" = :addressId") int deleteContact(long addressId);

}
