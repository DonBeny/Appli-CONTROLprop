package org.orgaprop.test7.models;

import android.content.ContentValues;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.orgaprop.test7.databases.PrefDatabase;

@Entity(tableName = PrefDatabase.PREF_TABLE_NAME)
public class Pref {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = PrefDatabase.PREF_COL_ID_NAME)
    private long id;

    @ColumnInfo(name = PrefDatabase.PREF_COL_PARAM_NAME)
    private String param;

    @ColumnInfo(name = PrefDatabase.PREF_COL_VALUE_NAME)
    private String value;

    @Ignore
    public Pref() {}
    public Pref(String param, String value) {
        this.param = param;
        this.value = value;
    }

    // --- GETTERS ---

    public long getId() { return this.id; }
    public String getParam() { return this.param; }
    public String getValue() { return this.value; }

    // --- SETTERS ---

    public void setId(long id) { this.id = id; }
    public void setParam(String param) { this.param = param; }
    public void setValue(String value) { this.value = value; }

    // --- UTILS ---

    public static Pref fromContentValues(ContentValues values) {
        final Pref pref = new Pref();

        if( values.containsKey("id") ) {
            pref.setId(values.getAsLong("id"));
        }
        if( values.containsKey("param") ) {
            pref.setParam(values.getAsString("param"));
        }
        if( values.containsKey("value") ) {
            pref.setValue(values.getAsString("value"));
        }

        return pref;
    }

}
