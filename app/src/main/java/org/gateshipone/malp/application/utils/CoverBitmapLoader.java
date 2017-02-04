/*
 *  Copyright (C) 2017 Team Gateship-One
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
import android.graphics.Bitmap;
import android.util.Log;

import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.artworkdatabase.ImageNotFoundException;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

public class CoverBitmapLoader {
    private static final String TAG = CoverBitmapLoader.class.getSimpleName();
    private final CoverBitmapListener mListener;
    private final Context mContext;
    private MPDTrack mTrack;

    public CoverBitmapLoader(Context context, CoverBitmapListener listener) {
        mContext = context;
        mListener = listener;
    }

    /**
     * Enum to define the type of the image that was retrieved
     */
    public enum IMAGE_TYPE {
        ALBUM_IMAGE,
        ARTIST_IMAGE,
    }

    /**
     * Load the image for the given track from the mediastore.
     */
    public void getImage(MPDTrack track, boolean fetchImage) {
        if (track != null) {
            mTrack = track;
            // start the loader thread to load the image async
            Thread loaderThread = new Thread(new ImageRunner(fetchImage));
            loaderThread.start();
        }
    }

    public void getArtistImage(MPDArtist artist, boolean fetchImage) {
        if (artist == null) {
            return;
        }

        // start the loader thread to load the image async
        Thread loaderThread = new Thread(new ArtistImageRunner(artist, fetchImage));
        loaderThread.start();
    }

    public void getArtistImage(MPDTrack track, boolean fetchImage) {
        if (track == null) {
            return;
        }

        // start the loader thread to load the image async
        Thread loaderThread = new Thread(new TrackArtistImageRunner(track, fetchImage));
        loaderThread.start();
    }

    public void getAlbumImage(MPDAlbum album, boolean fetchImage) {
        if (album == null) {
            return;
        }

        // start the loader thread to load the image async
        Thread loaderThread = new Thread(new AlbumImageRunner(album, fetchImage));
        loaderThread.start();
    }

    private class ImageRunner implements Runnable {

        private boolean mFetchImage;

        public ImageRunner(boolean fetchImage) {
            mFetchImage = fetchImage;
        }

        /**
         * Load the image for the given track from the mediastore.
         */
        @Override
        public void run() {
            try {
                Bitmap albumImage = ArtworkManager.getInstance(mContext.getApplicationContext()).getAlbumImageForTrack(mTrack);
                mListener.receiveBitmap(albumImage, IMAGE_TYPE.ALBUM_IMAGE);
            } catch (ImageNotFoundException e) {
                if (mFetchImage) {
                    ArtworkManager.getInstance(mContext.getApplicationContext()).fetchAlbumImage(mTrack);
                }
            }
        }
    }

    private class ArtistImageRunner implements Runnable {

        private MPDArtist mArtist;
        private boolean mFetchImage;

        public ArtistImageRunner(MPDArtist artist, boolean fetchImage) {
            mArtist = artist;
            mFetchImage = fetchImage;
        }

        /**
         * Load the image for the given track from the mediastore.
         */
        @Override
        public void run() {
            try {
                Bitmap artistImage = ArtworkManager.getInstance(mContext.getApplicationContext()).getArtistImage(mArtist);
                mListener.receiveBitmap(artistImage, IMAGE_TYPE.ARTIST_IMAGE);
            } catch (ImageNotFoundException e) {
                if (mFetchImage) {
                    ArtworkManager.getInstance(mContext.getApplicationContext()).fetchArtistImage(mArtist);
                }
            }
        }
    }

    private class TrackArtistImageRunner implements Runnable {

        private MPDArtist mArtist;
        private boolean mFetchImage;

        public TrackArtistImageRunner(MPDTrack track, boolean fetchImage) {
            mArtist = new MPDArtist(track.getTrackArtist());
            if ( !track.getTrackArtistMBID().isEmpty()) {
                mArtist.addMBID(track.getTrackArtistMBID());
            }
            mFetchImage = fetchImage;
        }

        /**
         * Load the image for the given track from the mediastore.
         */
        @Override
        public void run() {
            try {
                Bitmap artistImage = ArtworkManager.getInstance(mContext.getApplicationContext()).getArtistImage(mArtist);
                mListener.receiveBitmap(artistImage, IMAGE_TYPE.ARTIST_IMAGE);
            } catch (ImageNotFoundException e) {
                if (mFetchImage) {
                    ArtworkManager.getInstance(mContext.getApplicationContext()).fetchArtistImage(mArtist);
                }
            }
        }
    }

    private class AlbumImageRunner implements Runnable {

        private MPDAlbum mAlbum;
        private boolean mFetchImage;

        public AlbumImageRunner(MPDAlbum album, boolean fetchImage) {
            mAlbum = album;
            mFetchImage = fetchImage;
        }

        /**
         * Load the image for the given track from the mediastore.
         */
        @Override
        public void run() {
            try {
                Bitmap albumImage = ArtworkManager.getInstance(mContext.getApplicationContext()).getAlbumImage(mAlbum);
                mListener.receiveBitmap(albumImage, IMAGE_TYPE.ALBUM_IMAGE);
            } catch (ImageNotFoundException e) {
                if (mFetchImage) {
                    ArtworkManager.getInstance(mContext.getApplicationContext()).fetchAlbumImage(mAlbum);
                }
            }
        }
    }


    /**
     * Callback if image was loaded.
     */
    public interface CoverBitmapListener {
        void receiveBitmap(Bitmap bm, IMAGE_TYPE type);
    }
}
