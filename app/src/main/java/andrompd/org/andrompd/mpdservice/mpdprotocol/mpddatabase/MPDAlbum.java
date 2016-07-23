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

/**
 * Created by hendrik on 16.07.16.
 */
public class MPDAlbum {
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
}
