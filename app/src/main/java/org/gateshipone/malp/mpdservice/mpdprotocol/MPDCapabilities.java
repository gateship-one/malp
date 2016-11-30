/*
 * Copyright (C) 2016 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

    private boolean mHasMusicBrainzTags;
    private boolean mHasListGroup;

    private boolean mHasListFiltering;

    public MPDCapabilities(String version, List<String> commands, List<String> tags) {
        String[] versions = version.split("\\.");
        if (versions.length == 3) {
            pMajorVersion = Integer.valueOf(versions[0]);
            pMinorVersion = Integer.valueOf(versions[1]);
        }

        // Only MPD servers greater version 0.14 have ranged playlist fetching, this allows fallback
        if ( pMinorVersion > 14 || pMajorVersion > 0) {
            mHasRangedCurrentPlaylist = true;
        } else {
            mHasRangedCurrentPlaylist = false;
        }

        if ( pMinorVersion >= 19 || pMajorVersion > 0 ) {
            mHasListGroup = true;
            mHasListFiltering = true;
        }

        if ( null != commands ) {
            if (commands.contains(MPDCommands.MPD_COMMAND_START_IDLE)) {
                mHasIdle = true;
            } else {
                mHasIdle = false;
            }

            if (commands.contains(MPDCommands.MPD_COMMAND_ADD_SEARCH_FILES_CMD_NAME)) {
                Log.v(TAG, "Searchadd available");
                mHasSearchAdd = true;
            } else {
                Log.v(TAG, "Searchadd not available");
                mHasSearchAdd = false;
            }
        }



        if ( null != tags ) {
            for (String tag : tags ) {
                if ( tag.contains("MUSICBRAINZ")) {
                    mHasMusicBrainzTags = true;
                    Log.v(TAG,"Server has MusicBrainz support");
                    break;
                }
            }
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

    public boolean hasListGroup() { return mHasListGroup;}

    public boolean hasListFiltering() { return mHasListFiltering;}

    public boolean hasMusicBrainzTags() {
        return mHasMusicBrainzTags;
    }
}
