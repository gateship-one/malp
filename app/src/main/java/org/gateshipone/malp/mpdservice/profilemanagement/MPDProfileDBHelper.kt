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

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class MPDProfileDBHelper
/**
 * Constructor to create the database.
 * @param context Application context to create the database in.
 */
(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    /**
     * Initializes the tables of this database.
     * Should call all table helpers.
     * @param database Database to use for tables.
     */
    override fun onCreate(database: SQLiteDatabase) {
        MPDServerProfileTable.onCreate(database)
    }

    /**
     * Method to migrate the database to a new version. Nothing implemented for now.
     * @param database Database to migrate to a different version.
     * @param oldVersion Old version of the database to migrate from
     * @param newVersion New version of the database to migrate to
     */
    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.v(TAG, "Upgrading database from version: $oldVersion to new version: $newVersion")
        when (oldVersion) {
        // Upgrade from version 1 to 2 needs introduction of the streaming port and streaming
        // enable column.
            1 -> {
                run {
                    var sqlString = "ALTER TABLE " + MPDServerProfileTable.SQL_TABLE_NAME + " ADD COLUMN " + MPDServerProfileTable.COLUMN_PROFILE_STREAMING_PORT + " integer;"
                    database.execSQL(sqlString)

                    sqlString = "ALTER TABLE " + MPDServerProfileTable.SQL_TABLE_NAME + " ADD COLUMN " + MPDServerProfileTable.COLUMN_PROFILE_STREAMING_ENABLED + " integer;"
                    database.execSQL(sqlString)
                }
                run {
                    // Upgrading from version 2 to 3 needs new http regex columns
                    var sqlString = "ALTER TABLE " + MPDServerProfileTable.SQL_TABLE_NAME + " ADD COLUMN " + MPDServerProfileTable.COLUMN_PROFILE_HTTP_COVER_REGEX + " text;"
                    database.execSQL(sqlString)

                    sqlString = "ALTER TABLE " + MPDServerProfileTable.SQL_TABLE_NAME + " ADD COLUMN " + MPDServerProfileTable.COLUMN_PROFILE_HTTP_COVER_ENABLED + " integer;"
                    database.execSQL(sqlString)
                }
            }
            2 -> run {
                var sqlString = "ALTER TABLE " + MPDServerProfileTable.SQL_TABLE_NAME + " ADD COLUMN " + MPDServerProfileTable.COLUMN_PROFILE_HTTP_COVER_REGEX + " text;"
                database.execSQL(sqlString)
                sqlString = "ALTER TABLE " + MPDServerProfileTable.SQL_TABLE_NAME + " ADD COLUMN " + MPDServerProfileTable.COLUMN_PROFILE_HTTP_COVER_ENABLED + " integer;"
                database.execSQL(sqlString)
            }
            else -> {
            }
        }
    }

    companion object {
        private val TAG = MPDProfileManager::class.java.simpleName
        /**
         * Database name for the profiles database
         */
        val DATABASE_NAME = "andrompd_database"

        /**
         * Database version, used for migrating to new versions.
         */
        val DATABASE_VERSION = 3
    }
}
