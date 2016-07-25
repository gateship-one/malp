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

package andrompd.org.andrompd.mpdservice.handlers;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDConnection;

public class MPDStateMonitoringHandler extends Handler implements MPDConnection.MPDConnectionStateChangeListener{
    private static final String THREAD_NAME = "AndroMPD-StateMonitoring";


    private static HandlerThread mHandlerThread = null;
    private static MPDStateMonitoringHandler mHandlerSingleton = null;

    /**
     * Private constructor for use in singleton.
     * @param looper Looper of a HandlerThread (that is NOT the UI thread)
     */
    private MPDStateMonitoringHandler(Looper looper) {
        super(looper);
    }

    /**
     * Private method to ensure that the singleton runs in a separate thread.
     * Otherwise android will deny network access because of UI blocks.
     * @return
     */
    private synchronized static MPDStateMonitoringHandler getHandler() {
        if ( null == mHandlerSingleton ) {
            mHandlerThread = new HandlerThread(THREAD_NAME);
            mHandlerThread.start();
            mHandlerSingleton = new MPDStateMonitoringHandler(mHandlerSingleton.getLooper());
        }
        return mHandlerSingleton;
    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onDisconnected() {

    }
}
