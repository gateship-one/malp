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
import android.util.Log;

import org.gateshipone.malp.application.utils.FileUtils;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;


public class ArtworkDatabaseManager extends SQLiteOpenHelper {
    private static final String TAG = ArtworkDatabaseManager.class.getSimpleName();
    /**
     * The name of the database
     */
    private static final String DATABASE_NAME = "OdysseyArtworkDB";

    /**
     * The version of the database
     */
    private static final int DATABASE_VERSION = 22;

    private static ArtworkDatabaseManager mInstance;

    private static final String DIRECTORY_ALBUM_IMAGES = "albumArt";

    private static final String DIRECTORY_ARTIST_IMAGES = "artistArt";

    private ArtworkDatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized ArtworkDatabaseManager getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new ArtworkDatabaseManager(context);
        }
        return mInstance;
    }

    /**
     * Creates the database tables if they are not already existing
     *
     * @param db The {@link SQLiteDatabase} instance that will be used to create the tables.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        AlbumArtTable.createTable(db);
        ArtistArtTable.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == 22) {
            AlbumArtTable.dropTable(db);
            ArtistArtTable.dropTable(db);
            onCreate(db);
        }
    }

    /**
     * Tries to fetch an image for the album with the given id (android album id).
     *
     * @param album Album containing a valid mbid
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized byte[] getAlbumImage(final Context context, final MPDAlbum album) throws ImageNotFoundException {
        return getAlbumImageFromMBID(context, album.getMBID());
    }

    /**
     * Tries to fetch an image for the album with the given id (android album id).
     *
     * @param mbid MBID for the album to check
     * @return The byte[] containing the raw image file. This can be decoded with BitmapFactory.
     * @throws ImageNotFoundException If the image is not in the database and it was not searched for before.
     */
    public synchronized byte[] getAlbumImageFromMBID(final Context context, String mbid) throws ImageNotFoundException {
        final SQLiteDatabase database = getReadableDatabase();

        final String selection = AlbumArtTable.COLUMN_ALBUM_MBID + "=?";

        final Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_ALBUM_MBID, AlbumArtTable.COLUMN_IMAGE_FILE_PATH, AlbumArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{mbid}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                requestCursor.close();
                database.close();
                return null;
            }
            final String artworkFilename = requestCursor.getString(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();
            database.close();

            // try to read the file
            final byte[] image = FileUtils.readArtworkFile(context, artworkFilename, DIRECTORY_ALBUM_IMAGES);

            if (image != null) {
                return image;
            }
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
    public synchronized byte[] getArtistImage(final Context context, final MPDArtist artist) throws ImageNotFoundException {
        final SQLiteDatabase database = getReadableDatabase();

        final String selection = ArtistArtTable.COLUMN_ARTIST_MBID + "=?";

        final StringBuilder mbids = new StringBuilder();
        for (int i = 0; i < artist.getMBIDCount(); i++) {
            mbids.append(artist.getMBID(i));
        }

        final Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_ARTIST_MBID, ArtistArtTable.COLUMN_IMAGE_FILE_PATH, ArtistArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{String.valueOf(mbids.toString())}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                requestCursor.close();
                database.close();
                return null;
            }

            // get the filename for the image
            final String artworkFilename = requestCursor.getString(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();
            database.close();

            // try to read the file
            final byte[] image = FileUtils.readArtworkFile(context, artworkFilename, DIRECTORY_ARTIST_IMAGES);

            if (image != null) {
                return image;
            }
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
    public synchronized byte[] getArtistImage(final Context context, final String artistName) throws ImageNotFoundException {
        final SQLiteDatabase database = getReadableDatabase();

        final String selection = ArtistArtTable.COLUMN_ARTIST_NAME + "=?";

        final Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_ARTIST_NAME, ArtistArtTable.COLUMN_IMAGE_FILE_PATH, ArtistArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{artistName}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                requestCursor.close();
                database.close();
                return null;
            }

            // get the filename for the image
            final String artworkFilename = requestCursor.getString(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();
            database.close();

            // try to read the file
            final byte[] image = FileUtils.readArtworkFile(context, artworkFilename, DIRECTORY_ARTIST_IMAGES);

            if (image != null) {
                return image;
            }
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
    public synchronized void insertArtistImage(final Context context, final MPDArtist artist, final byte[] image) {
        final SQLiteDatabase database = getWritableDatabase();

        final StringBuilder mbids = new StringBuilder();
        for (int i = 0; i < artist.getMBIDCount(); i++) {
            mbids.append(artist.getMBID(i));
        }

        final String artistName = artist.getArtistName();

        String artworkFilename = null;
        if (image != null) {
            try {
                artworkFilename = FileUtils.createSHA256HashForString(mbids.toString(), artistName) + ".jpg";
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return;
            }

            try {
                FileUtils.saveArtworkFile(context, artworkFilename, DIRECTORY_ARTIST_IMAGES, image);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        final ContentValues values = new ContentValues();
        values.put(ArtistArtTable.COLUMN_ARTIST_MBID, mbids.toString());
        values.put(ArtistArtTable.COLUMN_ARTIST_NAME, artist.getArtistName());
        values.put(ArtistArtTable.COLUMN_IMAGE_FILE_PATH, artworkFilename);

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
    public synchronized byte[] getAlbumImage(final Context context, final String albumName) throws ImageNotFoundException {
        final SQLiteDatabase database = getReadableDatabase();

        final String selection = AlbumArtTable.COLUMN_ALBUM_NAME + "=?";

        final Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_ALBUM_NAME, AlbumArtTable.COLUMN_IMAGE_FILE_PATH, AlbumArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{albumName}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                requestCursor.close();
                database.close();
                return null;
            }
            final String artworkFilename = requestCursor.getString(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();
            database.close();

            final byte[] image = FileUtils.readArtworkFile(context, artworkFilename, DIRECTORY_ALBUM_IMAGES);

            if (image != null) {
                return image;
            }
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
    public synchronized byte[] getAlbumImage(final Context context, final String albumName, final String artistName) throws ImageNotFoundException {
        final SQLiteDatabase database = getReadableDatabase();

        final String selection = AlbumArtTable.COLUMN_ALBUM_NAME + "=? AND " + AlbumArtTable.COLUMN_ARTIST_NAME + "=?";

        final Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_ALBUM_NAME, AlbumArtTable.COLUMN_IMAGE_FILE_PATH, AlbumArtTable.COLUMN_IMAGE_NOT_FOUND},
                selection, new String[]{albumName, artistName}, null, null, null);

        // Check if an image was found
        if (requestCursor.moveToFirst()) {
            // If the not_found flag is set then return null here, to indicate that the image is not here but was searched for before.
            if (requestCursor.getInt(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND)) == 1) {
                requestCursor.close();
                database.close();
                return null;
            }
            final String artworkFilename = requestCursor.getString(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();
            database.close();

            final byte[] image = FileUtils.readArtworkFile(context, artworkFilename, DIRECTORY_ALBUM_IMAGES);

            if (image != null) {
                return image;
            }
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
    public synchronized void insertAlbumImage(final Context context, final MPDAlbum album, final byte[] image) {
        final SQLiteDatabase database = getWritableDatabase();

        final String albumMBID = album.getMBID();
        final String albumName = album.getName();
        final String albumArtistName = album.getArtistName();

        String artworkFilename = null;
        if (image != null) {
            try {
                artworkFilename = FileUtils.createSHA256HashForString(albumMBID, albumName, albumArtistName) + ".jpg";
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return;
            }

            try {
                FileUtils.saveArtworkFile(context, artworkFilename, DIRECTORY_ALBUM_IMAGES, image);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        final ContentValues values = new ContentValues();
        values.put(AlbumArtTable.COLUMN_ALBUM_MBID, album.getMBID());
        values.put(AlbumArtTable.COLUMN_ALBUM_NAME, album.getName());
        values.put(AlbumArtTable.COLUMN_ARTIST_NAME, album.getArtistName());
        values.put(AlbumArtTable.COLUMN_IMAGE_FILE_PATH, artworkFilename);

        // If null was given as byte[] set the not_found flag for this entry.
        values.put(AlbumArtTable.COLUMN_IMAGE_NOT_FOUND, image == null ? 1 : 0);

        database.replace(AlbumArtTable.TABLE_NAME, "", values);

        database.close();
    }

    /**
     * Removes all lines from the artists table
     */
    public synchronized void clearArtistImages(final Context context) {
        SQLiteDatabase database = getWritableDatabase();

        database.delete(ArtistArtTable.TABLE_NAME, null, null);

        database.close();

        FileUtils.removeArtworkDirectory(context, DIRECTORY_ARTIST_IMAGES);
    }

    /**
     * Removes all lines from the albums table
     */
    public synchronized void clearAlbumImages(final Context context) {
        SQLiteDatabase database = getWritableDatabase();

        database.delete(AlbumArtTable.TABLE_NAME, null, null);

        database.close();

        FileUtils.removeArtworkDirectory(context, DIRECTORY_ALBUM_IMAGES);
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

    public synchronized void removeArtistImage(final Context context, final MPDArtist artist) {
        final SQLiteDatabase database = getWritableDatabase();

        String where;
        String whereArgs[];

        if (artist.getMBIDCount() == 0) {
            where = ArtistArtTable.COLUMN_ARTIST_NAME + "=?";
            whereArgs = new String[]{artist.getArtistName()};
        } else {
            where = ArtistArtTable.COLUMN_ARTIST_MBID + "=? OR " + ArtistArtTable.COLUMN_ARTIST_NAME + "=?";
            whereArgs = new String[]{artist.getMBID(0), artist.getArtistName()};
        }

        final Cursor requestCursor = database.query(ArtistArtTable.TABLE_NAME, new String[]{ArtistArtTable.COLUMN_IMAGE_FILE_PATH},
                where, whereArgs, null, null, null);

        if (requestCursor.moveToFirst()) {

            final String artworkFilename = requestCursor.getString(requestCursor.getColumnIndex(ArtistArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();

            FileUtils.removeArtworkFile(context, artworkFilename, DIRECTORY_ARTIST_IMAGES);
        }

        database.delete(ArtistArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

    public synchronized void removeAlbumImage(final Context context, final MPDAlbum album) {
        final SQLiteDatabase database = getWritableDatabase();

        String where;
        String whereArgs[];

        // Check if a MBID is present or not
        if (album.getMBID().isEmpty()) {
            where = "(" + AlbumArtTable.COLUMN_ALBUM_NAME + "=? AND " + AlbumArtTable.COLUMN_ARTIST_NAME + "=? ) ";
            whereArgs = new String[]{album.getName(), album.getArtistName()};
        } else {
            where = AlbumArtTable.COLUMN_ALBUM_MBID + "=? OR (" + AlbumArtTable.COLUMN_ALBUM_NAME + "=? AND " + AlbumArtTable.COLUMN_ARTIST_NAME + "=? ) ";
            whereArgs = new String[]{album.getMBID(), album.getName(), album.getArtistName()};
        }

        final Cursor requestCursor = database.query(AlbumArtTable.TABLE_NAME, new String[]{AlbumArtTable.COLUMN_IMAGE_FILE_PATH},
                where, whereArgs, null, null, null);

        if (requestCursor.moveToFirst()) {

            final String artworkFilename = requestCursor.getString(requestCursor.getColumnIndex(AlbumArtTable.COLUMN_IMAGE_FILE_PATH));

            requestCursor.close();

            FileUtils.removeArtworkFile(context, artworkFilename, DIRECTORY_ARTIST_IMAGES);
        }

        database.delete(AlbumArtTable.TABLE_NAME, where, whereArgs);

        database.close();
    }

}
