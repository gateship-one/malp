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


public class MPDArtist implements MPDGenericItem, Comparable<MPDArtist> {
    /* Artist properties */
    private String pArtistName;

    /* Musicbrainz ID */
    private String pMBID;

    public MPDArtist(String name, String mbid) {
        pArtistName = name;
        pMBID = mbid;
    }

    public String getArtistName() {
        return pArtistName;
    }

    public String getMBID() {
        return pMBID;
    }

    @Override
    public String getSectionTitle() {
        return pArtistName;
    }

    public boolean equals(MPDArtist artist) {
        if ((pArtistName.equals(artist.pArtistName)) &&
                (pMBID.equals(artist.pMBID))) {
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(MPDArtist another) {
        if (another.equals(this)) {
            return 0;
        }
        return pArtistName.toLowerCase().compareTo(another.pArtistName.toLowerCase());
    }
}
