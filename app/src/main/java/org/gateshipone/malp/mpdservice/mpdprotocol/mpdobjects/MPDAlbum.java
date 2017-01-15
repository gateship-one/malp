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

package org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects;


import android.os.Parcel;
import android.os.Parcelable;

public class MPDAlbum implements MPDGenericItem, Comparable<MPDAlbum>, Parcelable {
    /* Album properties */
    private String mName;

    /* Musicbrainz ID */
    private String mMBID;

    /* Artists name (if any) */
    private String mArtistName;

    private boolean mImageFetching;

    public MPDAlbum(String name ) {
        mName = name;
        mMBID = "";
        mArtistName = "";
    }

    /* Getters */

    protected MPDAlbum(Parcel in) {
        mName = in.readString();
        mMBID = in.readString();
        mArtistName = in.readString();
        mImageFetching = in.readByte() != 0;
    }

    public static final Creator<MPDAlbum> CREATOR = new Creator<MPDAlbum>() {
        @Override
        public MPDAlbum createFromParcel(Parcel in) {
            return new MPDAlbum(in);
        }

        @Override
        public MPDAlbum[] newArray(int size) {
            return new MPDAlbum[size];
        }
    };

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

    public synchronized void setFetching(boolean fetching) {
        mImageFetching = fetching;
    }

    public synchronized boolean getFetching() {
        return mImageFetching;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mMBID);
        dest.writeString(mArtistName);
        dest.writeByte((byte) (mImageFetching ? 1 : 0));
    }
}
