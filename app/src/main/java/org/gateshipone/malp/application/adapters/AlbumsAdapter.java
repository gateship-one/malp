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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.listviewitems.GenericGridItem;
import org.gateshipone.malp.application.listviewitems.SimpleListItem;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;

public class AlbumsAdapter extends GenericSectionAdapter<MPDAlbum> implements ArtworkManager.onNewAlbumImageListener {
    private static final String TAG = AlbumsAdapter.class.getSimpleName();
    private final AbsListView mListView;
    private final Context mContext;

    private boolean mUseList;

    private ArtworkManager mArtworkManager;

    public AlbumsAdapter(Context context, AbsListView listView, boolean useList) {
        super();

        mContext = context;
        mListView = listView;

        mUseList = useList;

        mArtworkManager = ArtworkManager.getInstance(context.getApplicationContext());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MPDAlbum album = (MPDAlbum)getItem(position);
        String label = album.getName();

        if ( label.isEmpty() ) {
            label = mContext.getResources().getString(R.string.no_album_tag);
        }

        String albumArtist = album.getArtistName();

        if ( mUseList ) {
            // Check if a view can be recycled
            if (convertView != null) {
                SimpleListItem listItem = (SimpleListItem) convertView;

                // Make sure to reset the layoutParams in case of change (rotation for example)
                listItem.setText(label);
                listItem.setDetails(albumArtist );
            } else {
                // Create new view if no reusable is available
                convertView = new SimpleListItem(mContext,label, albumArtist);
            }
        } else {
            GenericGridItem gridItem;
            ViewGroup.LayoutParams layoutParams;
            // Check if a view can be recycled
            if (convertView == null) {
                // Create new view if no reusable is available
                gridItem = new GenericGridItem(mContext, label, this);
                layoutParams = new android.widget.AbsListView.LayoutParams(((GridView)mListView).getColumnWidth(), ((GridView)mListView).getColumnWidth());
            } else {
                gridItem = (GenericGridItem) convertView;
                gridItem.setTitle(label);
                layoutParams = gridItem.getLayoutParams();
                layoutParams.height = ((GridView)mListView).getColumnWidth();
                layoutParams.width = ((GridView)mListView).getColumnWidth();
            }

            // Make sure to reset the layoutParams in case of change (rotation for example)
            gridItem.setLayoutParams(layoutParams);


            // This will prepare the view for fetching the image from the internet if not already saved in local database.
            gridItem.prepareArtworkFetching(mArtworkManager, album);

            // Check if the scroll speed currently is already 0, then start the image task right away.
            if (mScrollSpeed == 0) {
                gridItem.startCoverImageTask();
            }
            return gridItem;
        }

        return convertView;
    }

    @Override
    public void newAlbumImage(MPDAlbum album) {
        notifyDataSetChanged();
    }
}
