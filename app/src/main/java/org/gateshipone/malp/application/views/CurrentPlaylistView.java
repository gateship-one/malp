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

package org.gateshipone.malp.application.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.CurrentPlaylistAdapter;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;

public class CurrentPlaylistView extends LinearLayout implements AdapterView.OnItemClickListener {
    /**
     * Parameters for bundled extra arguments for this fragment. Necessary to define which playlist to
     * retrieve from the MPD server.
     */
    public static final String BUNDLE_STRING_EXTRA_PLAYLISTNAME = "playlistname";

    private String mPlaylistPath;

    /**
     * Main ListView of this fragment
     */
    private ListView mListView;

    Context mContext;


    /**
     * Adapter used by the ListView
     */
    private CurrentPlaylistAdapter mPlaylistAdapter;

    public CurrentPlaylistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Inflate the layout for this fragment
        LayoutInflater.from(context).inflate(R.layout.listview_layout, this, true);

        // Get the main ListView of this fragment
        mListView = (ListView) this.findViewById(R.id.main_listview);


        // Create the needed adapter for the ListView
        mPlaylistAdapter = new CurrentPlaylistAdapter(getContext(),mListView);

        mListView.setOnItemClickListener(this);

        // Return the ready inflated and configured fragment view.
        mContext = context;
    }

    /**
     * Play the selected track.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MPDCommandHandler.playSongIndex(position);
    }

    public void onResume() {
        mPlaylistAdapter.onResume();
    }

    public void onPause() {
        mPlaylistAdapter.onPause();
    }

    public MPDFileEntry getItem(int position) {
        if (null != mPlaylistAdapter ) {
            return (MPDFileEntry)mPlaylistAdapter.getItem(position);
        }
        return null;
    }

    public void jumpToCurrentSong() {
        mPlaylistAdapter.jumpToCurrent();
    }

}
