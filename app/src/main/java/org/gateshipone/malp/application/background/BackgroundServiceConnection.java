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

package org.gateshipone.malp.application.background;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * {@link ServiceConnection} class to allow connection between the UI and the BackgroundService.
 * This is necessary for control of the background service and query of the status.
 */
public class BackgroundServiceConnection implements ServiceConnection {

    private IBackgroundService mBackgroundService;

    /**
     * Context used for binding to the service
     */
    private Context mContext;

    private OnConnectionStatusChangedListener mListener;

    public BackgroundServiceConnection(Context context, OnConnectionStatusChangedListener listener) {
        mContext = context;
        mListener = listener;
    }

    /**
     * This initiates the connection to the PlaybackService by binding to it
     */
    public void openConnection() {
        Intent serviceStartIntent = new Intent(mContext, BackgroundService.class);
        mContext.bindService(serviceStartIntent, this, Context.BIND_AUTO_CREATE);
    }

    /**
     * Disconnects the connection by unbinding from the service (not needed anymore)
     */
    public void closeConnection() {
        mContext.unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mBackgroundService = IBackgroundService.Stub.asInterface(service);
        if(null != mListener) {
            mListener.onConnected();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mBackgroundService = null;
        if(null != mListener) {
            mListener.onDisconnected();
        }
    }

    public synchronized IBackgroundService getService() throws RemoteException {
        if (null != mBackgroundService) {
            return mBackgroundService;
        } else {
            throw new RemoteException();
        }
    }

    public interface OnConnectionStatusChangedListener {
        void onConnected();
        void onDisconnected();
    }
}
