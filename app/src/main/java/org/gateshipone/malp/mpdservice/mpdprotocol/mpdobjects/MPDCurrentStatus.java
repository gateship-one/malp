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

package org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects;


import android.os.Parcel;
import android.os.Parcelable;

public class MPDCurrentStatus implements Parcelable{

    public enum MPD_PLAYBACK_STATE {
        MPD_PLAYING,
        MPD_PAUSING,
        MPD_STOPPED
    };

   
    /**
     * Volume: 0 - 100;
     */
    private int pVolume;

    /**
     * Repeat: 0,1
     */
    private int pRepeat;

    /**
     * Random playback: 0,1
     */
    private int pRandom;

    /**
     * Single playback: 0,1
     */
    private int pSinglePlayback;

    /**
     * Consume after playback: 0,1
     */
    private int pConsume;

    /**
     * Version of the playlist. If changed the user needs a new update.
     */
    private int pPlaylistVersion;

    /**
     * Number of songs in the current playlist
     */
    private int pPlaylistLength;

    /**
     * Index of the currently playing song
     */
    private int pCurrentSongIndex;

    /**
     * Index of the next song to play (could be index+1 or random)
     */
    private int pNextSongIndex;

    /**
     * Samplerate of the audio file (extracted out of: "audio"-field)
     */
    private int pSamplerate;

    /**
     * Sample resolution in bits. (also audio-field)
     */
    private String pBitDepth;

    /**
     * Channel count of audiofile (also audio-field)
     */
    private int pChannelCount;

    /**
     * Bitrate of the codec used
     */
    private int pBitrate;

    /**
     * Position of the player in current song
     */
    private int pElapsedTime;

    /**
     * Length of the currently playing song.
     */
    private int pTrackLength;

    /**
     * If an updateing job of the database is running, the id gets saved here.
     * Also the update commands sends back the id of the corresponding update job.
     */
    private int pUpdateDBJob;

    /**
     * State of the MPD server (playing, pause, stop)
     */
    private MPD_PLAYBACK_STATE pPlaybackState;

    protected MPDCurrentStatus(Parcel in) {
        /* Create this object from parcel */
        pVolume = in.readInt();
        pRepeat = in.readInt();
        pRandom = in.readInt();
        pSinglePlayback = in.readInt();
        pConsume = in.readInt();
        pPlaylistVersion = in.readInt();
        pPlaylistLength = in.readInt();
        pCurrentSongIndex = in.readInt();
        pNextSongIndex = in.readInt();
        pSamplerate = in.readInt();
        pBitDepth = in.readString();
        pChannelCount = in.readInt();
        pBitrate = in.readInt();
        pElapsedTime = in.readInt();
        pTrackLength = in.readInt();
        pUpdateDBJob = in.readInt();
        pPlaybackState = MPD_PLAYBACK_STATE.values()[in.readInt()];
    }

    public MPDCurrentStatus() {
        pVolume = 0;
        pRepeat = 0;
        pRandom = 0;
        pSinglePlayback = 0;
        pConsume = 0;
        pPlaylistVersion = 0;
        pPlaylistLength = 0;
        pCurrentSongIndex = -1;
        pNextSongIndex = 0;
        pSamplerate = 0;
        pBitDepth = "0";
        pChannelCount = 0;
        pBitrate = 0;
        pElapsedTime = 0;
        pTrackLength = 0;
        pUpdateDBJob = 0;
        pPlaybackState = MPD_PLAYBACK_STATE.MPD_STOPPED;
    }

    /**
     * Copy constructor.
     * @param status Object to copy values from
     */
    public MPDCurrentStatus(MPDCurrentStatus status ) {
        pVolume = status.pVolume;
        pRepeat = status.pRepeat;
        pRandom = status.pRandom;
        pSinglePlayback = status.pSinglePlayback;
        pConsume = status.pConsume;
        pPlaylistVersion = status.pPlaylistVersion;
        pPlaylistLength = status.pPlaylistLength;
        pCurrentSongIndex = status.pCurrentSongIndex;
        pNextSongIndex = status.pNextSongIndex;
        pSamplerate = status.pSamplerate;
        pBitDepth = status.pBitDepth;
        pChannelCount = status.pChannelCount;
        pBitrate = status.pBitrate;
        pElapsedTime = status.pElapsedTime;
        pTrackLength = status.pTrackLength;
        pUpdateDBJob = status.pUpdateDBJob;
        pPlaybackState = status.pPlaybackState;
    }


    public int getVolume() {
        return pVolume;
    }

    public void setVolume(int pVolume) {
        if ( pVolume >= 0 && pVolume <= 100 ) {
            this.pVolume = pVolume;
        } else {
            this.pVolume = 0;
        }
    }

    public int getRepeat() {
        return pRepeat;
    }

    public void setRepeat(int pRepeat) {
        this.pRepeat = pRepeat;
    }

    public int getRandom() {
        return pRandom;
    }

    public void setRandom(int pRandom) {
        this.pRandom = pRandom;
    }

    public int getSinglePlayback() {
        return pSinglePlayback;
    }

    public void setSinglePlayback(int pSinglePlayback) {
        this.pSinglePlayback = pSinglePlayback;
    }

    public int getConsume() {
        return pConsume;
    }

    public void setConsume(int pConsume) {
        this.pConsume = pConsume;
    }

    public int getPlaylistVersion() {
        return pPlaylistVersion;
    }

    public void setPlaylistVersion(int pPlaylistVersion) {
        this.pPlaylistVersion = pPlaylistVersion;
    }

    public int getPlaylistLength() {
        return pPlaylistLength;
    }

    public void setPlaylistLength(int pPlaylistLength) {
        this.pPlaylistLength = pPlaylistLength;
    }

    public int getCurrentSongIndex() {
        return pCurrentSongIndex;
    }

    public void setCurrentSongIndex(int pCurrentSongIndex) {
        this.pCurrentSongIndex = pCurrentSongIndex;
    }

    public int getNextSongIndex() {
        return pNextSongIndex;
    }

    public void setNextSongIndex(int pNextSongIndex) {
        this.pNextSongIndex = pNextSongIndex;
    }

    public int getSamplerate() {
        return pSamplerate;
    }

    public void setSamplerate(int pSamplerate) {
        this.pSamplerate = pSamplerate;
    }

    public String getBitDepth() {
        return pBitDepth;
    }

    public void setBitDepth(String pBitDepth) {
        this.pBitDepth = pBitDepth;
    }

    public int getChannelCount() {
        return pChannelCount;
    }

    public void setChannelCount(int pChannelCount) {
        this.pChannelCount = pChannelCount;
    }

    public int getBitrate() {
        return pBitrate;
    }

    public void setBitrate(int pBitrate) {
        this.pBitrate = pBitrate;
    }

    public int getElapsedTime() {
        return pElapsedTime;
    }

    public void setElapsedTime(int pElapsedTime) {
        this.pElapsedTime = pElapsedTime;
    }

    public int getTrackLength() {
        return pTrackLength;
    }

    public void setTrackLength(int pTrackLength) {
        this.pTrackLength = pTrackLength;
    }

    public int getUpdateDBJob() {
        return pUpdateDBJob;
    }

    public void setUpdateDBJob(int pUpdateDBJob) {
        this.pUpdateDBJob = pUpdateDBJob;
    }

    public MPD_PLAYBACK_STATE getPlaybackState() {
        return pPlaybackState;
    }

    public void setPlaybackState(MPD_PLAYBACK_STATE pPlaybackState) {
        this.pPlaybackState = pPlaybackState;
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        /* Serialize the class attributes here */
        dest.writeInt(pVolume);
        dest.writeInt(pRepeat);
        dest.writeInt(pRandom);
        dest.writeInt(pSinglePlayback);
        dest.writeInt(pConsume);
        dest.writeInt(pPlaylistVersion);
        dest.writeInt(pPlaylistLength);
        dest.writeInt(pCurrentSongIndex);
        dest.writeInt(pNextSongIndex);
        dest.writeInt(pSamplerate);
        dest.writeString(pBitDepth);
        dest.writeInt(pChannelCount);
        dest.writeInt(pBitrate);
        dest.writeInt(pElapsedTime);
        dest.writeInt(pTrackLength);
        dest.writeInt(pUpdateDBJob);
        /* Convert enum-type to int here and back when deserializing */
        dest.writeInt(pPlaybackState.ordinal());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MPDCurrentStatus> CREATOR = new Creator<MPDCurrentStatus>() {
        @Override
        public MPDCurrentStatus createFromParcel(Parcel in) {
            return new MPDCurrentStatus(in);
        }

        @Override
        public MPDCurrentStatus[] newArray(int size) {
            return new MPDCurrentStatus[size];
        }
    };



    public String printStatus() {
        /* String output for debug purposes */
        String retString = "";

        retString += "Volume: " + String.valueOf(pVolume) + "\n";
        retString += "Repeat: " + String.valueOf(pRepeat) + "\n";
        retString += "Random: " + String.valueOf(pRandom) + "\n";
        retString += "Single: " + String.valueOf(pSinglePlayback) + "\n";
        retString += "Consume: " + String.valueOf(pConsume) + "\n";
        retString += "Playlist version: " + String.valueOf(pPlaylistVersion) + "\n";
        retString += "Playlist length: " + String.valueOf(pPlaylistLength) + "\n";
        retString += "Current song index: " + String.valueOf(pCurrentSongIndex) + "\n";
        retString += "Next song index: " + String.valueOf(pNextSongIndex) + "\n";
        retString += "Samplerate: " + String.valueOf(pSamplerate) + "\n";
        retString += "Bitdepth: " + pBitDepth + "\n";
        retString += "Channel count: " + String.valueOf(pChannelCount) + "\n";
        retString += "Bitrate: " + String.valueOf(pBitrate) + "\n";
        retString += "Elapsed time: " + String.valueOf(pElapsedTime) + "\n";
        retString += "Track length: " + String.valueOf(pTrackLength) + "\n";
        retString += "UpdateDB job id: " + String.valueOf(pUpdateDBJob) + "\n";
        retString += "Playback state: " + String.valueOf(pPlaybackState.ordinal()) + "\n";

        return retString;
    }
}
