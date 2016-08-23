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

package org.gateshipone.malp.application.fragments.serverfragments;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.callbacks.FABFragmentCallback;
import org.gateshipone.malp.application.utils.FormatHelper;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseServerStatistics;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDStatistics;

public class ServerStatisticFragment extends Fragment {
    public final static String TAG = ServerStatisticFragment.class.getSimpleName();
    private FABFragmentCallback mFABCallback = null;

    private TextView mArtistCount;
    private TextView mAlbumsCount;
    private TextView mSongsCount;

    private TextView mUptime;
    private TextView mPlaytime;
    private TextView mLastUpdate;
    private TextView mDBLength;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_server_statistic, container, false);

        mArtistCount = (TextView)rootView.findViewById(R.id.server_statistic_artist_count);
        mAlbumsCount = (TextView)rootView.findViewById(R.id.server_statistic_albums_count);
        mSongsCount = (TextView)rootView.findViewById(R.id.server_statistic_songs_count);

        mUptime = (TextView)rootView.findViewById(R.id.server_statistic_server_uptime);
        mPlaytime = (TextView)rootView.findViewById(R.id.server_statistic_server_playtime);
        mLastUpdate = (TextView)rootView.findViewById(R.id.server_statistic_db_update);
        mDBLength = (TextView)rootView.findViewById(R.id.server_statistic_db_playtime);

        ((Button)rootView.findViewById(R.id.server_statistic_update_db_btn)).setOnClickListener(new DBUpdateBtnListener());

        // Return the ready inflated and configured fragment view.
        return rootView;
    }

    /**
     * Attaches callbacks
     */
    @Override
    public void onResume() {
        super.onResume();

        if (null != mFABCallback) {
            mFABCallback.setupFAB(false, null);
            mFABCallback.setupToolbar(getString(R.string.menu_statistic), false, true, false);
        }
        MPDQueryHandler.getStatistics(new StatisticResponseHandler());
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mFABCallback = (FABFragmentCallback) context;
        } catch (ClassCastException e) {
            mFABCallback = null;
        }
    }

    private class StatisticResponseHandler extends MPDResponseServerStatistics {

        @Override
        public void handleStatistic(MPDStatistics statistics) {
            mArtistCount.setText(String.valueOf(statistics.getArtistsCount()));
            mAlbumsCount.setText(String.valueOf(statistics.getAlbumCount()));
            mSongsCount.setText(String.valueOf(statistics.getSongCount()));

            mUptime.setText(FormatHelper.formatTracktimeFromS(statistics.getServerUptime()));
            mPlaytime.setText(FormatHelper.formatTracktimeFromS(statistics.getPlayDuration()));
            mDBLength.setText(FormatHelper.formatTracktimeFromS(statistics.getAllSongDuration()));
            mLastUpdate.setText(FormatHelper.formatTimeStampToString(statistics.getLastDBUpdate()*1000));
        }
    }

    private class DBUpdateBtnListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            MPDQueryHandler.updateDatabase();
        }
    }
}
