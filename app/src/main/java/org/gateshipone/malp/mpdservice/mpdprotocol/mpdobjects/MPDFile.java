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

package org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects;


import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class represents an MPDFile. This is the same type for tracks and files.
 * This is used for tracks in playlist, album, search results,... and for music files when
 * retrieving an directory listing from the mpd server.
 */
public class MPDFile extends MPDFileEntry implements MPDGenericItem, Parcelable {


    /**
     * Title of the song
     */
    private String pTrackTitle;

    /**
     * Artist of the song
     */
    private String pTrackArtist;

    /**
     * Associated album of the song
     */
    private String pTrackAlbum;

    /**
     * The artist of the album of this song. E.g. Various Artists for compilations
     */
    private String pTrackAlbumArtist;

    /**
     * The date of the song
     */
    private String pDate;

    /**
     * MusicBrainz ID for the artist
     */
    private String pTrackArtistMBID;

    /**
     * MusicBrainz ID for the song itself
     */
    private String pTrackMBID;

    /**
     * MusicBrainz ID for the album of the song
     */
    private String pTrackAlbumMBID;

    /**
     * MusicBrainz ID for the album artist
     */
    private String pTrackAlbumArtistMBID;

    /**
     * Length in seconds
     */
    private int pLength;

    /**
     * Track number within the album of the song
     */
    private int pTrackNumber;

    /**
     * Count of songs on the album of the song. Can be 0
     */
    private int pAlbumTrackCount;

    /**
     * The number of the medium(of the songs album) the song is on
     */
    private int pDiscNumber;

    /**
     * The count of mediums of the album the track is on. Can be 0.
     */
    private int pAlbumDiscCount;


    /**
     * Create empty MPDFile (track). Fill it with setter methods during
     * parsing of mpds output.
     *
     * @param path The path of the file. This should never change.
     */
    public MPDFile(String path) {
        super(path);
        pTrackTitle = "";

        pTrackArtist = "";
        pTrackAlbum = "";
        pTrackAlbumArtist = "";

        pDate = "";

        pTrackArtistMBID = "";
        pTrackMBID = "";
        pTrackAlbumMBID = "";
        pTrackAlbumArtistMBID = "";

        pLength = 0;
    }

    /**
     * Create a MPDFile from a parcel
     *
     * @param in Parcel to deserialize
     */
    protected MPDFile(Parcel in) {
        super(in.readString());

        /**
         * Deserialize all properties. Check with serialization method. BOTH NEED TO BE EQUIVALENT
         */
        pTrackTitle = in.readString();
        pTrackAlbum = in.readString();
        pTrackArtist = in.readString();
        pTrackAlbumArtist = in.readString();

        pDate = in.readString();

        pTrackMBID = in.readString();
        pTrackAlbumMBID = in.readString();
        pTrackArtistMBID = in.readString();
        pTrackAlbumArtistMBID = in.readString();

        pLength = in.readInt();
        pTrackNumber = in.readInt();
        pAlbumTrackCount = in.readInt();
        pDiscNumber = in.readInt();
        pAlbumDiscCount = in.readInt();
    }

    public String getTrackTitle() {
        return pTrackTitle;
    }

    public void setTrackTitle(String pTrackTitle) {
        this.pTrackTitle = pTrackTitle;
    }


    public String getTrackArtist() {
        return pTrackArtist;
    }

    public void setTrackArtist(String pTrackArtist) {
        this.pTrackArtist = pTrackArtist;
    }

    public String getTrackAlbum() {
        return pTrackAlbum;
    }

    public void setTrackAlbum(String pTrackAlbum) {
        this.pTrackAlbum = pTrackAlbum;
    }

    public String getTrackAlbumArtist() {
        return pTrackAlbumArtist;
    }

    public void setTrackAlbumArtist(String pTrackAlbumArtist) {
        this.pTrackAlbumArtist = pTrackAlbumArtist;
    }

    public String getDate() {
        return pDate;
    }

    public void setDate(String pDate) {
        this.pDate = pDate;
    }

    public String getTrackArtistMBID() {
        return pTrackArtistMBID;
    }

    public void setTrackArtistMBID(String pTrackArtistMBID) {
        this.pTrackArtistMBID = pTrackArtistMBID;
    }

    public String getTrackAlbumArtistMBID() {
        return pTrackAlbumArtistMBID;
    }

    public void setTrackAlbumArtistMBID(String pTrackArtistMBID) {
        this.pTrackAlbumArtistMBID = pTrackArtistMBID;
    }

    public String getTrackMBID() {
        return pTrackMBID;
    }

    public void setTrackMBID(String pTrackMBID) {
        this.pTrackMBID = pTrackMBID;
    }

    public String getTrackAlbumMBID() {
        return pTrackAlbumMBID;
    }

    public void setTrackAlbumMBID(String pTrackAlbumMBID) {
        this.pTrackAlbumMBID = pTrackAlbumMBID;
    }

    public int getLength() {
        return pLength;
    }

    public void setLength(int pLength) {
        this.pLength = pLength;
    }

    public void setTrackNumber(int trackNumber) {
        pTrackNumber = trackNumber;
    }

    public int getTrackNumber() {
        return pTrackNumber;
    }

    public void setDiscNumber(int discNumber) {
        pDiscNumber = discNumber;
    }

    public int getDiscNumber() {
        return pDiscNumber;
    }

    public int getAlbumTrackCount() {
        return pAlbumTrackCount;
    }

    public void setAlbumTrackCount(int albumTrackCount) {
        pAlbumTrackCount = albumTrackCount;
    }

    public int getAlbumDiscCount() {
        return pAlbumDiscCount;
    }

    public void psetAlbumDiscCount(int discCount) {
        pAlbumDiscCount = discCount;
    }

    /**
     * @return String that is used for section based scrolling
     */
    @Override
    public String getSectionTitle() {
        return pTrackTitle.equals("") ? mPath : pTrackTitle;
    }


    /**
     * Describes if it is a special parcel type (no)
     *
     * @return 0
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Static creator class to create MPDFile objects from parcels.
     */
    public static final Creator<MPDFile> CREATOR = new Creator<MPDFile>() {
        /**
         * Create a new MPDFile with parcel creator.
         * @param in Parcel to use for creating the MPDFile object
         * @return The deserialized MPDFile object
         */
        @Override
        public MPDFile createFromParcel(Parcel in) {
            return new MPDFile(in);
        }

        /**
         * Used to create an array of MPDFile objects
         * @param size Size of the array to create
         * @return The created array
         */
        @Override
        public MPDFile[] newArray(int size) {
            return new MPDFile[size];
        }
    };

    /**
     * Serialized the MPDFile object to a parcel. Check that this method is equivalent with the
     * deserializing creator above.
     *
     * @param dest  Parcel to write the properties to
     * @param flags Special flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Serialize MPDFile properties
        dest.writeString(mPath);
        dest.writeString(pTrackTitle);
        dest.writeString(pTrackAlbum);
        dest.writeString(pTrackArtist);
        dest.writeString(pTrackAlbumArtist);

        dest.writeString(pDate);

        dest.writeString(pTrackMBID);
        dest.writeString(pTrackAlbumMBID);
        dest.writeString(pTrackArtistMBID);
        dest.writeString(pTrackAlbumArtistMBID);

        dest.writeInt(pLength);
        dest.writeInt(pTrackNumber);
        dest.writeInt(pAlbumTrackCount);
        dest.writeInt(pDiscNumber);
        dest.writeInt(pAlbumDiscCount);
    }

    public int indexCompare(MPDFile compFile) {
        if ( !pTrackAlbumMBID.equals(compFile.pTrackAlbumMBID)) {
            return pTrackAlbumMBID.compareTo(compFile.pTrackAlbumMBID);
        }
        // Compare disc numbers first
        if (pDiscNumber > compFile.pDiscNumber) {
            return 1;
        } else if (pDiscNumber == compFile.pDiscNumber) {
            // Compare track number field
            if (pTrackNumber > compFile.pTrackNumber) {
                return 1;
            } else if (pTrackNumber == compFile.pTrackNumber) {
                return 0;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }
}
