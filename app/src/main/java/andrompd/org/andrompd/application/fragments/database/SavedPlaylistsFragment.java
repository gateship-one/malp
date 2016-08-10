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

package andrompd.org.andrompd.application.fragments.database;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.adapters.SavedPlaylistsAdapter;
import andrompd.org.andrompd.application.callbacks.FABFragmentCallback;
import andrompd.org.andrompd.application.loaders.PlaylistsLoader;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDPlaylist;

public class SavedPlaylistsFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<MPDPlaylist>>, AbsListView.OnItemClickListener{
    public final static String TAG = SavedPlaylistsFragment.class.getSimpleName();
    /**
     * Adapter used by the ListView
     */
    private SavedPlaylistsAdapter mPlaylistAdapter;

    /**
     * Main ListView of this fragment
     */
    private ListView mListView;

    /**
     * Callback for activity this fragment gets attached to
     */
    private SavedPlaylistsCallback mCallback;

    private FABFragmentCallback mFABCallback = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.listview_layout, container, false);

        // Get the main ListView of this fragment
        mListView = (ListView) rootView.findViewById(R.id.main_listview);


        // Create the needed adapter for the ListView
        mPlaylistAdapter = new SavedPlaylistsAdapter(getActivity());

        // Combine the two to a happy couple
        mListView.setAdapter(mPlaylistAdapter);
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);

        // Return the ready inflated and configured fragment view.
        return rootView;
    }

    /**
     * Starts the loader to make sure the data is up-to-date after resuming the fragment (from background)
     */
    @Override
    public void onResume() {
        super.onResume();
        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);
        if ( null != mFABCallback ) {
            mFABCallback.setupFAB(false,null);
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
            mCallback = (SavedPlaylistsCallback) context;
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

        MPDPlaylist playlist = (MPDPlaylist)mPlaylistAdapter.getItem(info.position);
        switch (item.getItemId()) {
            case R.id.action_add_playlist:
                MPDQueryHandler.loadPlaylist(playlist.getName());
                return true;
            case R.id.action_remove_playlist:
                MPDQueryHandler.removePlaylist(playlist.getName());
                mPlaylistAdapter.swapModel(null);
                getLoaderManager().destroyLoader(0);
                getLoaderManager().initLoader(0, getArguments(), this);
                return true;
            case R.id.action_play_playlist:
                MPDQueryHandler.playPlaylist(playlist.getName());
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
    public Loader<List<MPDPlaylist>> onCreateLoader(int id, Bundle args) {
        return new PlaylistsLoader(getActivity(), false);
    }

    /**
     * When the loader finished its loading of the data it is transferred to the adapter.
     *
     * @param loader Loader that finished its loading
     * @param data   Data that was retrieved by the laoder
     */
    @Override
    public void onLoadFinished(Loader<List<MPDPlaylist>> loader, List<MPDPlaylist> data) {
        mPlaylistAdapter.swapModel(data);
    }

    /**
     * Resets the loader and clears the model data set
     *
     * @param loader The loader that gets cleared.
     */
    @Override
    public void onLoaderReset(Loader<List<MPDPlaylist>> loader) {
        // Clear the model data of the used adapter
        mPlaylistAdapter.swapModel(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if ( null != mCallback ) {
            MPDPlaylist playlist = (MPDPlaylist)mPlaylistAdapter.getItem(position);
            mCallback.openPlaylist(playlist.getName());
        }
    }

    public interface SavedPlaylistsCallback {
        void openPlaylist(String name);
    }
}
