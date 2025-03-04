package org.orgaprop.test7.databases;

import android.content.ContentValues;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.OnConflictStrategy;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.orgaprop.test7.databases.dao.ContactDao;
import org.orgaprop.test7.databases.dao.PrefDao;
import org.orgaprop.test7.databases.dao.StorageDao;
import org.orgaprop.test7.models.Contact;
import org.orgaprop.test7.models.Pref;
import org.orgaprop.test7.models.Storage;

@Database(entities = {Pref.class, Contact.class, Storage.class}, version = 3, exportSchema = false)
public abstract class PrefDatabase extends RoomDatabase {

    public static final String TAG = "PrefDatabase";

    public static final String PREF_TABLE_NAME = "Pref";
    public static final String PREF_COL_ID_NAME = "id";
    public static final int PREF_COL_ID_NUM = 0;
    public static final String PREF_COL_PARAM_NAME = "param";
    public static final int PREF_COL_PARAM_NUM = 1;
    public static final String PREF_COL_VALUE_NAME = "value";
    public static final int PREF_COL_VALUE_NUM = 2;

    public static final String PREF_ROW_ID_MBR = "id_mbr";
    public static final String PREF_ROW_ID_MBR_NUM = "1";
    public static final String PREF_ROW_ADR_MAC = "adr_mac";
    public static final String PREF_ROW_ADR_MAC_NUM = "2";
    public static final String PREF_ROW_AGENCY = "agc";
    public static final String PREF_ROW_AGENCY_NUM = "3";
    public static final String PREF_ROW_GROUP = "grp";
    public static final String PREF_ROW_GROUP_NUM = "4";
    public static final String PREF_ROW_RESIDENCE = "rsd";
    public static final String PREF_ROW_RESIDENCE_NUM = "5";

    public static final String CONTACT_TABLE_NAME = "Contact";
    public static final String CONTACT_COL_ID = "id";
    public static final int CONTACT_COL_ID_NUM = 0;
    public static final String CONTACT_COL_ADR = "address";
    public static final int CONTACT_COL_ADR_NUM = 1;

    public static final String STORAGE_TABLE_NAME = "Storage";
    public static final String STORAGE_COL_ID = "id";
    public static final int STORAGE_COL_ID_NUM = 0;
    public static final String STORAGE_COL_RESID = "resid";
    public static final int STORAGE_COL_RESID_NUM = 1;
    public static final String STORAGE_COL_DATE = "date";
    public static final int STORAGE_COL_DATE_NUM = 2;
    public static final String STORAGE_COL_CONFIG = "config";
    public static final int STORAGE_COL_CONFIG_NUM = 3;
    public static final String STORAGE_COL_TYPE_CTRL = "typeCtrl";
    public static final int STORAGE_COL_TYPE_CTRL_NUM = 4;
    public static final String STORAGE_COL_CTRL_TYPE = "ctrl_type";
    public static final int STORAGE_COL_CTRL_TYPE_NUM = 5;
    public static final String STORAGE_COL_CTRL_CTRL = "ctrl_ctrl";
    public static final int STORAGE_COL_CTRL_CTRL_NUM = 6;
    public static final String STORAGE_COL_CTRL_SIG1 = "ctrl_sig1";
    public static final int STORAGE_COL_CTRL_SIG1_NUM = 7;
    public static final String STORAGE_COL_CTRL_SIG2 = "ctrl_sig2";
    public static final int STORAGE_COL_CTRL_SIG2_NUM = 8;
    public static final String STORAGE_COL_CTRL_SIG = "ctrl_sig";
    public static final int STORAGE_COL_CTRL_SIG_NUM = 9;
    public static final String STORAGE_COL_PLAN_END = "plan_end";
    public static final int STORAGE_COL_PLAN_END_NUM = 10;
    public static final String STORAGE_COL_PLAN_CONTENT = "plan_content";
    public static final int STORAGE_COL_PLAN_CONTENT_NUM = 11;
    public static final String STORAGE_COL_PLAN_VALIDATE = "plan_validate";
    public static final int STORAGE_COL_PLAN_VALIDATE_NUM = 12;
    public static final String STORAGE_COL_SEND_DEST = "send_dest";
    public static final int STORAGE_COL_SEND_DEST_NUM = 13;
    public static final String STORAGE_COL_SEND_ID_PLAN = "send_idPlan";
    public static final int STORAGE_COL_SEND_ID_PLAN_NUM = 14;
    public static final String STORAGE_COL_SEND_DATE_CTRL = "send_dateCtrl";
    public static final int STORAGE_COL_SEND_DATE_CTRL_NUM = 15;
    public static final String STORAGE_COL_SEND_TYPE_CTRL = "send_typeCtrl";
    public static final int STORAGE_COL_SEND_TYPE_CTRL_NUM = 16;
    public static final String STORAGE_COL_SEND_SRC = "send_src";
    public static final int STORAGE_COL_SEND_SRC_NUM = 17;

    public static final String[] STORAGE_PROJECTION = {
        STORAGE_COL_ID,
        STORAGE_COL_RESID,
        STORAGE_COL_DATE,
        STORAGE_COL_CONFIG,
        STORAGE_COL_TYPE_CTRL,
        STORAGE_COL_CTRL_TYPE,
        STORAGE_COL_CTRL_CTRL,
        STORAGE_COL_CTRL_SIG1,
        STORAGE_COL_CTRL_SIG2,
        STORAGE_COL_CTRL_SIG,
        STORAGE_COL_PLAN_END,
        STORAGE_COL_PLAN_CONTENT,
        STORAGE_COL_PLAN_VALIDATE,
        STORAGE_COL_SEND_DEST,
        STORAGE_COL_SEND_ID_PLAN,
        STORAGE_COL_SEND_DATE_CTRL,
        STORAGE_COL_SEND_TYPE_CTRL,
        STORAGE_COL_SEND_SRC
    };

    // --- MIGRATIONS ---
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + CONTACT_TABLE_NAME +
                    "(" + CONTACT_COL_ID + " INTEGER PRIMARY KEY NOT NULL" +
                    "," + CONTACT_COL_ADR + " TEXT UNIQUE" +
                    ")");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + STORAGE_TABLE_NAME +
                    "(" + STORAGE_COL_ID + " INTEGER PRIMARY KEY NOT NULL" +
                    "," + STORAGE_COL_RESID + " INTEGER UNIQUE NOT NULL" +
                    "," + STORAGE_COL_DATE + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_CONFIG + " TEXT NOT NULL" +
                    "," + STORAGE_COL_TYPE_CTRL + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_TYPE + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_CTRL + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_SIG1 + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_SIG2 + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_SIG + " TEXT NOT NULL" +
                    "," + STORAGE_COL_PLAN_END + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_PLAN_CONTENT + " TEXT NOT NULL" +
                    "," + STORAGE_COL_PLAN_VALIDATE + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_SEND_DEST + " TEXT NOT NULL" +
                    "," + STORAGE_COL_SEND_ID_PLAN + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_SEND_DATE_CTRL + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_SEND_TYPE_CTRL + " TEXT NOT NULL" +
                    "," + STORAGE_COL_SEND_SRC + " TEXT NOT NULL" +
                    ")");
        }
    };
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE new_" + STORAGE_TABLE_NAME +
                    "(" + STORAGE_COL_ID + " INTEGER PRIMARY KEY NOT NULL" +
                    "," + STORAGE_COL_RESID + " INTEGER UNIQUE NOT NULL" +
                    "," + STORAGE_COL_DATE + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_CONFIG + " TEXT NOT NULL" +
                    "," + STORAGE_COL_TYPE_CTRL + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_TYPE + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_CTRL + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_SIG1 + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_SIG2 + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_SIG + " TEXT NOT NULL" +
                    "," + STORAGE_COL_PLAN_END + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_PLAN_CONTENT + " TEXT NOT NULL" +
                    "," + STORAGE_COL_PLAN_VALIDATE + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_SEND_DEST + " TEXT NOT NULL" +
                    "," + STORAGE_COL_SEND_ID_PLAN + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_SEND_DATE_CTRL + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_SEND_TYPE_CTRL + " TEXT NOT NULL" +
                    "," + STORAGE_COL_SEND_SRC + " TEXT NOT NULL" +
                    ")");
            db.execSQL("INSERT INTO new_" + STORAGE_TABLE_NAME +
                    "(" + STORAGE_COL_ID +
                    "," + STORAGE_COL_RESID +
                    "," + STORAGE_COL_DATE +
                    "," + STORAGE_COL_CONFIG +
                    "," + STORAGE_COL_TYPE_CTRL +
                    "," + STORAGE_COL_CTRL_TYPE +
                    "," + STORAGE_COL_CTRL_CTRL +
                    "," + STORAGE_COL_CTRL_SIG1 +
                    "," + STORAGE_COL_CTRL_SIG2 +
                    "," + STORAGE_COL_CTRL_SIG +
                    "," + STORAGE_COL_PLAN_END +
                    "," + STORAGE_COL_PLAN_CONTENT +
                    "," + STORAGE_COL_PLAN_VALIDATE +
                    "," + STORAGE_COL_SEND_DEST +
                    "," + STORAGE_COL_SEND_ID_PLAN +
                    "," + STORAGE_COL_SEND_DATE_CTRL +
                    "," + STORAGE_COL_SEND_TYPE_CTRL +
                    "," + STORAGE_COL_SEND_SRC +
                    ") SELECT " + STORAGE_COL_ID +
                    "," + STORAGE_COL_RESID +
                    "," + STORAGE_COL_DATE +
                    "," + STORAGE_COL_CONFIG +
                    "," + STORAGE_COL_TYPE_CTRL +
                    "," + STORAGE_COL_CTRL_TYPE +
                    "," + STORAGE_COL_CTRL_CTRL +
                    "," + STORAGE_COL_CTRL_SIG1 +
                    "," + STORAGE_COL_CTRL_SIG2 +
                    "," + STORAGE_COL_CTRL_SIG +
                    "," + STORAGE_COL_PLAN_END +
                    "," + STORAGE_COL_PLAN_CONTENT +
                    "," + STORAGE_COL_PLAN_VALIDATE +
                    "," + STORAGE_COL_SEND_DEST +
                    "," + STORAGE_COL_SEND_ID_PLAN +
                    "," + STORAGE_COL_SEND_DATE_CTRL +
                    "," + STORAGE_COL_SEND_TYPE_CTRL +
                    "," + STORAGE_COL_SEND_SRC +
                    " FROM storage");
            db.execSQL("DROP TABLE "+STORAGE_TABLE_NAME);
            db.execSQL("ALTER TABLE new_"+STORAGE_TABLE_NAME+" RENAME TO "+STORAGE_TABLE_NAME);
        }
    };
    static final Migration MIGRATION_1_3 = new Migration(1, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + CONTACT_TABLE_NAME +
                    "(" + CONTACT_COL_ID + " INTEGER PRIMARY KEY NOT NULL" +
                    "," + CONTACT_COL_ADR + " TEXT UNIQUE" +
                    ")");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + STORAGE_TABLE_NAME +
                    "(" + STORAGE_COL_ID + " INTEGER PRIMARY KEY NOT NULL" +
                    "," + STORAGE_COL_RESID + " INTEGER UNIQUE NOT NULL" +
                    "," + STORAGE_COL_DATE + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_CONFIG + " TEXT NOT NULL" +
                    "," + STORAGE_COL_TYPE_CTRL + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_TYPE + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_CTRL + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_SIG1 + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_SIG2 + " TEXT NOT NULL" +
                    "," + STORAGE_COL_CTRL_SIG + " TEXT NOT NULL" +
                    "," + STORAGE_COL_PLAN_END + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_PLAN_CONTENT + " TEXT NOT NULL" +
                    "," + STORAGE_COL_PLAN_VALIDATE + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_SEND_DEST + " TEXT NOT NULL" +
                    "," + STORAGE_COL_SEND_ID_PLAN + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_SEND_DATE_CTRL + " INTEGER NOT NULL" +
                    "," + STORAGE_COL_SEND_TYPE_CTRL + " TEXT NOT NULL" +
                    "," + STORAGE_COL_SEND_SRC + " TEXT NOT NULL" +
                    ")");
        }
    };

    // --- SINGLETON ---
    private static volatile PrefDatabase INSTANCE;

    // --- DAO ---
    public abstract PrefDao mPrefDao();
    public abstract ContactDao mContactDao();
    public abstract StorageDao mStorageDao();

    // --- INSTANCE ---
    public static PrefDatabase getInstance(Context context) {
        if( INSTANCE == null ) {
            synchronized (PrefDatabase.class) {
                if( INSTANCE == null ) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), PrefDatabase.class, "pref2.db")
                        .addCallback(prepopulateDatabase())
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                        .build();
                }
            }
        }

        return INSTANCE;
    }

    // --- CALLBACK --
    private static RoomDatabase.Callback prepopulateDatabase() {
        return new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);

                ContentValues contentValues = new ContentValues();

                contentValues.put("id", Long.parseLong(PREF_ROW_ID_MBR_NUM));
                contentValues.put("param", PREF_ROW_ID_MBR);
                contentValues.put("value", "new");

                db.insert("Pref", OnConflictStrategy.IGNORE, contentValues);

                contentValues.clear();

                contentValues.put("id", Long.parseLong(PREF_ROW_ADR_MAC_NUM));
                contentValues.put("param", PREF_ROW_ADR_MAC);
                contentValues.put("value", "new");

                db.insert("Pref", OnConflictStrategy.IGNORE, contentValues);

                contentValues.clear();

                contentValues.put("id", Long.parseLong(PREF_ROW_AGENCY_NUM));
                contentValues.put("param", PREF_ROW_AGENCY);
                contentValues.put("value", "");

                db.insert("Pref", OnConflictStrategy.IGNORE, contentValues);

                contentValues.clear();

                contentValues.put("id", Long.parseLong(PREF_ROW_GROUP_NUM));
                contentValues.put("param", PREF_ROW_GROUP);
                contentValues.put("value", "");

                db.insert("Pref", OnConflictStrategy.IGNORE, contentValues);

                contentValues.clear();

                contentValues.put("id", Long.parseLong(PREF_ROW_RESIDENCE_NUM));
                contentValues.put("param", PREF_ROW_RESIDENCE);
                contentValues.put("value", "");

                db.insert("Pref", OnConflictStrategy.IGNORE, contentValues);
            }

        };
    }

}
