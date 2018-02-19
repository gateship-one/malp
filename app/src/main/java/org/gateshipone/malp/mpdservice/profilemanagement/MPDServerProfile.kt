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


import android.os.Parcel
import android.os.Parcelable

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDGenericItem

class MPDServerProfile : MPDGenericItem, Parcelable {
    /**
     * Profile parameters
     */
    /**
     *
     * @return The profile name of this profile
     */
    /**
     * Sets the profile name of this profile
     * @param profileName Profile name to set
     */
    var profileName = ""

    /**
     * If this boolean is set the application should use this profile to automatically
     * connect to a server. There should always be only one profile with this flag set
     * to true in the database.
     */
    /**
     *
     * @return The autoconnect flag of this profile
     */
    /**
     * The flag should only be set for one profile. The database helper checks and enforces this.
     * @param autoconnect If profile should be used to automatically connect
     */
    var autoconnect: Boolean = false

    /**
     * Server parameters.
     */
    /**
     *
     * @return Servers hostname or ip address.
     */
    /**
     * Sets the hostname of this profile
     * @param hostname Hostname to use
     */
    var hostname = ""
    /**
     * The password of a profile can be left empty.
     * @return The password string or an empty string (null).
     */
    /**
     * Password string. Could be set to null.
     * @param password
     */
    var password = ""
    /**
     * The port of a server profile. The default port is 6600.
     * @return TCP port of the profile
     */
    /**
     * Sets the port of the profile. Usually is 6600.
     * @param port TCP port of the profile
     */
    var port: Int = 0

    var streamingURL = ""
    var streamingEnabled: Boolean = false

    var httpRegex = ""
    var httpCoverEnabled: Boolean = false

    var creationDate: Long = 0
        private set

    constructor()

    constructor(profileName: String, autoConnect: Boolean) {
        this.profileName = profileName
        autoconnect = autoConnect

        creationDate = System.currentTimeMillis()

        /* Just set the default mpd port here */
        port = 6600
    }

    constructor(profileName: String, autoConnect: Boolean, creationDate: Long) {
        this.profileName = profileName
        autoconnect = autoConnect

        this.creationDate = System.currentTimeMillis()

        /* Just set the default mpd port here */
        port = 6600
        this.creationDate = creationDate
    }

    protected constructor(`in`: Parcel) {
        profileName = `in`.readString()
        autoconnect = `in`.readByte().toInt() != 0
        hostname = `in`.readString()
        password = `in`.readString()
        port = `in`.readInt()
        streamingURL = `in`.readString()
        streamingEnabled = `in`.readByte().toInt() != 0
        httpRegex = `in`.readString()
        httpCoverEnabled = `in`.readByte().toInt() != 0
        creationDate = `in`.readLong()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(profileName)
        dest.writeByte((if (autoconnect) 1 else 0).toByte())
        dest.writeString(hostname)
        dest.writeString(password)
        dest.writeInt(port)
        dest.writeString(streamingURL)
        dest.writeByte((if (streamingEnabled) 1 else 0).toByte())
        dest.writeString(httpRegex)
        dest.writeByte((if (httpCoverEnabled) 1 else 0).toByte())
        dest.writeLong(creationDate)
    }

    override fun describeContents(): Int {
        return 0
    }

    /**
     * Creates a string of the server profile. Be careful printing this out, because
     * it includes potential passwords.
     * @return The profile in string form.
     */
    override fun toString(): String {
        var retString = ""

        retString += "Profilename: " + profileName + "\n"
        retString += "Profile autoconnect: " + autoconnect + "\n"
        retString += "Hostname: " + hostname + "\n"
        retString += "Password: " + password + "\n"
        retString += "Port: " + port + "\n"
        retString += "Created: " + creationDate + "\n"

        return retString
    }

    override fun getSectionTitle(): String {
        return profileName
    }

    companion object {

        val CREATOR: Parcelable.Creator<MPDServerProfile> = object : Parcelable.Creator<MPDServerProfile> {
            override fun createFromParcel(`in`: Parcel): MPDServerProfile {
                return MPDServerProfile(`in`)
            }

            override fun newArray(size: Int): Array<MPDServerProfile?> {
                return arrayOfNulls(size)
            }
        }
    }
}
