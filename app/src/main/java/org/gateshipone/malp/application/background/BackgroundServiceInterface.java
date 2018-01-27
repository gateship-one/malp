/*
 *  Copyright (C) 2018 Team Gateship-One
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

import android.os.Message;
import android.os.RemoteException;

import java.lang.ref.WeakReference;

/**
 * Interface to handle requests over the {@link android.content.ServiceConnection}.
 * Start/stop of stream playback is handled over an extra handler to ensure handling
 * in the right thread. Otherwise it will fail.
 */
public class BackgroundServiceInterface extends IBackgroundService.Stub {
    WeakReference<BackgroundService> mService;

    public BackgroundServiceInterface(BackgroundService service) {
        mService = new WeakReference<>(service);
    }

    @Override
    public void stopStreamingPlayback() throws RemoteException {
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = new BackgroundServiceHandler.HandlerAction(BackgroundServiceHandler.HANDLER_ACTION_TYPE.ACTION_STOP_STREAMING);
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public void startStreamingPlayback() throws RemoteException {
        Message msg = mService.get().getHandler().obtainMessage();
        msg.obj = new BackgroundServiceHandler.HandlerAction(BackgroundServiceHandler.HANDLER_ACTION_TYPE.ACTION_START_STREAMING);
        mService.get().getHandler().sendMessage(msg);
    }

    @Override
    public int getStreamingStatus() throws RemoteException {
        return mService.get().getStreamingStatus();
    }
}
