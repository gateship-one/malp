/*
 *  Copyright (C) 2018 Team Gateship-One
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

package org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects;


import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * This class represents an MPDTrack. This is the same type for tracks and files.
 * This is used for tracks in playlist, album, search results,... and for music files when
 * retrieving an directory listing from the mpd server.
 */
public class MPDTrack extends MPDFileEntry implements MPDGenericItem, Parcelable {


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
     * ArtistSort field, can be optionally be used as sort order
     */
    private String pTrackArtistSort;

    /**
     * AlbumArtistSort field, can be optionally be used as sort order
     */
    private String pTrackAlbumArtistSort;

    /**
     * Track "Name" unspecified tag, could be shown if trackTitle is not set
     */
    private String pTrackName;

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
     * Available for tracks in the current playlist
     */
    private int pSongPosition;

    /**
     * Available for tracks in the current playlist
     */
    private int pSongID;

    /**
     * Used for {@link org.gateshipone.malp.application.adapters.CurrentPlaylistAdapter} to save if an
     * image is already being fetchted from the internet for this item
     */
    private boolean pImageFetching;

    /**
     * Create empty MPDTrack (track). Fill it with setter methods during
     * parsing of mpds output.
     *
     * @param path The path of the file. This should never change.
     */
    public MPDTrack(String path) {
        super(path);
        pTrackTitle = "";

        pTrackArtist = "";
        pTrackAlbum = "";
        pTrackAlbumArtist = "";
        pTrackName = "";

        pDate = "";

        pTrackArtistMBID = "";
        pTrackMBID = "";
        pTrackAlbumMBID = "";
        pTrackAlbumArtistMBID = "";

        pLength = 0;

        pImageFetching = false;
    }


    protected MPDTrack(Parcel in) {
        pTrackTitle = in.readString();
        pTrackArtist = in.readString();
        pTrackAlbum = in.readString();
        pTrackAlbumArtist = in.readString();
        pTrackArtistSort = in.readString();
        pTrackAlbumArtistSort = in.readString();
        pTrackName = in.readString();
        pDate = in.readString();
        pTrackArtistMBID = in.readString();
        pTrackMBID = in.readString();
        pTrackAlbumMBID = in.readString();
        pTrackAlbumArtistMBID = in.readString();
        pLength = in.readInt();
        pTrackNumber = in.readInt();
        pAlbumTrackCount = in.readInt();
        pDiscNumber = in.readInt();
        pAlbumDiscCount = in.readInt();
        pSongPosition = in.readInt();
        pSongID = in.readInt();
        pImageFetching = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(pTrackTitle);
        dest.writeString(pTrackArtist);
        dest.writeString(pTrackAlbum);
        dest.writeString(pTrackAlbumArtist);
        dest.writeString(pTrackArtistSort);
        dest.writeString(pTrackAlbumArtistSort);
        dest.writeString(pTrackName);
        dest.writeString(pDate);
        dest.writeString(pTrackArtistMBID);
        dest.writeString(pTrackMBID);
        dest.writeString(pTrackAlbumMBID);
        dest.writeString(pTrackAlbumArtistMBID);
        dest.writeInt(pLength);
        dest.writeInt(pTrackNumber);
        dest.writeInt(pAlbumTrackCount);
        dest.writeInt(pDiscNumber);
        dest.writeInt(pAlbumDiscCount);
        dest.writeInt(pSongPosition);
        dest.writeInt(pSongID);
        dest.writeByte((byte) (pImageFetching ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MPDTrack> CREATOR = new Creator<MPDTrack>() {
        @Override
        public MPDTrack createFromParcel(Parcel in) {
            return new MPDTrack(in);
        }

        @Override
        public MPDTrack[] newArray(int size) {
            return new MPDTrack[size];
        }
    };

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

    public void setTrackArtistSort(String artistSort) {
        pTrackArtistSort = artistSort;
    }

    public String getTrackArtistSort() {
        return pTrackArtistSort;
    }

    public void setTrackAlbumArtistSort(String albumArtistSort) {
        pTrackAlbumArtistSort = albumArtistSort;
    }

    public String getTrackAlbumArtistSort() {
        return pTrackAlbumArtistSort;
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

    public String getTrackName() {
        return pTrackName;
    }

    public void setTrackName(String name) {
        pTrackName = name;
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

    public int getSongPosition() {
        return pSongPosition;
    }

    public void setSongPosition(int position) {
        pSongPosition = position;
    }

    public int getSongID() {
        return pSongID;
    }

    public void setSongID(int id) {
        pSongID = id;
    }

    public boolean getFetching() {
        return pImageFetching;
    }

    public void setFetching(boolean fetching) {
        pImageFetching = fetching;
    }

    /**
     * Returns either the track title, name or filename depending on which is set.
     */
    public String getVisibleTitle() {
        if (!pTrackTitle.isEmpty()) {
            return pTrackTitle;
        } else if (!pTrackName.isEmpty()) {
            return pTrackName;
        } else {
            return getFilename();
        }
    }

    /**
     * @return String that is used for section based scrolling
     */
    @Override
    public String getSectionTitle() {
        return pTrackTitle.equals("") ? mPath : pTrackTitle;
    }



    public int indexCompare(MPDTrack compFile) {
        if (!pTrackAlbumMBID.equals(compFile.pTrackAlbumMBID)) {
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

    /**
     * Compares the file names of two tracks with each other. The prefix path is discarded before
     * comparing.
     * @param another {@link MPDTrack} to compare
     * @return see super class
     */
    public int compareTo(@NonNull MPDTrack another) {
        String title = getFilename();
        String anotherTitle = another.getFilename();

        return title.toLowerCase().compareTo(anotherTitle.toLowerCase());
    }
}
