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

package org.gateshipone.malp.application.fragments.serverfragments;


import android.app.Activity;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;

import java.lang.ref.WeakReference;
import java.util.List;

import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDInterface;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;

public abstract class GenericMPDFragment<T extends Object> extends DialogFragment implements LoaderManager.LoaderCallbacks<T> {
    private static final String TAG = GenericMPDFragment.class.getSimpleName();

    protected ConnectionStateListener mConnectionStateListener;

    protected SwipeRefreshLayout mSwipeRefreshLayout = null;

    protected GenericMPDFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContent();
        Activity activity = getActivity();
        if (activity != null) {
            mConnectionStateListener = new ConnectionStateListener(this, activity.getMainLooper());
            MPDInterface.mInstance.addMPDConnectionStateChangeListener(mConnectionStateListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        synchronized (this) {
            getLoaderManager().destroyLoader(0);
            MPDInterface.mInstance.removeMPDConnectionStateChangeListener(mConnectionStateListener);
            mConnectionStateListener = null;
        }
    }


    protected void refreshContent() {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(true));
        }
        if ( !isDetached()) {
            getLoaderManager().restartLoader(0, getArguments(), this);
        }
    }


    private static class ConnectionStateListener extends MPDConnectionStateChangeHandler {
        private WeakReference<GenericMPDFragment> pFragment;

        public ConnectionStateListener(GenericMPDFragment fragment, Looper looper) {
            super(looper);
            pFragment = new WeakReference<GenericMPDFragment>(fragment);
        }

        @Override
        public void onConnected() {
            pFragment.get().refreshContent();
        }

        @Override
        public void onDisconnected() {
            synchronized (pFragment.get()) {
                if (!pFragment.get().isDetached()) {
                    pFragment.get().getLoaderManager().destroyLoader(0);
                    pFragment.get().finishedLoading();
                }
            }
        }
    }

    private void finishedLoading() {
        if (null != mSwipeRefreshLayout) {
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(false));
        }
    }

    /**
     * Called when the loader finished loading its data.
     * <p/>
     * The refresh indicator will be stopped if a refreshlayout exists.
     *
     * @param loader The used loader itself
     * @param model  Data of the loader
     */
    @Override
    public void onLoadFinished(Loader<T> loader, T model) {
        finishedLoading();
    }

    @Override
    public void onLoaderReset(Loader<T> loader) {
        finishedLoading();
    }

    /**
     * Method to apply a filter to the view model of the fragment.
     * <p/>
     * This method must be overridden by the subclass.
     */
    public void applyFilter(String filter) {
        throw new IllegalStateException("filterView hasn't been implemented in the subclass");
    }

    /**
     * Method to remove a previous set filter.
     * <p/>
     * This method must be overridden by the subclass.
     */
    public void removeFilter() {
        throw new IllegalStateException("removeFilter hasn't been implemented in the subclass");
    }
}
