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

package andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects;


public class MPDStatistics {
    private int mArtistsCount;
    private int mAlbumCount;
    private int mSongCount;

    private int mServerUptime;
    private int mAllSongDuration;

    private long mLastDBUpdate;
    private int mPlayDuration;

    public MPDStatistics () {
        mArtistsCount = 0;
        mAlbumCount = 0;
        mSongCount = 0;

        mServerUptime = 0;
        mAllSongDuration = 0;

        mLastDBUpdate = System.currentTimeMillis();
        mPlayDuration = 0;
    }

    public int getArtistsCount() {
        return mArtistsCount;
    }

    public void setArtistsCount(int mArtistsCount) {
        this.mArtistsCount = mArtistsCount;
    }

    public int getAlbumCount() {
        return mAlbumCount;
    }

    public void setAlbumCount(int mAlbumCount) {
        this.mAlbumCount = mAlbumCount;
    }

    public int getSongCount() {
        return mSongCount;
    }

    public void setSongCount(int mSongCount) {
        this.mSongCount = mSongCount;
    }

    public int getServerUptime() {
        return mServerUptime;
    }

    public void setServerUptime(int mServerUptime) {
        this.mServerUptime = mServerUptime;
    }

    public int getAllSongDuration() {
        return mAllSongDuration;
    }

    public void setAllSongDuration(int mAllSongDuration) {
        this.mAllSongDuration = mAllSongDuration;
    }

    public long getLastDBUpdate() {
        return mLastDBUpdate;
    }

    public void setLastDBUpdate(long mLastDBUpdate) {
        this.mLastDBUpdate = mLastDBUpdate;
    }

    public int getPlayDuration() {
        return mPlayDuration;
    }

    public void setPlayDuration(int mPlayDuration) {
        this.mPlayDuration = mPlayDuration;
    }


}
