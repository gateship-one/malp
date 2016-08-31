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


public class MPDAlbum implements MPDGenericItem, Comparable<MPDAlbum> {
    /* Album properties */
    private String mName;

    /* Musicbrainz ID */
    private String mMBID;

    /* Artists name (if any) */
    private String mArtistName;

    public MPDAlbum(String name ) {
        mName = name;
        mMBID = "";
        mArtistName = "";
    }

    /* Getters */

    public String getName() {
        return mName;
    }

    public String getMBID() {
        return mMBID;
    }

    public String getArtistName() {
        return mArtistName;
    }

    public void setArtistName(String artistName) {
        if ( artistName != null ) {
            mArtistName = artistName;
        }
    }

    public void setMBID(String mbid) {
        if (null != mbid ) {
            mMBID = mbid;
        }
    }

    @Override
    public String getSectionTitle() {
        return mName;
    }

    @Override
    public boolean equals(Object object) {
        if ( !(object instanceof MPDAlbum)) {
            return false;
        }

        MPDAlbum album = (MPDAlbum)object;
        if ( (mName.equals(album.mName)) && (mArtistName.equals(album.mArtistName)) &&
                (mMBID.equals(album.mMBID))) {
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(MPDAlbum another) {
        if ( another.equals(this) ) {
            return 0;
        }
        return mName.toLowerCase().compareTo(another.mName.toLowerCase());
    }

    @Override
    public int hashCode() {
        return (mName + mArtistName + mMBID).hashCode();
    }
}
