/*
 *  Copyright (C) 2017 Team Gateship-One
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

package org.gateshipone.malp.mpdservice.mpdprotocol;


import android.util.Log;

import java.util.List;

public class MPDCapabilities {
    private static final String TAG = MPDCapabilities.class.getSimpleName();

    private static final String MPD_TAG_TYPE_MUSICBRAINZ = "musicbrainz";
    private static final String MPD_TAG_TYPE_ALBUMARTIST = "albumartist";
    private static final String MPD_TAG_TYPE_DATE = "date";

    private int mMajorVersion;
    private int mMinorVersion;

    private boolean mHasIdle;
    private boolean mHasRangedCurrentPlaylist;
    private boolean mHasSearchAdd;

    private boolean mHasMusicBrainzTags;
    private boolean mHasListGroup;

    private boolean mHasListFiltering;

    private boolean mHasCurrentPlaylistRemoveRange;

    private boolean mHasToggleOutput;

    private boolean mMopidyDetected;

    private boolean mTagAlbumArtist;
    private boolean mTagDate;

    public MPDCapabilities(String version, List<String> commands, List<String> tags) {
        String[] versions = version.split("\\.");
        if (versions.length == 3) {
            mMajorVersion = Integer.valueOf(versions[0]);
            mMinorVersion = Integer.valueOf(versions[1]);
        }

        // Only MPD servers greater version 0.14 have ranged playlist fetching, this allows fallback
        if (mMinorVersion > 14 || mMajorVersion > 0) {
            mHasRangedCurrentPlaylist = true;
        } else {
            mHasRangedCurrentPlaylist = false;
        }

        if (mMinorVersion >= 16 || mMajorVersion > 0) {
            mHasCurrentPlaylistRemoveRange = true;
        }

        if (mMinorVersion >= 18 || mMajorVersion > 0) {
            mHasToggleOutput = true;
        }

        if (mMinorVersion >= 19 || mMajorVersion > 0) {
            mHasListGroup = true;
            mHasListFiltering = true;
        }



        if (null != commands) {
            if (commands.contains(MPDCommands.MPD_COMMAND_START_IDLE)) {
                mHasIdle = true;
            } else {
                mHasIdle = false;
            }

            if (commands.contains(MPDCommands.MPD_COMMAND_ADD_SEARCH_FILES_CMD_NAME)) {
                mHasSearchAdd = true;
            } else {
                mHasSearchAdd = false;
            }
        }


        if (null != tags) {
            for (String tag : tags) {
                String tagLC = tag.toLowerCase();
                if (tagLC.contains(MPD_TAG_TYPE_MUSICBRAINZ)) {
                    mHasMusicBrainzTags = true;
                    break;
                } else if (tagLC.equals(MPD_TAG_TYPE_ALBUMARTIST)) {
                    mTagAlbumArtist = true;
                } else if (tagLC.equals(MPD_TAG_TYPE_DATE)) {
                    mTagDate = true;
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

    public boolean hasListGroup() {
        return mHasListGroup;
    }

    public boolean hasListFiltering() {
        return mHasListFiltering;
    }

    public int getMajorVersion() {
        return mMajorVersion;
    }

    public int getMinorVersion() {
        return mMinorVersion;
    }

    public boolean hasMusicBrainzTags() {
        return mHasMusicBrainzTags;
    }

    public boolean hasCurrentPlaylistRemoveRange() {
        return mHasCurrentPlaylistRemoveRange;
    }

    public boolean hasTagAlbumArtist() {
        return mTagAlbumArtist;
    }

    public boolean hasTagDate() {
        return mTagDate;
    }

    public boolean hasToggleOutput() {
        return mHasToggleOutput;
    }

    public String getServerFeatures() {
        return "MPD protocol version: " + mMajorVersion + '.' + mMinorVersion + '\n'
                + "TAGS:" + '\n'
                + "MUSICBRAINZ: " + mHasMusicBrainzTags + '\n'
                + "AlbumArtist: " + mTagAlbumArtist + '\n'
                + "Date: " + mTagDate + '\n'
                + "IDLE support: " + mHasIdle + '\n'
                + "Windowed playlist: " + mHasRangedCurrentPlaylist + '\n'
                + "Fast search add: " + mHasSearchAdd + '\n'
                + "List grouping: " + mHasListGroup + '\n'
                + "List filtering: " + mHasListFiltering + '\n'
                + "Fast ranged currentplaylist delete: " + mHasCurrentPlaylistRemoveRange
                + (mMopidyDetected ? "\nMopidy detected, consider using the real MPD server (www.musicpd.org)!" : "");
    }

    public void enableMopidyWorkaround() {
        Log.w(TAG, "Enabling workarounds for detected Mopidy server");
        mHasListGroup = false;
        mHasListFiltering = false;
        mMopidyDetected = true;
    }
}
