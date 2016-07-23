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

package andrompd.org.andrompd.mpdservice.profilemanagement;


public class MPDServerProfile {
    /**
     * Profile parameters
     */
    private String mProfileName;

    /**
     * If this boolean is set the application should use this profile to automatically
     * connect to a server. There should always be only one profile with this flag set
     * to true in the database.
     */
    private boolean mAutoconnect;

    /**
     * Server parameters.
     */
    private String mHostname;
    private String mPassword;
    private int mPort;

    public MPDServerProfile(String profileName, boolean autoConnect) {
        mProfileName = profileName;
        mAutoconnect = autoConnect;


        /* Just set the default mpd port here */
        mPort = 6600;
    }

    /**
     *
     * @return The profile name of this profile
     */
    public String getProfileName() {
        return mProfileName;
    }

    /**
     *
     * @return The autoconnect flag of this profile
     */
    public boolean getAutoconnect() {
        return mAutoconnect;
    }

    /**
     * The flag should only be set for one profile. The database helper checks and enforces this.
     * @param autoconnect If profile should be used to automatically connect
     */
    public void setAutoconnect(boolean autoconnect) {
        this.mAutoconnect = autoconnect;
    }

    /**
     *
     * @return Servers hostname or ip address.
     */
    public String getHostname() {
        return mHostname;
    }

    /**
     * Sets the hostname of this profile
     * @param hostname Hostname to use
     */
    public void setHostname(String hostname) {
        this.mHostname = hostname;
    }

    /**
     * The password of a profile can be left empty.
     * @return The password string or an empty string (null).
     */
    public String getPassword() {
        return mPassword;
    }

    /**
     * Password string. Could be set to null.
     * @param password
     */
    public void setPassword(String password) {
        if ( password == null) {
            mPassword = "";
            return;
        }
        this.mPassword = password;
    }

    /**
     * The port of a server profile. The default port is 6600.
     * @return TCP port of the profile
     */
    public int getPort() {
        return mPort;
    }

    /**
     * Sets the port of the profile. Usually is 6600.
     * @param port TCP port of the profile
     */
    public void setPort(int port) {
        this.mPort = port;
    }

    /**
     * Creates a string of the server profile. Be careful printing this out, because
     * it includes potential passwords.
     * @return The profile in string form.
     */
    @Override
    public String toString() {
        String retString = "";

        retString += "Profilename: " + mProfileName + "\n";
        retString += "Profile autoconnect: " + mAutoconnect + "\n";
        retString += "Hostname: " + mHostname + "\n";
        retString += "Password: " + mPassword + "\n";
        retString += "Port: " + mPort + "\n";

        return retString;
    }

}
