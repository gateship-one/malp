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


public class MPDFile extends MPDFileEntry implements MPDGenericItem {
    private String pTrackTitle;


    private String pTrackArtist;
    private String pTrackAlbum;
    private String pTrackAlbumArtist;

    private String pDate;

    private String pTrackArtistMBID;
    private String pTrackMBID;
    private String pTrackAlbumMBID;

    private int pLength;
    private int pTrackNumber;
    private int pAlbumTrackCount;
    private int pDiscNumber;
    private int pAlbumDiscCount;

    private boolean mPlaying;

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

        pLength = 0;
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

    public void setPlaying(boolean playing) {
        mPlaying = playing;
    }

    public boolean getPlaying() {
        return mPlaying;
    }
}
