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

package org.gateshipone.malp.application.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.background.BackgroundService;
import org.gateshipone.malp.application.background.BackgroundServiceConnection;
import org.gateshipone.malp.application.utils.HardwareKeyHandler;
import org.gateshipone.malp.application.views.NowPlayingView;
import org.gateshipone.malp.mpdservice.ConnectionManager;
import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;

import java.lang.ref.WeakReference;


public abstract class GenericActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = GenericActivity.class.getSimpleName();

    private boolean mHardwareControls;

    private BackgroundServiceConnection mBackgroundServiceConnection;

    private BackgroundService.STREAMING_STATUS mStreamingStatus;

    private MPDConnectionStateCallbackHandler mConnectionCallback;

    private StreamingStatusReceiver mStreamingStatusReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read theme preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String themePref = sharedPref.getString(getString(R.string.pref_theme_key), getString(R.string.pref_theme_default));
        boolean darkTheme = sharedPref.getBoolean(getString(R.string.pref_dark_theme_key), getResources().getBoolean(R.bool.pref_theme_dark_default));
        if (darkTheme) {
            if (themePref.equals(getString(R.string.pref_indigo_key))) {
                setTheme(R.style.AppTheme_indigo);
            } else if (themePref.equals(getString(R.string.pref_orange_key))) {
                setTheme(R.style.AppTheme_orange);
            } else if (themePref.equals(getString(R.string.pref_deeporange_key))) {
                setTheme(R.style.AppTheme_deepOrange);
            } else if (themePref.equals(getString(R.string.pref_blue_key))) {
                setTheme(R.style.AppTheme_blue);
            } else if (themePref.equals(getString(R.string.pref_darkgrey_key))) {
                setTheme(R.style.AppTheme_darkGrey);
            } else if (themePref.equals(getString(R.string.pref_brown_key))) {
                setTheme(R.style.AppTheme_brown);
            } else if (themePref.equals(getString(R.string.pref_lightgreen_key))) {
                setTheme(R.style.AppTheme_lightGreen);
            } else if (themePref.equals(getString(R.string.pref_red_key))) {
                setTheme(R.style.AppTheme_red);
            }
        } else {
            if (themePref.equals(getString(R.string.pref_indigo_key))) {
                setTheme(R.style.AppTheme_indigo_light);
            } else if (themePref.equals(getString(R.string.pref_orange_key))) {
                setTheme(R.style.AppTheme_orange_light);
            } else if (themePref.equals(getString(R.string.pref_deeporange_key))) {
                setTheme(R.style.AppTheme_deepOrange_light);
            } else if (themePref.equals(getString(R.string.pref_blue_key))) {
                setTheme(R.style.AppTheme_blue_light);
            } else if (themePref.equals(getString(R.string.pref_darkgrey_key))) {
                setTheme(R.style.AppTheme_darkGrey_light);
            } else if (themePref.equals(getString(R.string.pref_brown_key))) {
                setTheme(R.style.AppTheme_brown_light);
            } else if (themePref.equals(getString(R.string.pref_lightgreen_key))) {
                setTheme(R.style.AppTheme_lightGreen_light);
            } else if (themePref.equals(getString(R.string.pref_red_key))) {
                setTheme(R.style.AppTheme_red_light);
            }
        }
        if (themePref.equals(getString(R.string.pref_oleddark_key))) {
            setTheme(R.style.AppTheme_oledDark);
        }
        mConnectionCallback = new MPDConnectionStateCallbackHandler(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        MPDStateMonitoringHandler.registerConnectionStateListener(mConnectionCallback);

        ConnectionManager.getInstance().registerMPDUse(getApplicationContext());

        if (null == mBackgroundServiceConnection) {
            mBackgroundServiceConnection = new BackgroundServiceConnection(getApplicationContext(), new BackgroundServiceConnectionStateListener());
        }
        mBackgroundServiceConnection.openConnection();

        if (mStreamingStatusReceiver == null) {
            mStreamingStatusReceiver = new StreamingStatusReceiver();
        }


        IntentFilter filter = new IntentFilter();
        filter.addAction(BackgroundService.ACTION_STREAMING_STATUS_CHANGED);
        getApplicationContext().registerReceiver(mStreamingStatusReceiver, filter);

        // Check if hardware key control is enabled by the user
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        mHardwareControls = sharedPref.getBoolean(getString(R.string.pref_hardware_controls_key), getResources().getBoolean(R.bool.pref_hardware_controls_default));
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Disconnect from MPD server
        ConnectionManager.getInstance().unregisterMPDUse(getApplicationContext());

        mBackgroundServiceConnection.closeConnection();

        sharedPref.unregisterOnSharedPreferenceChangeListener(this);

        MPDStateMonitoringHandler.registerConnectionStateListener(mConnectionCallback);

        getApplicationContext().unregisterReceiver(mStreamingStatusReceiver);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_hardware_controls_key))) {
            mHardwareControls = sharedPreferences.getBoolean(getString(R.string.pref_hardware_controls_key), getResources().getBoolean(R.bool.pref_hardware_controls_default));
        }
    }

    /**
     * Handles the volume keys of the device to control MPDs volume.
     *
     * @param event KeyEvent that was pressed by the user.
     * @return True if handled by MALP
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean streamingActive = !(mStreamingStatus == BackgroundService.STREAMING_STATUS.STOPPED);
        if (mHardwareControls) {
            if (!HardwareKeyHandler.getInstance().handleKeyEvent(event,!streamingActive)) {
                return super.dispatchKeyEvent(event);
            } else return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    protected abstract void onConnected();

    protected abstract void onDisconnected();

    private static class MPDConnectionStateCallbackHandler extends MPDConnectionStateChangeHandler {
        private WeakReference<GenericActivity> mActivity;

        MPDConnectionStateCallbackHandler(GenericActivity activity) {
            mActivity = new WeakReference<GenericActivity>(activity);
        }

        @Override
        public void onConnected() {
            final GenericActivity activity = mActivity.get();
            if (null != activity) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.onConnected();
                    }
                });
            }
        }

        @Override
        public void onDisconnected() {
            final GenericActivity activity = mActivity.get();
            if (null != activity) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.onDisconnected();
                    }
                });
            }
        }
    }

    /**
     * Receives stream playback status updates. When stream playback is started the status
     * is necessary to show the right menu item.
     */
    private class StreamingStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BackgroundService.ACTION_STREAMING_STATUS_CHANGED)) {
                mStreamingStatus = BackgroundService.STREAMING_STATUS.values()[intent.getIntExtra(BackgroundService.INTENT_EXTRA_STREAMING_STATUS, 0)];
            }
        }
    }


    /**
     * Private class to handle when a {@link android.content.ServiceConnection} to the {@link BackgroundService}
     * is established. When the connection is established, the stream playback status is retrieved.
     */
    private class BackgroundServiceConnectionStateListener implements BackgroundServiceConnection.OnConnectionStatusChangedListener {

        @Override
        public void onConnected() {
            try {
                mStreamingStatus = BackgroundService.STREAMING_STATUS.values()[mBackgroundServiceConnection.getService().getStreamingStatus()];
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected() {

        }
    }
}
