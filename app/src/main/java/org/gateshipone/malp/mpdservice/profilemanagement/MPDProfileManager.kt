/*
 *  Copyright (C) 2018 Team Gateship-One
 *  (Hendrik Borghorst & Frederik Luetkes)
 *
 *  The AUTHORS.md file contains a detailed contributors list:
 *  <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


package org.gateshipone.malp.mpdservice.profilemanagement

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

import java.util.ArrayList
import java.util.Observable

object MPDProfileManager : Observable() {

    /**
     * Creates a list of all available server profiles.
     * @return The list of currently saved server profiles.
     */
    @Synchronized
    fun getProfiles(context: Context): ArrayList<MPDServerProfile> {
        val profileList = ArrayList<MPDServerProfile>()
        val db = MPDProfileDBHelper(context).readableDatabase
        // Query the database table for profiles
        val cursor = db.query(MPDServerProfileTable.SQL_TABLE_NAME, MPDServerProfileTable.PROJECTION_SERVER_PROFILES, null, null, null, null, MPDServerProfileTable.COLUMN_PROFILE_NAME)
        if (cursor.moveToFirst()) {
            // Iterate over the cursor and create MPDServerProfile objects
            do {
                // Profile parameters
                val profileName = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_NAME))
                val autoConnect = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT)) == 1
                // Server parameters
                val serverHostname = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_HOSTNAME))
                val serverPassword = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_PASSWORD))
                val serverPort = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_PORT))
                val creationDate = cursor.getLong(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_DATE_CREATED))
                // Streaming parameters
                val streamingURL = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_STREAMING_PORT))
                val streamingEnabled = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_STREAMING_ENABLED)) == 1
                // HTTP cover parameters
                val httpCoverRegex = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_HTTP_COVER_REGEX))
                val httpCoverEnabled = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_HTTP_COVER_ENABLED)) == 1
                // Create temporary object to append to list.
                val profile = MPDServerProfile(profileName, autoConnect, creationDate)
                profile.hostname = serverHostname
                profile.password = serverPassword
                profile.port = serverPort

                profile.streamingURL = streamingURL
                profile.streamingEnabled = streamingEnabled

                profile.httpRegex = httpCoverRegex
                profile.httpCoverEnabled = httpCoverEnabled
                // Finish and add to list
                profileList.add(profile)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return profileList
    }


    /**
     * This method is convient to call to easily get the automatic connect server profile (if any).
     * @return Profile to connect to otherwise null.
     */
    @Synchronized
    fun getAutoconnectProfile(context: Context) : MPDServerProfile {
        val db = MPDProfileDBHelper(context).readableDatabase
        // Query the database table for profiles
        val cursor = db.query(MPDServerProfileTable.SQL_TABLE_NAME, MPDServerProfileTable.PROJECTION_SERVER_PROFILES, MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT + "=?", arrayOf("1"), null, null, null)
        if (cursor.moveToFirst()) {
            // Profile parameters
            val profileName = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_NAME))
            val autoConnect = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT)) == 1
            // Server parameters
            val serverHostname = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_HOSTNAME))
            val serverPassword = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_PASSWORD))
            val serverPort = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_SERVER_PORT))
            val creationDate = cursor.getLong(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_DATE_CREATED))
            // Streaming parameters
            val streamingURL = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_STREAMING_PORT))
            val streamingEnabled = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_STREAMING_ENABLED)) == 1
            // HTTP cover parameters
            val httpCoverRegex = cursor.getString(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_HTTP_COVER_REGEX))
            val httpCoverEnabled = cursor.getInt(cursor.getColumnIndex(MPDServerProfileTable.COLUMN_PROFILE_HTTP_COVER_ENABLED)) == 1
            // Create temporary object to append to list.
            val profile = MPDServerProfile(profileName, autoConnect, creationDate)
            profile.hostname = serverHostname
            profile.password = serverPassword
            profile.port = serverPort

            profile.streamingURL = streamingURL
            profile.streamingEnabled = streamingEnabled

            profile.httpRegex = httpCoverRegex
            profile.httpCoverEnabled = httpCoverEnabled

            cursor.close()
            db.close()
            return profile
        }

        cursor.close()
        db.close()
        return MPDServerProfile()
    }

    /**
     * Adds a new server profile. There is no way to change a profile directly in the table.
     * Just delete and readd the profile.
     * @param profile Profile to add to the database.
     */

    @Synchronized
    fun addProfile(profile: MPDServerProfile, context: Context) {
        val db = MPDProfileDBHelper(context).writableDatabase
        // Check if autoconnect is set, if it is, all other autoconnects need to be set to 0
        if (profile.autoconnect) {
            val autoConValues = ContentValues()
            autoConValues.put(MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT, 0)

            // Update the table columns to 0.
            db.update(MPDServerProfileTable.SQL_TABLE_NAME, autoConValues, MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT + "=?", arrayOf("1"))
        }


        // Prepare the sql transaction
        val values = ContentValues()

        // Profile parameters
        values.put(MPDServerProfileTable.COLUMN_PROFILE_NAME, profile.profileName)
        values.put(MPDServerProfileTable.COLUMN_PROFILE_AUTO_CONNECT, profile.autoconnect)

        // Server parameter
        values.put(MPDServerProfileTable.COLUMN_SERVER_HOSTNAME, profile.hostname)
        values.put(MPDServerProfileTable.COLUMN_SERVER_PASSWORD, profile.password)
        values.put(MPDServerProfileTable.COLUMN_SERVER_PORT, profile.port)
        values.put(MPDServerProfileTable.COLUMN_PROFILE_DATE_CREATED, profile.creationDate)

        // Streaming parameters
        values.put(MPDServerProfileTable.COLUMN_PROFILE_STREAMING_PORT, profile.streamingURL)
        values.put(MPDServerProfileTable.COLUMN_PROFILE_STREAMING_ENABLED, profile.streamingEnabled)

        // HTTP cover parameters
        values.put(MPDServerProfileTable.COLUMN_PROFILE_HTTP_COVER_REGEX, profile.httpRegex)
        values.put(MPDServerProfileTable.COLUMN_PROFILE_HTTP_COVER_ENABLED, profile.httpCoverEnabled)

        // Insert the table in the database
        db.insert(MPDServerProfileTable.SQL_TABLE_NAME, null, values)

        db.close()

        notifyObservers()
    }


    /**
     * Removes a profile from the database. Make sure that you provide the correct profile.
     * @param profile Profile to remove.
     */

    @Synchronized
    fun deleteProfile(profile: MPDServerProfile, context: Context) {
        val db = MPDProfileDBHelper(context).writableDatabase
        // Create the where clauses
        val whereClause = MPDServerProfileTable.COLUMN_PROFILE_DATE_CREATED + "=?"

        val whereValues = arrayOf(profile.creationDate.toString())
        db.delete(MPDServerProfileTable.SQL_TABLE_NAME, whereClause, whereValues)

        db.close()

        notifyObservers()
    }

}
