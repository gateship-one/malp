/*
 *  Copyright (C) 2018 Team Gateship-One
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


import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

import java.util.Iterator;
import java.util.List;

public class MPDFileListFilter {

    public static void filterAlbumArtistTracks(List<MPDFileEntry> list, String albumArtist) {
        filterMPDTrack(list, track -> {return albumArtist.toLowerCase().equals(track.getTrackAlbumArtist().toLowerCase())
                || albumArtist.toLowerCase().equals(track.getTrackArtist().toLowerCase());});
    }

    public static void filterAlbumMBID(List<MPDFileEntry> list, String albumMBID) {
        filterMPDTrack(list, track -> {return albumMBID.toLowerCase().equals(track.getTrackAlbumMBID().toLowerCase());});
    }

    public static void filterAlbumMBIDandAlbumArtist(List<MPDFileEntry> list, String albumMBID, String albumArtist) {
        filterMPDTrack(list, track -> {return albumMBID.toLowerCase().equals(track.getTrackAlbumMBID().toLowerCase())
                && (albumArtist.toLowerCase().equals(track.getTrackAlbumArtist().toLowerCase())
                || (albumArtist.toLowerCase().equals(track.getTrackArtist().toLowerCase())));});
    }

    public static void filterAlbumMBIDandAlbumArtistSort(List<MPDFileEntry> list, String albumMBID, String albumArtist) {
        filterMPDTrack(list, track -> {return albumMBID.toLowerCase().equals(track.getTrackAlbumMBID().toLowerCase())
                && (albumArtist.toLowerCase().equals(track.getTrackAlbumArtistSort().toLowerCase())
                || (albumArtist.toLowerCase().equals(track.getTrackArtistSort().toLowerCase())));});
    }


    private static void filterMPDTrack(List<MPDFileEntry> list, MPDFileFilter filter) {
        Iterator<MPDFileEntry> iterator = list.iterator();

        while(iterator.hasNext()) {
            MPDFileEntry item = iterator.next();
            if (item instanceof MPDTrack) {
                if(!filter.accept((MPDTrack)item)) {
                    // If filter does not match, remove element
                    iterator.remove();
                }
            }
        }
    }

    private interface MPDFileFilter {
        boolean accept(MPDTrack track);
    }
}
