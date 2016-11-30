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

package org.gateshipone.malp.application.utils;

import android.graphics.Bitmap;
import android.os.AsyncTask;


import org.gateshipone.malp.application.adapters.GenericSectionAdapter;
import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.artworkdatabase.ImageNotFoundException;
import org.gateshipone.malp.application.listviewitems.CoverLoadable;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDGenericItem;

/*
 * Loaderclass for covers
 */
public class AsyncLoader extends AsyncTask<AsyncLoader.CoverViewHolder, Void, Bitmap> {
    private static final String TAG = AsyncLoader.class.getSimpleName();
    private CoverViewHolder mCover;

    /**
     * Time when loading of the image started to determine the loading speed of images.
     */
    private long mStartTime;

    /**
     * Wrapper class for covers
     */
    public static class CoverViewHolder {
        public CoverLoadable coverLoadable;
        public ArtworkManager artworkManager;
        public MPDGenericItem modelItem;
        public GenericSectionAdapter mAdapter;
    }

    @Override
    protected Bitmap doInBackground(CoverViewHolder... params) {
        // Save the time when loading started for later duration calculation
        mStartTime = System.currentTimeMillis();
        mCover = params[0];
        Bitmap image = null;
        // Check if model item is artist or album
        if (mCover.modelItem instanceof MPDArtist) {
            MPDArtist artist = (MPDArtist)mCover.modelItem;
            try {
                // Check if image is available. If it is not yet fetched it will throw an exception
                // If it was already searched for and not found, this will be null.
                image = mCover.artworkManager.getArtistImage(artist);
            } catch (ImageNotFoundException e) {
                // Check if fetching for this item is already ongoing
                if (!artist.getFetching()) {
                    // If not set it as ongoing and request the image fetch.
                    mCover.artworkManager.fetchArtistImage(artist);
                    artist.setFetching(true);
                }
            }
        } else if (mCover.modelItem instanceof MPDAlbum) {
            MPDAlbum album = (MPDAlbum)mCover.modelItem;
            try {
                // Check if image is available. If it is not yet fetched it will throw an exception.
                // If it was already searched for and not found, this will be null.
                image = mCover.artworkManager.getAlbumImage(album);
            } catch (ImageNotFoundException e) {
                // Check if fetching for this item is already ongoing
                if (!album.getFetching()) {
                    // If not set it as ongoing and request the image fetch.
                    mCover.artworkManager.fetchAlbumImage(album);
                    album.setFetching(true);
                }
            }
        }
        return image;
    }


    @Override
    protected void onPostExecute(Bitmap result) {
        super.onPostExecute(result);

        // set mCover if exists
        if ( null != result ) {
            // Check how long image loading took and notify the adapter about the time.
            if ( mCover.mAdapter != null) {
                mCover.mAdapter.addImageLoadTime(System.currentTimeMillis() - mStartTime);
            }
            mCover.coverLoadable.setImage(result);
        }
    }
}