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
import android.widget.GridView;

import andrompd.org.andrompd.application.listviewitems.ArtistGridViewItem;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDAlbum;

public class AlbumsGridAdapter extends GenericSectionAdapter<MPDAlbum> {

    private final GridView mRootGrid;
    private final Context mContext;

    public AlbumsGridAdapter(Context context, GridView rootGrid) {
        super();

        mContext = context;
        mRootGrid = rootGrid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MPDAlbum album = mModelData.get(position);
        String label = album.getName();

        // Check if a view can be recycled
        if (convertView != null) {
            ArtistGridViewItem gridItem = (ArtistGridViewItem) convertView;

            // Make sure to reset the layoutParams in case of change (rotation for example)
            ViewGroup.LayoutParams layoutParams = gridItem.getLayoutParams();
            layoutParams.height = mRootGrid.getColumnWidth();
            layoutParams.width = mRootGrid.getColumnWidth();
            gridItem.setLayoutParams(layoutParams);
            gridItem.setTitle(label);
        } else {
            // Create new view if no reusable is available
            convertView = new ArtistGridViewItem(mContext, label, null, new android.widget.AbsListView.LayoutParams(mRootGrid.getColumnWidth(), mRootGrid.getColumnWidth()));
        }

        // Check if the scroll speed currently is already 0, then start the image task right away.
        if (mScrollSpeed == 0) {
            ((ArtistGridViewItem) convertView).startCoverImageTask();
        }
        return convertView;
    }
}
