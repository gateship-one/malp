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

package org.gateshipone.malp.application.artworkdatabase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.gateshipone.malp.BuildConfig;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;


public class ArtworkDatabaseManager extends SQLiteOpenHelper {

    /**
     * The name of the database
     */
    private static final String DATABASE_NAME = "OdysseyArtworkDB";

    /**
     * The version of the database
     */
    private static final int DATABASE_VERSION = BuildConfig.VERSION_CODE;

    private Context mContext;

    private static ArtworkDatabaseManager mInstance;

    private ArtworkDatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized ArtworkDatabaseManager getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new ArtworkDatabaseManager(context);
            ArtworkDatabaseManager.mInstance.mContext = context;
        }
        return mInstance;
    }


    /**
     * Creates the database tables if they are not already existing
     *
     * @param db
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        AlbumArtTable.createTable(db);
        ArtistArtTable.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //FIXME
    }

    /**
     * Tries to fetch an image for the album with the given id (android album id).
     *
     * @param album Album containing a valid mbid
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized byte[] getAlbumImage(MPDAlbum album) throws ImageNotFoundException {
        return getAlbumImageFromMBID(album.getMBID());
    }

    /**
     * Tries to fetch an image for the album with the given id (android album id).
     *
     * @param mbid MBID for the album to check
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized byte[] getAlbumImageFromMBID(String mbid) throws ImageNotFoundException {
        SQLiteDatabase database = getReadableDatabase();

        String selection = AlbumArtTable.COLUMN_ALBUM_MBID + "=?";


        Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_ALBUM_MBID, AlbumArtTable.COLUMN_IMAGE_DATA, AlbumArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{mbid}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                return null;
            }
            byte[] imageData = requestCursor.getBlob(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_DATA));

            requestCursor.close();
            database.close();
            return imageData;
        }

        // If we reach this, no entry was found for the given request. Throw an exception
        requestCursor.close();
        database.close();
        throw new ImageNotFoundException();
    }

    /**
     * Tries to fetch an image for the artist with the given id (android artist id).
     *
     * @param artist Artist containing a musicbrainz id
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized byte[] getArtistImage(MPDArtist artist) throws ImageNotFoundException {
        SQLiteDatabase database = getReadableDatabase();

        String selection = ArtistArtTable.COLUMN_ARTIST_MBID + "=?";

        String mbids = "";
        for ( int i = 0; i < artist.getMBIDCount(); i++ ) {
            mbids += artist.getMBID(i);
        }

        Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_ARTIST_MBID, ArtistArtTable.COLUMN_IMAGE_DATA, ArtistArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{String.valueOf(mbids)}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                return null;
            }
            byte[] imageData = requestCursor.getBlob(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_DATA));

            requestCursor.close();
            database.close();
            return imageData;
        }

        // If we reach this, no entry was found for the given request. Throw an exception
        requestCursor.close();
        database.close();
        throw new ImageNotFoundException();
    }

    /**
     * Tries to fetch an image for the album with the given name. This is useful if artist_id is not set
     *
     * @param artistName The name of the artist to search for.
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized byte[] getArtistImage(String artistName) throws ImageNotFoundException {
        SQLiteDatabase database = getReadableDatabase();

        String selection = ArtistArtTable.COLUMN_ARTIST_NAME + "=?";


        Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_ARTIST_NAME, ArtistArtTable.COLUMN_IMAGE_DATA, ArtistArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{artistName}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                return null;
            }
            byte[] imageData = requestCursor.getBlob(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_DATA));

            requestCursor.close();
            database.close();
            return imageData;
        }

        // If we reach this, no entry was found for the given request. Throw an exception
        requestCursor.close();
        database.close();
        throw new ImageNotFoundException();
    }

    /**
     * Inserts the given byte[] image to the artists table.
     *
     * @param artist Artist for the associated image byte[].
     * @param image  byte[] containing the raw image that was downloaded. This can be null in which case
     *               the database entry will have the not_found flag set.
     */
    public synchronized void insertArtistImage(MPDArtist artist, byte[] image) {
        SQLiteDatabase database = getWritableDatabase();



        ContentValues values = new ContentValues();

        String mbids = "";
        for ( int i = 0; i < artist.getMBIDCount(); i++ ) {
            mbids += artist.getMBID(i);
        }

        values.put(ArtistArtTable.COLUMN_ARTIST_MBID, mbids);
        values.put(ArtistArtTable.COLUMN_ARTIST_NAME, artist.getArtistName());
        values.put(ArtistArtTable.COLUMN_IMAGE_DATA, image);

        // If null was given as byte[] set the not_found flag for this entry.
        values.put(ArtistArtTable.COLUMN_IMAGE_NOT_FOUND, image == null ? 1 : 0);

        database.replace(ArtistArtTable.TABLE_NAME, "", values);

        database.close();
    }


    /**
     * Tries to fetch an image for the album with the given name. This can result in wrong results for e.g. "Greatest Hits"
     *
     * @param albumName The name of the album to search for.
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized byte[] getAlbumImage(String albumName) throws ImageNotFoundException {
        SQLiteDatabase database = getReadableDatabase();

        String selection = AlbumArtTable.COLUMN_ALBUM_NAME + "=?";


        Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_ALBUM_NAME, AlbumArtTable.COLUMN_IMAGE_DATA, AlbumArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{albumName}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                return null;
            }
            byte[] imageData = requestCursor.getBlob(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_DATA));

            requestCursor.close();
            database.close();
            return imageData;
        }

        // If we reach this, no entry was found for the given request. Throw an exception
        requestCursor.close();
        database.close();
        throw new ImageNotFoundException();
    }

    /**
     * Tries to fetch an image for the album with the given name. This can result in wrong results for e.g. "Greatest Hits"
     *
     * @param albumName The name of the album to search for.
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized byte[] getAlbumImage(String albumName, String artistName) throws ImageNotFoundException {
        SQLiteDatabase database = getReadableDatabase();

        String selection = AlbumArtTable.COLUMN_ALBUM_NAME + "=? AND " + AlbumArtTable.COLUMN_ARTIST_NAME + "=?";


        Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_ALBUM_NAME, AlbumArtTable.COLUMN_IMAGE_DATA, AlbumArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{albumName,artistName}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                return null;
            }
            byte[] imageData = requestCursor.getBlob(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_DATA));

            requestCursor.close();
            database.close();
            return imageData;
        }

        // If we reach this, no entry was found for the given request. Throw an exception
        requestCursor.close();
        database.close();
        throw new ImageNotFoundException();
    }

    /**
     * Inserts the given byte[] image to the albums table.
     *
     * @param album Album for the associated image byte[].
     * @param image byte[] containing the raw image that was downloaded. This can be null in which case
     *              the database entry will have the not_found flag set.
     */
    public synchronized void insertAlbumImage(MPDAlbum album, byte[] image) {
        SQLiteDatabase database = getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(AlbumArtTable.COLUMN_ALBUM_MBID, album.getMBID());
        values.put(AlbumArtTable.COLUMN_ALBUM_NAME, album.getName());
        values.put(AlbumArtTable.COLUMN_ARTIST_NAME, album.getArtistName());
        values.put(AlbumArtTable.COLUMN_IMAGE_DATA, image);

        // If null was given as byte[] set the not_found flag for this entry.
        values.put(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND, image == null ? 1 : 0);

        database.replace(AlbumArtTable.TABLE_NAME, "", values);

        database.close();
    }

    /**
     * Removes all lines from the artists table
     */
    public synchronized void clearArtistImages() {
        SQLiteDatabase database = getWritableDatabase();

        database.delete(ArtistArtTable.TABLE_NAME, null, null);

        database.close();
    }

    /**
     * Removes all lines from the albums table
     */
    public synchronized void clearAlbumImages() {
        SQLiteDatabase database = getWritableDatabase();

        database.delete(AlbumArtTable.TABLE_NAME, null, null);

        database.close();
    }

    public synchronized void clearBlockedArtistImages() {
        SQLiteDatabase database = getWritableDatabase();

        String where = ArtistArtTable.COLUMN_IMAGE_NOT_FOUND + "=?";
        String whereArgs[] = {"1"};

        database.delete(ArtistArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

    public synchronized void clearBlockedAlbumImages() {
        SQLiteDatabase database = getWritableDatabase();

        String where = AlbumArtTable.COLUMN_IMAGE_NOT_FOUND + "=?";
        String whereArgs[] = {"1"};

        database.delete(AlbumArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

}
