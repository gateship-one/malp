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


import android.database.sqlite.SQLiteDatabase

object MPDServerProfileTable {
    /**
     * Table name of the SQL table inside a database
     */
    val SQL_TABLE_NAME = "andrompd_mpd_server_profiles"

    /**
     * Column descriptions
     */
    val COLUMN_PROFILE_NAME = "profile_name"
    val COLUMN_SERVER_HOSTNAME = "server_hostname"
    val COLUMN_SERVER_PASSWORD = "server_password"
    val COLUMN_SERVER_PORT = "server_port"
    val COLUMN_PROFILE_AUTO_CONNECT = "autoconnect"
    val COLUMN_PROFILE_DATE_CREATED = "date"

    val COLUMN_PROFILE_STREAMING_PORT = "streaming_port"
    val COLUMN_PROFILE_STREAMING_ENABLED = "streaming_enabled"

    val COLUMN_PROFILE_HTTP_COVER_REGEX = "http_cover_regex"
    val COLUMN_PROFILE_HTTP_COVER_ENABLED = "http_cover_enabled"


    /**
     * Projection string array used for queries on this table
     */
    val PROJECTION_SERVER_PROFILES = arrayOf(COLUMN_PROFILE_NAME, COLUMN_PROFILE_AUTO_CONNECT, COLUMN_SERVER_HOSTNAME, COLUMN_SERVER_PASSWORD, COLUMN_SERVER_PORT, COLUMN_PROFILE_DATE_CREATED, COLUMN_PROFILE_STREAMING_PORT, COLUMN_PROFILE_STREAMING_ENABLED, COLUMN_PROFILE_HTTP_COVER_REGEX, COLUMN_PROFILE_HTTP_COVER_ENABLED)


    /**
     * String to initially create the table
     */
    val DATABASE_CREATE = "create table if not exists " + SQL_TABLE_NAME + " (" +
            COLUMN_PROFILE_NAME + " text," + COLUMN_PROFILE_AUTO_CONNECT + " integer," +
            COLUMN_SERVER_HOSTNAME + " text," + COLUMN_SERVER_PASSWORD + " text," +
            COLUMN_SERVER_PORT + " integer," + COLUMN_PROFILE_DATE_CREATED + " integer PRIMARY KEY, " +
            COLUMN_PROFILE_STREAMING_PORT + " integer," + COLUMN_PROFILE_STREAMING_ENABLED + " integer," +
            COLUMN_PROFILE_HTTP_COVER_REGEX + " text," + COLUMN_PROFILE_HTTP_COVER_ENABLED + " integer" + " );"

    /**
     * Creates the inital database table.
     * @param database Database to use for table creation.
     */
    fun onCreate(database: SQLiteDatabase) {
        /*
         * Create table in the given database here.
         */
        database.execSQL(DATABASE_CREATE)
    }
}
