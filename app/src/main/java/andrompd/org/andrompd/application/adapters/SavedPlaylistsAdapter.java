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

package andrompd.org.andrompd.application.adapters;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import andrompd.org.andrompd.application.listviewitems.ProfileListItem;
import andrompd.org.andrompd.application.listviewitems.SavedPlaylistListItem;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDPlaylist;
import andrompd.org.andrompd.mpdservice.profilemanagement.MPDServerProfile;

public class SavedPlaylistsAdapter extends GenericSectionAdapter<MPDPlaylist> {
    private Context mContext;

    public SavedPlaylistsAdapter(Context context) {
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MPDPlaylist playlist = mModelData.get(position);

        // title
        String playlistName = playlist.getName();
        String lastModified = playlist.getLastModified();


        if (convertView != null) {
            SavedPlaylistListItem savedPlaylistListItem = (SavedPlaylistListItem) convertView;

            savedPlaylistListItem.setPlaylistName(playlistName);
            savedPlaylistListItem.setLastModified(lastModified);
        } else {
            convertView = new SavedPlaylistListItem(mContext, playlistName, lastModified);
        }

        return convertView;
    }
}
