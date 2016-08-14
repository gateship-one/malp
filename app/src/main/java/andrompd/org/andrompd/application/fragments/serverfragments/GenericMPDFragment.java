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

package andrompd.org.andrompd.application.fragments.serverfragments;


import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;

import andrompd.org.andrompd.mpdservice.handlers.MPDConnectionStateChangeHandler;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;

public abstract class GenericMPDFragment<T extends Object > extends Fragment implements LoaderManager.LoaderCallbacks<T>  {
    private static final String TAG = GenericMPDFragment.class.getSimpleName();

    protected ConnectionStateListener mConnectionStateListener;

    protected GenericMPDFragment() {
        mConnectionStateListener = new ConnectionStateListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);

        MPDQueryHandler.registerConnectionStateListener(mConnectionStateListener);

    }

    @Override
    public void onPause() {
        super.onPause();
        getLoaderManager().destroyLoader(0);
        MPDQueryHandler.unregisterConnectionStateListener(mConnectionStateListener);
    }


    protected void refreshContent() {
        getLoaderManager().destroyLoader(0);
        getLoaderManager().restartLoader(0, getArguments(), this);
    }



    private class ConnectionStateListener extends MPDConnectionStateChangeHandler {
        private GenericMPDFragment pFragment;

        public ConnectionStateListener(GenericMPDFragment fragment) {
            pFragment = fragment;
        }

        @Override
        public void onConnected() {
            refreshContent();
        }

        @Override
        public void onDisconnected() {
            getLoaderManager().destroyLoader(0);
        }
    }
}
