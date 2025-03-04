package org.orgaprop.test7.models;

import android.content.ContentValues;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.orgaprop.test7.databases.PrefDatabase;

@Entity(tableName = PrefDatabase.CONTACT_TABLE_NAME)
public class Contact {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = PrefDatabase.CONTACT_COL_ID)
    private int id;

    @ColumnInfo(name = PrefDatabase.CONTACT_COL_ADR)
    private String address;

    @Ignore
    public Contact() {}
    public Contact(String address) {
        this.address = address;
    }

    // --- GETTERS ---

    public int getId() { return this.id; }
    public String getAddress() { return this.address; }

    // --- SETTERS ---

    public void setId(int id) { this.id = id; }
    public void setAddress(String address) { this.address = address; }

    // --- UTILS ---

    public static Contact fromContentValues(ContentValues values) {
        final Contact contact = new Contact();

        if( values.containsKey("id") ) {
            contact.setId(values.getAsInteger("id"));
        }
        if( values.containsKey("address") ) {
            contact.setAddress(values.getAsString("address"));
        }
        if( values.containsKey("mail") ) {
            contact.setAddress(values.getAsString("mail"));
        }
        if( values.containsKey("courriel") ) {
            contact.setAddress(values.getAsString("courriel"));
        }
        if( values.containsKey("contact") ) {
            contact.setAddress(values.getAsString("contact"));
        }

        return contact;
    }

}
