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

package org.gateshipone.malp.application.adapters;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.listviewitems.GenericFileListItem;
import org.gateshipone.malp.application.listviewitems.TrackListViewItem;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDDirectory;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFile;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDPlaylist;

/**
 * Adapter class that creates all the listitems for an album track view
 */
public class FileAdapter extends GenericSectionAdapter<MPDFileEntry> {

    Context mContext;

    boolean mShowIcons;

    boolean mShowTrackNumbers;

    /**
     * Standard constructor
     *
     * @param context Context used for creating listview items
     * @param showIcons If icons should be shown in view (e.g. for FileExplorer)
     * @param showTrackNumbers If track numbers should be used for index or the position (Albums: tracknumbers, playlists: indices)
     */
    public FileAdapter(Context context, boolean showIcons, boolean showTrackNumbers) {
        super();

        mShowIcons = showIcons;
        mContext = context;
        mShowTrackNumbers = showTrackNumbers;
    }

    /**
     * Create the actual listview items if no reusable object is provided.
     *
     * @param position    Index of the item to create.
     * @param convertView If != null this view can be reused to optimize performance.
     * @param parent      Parent of the view
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get MPDFile at the given index used for this item.
        MPDFileEntry file = (MPDFileEntry)getItem(position);
        if (file instanceof MPDFile) {

            MPDFile track = (MPDFile) file;

            // Get track title
            String trackTitle = track.getTrackTitle();

            if ( null == trackTitle || trackTitle.isEmpty() ) {
                trackTitle = track.getPath();
            }

            // additional information (artist + album)
            String trackInformation = track.getTrackArtist() + mContext.getString(R.string.track_item_separator) + track.getTrackAlbum();

            String trackNumber;
            if (mShowTrackNumbers) {
                // Get the number of the track
                trackNumber = String.valueOf(track.getTrackNumber());
            } else {
                trackNumber = String.valueOf(position + 1);
            }


            // Get the preformatted duration of the track.
            String trackDuration = track.getLengthString();

            // Check if reusable object is available

            // If not create a new Listitem
            convertView = new TrackListViewItem(mContext, trackNumber, trackTitle, trackInformation, trackDuration,mShowIcons);


            return convertView;
        } else if (file instanceof MPDPlaylist || file instanceof MPDDirectory) {

            convertView = new GenericFileListItem(mContext, (MPDFileEntry)file, mShowIcons);


            return convertView;
        }
        return new TrackListViewItem(mContext, "", "", "", "",mShowIcons);
    }
}
