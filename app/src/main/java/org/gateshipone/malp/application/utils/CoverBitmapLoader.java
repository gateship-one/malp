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

package org.gateshipone.malp.application.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.artworkdatabase.ImageNotFoundException;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFile;

public class CoverBitmapLoader {
    private static final String TAG = CoverBitmapLoader.class.getSimpleName();
    private final CoverBitmapListener mListener;
    private final Context mContext;
    private MPDFile mTrack;

    public CoverBitmapLoader(Context context, CoverBitmapListener listener) {
        mContext = context;
        mListener = listener;
    }

    /**
     * Load the image for the given track from the mediastore.
     */
    public void getImage(MPDFile track) {
        if (track != null) {
            mTrack = track;
            // start the loader thread to load the image async
            Thread loaderThread = new Thread(new ImageRunner());
            loaderThread.start();
        }
    }

    public void getArtistImage(MPDArtist artist) {
        if ( artist == null) {
            return;
        }

        // start the loader thread to load the image async
        Thread loaderThread = new Thread(new ArtistImageRunner(artist));
        loaderThread.start();
    }

    public void getAlbumImage(MPDAlbum album) {
        if ( album == null) {
            return;
        }

        // start the loader thread to load the image async
        Thread loaderThread = new Thread(new AlbumImageRunner(album));
        loaderThread.start();
    }

    private class ImageRunner implements Runnable {

        /**
         * Load the image for the given track from the mediastore.
         */
        @Override
        public void run() {
            try {
                Bitmap albumImage = ArtworkManager.getInstance(mContext.getApplicationContext()).getAlbumImageForTrack(mTrack);
                mListener.receiveBitmap(albumImage);
            } catch (ImageNotFoundException e) {
                ArtworkManager.getInstance(mContext.getApplicationContext()).fetchAlbumImage(mTrack);
            }
        }
    }

    private class ArtistImageRunner implements Runnable {

        private MPDArtist mArtist;

        public ArtistImageRunner(MPDArtist artist) {
            mArtist = artist;
        }

        /**
         * Load the image for the given track from the mediastore.
         */
        @Override
        public void run() {
            try {
                Bitmap artistImage = ArtworkManager.getInstance(mContext.getApplicationContext()).getArtistImage(mArtist);
                mListener.receiveBitmap(artistImage);
            } catch (ImageNotFoundException e) {
                ArtworkManager.getInstance(mContext.getApplicationContext()).fetchArtistImage(mArtist);
            }
        }
    }

    private class AlbumImageRunner implements Runnable {

        private MPDAlbum mAlbum;

        public AlbumImageRunner(MPDAlbum album) {
            mAlbum = album;
        }

        /**
         * Load the image for the given track from the mediastore.
         */
        @Override
        public void run() {
            try {
                Bitmap artistImage = ArtworkManager.getInstance(mContext.getApplicationContext()).getAlbumImage(mAlbum);
                mListener.receiveBitmap(artistImage);
                Log.v(TAG,"Image found");
            } catch (ImageNotFoundException e) {
                Log.v(TAG,"Image notfound");
                ArtworkManager.getInstance(mContext.getApplicationContext()).fetchAlbumImage(mAlbum);
            }
        }
    }


    /**
     * Callback if image was loaded.
     */
    public interface CoverBitmapListener {
        void receiveBitmap(Bitmap bm);
    }
}
