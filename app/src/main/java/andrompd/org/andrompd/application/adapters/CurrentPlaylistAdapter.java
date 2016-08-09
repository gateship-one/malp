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

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.listviewitems.CurrentPlaylistTrackItem;
import andrompd.org.andrompd.application.listviewitems.TrackListViewItem;
import andrompd.org.andrompd.mpdservice.handlers.MPDConnectionStateChangeHandler;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;
import andrompd.org.andrompd.mpdservice.handlers.MPDStatusChangeHandler;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseTrackList;
import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDCurrentStatus;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDFile;

public class CurrentPlaylistAdapter extends BaseAdapter {
    private static final String TAG = "CurrentPLAdapter";
    private Context mContext;

    private List<MPDFile> mPlaylist = null;


    private MPDCurrentStatus mLastStatus = null;

    private PlaylistFetchResponseHandler mTrackResponseHandler;
    private PlaylistStateListener mStateListener;
    private MPDConnectionStateChangeHandler mConnectionListener;

    private ListView mListView;

    public CurrentPlaylistAdapter(Context context, ListView listView) {
        super();
        mContext = context;

        mTrackResponseHandler = new PlaylistFetchResponseHandler();
        mStateListener = new PlaylistStateListener();
        mConnectionListener = new ConnectionStateChangeListener();

        if ( null != listView) {
            listView.setAdapter(this);
            mListView = listView;
            mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
    }

    @Override
    public int getCount() {
        if (null == mPlaylist) {
            return 0;
        } else {
            return mPlaylist.size();
        }
    }

    @Override
    public Object getItem(int position) {
        if (null == mPlaylist) {
            return 0;
        } else {
            return mPlaylist.get(position);
        }

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
        MPDFile track = mPlaylist.get(position);

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

        if (track.getPlaying()) {
            ((CurrentPlaylistTrackItem) convertView).setPlaying(true);
        } else {
            ((CurrentPlaylistTrackItem) convertView).setPlaying(false);
        }

        return convertView;
    }

    private void setPlaying(int index, boolean playing) {
        if ((index >= 0) && (null != mPlaylist) && (index < mPlaylist.size())) {
            mPlaylist.get(index).setPlaying(playing);

            notifyDataSetChanged();
            if ( playing ) {
                Log.v(TAG,"Move listview to: " + index);
                mListView.setSelection(index);
            }
        }
    }


    private class PlaylistStateListener extends MPDStatusChangeHandler {
        protected void onNewStatusReady(MPDCurrentStatus status) {
            Log.v(TAG, "Received new status: " + status + " last status: " + mLastStatus);
            if ((null == mLastStatus) || (mLastStatus.getPlaylistVersion() != status.getPlaylistVersion())) {
                // The playlist has changed and we need to fetch a new one.
                MPDQueryHandler.getCurrentPlaylist(mTrackResponseHandler);
            }

            if (null == mLastStatus) {
                // The current song index has changed. Set the old one to false and the new one to true.
                int index = status.getCurrentSongIndex();

                if ((null != mPlaylist) && (index < mPlaylist.size())) {
                    if (status.getPlaybackState() != MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_STOPPED) {
                        setPlaying(index, true);
                    }
                }
            } else {
                if (mLastStatus.getCurrentSongIndex() != status.getCurrentSongIndex()) {
                    // The current song index has changed. Set the old one to false and the new one to true.
                    int index = status.getCurrentSongIndex();
                    int oldIndex = mLastStatus.getCurrentSongIndex();

                    if ((null != mPlaylist) && (index < mPlaylist.size()) && (oldIndex < mPlaylist.size())) {
                        // Set the old track playing to false
                        setPlaying(oldIndex, false);
                        if (status.getPlaybackState() != MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_STOPPED) {
                            setPlaying(index, true);
                        }
                    }
                }

                MPDCurrentStatus.MPD_PLAYBACK_STATE oldState = mLastStatus.getPlaybackState();
                MPDCurrentStatus.MPD_PLAYBACK_STATE newState = status.getPlaybackState();
                if ((oldState != newState) && (newState == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_STOPPED)) {
                    setPlaying(status.getCurrentSongIndex(), false);
                } else if ( (oldState != newState) && (newState == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PLAYING) ){
                    setPlaying(status.getCurrentSongIndex(), true);
                }
            }

            mLastStatus = status;
        }

        protected void onNewTrackReady(MPDFile track) {

        }
    }

    private class PlaylistFetchResponseHandler extends MPDResponseTrackList {

        @Override
        public void handleTracks(List<MPDFile> trackList) {
            Log.v(TAG, "Received new playlist with size: " + trackList.size());
            // Save the new playlist
            mPlaylist = trackList;

            // Set the index active for the currently playing/paused song (if any)
            if (null != mLastStatus) {
                int index = mLastStatus.getCurrentSongIndex();
                if ((null != mPlaylist) && (index < mPlaylist.size())) {
                    if (mLastStatus.getPlaybackState() != MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_STOPPED) {
                        setPlaying(index, true);
                    }
                }
            }

            // Notify the listener for this adapter
            notifyDataSetInvalidated();
        }
    }

    private class ConnectionStateChangeListener extends MPDConnectionStateChangeHandler {

        @Override
        public void onConnected() {
            Log.v(TAG, "Server connected, fetch PL");
            MPDQueryHandler.getCurrentPlaylist(mTrackResponseHandler);
        }

        @Override
        public void onDisconnected() {
            mPlaylist = null;
            mLastStatus = new MPDCurrentStatus();
            notifyDataSetInvalidated();
        }
    }

    public void onResume() {
        // Register to the MPDStateNotifyHandler singleton
        MPDStateMonitoringHandler.registerStatusListener(mStateListener);
        MPDStateMonitoringHandler.registerConnectionStateListener(mConnectionListener);


        mStateListener.onNewStatusReady(MPDStateMonitoringHandler.getLastStatus());
        MPDQueryHandler.getCurrentPlaylist(mTrackResponseHandler);
    }

    public void onPause() {
        // Unregister to the MPDStateNotifyHandler singleton
        MPDStateMonitoringHandler.unregisterStatusListener(mStateListener);
        MPDStateMonitoringHandler.unregisterConnectionStateListener(mConnectionListener);

        mPlaylist = null;
        mLastStatus = new MPDCurrentStatus();

    }

}
