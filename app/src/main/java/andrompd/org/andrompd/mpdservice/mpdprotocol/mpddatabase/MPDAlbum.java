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

package andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase;


public class MPDAlbum implements MPDGenericItem, Comparable<MPDAlbum> {
    /* Album properties */
    private String pName;

    /* Musicbrainz ID */
    private String pMBID;

    /* Artists name (if any) */
    private String pArtistName;

    public MPDAlbum(String name, String mbid, String artist ) {
        pName = name;
        pMBID = mbid;
        pArtistName = artist;
    }

    /* Getters */

    public String getName() {
        return pName;
    }

    public String getMBID() {
        return pMBID;
    }

    public String getArtistName() {
        return pArtistName;
    }



    @Override
    public String getSectionTitle() {
        return pName;
    }

    public boolean equals(MPDAlbum album) {
        if ( (pName.equals(album.pName)) && (pArtistName.equals(album.pArtistName)) &&
                (pMBID.equals(album.pMBID))) {
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(MPDAlbum another) {
        if ( another.equals(this) ) {
            return 0;
        }
        return pName.toLowerCase().compareTo(another.pName.toLowerCase());
    }
}
