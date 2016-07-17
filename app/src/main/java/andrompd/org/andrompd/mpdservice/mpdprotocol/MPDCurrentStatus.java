package andrompd.org.andrompd.mpdservice.mpdprotocol;


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
    private int pBitDepth;

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
        pBitDepth = in.readInt();
        pChannelCount = in.readInt();
        pBitrate = in.readInt();
        pElapsedTime = in.readInt();
        pTrackLength = in.readInt();
        pUpdateDBJob = in.readInt();
        pPlaybackState = MPD_PLAYBACK_STATE.values()[in.readInt()];
    }

    public MPDCurrentStatus() {

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

    public int getBitDepth() {
        return pBitDepth;
    }

    public void setBitDepth(int pBitDepth) {
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
        dest.writeInt(pBitDepth);
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
}
