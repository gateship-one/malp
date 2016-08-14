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

package andrompd.org.andrompd.application.adapters;


import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.listviewitems.CurrentPlaylistTrackItem;
import andrompd.org.andrompd.mpdservice.handlers.MPDConnectionStateChangeHandler;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;
import andrompd.org.andrompd.mpdservice.handlers.MPDStatusChangeHandler;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseFileList;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDFile;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;

public class CurrentPlaylistAdapter extends BaseAdapter {
    private enum LIST_STATE {
        LIST_EMPTY,
        LIST_LOADING,
        LIST_READY
    }

    private static final int CLEANUP_TIMEOUT = 30 * 1000;

    private static final String TAG = "CurrentPLAdapter";

    private static final int WINDOW_SIZE = 500;
    private Context mContext;

    private List<MPDFileEntry> mPlaylist = null;

    private List<MPDFileEntry>[] mWindowedPlaylists;
    private LIST_STATE[] mWindowedListStates;
    private int mLastAccessedList;
    private Semaphore mListsLock;
    private Timer mClearTimer;

    private MPDCurrentStatus mLastStatus = null;

    private PlaylistFetchResponseHandler mTrackResponseHandler;
    private PlaylistStateListener mStateListener;
    private MPDConnectionStateChangeHandler mConnectionListener;

    private ListView mListView;

    private boolean mWindowEnabled = true;


    public CurrentPlaylistAdapter(Context context, ListView listView) {
        super();
        mContext = context;

        mTrackResponseHandler = new PlaylistFetchResponseHandler();
        mStateListener = new PlaylistStateListener();
        mConnectionListener = new ConnectionStateChangeListener();

        if (null != listView) {
            listView.setAdapter(this);
            mListView = listView;
            mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
        mListsLock = new Semaphore(1);
        mClearTimer = null;
    }

    @Override
    public int getCount() {
        if (null != mLastStatus) {
            return mLastStatus.getPlaylistLength();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        return getTrack(position);

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Create the actual listview items if no reusable object is provided.
     *
     * @param position    Index of the item to create.
     * @param convertView If != null this view can be reused to optimize performance.
     * @param parent      Parent of the view
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get MPDFile at the given index used for this item.
        MPDFile track = getTrack(position);
        if (track != null) {

            // Get track title
            String trackTitle = track.getTrackTitle();

            // additional information (artist + album)
            String trackInformation = track.getTrackArtist() + mContext.getString(R.string.track_item_separator) + track.getTrackAlbum();

            // Get the number of the track
            String trackNumber = String.valueOf(position + 1);

            // Get the preformatted duration of the track.
            String trackDuration = track.getLengthString();

            // Check if reusable object is available
            if (convertView != null) {
                CurrentPlaylistTrackItem tracksListViewItem = (CurrentPlaylistTrackItem) convertView;
                tracksListViewItem.setTrackNumber(trackNumber);
                tracksListViewItem.setTitle(trackTitle);
                tracksListViewItem.setAdditionalInformation(trackInformation);
                tracksListViewItem.setDuration(trackDuration);
            } else {
                // If not create a new Listitem
                convertView = new CurrentPlaylistTrackItem(mContext, trackNumber, trackTitle, trackInformation, trackDuration);
            }

            if (null != mLastStatus && mLastStatus.getCurrentSongIndex() == position) {
                ((CurrentPlaylistTrackItem) convertView).setPlaying(true);
            } else {
                ((CurrentPlaylistTrackItem) convertView).setPlaying(false);
            }
        } else {
            convertView = new CurrentPlaylistTrackItem(mContext, "", "", "", "");
        }
        return convertView;
    }

    private void setCurrentIndex(int index) {
        if ((index >= 0) && (index < getCount())) {
            notifyDataSetChanged();
            mListView.setSelection(index);
        }
    }


    private class PlaylistStateListener extends MPDStatusChangeHandler {
        protected void onNewStatusReady(MPDCurrentStatus status) {
            boolean newPl = false;
            if ((null == mLastStatus) || (mLastStatus.getPlaylistVersion() != status.getPlaylistVersion())) {
                newPl = true;
            }

            if (null == mLastStatus) {
                Log.v(TAG, "First status");
                // The current song index has changed. Set the old one to false and the new one to true.
                int index = status.getCurrentSongIndex();

                if (index < getCount()) {
                    if (status.getPlaybackState() != MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_STOPPED) {
                        setCurrentIndex(index);
                    }
                }
            } else {
                if (mLastStatus.getCurrentSongIndex() != status.getCurrentSongIndex()) {
                    Log.v(TAG, "New song index");
                    // The current song index has changed. Set the old one to false and the new one to true.
                    int index = status.getCurrentSongIndex();

                    setCurrentIndex(index);
                }
            }


            mLastStatus = status;
            if (newPl) {
                updatePlaylist();
            }
        }

        protected void onNewTrackReady(MPDFile track) {

        }
    }

    private class PlaylistFetchResponseHandler extends MPDResponseFileList {

        @Override
        public void handleTracks(List<MPDFileEntry> trackList, int start, int end) {
            Log.v(TAG, "Received new playlist with size: " + trackList.size() + " start: " + start + " end: " + end);
            if (!mWindowEnabled) {
                // Save the new playlist
                mPlaylist = trackList;

                // Set the index active for the currently playing/paused song (if any)
                if (null != mLastStatus) {
                    int index = mLastStatus.getCurrentSongIndex();
                    if ((null != mPlaylist) && (index < mPlaylist.size())) {
                        setCurrentIndex(index);
                    }
                }
                // Notify the listener for this adapter
                notifyDataSetChanged();
            } else {
                mWindowedPlaylists[start / WINDOW_SIZE] = trackList;
                mWindowedListStates[start / WINDOW_SIZE] = LIST_STATE.LIST_READY;
                notifyDataSetChanged();
            }


        }
    }

    private class ConnectionStateChangeListener extends MPDConnectionStateChangeHandler {

        @Override
        public void onConnected() {
            Log.v(TAG, "Server connected, fetch PL");
            updatePlaylist();
        }

        @Override
        public void onDisconnected() {
            mPlaylist = null;
            mLastStatus = null;
            notifyDataSetChanged();
        }
    }

    public void onResume() {
        // Register to the MPDStateNotifyHandler singleton
        MPDStateMonitoringHandler.registerStatusListener(mStateListener);
        MPDStateMonitoringHandler.registerConnectionStateListener(mConnectionListener);


        mLastStatus = null;
        updatePlaylist();
        mStateListener.onNewStatusReady(MPDStateMonitoringHandler.getLastStatus());
    }

    public void onPause() {
        // Unregister to the MPDStateNotifyHandler singleton
        MPDStateMonitoringHandler.unregisterStatusListener(mStateListener);
        MPDStateMonitoringHandler.unregisterConnectionStateListener(mConnectionListener);

        mPlaylist = null;
    }

    private void updatePlaylist() {
        if (!mWindowEnabled) {
            // The playlist has changed and we need to fetch a new one.
            MPDQueryHandler.getCurrentPlaylist(mTrackResponseHandler);
        } else {
            if (null != mLastStatus) {
                Log.v(TAG, "PL Update with length: " + mLastStatus.getPlaylistLength());
                try {
                    mListsLock.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int listCount = (mLastStatus.getPlaylistLength() / WINDOW_SIZE) + 1;
                mWindowedPlaylists = (List<MPDFileEntry>[]) new List[listCount];
                mWindowedListStates = new LIST_STATE[listCount];
                mLastAccessedList = 0;
                for (int i = 0; i < listCount; i++) {
                    mWindowedPlaylists[i] = null;
                    mWindowedListStates[i] = LIST_STATE.LIST_EMPTY;

                }

                mListsLock.release();
            }

        }
        notifyDataSetChanged();
    }

    private void fetchWindow(int index) {
        int tableIndex = index / WINDOW_SIZE;

        int start = tableIndex * WINDOW_SIZE;
        int end = start + WINDOW_SIZE;
        if (end > mLastStatus.getPlaylistLength()) {
            end = mLastStatus.getPlaylistLength();
        }
        Log.v(TAG, "PL Window requested: " + start + ':' + end);
        MPDQueryHandler.getCurrentPlaylist(mTrackResponseHandler, start, end);
    }

    private MPDFile getTrack(int position) {
        if (!mWindowEnabled) {
            return (MPDFile) mPlaylist.get(position);
        } else {
            int listIndex = position / WINDOW_SIZE;
            if (mWindowedListStates[position / WINDOW_SIZE] == LIST_STATE.LIST_READY) {
                mLastAccessedList = listIndex;
                if (null != mClearTimer) {
                    mClearTimer.cancel();
                }
                mClearTimer = new Timer();
                mClearTimer.schedule(new ListCleanUp(), CLEANUP_TIMEOUT);
                return (MPDFile) mWindowedPlaylists[listIndex].get(position % WINDOW_SIZE);
            } else if (mWindowedListStates[position / WINDOW_SIZE] == LIST_STATE.LIST_EMPTY) {
                mWindowedListStates[position / WINDOW_SIZE] = LIST_STATE.LIST_LOADING;
                fetchWindow(position);
            }
        }
        return null;
    }

    private class ListCleanUp extends TimerTask {

        @Override
        public void run() {
            try {
                mListsLock.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < mWindowedPlaylists.length; i++) {
                if (i != mLastAccessedList) {
                    mWindowedPlaylists[i] = null;
                    mWindowedListStates[i] = LIST_STATE.LIST_EMPTY;
                }
            }
            mClearTimer = null;
            mListsLock.release();
        }
    }
}
