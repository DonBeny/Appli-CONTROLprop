package org.orgaprop.test7.services;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

public class CalendarServices {

    public static final String TAG = "CalendarServices";

    public static final int PERMISSION_REQUEST_CALENDAR = 300;

    public static final String[] EVENT_PROJECTION_CALENDAR = new String[] {
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
            CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
    };

    // Les indices du tableau de projection ci-dessus.
    private static final int PROJECTION_CALENDAR_ID_INDEX = 0;
    private static final int PROJECTION_CALENDAR_ACCOUNT_NAME_INDEX = 1;
    private static final int PROJECTION_CALENDAR_DISPLAY_NAME_INDEX = 2;
    private static final int PROJECTION_CALENDAR_OWNER_ACCOUNT_INDEX = 3;

    public static final String CALENDAR_SERVICES_CALENDAR_OWNER_ACCOUNT = "orgaprop.org";
    public static final String CALENDAR_SERVICES_CALENDAR_ACCOUNT_TYPE = "orgaprop.org";
    public static final String CALENDAR_SERVICES_CALENDAR_ACCOUNT_NAME = "orgaprop@orgaprop.org";
    public static final String CALENDAR_SERVICES_CALENDAR_NAME = "ControlProp";
    public static final String CALENDAR_SERVICES_CALENDAR_DISPLAYED_NAME = "ControlProp";
    public static final int CALENDAR_SERVICES_CALENDAR_COLOR = Color.rgb(69,180,11);

//********* PUBLIC FUNCTIONS

    public static long getCalendar(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return -1;
        }

        ContentResolver cr = context.getContentResolver();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String selection = "(" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?) AND (" + CalendarContract.Calendars.OWNER_ACCOUNT + " = ?)";
        String[] selectionArgs = {CALENDAR_SERVICES_CALENDAR_ACCOUNT_NAME, CALENDAR_SERVICES_CALENDAR_OWNER_ACCOUNT};

        try (Cursor cursor = cr.query(uri, EVENT_PROJECTION_CALENDAR, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long calID = cursor.getLong(PROJECTION_CALENDAR_ID_INDEX);
                    String displayName = cursor.getString(PROJECTION_CALENDAR_DISPLAY_NAME_INDEX);
                    if (CALENDAR_SERVICES_CALENDAR_NAME.equals(displayName)) {
                        return calID;
                    }
                } while (cursor.moveToNext());
            }
        }

        return addCalendar(context);
    }
    public static long addEventTo(Context context, long idCalendar, Calendar DStart, Calendar DEnd, String title, String description, String location, boolean allDay) {
        long idEvent = -1;

        if( ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED ) {
            ContentResolver cr = context.getContentResolver();
            ContentValues contentValues = new ContentValues();

            Log.e(TAG, "DStart: " + DStart.getTimeInMillis() + " (" + new Date(DStart.getTimeInMillis()) + ")");
            Log.e(TAG, "DEnd: " + DEnd.getTimeInMillis() + " (" + new Date(DEnd.getTimeInMillis()) + ")");
            Log.e(TAG, "Timezone: " + TimeZone.getDefault().getID());

            contentValues.put(CalendarContract.Events.DTSTART, DStart.getTimeInMillis());
            contentValues.put(CalendarContract.Events.DTEND, DEnd.getTimeInMillis());
            contentValues.put(CalendarContract.Events.TITLE, title);
            contentValues.put(CalendarContract.Events.DESCRIPTION, description);
            contentValues.put(CalendarContract.Events.CALENDAR_ID, idCalendar);
            contentValues.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
            contentValues.put(CalendarContract.Events.EVENT_LOCATION, location);
            contentValues.put(CalendarContract.Events.HAS_ALARM, true);
            contentValues.put(CalendarContract.Events.GUESTS_CAN_INVITE_OTHERS, false);
            contentValues.put(CalendarContract.Events.GUESTS_CAN_SEE_GUESTS, false);
            contentValues.put(CalendarContract.Events.ALL_DAY, allDay);

            Uri uri = cr.insert(asSyncAdapter(CalendarContract.Events.CONTENT_URI), contentValues);

            if( uri != null ) {
                idEvent = Long.parseLong(Objects.requireNonNull(uri.getLastPathSegment()));
                setReminderTo(context, idEvent);
            }

            assert uri != null;
        }

        return idEvent;
    }



//********* PRIVATE FUNCTIONS

    private static long addCalendar(Context context) {
        long idCalendar = -1;

        if( ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED ) {
            ContentResolver cr = context.getContentResolver();
            ContentValues contentValues = new ContentValues();

            contentValues.put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_SERVICES_CALENDAR_ACCOUNT_NAME);
            contentValues.put(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDAR_SERVICES_CALENDAR_ACCOUNT_TYPE);
            contentValues.put(CalendarContract.Calendars.NAME, CALENDAR_SERVICES_CALENDAR_NAME);
            contentValues.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_SERVICES_CALENDAR_DISPLAYED_NAME);
            contentValues.put(CalendarContract.Calendars.CALENDAR_COLOR, CALENDAR_SERVICES_CALENDAR_COLOR);
            contentValues.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
            contentValues.put(CalendarContract.Calendars.OWNER_ACCOUNT, CALENDAR_SERVICES_CALENDAR_OWNER_ACCOUNT);
            contentValues.put(CalendarContract.Calendars.ALLOWED_REMINDERS, "METHOD_ALARM");
            contentValues.put(CalendarContract.Calendars.ALLOWED_ATTENDEE_TYPES, "TYPE_NONE");
            contentValues.put(CalendarContract.Calendars.ALLOWED_AVAILABILITY, "AVAILABILITY_FREE");

            Uri uri = cr.insert(asSyncAdapter(CalendarContract.Calendars.CONTENT_URI), contentValues);

            if( uri == null ) {
                return -1;
            } else {
                return Long.parseLong(Objects.requireNonNull(uri.getLastPathSegment()));
            }
        }

        return idCalendar;
    }
    private static void setReminderTo(Context context, long idEvent) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            ContentResolver cr = context.getContentResolver();
            ContentValues contentValues = new ContentValues();

            contentValues.put(CalendarContract.Reminders.EVENT_ID, idEvent);
            contentValues.put(CalendarContract.Reminders.MINUTES, 1200);
            contentValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);

            cr.insert(asSyncAdapter(CalendarContract.Reminders.CONTENT_URI), contentValues);
        }
    }
    private static Uri asSyncAdapter(Uri uri) {
        return uri.buildUpon()
                .appendQueryParameter(android.provider.CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_SERVICES_CALENDAR_ACCOUNT_NAME)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDAR_SERVICES_CALENDAR_ACCOUNT_TYPE).build();
    }

}
