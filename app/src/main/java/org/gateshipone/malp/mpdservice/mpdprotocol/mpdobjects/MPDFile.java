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

public class MPDFile extends MPDFileEntry implements MPDGenericItem, Parcelable {
    private String pTrackTitle;


    private String pTrackArtist;
    private String pTrackAlbum;
    private String pTrackAlbumArtist;

    private String pDate;

    private String pTrackArtistMBID;
    private String pTrackMBID;
    private String pTrackAlbumMBID;
    private String pTrackAlbumArtistMBID;

    private int pLength;
    private int pTrackNumber;
    private int pAlbumTrackCount;
    private int pDiscNumber;
    private int pAlbumDiscCount;


    /**
     * Create empty MPDFile (track). Fill it with setter methods during
     * parsing of mpds output.
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

    protected MPDFile(Parcel in) {
        super(in.readString());
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

    public String getLengthString() {
        String returnString = "";
        int hours = 0, minutes = 0, seconds = 0;
        hours = pLength / 3600;
        if (hours > 0) {
            minutes = (pLength - (3600 * hours)) / 60;
        } else {
            minutes = pLength / 60;
        }

        seconds = pLength - (hours * 3600) - (minutes * 60);

        if (hours == 0) {
            returnString = (minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
        } else {
            returnString = (hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
        }
        return returnString;
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

    @Override
    public String getSectionTitle() {
        return pTrackTitle.equals("") ? mPath : pTrackTitle;
    }



    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MPDFile> CREATOR = new Creator<MPDFile>() {
        @Override
        public MPDFile createFromParcel(Parcel in) {
            return new MPDFile(in);
        }

        @Override
        public MPDFile[] newArray(int size) {
            return new MPDFile[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Serialize MPDFile
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
}
