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

package org.gateshipone.malp.application.fragments.serverfragments;


import android.app.Activity;
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
import org.gateshipone.malp.mpdservice.handlers.MPDStatusChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseServerStatistics;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDCapabilities;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDStatistics;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

import java.lang.ref.WeakReference;

public class ServerStatisticFragment extends Fragment {
    public final static String TAG = ServerStatisticFragment.class.getSimpleName();

    private TextView mArtistCount;
    private TextView mAlbumsCount;
    private TextView mSongsCount;

    private TextView mUptime;
    private TextView mPlaytime;
    private TextView mLastUpdate;
    private TextView mDBLength;

    private TextView mDBUpdating;

    private TextView mServerFeatures;

    private ServerStatusHandler mServerStatusHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_server_statistic, container, false);

        mArtistCount = (TextView) rootView.findViewById(R.id.server_statistic_artist_count);
        mAlbumsCount = (TextView) rootView.findViewById(R.id.server_statistic_albums_count);
        mSongsCount = (TextView) rootView.findViewById(R.id.server_statistic_songs_count);

        mUptime = (TextView) rootView.findViewById(R.id.server_statistic_server_uptime);
        mPlaytime = (TextView) rootView.findViewById(R.id.server_statistic_server_playtime);
        mLastUpdate = (TextView) rootView.findViewById(R.id.server_statistic_db_update);
        mDBLength = (TextView) rootView.findViewById(R.id.server_statistic_db_playtime);

        mDBUpdating = (TextView) rootView.findViewById(R.id.server_statistic_updateing_db);

        mServerFeatures = (TextView) rootView.findViewById(R.id.server_statistic_malp_server_information);

        ((Button) rootView.findViewById(R.id.server_statistic_update_db_btn)).setOnClickListener(new DBUpdateBtnListener());

        mServerStatusHandler = new ServerStatusHandler(this);

        // Return the ready inflated and configured fragment view.
        return rootView;
    }

    /**
     * Attaches callbacks
     */
    @Override
    public void onResume() {
        super.onResume();

        MPDQueryHandler.getStatistics(new StatisticResponseHandler());

        MPDStateMonitoringHandler.registerStatusListener(mServerStatusHandler);
    }

    @Override
    public synchronized void onPause() {
        super.onPause();

        MPDStateMonitoringHandler.unregisterStatusListener(mServerStatusHandler);
    }

    private synchronized void showDatabaseUpdating(final boolean show) {
        Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    mDBUpdating.setVisibility(View.VISIBLE);
                } else {
                    mDBUpdating.setVisibility(View.GONE);
                }
            }
        });

    }


    private class StatisticResponseHandler extends MPDResponseServerStatistics {

        @Override
        public void handleStatistic(MPDStatistics statistics) {
            mArtistCount.setText(String.valueOf(statistics.getArtistsCount()));
            mAlbumsCount.setText(String.valueOf(statistics.getAlbumCount()));
            mSongsCount.setText(String.valueOf(statistics.getSongCount()));

            mUptime.setText(FormatHelper.formatTracktimeFromSWithDays(statistics.getServerUptime(),getContext()));
            mPlaytime.setText(FormatHelper.formatTracktimeFromSWithDays(statistics.getPlayDuration(),getContext()));
            mDBLength.setText(FormatHelper.formatTracktimeFromSWithDays(statistics.getAllSongDuration(),getContext()));
            mLastUpdate.setText(FormatHelper.formatTimeStampToString(statistics.getLastDBUpdate() * 1000));

            MPDCapabilities capabilities = MPDQueryHandler.getServerCapabilities();
            if (null != capabilities) {
                mServerFeatures.setText(MPDQueryHandler.getServerCapabilities().getServerFeatures());
            }
        }
    }

    private class DBUpdateBtnListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // Update the whole database => no path
            MPDQueryHandler.updateDatabase("");
        }
    }

    private static class ServerStatusHandler extends MPDStatusChangeHandler {
        WeakReference<ServerStatisticFragment> mFragment;

        public ServerStatusHandler(ServerStatisticFragment fragment) {
            mFragment = new WeakReference<ServerStatisticFragment>(fragment);
        }


        @Override
        protected void onNewStatusReady(MPDCurrentStatus status) {
            mFragment.get().showDatabaseUpdating(status.getUpdateDBJob() >= 0);
        }

        @Override
        protected void onNewTrackReady(MPDTrack track) {

        }
    }
}
