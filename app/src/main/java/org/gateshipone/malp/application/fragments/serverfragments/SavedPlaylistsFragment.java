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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.FileAdapter;
import org.gateshipone.malp.application.callbacks.FABFragmentCallback;
import org.gateshipone.malp.application.callbacks.PlaylistCallback;
import org.gateshipone.malp.application.loaders.PlaylistsLoader;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDPlaylist;

public class SavedPlaylistsFragment extends GenericMPDFragment<List<MPDFileEntry>> implements AbsListView.OnItemClickListener {
    public final static String TAG = SavedPlaylistsFragment.class.getSimpleName();
    /**
     * Adapter used by the ListView
     */
    private FileAdapter mPlaylistAdapter;

    /**
     * Main ListView of this fragment
     */
    private ListView mListView;

    /**
     * Callback for activity this fragment gets attached to
     */
    private PlaylistCallback mCallback;

    private FABFragmentCallback mFABCallback = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.listview_layout_refreshable, container, false);

        // Get the main ListView of this fragment
        mListView = (ListView) rootView.findViewById(R.id.main_listview);


        // Create the needed adapter for the ListView
        mPlaylistAdapter = new FileAdapter(getActivity(), false, false);

        // Combine the two to a happy couple
        mListView.setAdapter(mPlaylistAdapter);
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);


        // get swipe layout
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        // set swipe colors
        mSwipeRefreshLayout.setColorSchemeColors(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent),
                ThemeUtils.getThemeColor(getContext(), R.attr.colorPrimary));
        // set swipe refresh listener
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                refreshContent();
            }
        });


        // Return the ready inflated and configured fragment view.
        return rootView;
    }

    /**
     * Starts the loader to make sure the data is up-to-date after resuming the fragment (from background)
     */
    @Override
    public void onResume() {
        super.onResume();

        if (null != mFABCallback) {
            mFABCallback.setupFAB(false, null);
            mFABCallback.setupToolbar(getString(R.string.menu_playlists), false, true, false);
        }
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
            mCallback = (PlaylistCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mFABCallback = (FABFragmentCallback) context;
        } catch (ClassCastException e) {
            mFABCallback = null;
        }
    }


    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_playlist, menu);
    }


    /**
     * Hook called when an menu item in the context menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        final MPDPlaylist playlist = (MPDPlaylist) mPlaylistAdapter.getItem(info.position);
        switch (item.getItemId()) {
            case R.id.action_add_playlist:
                MPDQueryHandler.loadPlaylist(playlist.getPath());
                return true;
            case R.id.action_remove_playlist:
                final AlertDialog.Builder removeListBuilder = new AlertDialog.Builder(getContext());
                removeListBuilder.setTitle(getContext().getString(R.string.action_delete_playlist));
                removeListBuilder.setMessage(getContext().getString(R.string.dialog_message_delete_playlist) + ' ' + playlist.getSectionTitle() + '?');
                removeListBuilder.setPositiveButton(R.string.dialog_action_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MPDQueryHandler.removePlaylist(playlist.getPath());
                        mPlaylistAdapter.swapModel(null);
                        getLoaderManager().destroyLoader(0);
                        getLoaderManager().initLoader(0, getArguments(), SavedPlaylistsFragment.this);
                    }
                });
                removeListBuilder.setNegativeButton(R.string.dialog_action_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                removeListBuilder.create().show();

                return true;
            case R.id.action_play_playlist:
                MPDQueryHandler.playPlaylist(playlist.getPath());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Creates a new Loader that retrieves the list of playlists
     *
     * @param id
     * @param args
     * @return Newly created loader
     */
    @Override
    public Loader<List<MPDFileEntry>> onCreateLoader(int id, Bundle args) {
        return new PlaylistsLoader(getActivity(), false);
    }

    /**
     * When the loader finished its loading of the data it is transferred to the adapter.
     *
     * @param loader Loader that finished its loading
     * @param data   Data that was retrieved by the laoder
     */
    @Override
    public void onLoadFinished(Loader<List<MPDFileEntry>> loader, List<MPDFileEntry> data) {
        super.onLoadFinished(loader, data);
        mPlaylistAdapter.swapModel(data);
    }

    /**
     * Resets the loader and clears the model data set
     *
     * @param loader The loader that gets cleared.
     */
    @Override
    public void onLoaderReset(Loader<List<MPDFileEntry>> loader) {
        // Clear the model data of the used adapter
        mPlaylistAdapter.swapModel(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mCallback) {
            MPDPlaylist playlist = (MPDPlaylist) mPlaylistAdapter.getItem(position);
            mCallback.openPlaylist(playlist.getPath());
        }
    }
}
