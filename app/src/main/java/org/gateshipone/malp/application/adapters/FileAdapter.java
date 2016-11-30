/*
 * Copyright (C) 2016 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gateshipone.malp.application.adapters;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.gateshipone.malp.application.listviewitems.FileListItem;
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
        if ( file instanceof MPDFile) {
            if ( null != convertView ) {
                ((FileListItem)convertView).setTrack((MPDFile) file, mContext);
                return convertView;
            } else {
                return new FileListItem(mContext, (MPDFile) file, mShowIcons);
            }
        } else if (file instanceof MPDDirectory) {
            if ( null != convertView ) {
                ((FileListItem)convertView).setDirectory((MPDDirectory) file, mContext);
                return convertView;
            } else {
                return new FileListItem(mContext, (MPDDirectory) file, mShowIcons);
            }
        } else if ( file instanceof MPDPlaylist ) {
            if ( null != convertView ) {
                ((FileListItem)convertView).setPlaylist((MPDPlaylist) file, mContext);
                return convertView;
            }else {
                return new FileListItem(mContext, (MPDPlaylist) file, mShowIcons);
            }
        }
        return new FileListItem(mContext, mShowIcons);
    }
}
