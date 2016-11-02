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

package org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects;

public abstract class MPDFileEntry implements MPDGenericItem,Comparable<MPDFileEntry> {
    protected String mPath;

    // FIXME to some date format of java
    protected String mLastModified;

    protected MPDFileEntry(String path) {
        mPath = path;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public String getPath() {
        return mPath;
    }

    public void setLastModified(String lastModified) {
        mLastModified = lastModified;
    }

    public String getLastModified() {
        return mLastModified;
    }

    /**
     * This methods defines an hard order of directory, files, playlists
     * @param another
     * @return
     */
    @Override
    public int compareTo(MPDFileEntry another) {
        if ( another == null) {
            return -1;
        }

        if ( this instanceof MPDDirectory ) {
            if ( another instanceof MPDDirectory ) {
                return ((MPDDirectory)this).compareTo((MPDDirectory)another);
            } else if (another instanceof MPDPlaylist || another instanceof MPDFile) {
                return -1;
            }
        } else if ( this instanceof MPDFile ) {
            if ( another instanceof MPDDirectory ) {
                return 1;
            } else if (another instanceof MPDPlaylist) {
                return -1;
            } else if ( another instanceof MPDFile) {
                return ((MPDFile)this).compareTo((MPDFile)another);
            }
        } else if ( this instanceof MPDPlaylist ) {
            if ( another instanceof MPDPlaylist) {
                return ((MPDPlaylist)this).compareTo((MPDPlaylist)another);
            } else if ( another instanceof MPDDirectory || another instanceof MPDFile) {
                return 1;
            }
        }

        return -1;
    }

}
