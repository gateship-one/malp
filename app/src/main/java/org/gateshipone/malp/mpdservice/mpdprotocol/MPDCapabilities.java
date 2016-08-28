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

package org.gateshipone.malp.mpdservice.mpdprotocol;


import android.util.Log;

import java.util.List;

public class MPDCapabilities {
    private static final String TAG = MPDCapabilities.class.getSimpleName();

    private String pVersionString;
    private int pMajorVersion;
    private int pMinorVersion;

    private boolean mHasIdle;
    private boolean mHasRangedCurrentPlaylist;
    private boolean mHasSearchAdd;

    public MPDCapabilities(String version, List<String> commands) {
        String[] versions = version.split("\\.");
        if (versions.length == 3) {
            pMajorVersion = Integer.valueOf(versions[0]);
            pMinorVersion = Integer.valueOf(versions[1]);
        }

        // Only MPD servers greater version 0.14 have ranged playlist fetching, this allows fallback
        if ( pMinorVersion > 14) {
            mHasRangedCurrentPlaylist = true;
        } else {
            mHasRangedCurrentPlaylist = false;
        }

        if ( commands.contains(MPDCommands.MPD_COMMAND_START_IDLE)) {
            mHasIdle = true;
        } else {
            mHasIdle = false;
        }

        if ( commands.contains(MPDCommands.MPD_COMMAND_ADD_SEARCH_FILES_CMD_NAME)) {
            Log.v(TAG,"Searchadd available");
            mHasSearchAdd = true;
        } else {
            Log.v(TAG,"Searchadd not available");
            mHasSearchAdd = false;
        }
    }

    public boolean hasIdling() {
        return mHasIdle;
    }

    public boolean hasRangedCurrentPlaylist() {
        return mHasRangedCurrentPlaylist;
    }

    public boolean hasSearchAdd() {
        return mHasSearchAdd;
    }
}
