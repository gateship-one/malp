/*
 * Copyright (C) 2016  Hendrik Borghorst
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gateshipone.malp.mpdservice.profilemanagement;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.Tag;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MPDProfileManager {
    private static final String TAG = "ProfileManager";


    /**
     * Instance of the helper class to initialize the database.
     */
    private MPDProfileDBHelper mDBHelper;

    public MPDProfileManager(Context context) {
        /* Create instance of the helper class to get the writable DB later. */
        mDBHelper = new MPDProfileDBHelper(context);

    }

    /**
     * Creates a list of all available server profiles.
     * @return The list of currently saved server profiles.
     */
    public List<MPDServerProfile> getProfiles() {
        ArrayList<MPDServerProfile> profileList = new ArrayList<>();

        /* Query the database table for profiles */
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor cursor = db.query(MPDServerProfileTable.SQL_TABLE_NAME, MPDServerProfileTable.PROJECTION_SERVER_PROFILES, null, null, null, null, MPDServerProfileTable.COLUMN_PROFILE_NAME);


        /* Iterate over the cursor and create MPDServerProfile objects */
        if ( cursor.moveToFirst() ) {
            do {
                /* Profile parameters */
                String profileName = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_NAME));
                boolean autoConnect = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT)) == 1;

                /* Server parameters */
                String serverHostname = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_HOSTNAME));
                String serverPassword = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_PASSWORD));
                int serverPort = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_PORT));
                long creationDate = cursor.getLong(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_DATE_CREATED));

                /* Create temporary object to append to list. */
                MPDServerProfile profile = new MPDServerProfile(profileName, autoConnect, creationDate);
                profile.setHostname(serverHostname);
                profile.setPassword(serverPassword);
                profile.setPort(serverPort);

                /* Finish and add to list */
                profileList.add(profile);
            } while ( cursor.moveToNext() );
        }

        cursor.close();
        db.close();
        return profileList;
    }

    /**
     * Adds a new server profile. There is no way to change a profile directly in the table.
     * Just delete and readd the profile.
     * @param profile Profile to add to the database.
     */
    public void addProfile(MPDServerProfile profile) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        /* Check if autoconnect is set, if it is, all other autoconnects need to be set to 0 */
        if ( profile.getAutoconnect() ) {
            ContentValues autoConValues = new ContentValues();
            autoConValues.put(MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT, 0);

            /* Update the table columns to 0. */
            db.update(MPDServerProfileTable.SQL_TABLE_NAME, autoConValues, MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT + "=?", new String[]{"1"});
        }


        /* Prepare the sql transaction */
        ContentValues values = new ContentValues();

        /* Profile parameters */
        values.put(MPDServerProfileTable.COLUMN_PROFILE_NAME, profile.getProfileName());
        values.put(MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT, profile.getAutoconnect());

        /* Server parameter */
        values.put(MPDServerProfileTable.COLUMN_SERVER_HOSTNAME, profile.getHostname());
        values.put(MPDServerProfileTable.COLUMN_SERVER_PASSWORD, profile.getPassword());
        values.put(MPDServerProfileTable.COLUMN_SERVER_PORT, profile.getPort());
        values.put(MPDServerProfileTable.COLUMN_PROFILE_DATE_CREATED, profile.getCreationDate());

        /* Insert the table in the database */
        db.insert(MPDServerProfileTable.SQL_TABLE_NAME, null, values);

        db.close();
    }


    /**
     * Removes a profile from the database. Make sure that you provide the correct profile.
     * @param profile Profile to remove.
     */
    public void deleteProfile(MPDServerProfile profile) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        /* Create the where clauses */
        String whereClause = MPDServerProfileTable.COLUMN_PROFILE_DATE_CREATED + "=?";

        String[] whereValues = {String.valueOf(profile.getCreationDate())};
        db.delete(MPDServerProfileTable.SQL_TABLE_NAME,whereClause, whereValues);

        db.close();
    }


    /**
     * This method is convient to call to easily get the automatic connect server profile (if any).
     * @return Profile to connect to otherwise null.
     */
    public MPDServerProfile getAutoconnectProfile() {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        /* Query the database table for profiles */
        Cursor cursor  = db.query(MPDServerProfileTable.SQL_TABLE_NAME, MPDServerProfileTable.PROJECTION_SERVER_PROFILES,MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT + "=?", new String[]{"1"}, null, null, null);


        /* Iterate over the cursor and create MPDServerProfile objects */
        if ( cursor.moveToFirst() ) {
            /* Profile parameters */
            String profileName = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_NAME));
            boolean autoConnect = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT)) == 1;

            /* Server parameters */
            String serverHostname = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_HOSTNAME));
            String serverPassword = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_PASSWORD));
            int serverPort = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_PORT));
            long creationDate = cursor.getLong(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_DATE_CREATED));


            /* Create temporary object to append to list. */
            MPDServerProfile profile = new MPDServerProfile(profileName, autoConnect, creationDate);
            profile.setHostname(serverHostname);
            profile.setPassword(serverPassword);
            profile.setPort(serverPort);

            cursor.close();
            return profile;
        }

        cursor.close();
        db.close();
        return null;
    }

}
