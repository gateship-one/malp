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

package org.gateshipone.malp.application.artworkdatabase.network.artprovider;


import com.android.volley.Response;

import org.gateshipone.malp.application.artworkdatabase.network.responses.AlbumFetchError;
import org.gateshipone.malp.application.artworkdatabase.network.responses.AlbumImageResponse;
import org.gateshipone.malp.application.artworkdatabase.network.responses.TrackAlbumFetchError;
import org.gateshipone.malp.application.artworkdatabase.network.responses.TrackAlbumImageResponse;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

public interface TrackAlbumImageProvider {
    void fetchAlbumImage(final MPDTrack track, final Response.Listener<TrackAlbumImageResponse> listener, final TrackAlbumFetchError errorListener);

}
