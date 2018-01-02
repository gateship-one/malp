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

package org.gateshipone.malp.application.utils;


import android.content.Context;
import android.content.SharedPreferences;

import org.gateshipone.malp.R;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;

public class PreferenceHelper {

    public static MPDAlbum.MPD_ALBUM_SORT_ORDER getMPDAlbumSortOrder(SharedPreferences prefs, Context context) {
        String sortOrderPref = prefs.getString(context.getString(R.string.pref_album_sort_order_key), context.getString(R.string.pref_artist_albums_sort_default));

        // Check with settings keys.
        if(sortOrderPref.equals(context.getString(R.string.pref_artist_albums_sort_name_key))) {
            return MPDAlbum.MPD_ALBUM_SORT_ORDER.TITLE;
        } else if (sortOrderPref.equals(context.getString(R.string.pref_artist_albums_sort_year_key))) {
            return MPDAlbum.MPD_ALBUM_SORT_ORDER.DATE;
        }

        // Default value
        return MPDAlbum.MPD_ALBUM_SORT_ORDER.TITLE;
    }
}
