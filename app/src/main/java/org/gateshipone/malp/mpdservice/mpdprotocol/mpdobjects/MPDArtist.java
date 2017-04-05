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

import java.util.ArrayList;

public class MPDArtist implements MPDGenericItem, Comparable<MPDArtist>, Parcelable {
    /* Artist properties */
    private String pArtistName;

    /* Musicbrainz ID */
    private ArrayList<String> pMBIDs;

    private boolean mImageFetching;

    public MPDArtist(String name) {
        pArtistName = name;
        pMBIDs = new ArrayList<>();
    }

    protected MPDArtist(Parcel in) {
        pArtistName = in.readString();
        pMBIDs = in.createStringArrayList();
        mImageFetching = in.readByte() != 0;
    }

    public static final Creator<MPDArtist> CREATOR = new Creator<MPDArtist>() {
        @Override
        public MPDArtist createFromParcel(Parcel in) {
            return new MPDArtist(in);
        }

        @Override
        public MPDArtist[] newArray(int size) {
            return new MPDArtist[size];
        }
    };

    public String getArtistName() {
        return pArtistName;
    }

    public int getMBIDCount() {
        return pMBIDs.size();
    }

    public String getMBID(int position) {
        return pMBIDs.get(position);
    }

    public void addMBID(String mbid) {
        pMBIDs.add(mbid);
    }


    @Override
    public String getSectionTitle() {
        return pArtistName;
    }

    @Override
    public boolean equals(Object object) {
        if ( !(object instanceof MPDArtist)) {
            return false;
        }

        MPDArtist artist = (MPDArtist)object;
        if ( !artist.pArtistName.equals(pArtistName) || artist.pMBIDs.size() !=  pMBIDs.size()) {
            return false;
        }

        for ( int i = 0; i < pMBIDs.size(); i++) {
            if ( !pMBIDs.get(i).equals(artist.pMBIDs.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int compareTo(MPDArtist another) {
        if (another.equals(this)) {
            return 0;
        }

        if ( another.pArtistName.toLowerCase().equals(pArtistName.toLowerCase()) ) {
            // Use MBID as sort criteria, without MBID before the ones with
            if ( another.pMBIDs.size() > pMBIDs.size() ) {
                return -1;
            } else if (another.pMBIDs.size() < pMBIDs.size()){
                return 1;
            } else {
                return 0;
            }
        }

        return pArtistName.toLowerCase().compareTo(another.pArtistName.toLowerCase());
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
        dest.writeString(pArtistName);
        String[] mbids = pMBIDs.toArray(new String[pMBIDs.size()]);
        dest.writeStringArray(mbids);
        dest.writeByte(mImageFetching ? (byte)1 : (byte)0);
    }
}
