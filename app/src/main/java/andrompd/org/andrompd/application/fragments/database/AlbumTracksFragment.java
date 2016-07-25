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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.List;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.adapters.AlbumTracksAdapter;
import andrompd.org.andrompd.application.loaders.AlbumTracksLoader;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDFile;

public class AlbumTracksFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<MPDFile>> {
    /**
     * Parameters for bundled extra arguments for this fragment. Necessary to define which album to
     * retrieve from the MPD server.
     */
    public static final String BUNDLE_STRING_EXTRA_ARTISTNAME = "artistnaem";
    public static final String BUNDLE_STRING_EXTRA_ALBUMNAME = "albumname";

    /**
     * Album definition variables
     */
    private String mAlbumName;
    private String mArtistName;

    /**
     * Main ListView of this fragment
     */
    private ListView mListView;


    /**
     * Adapter used by the ListView
     */
    private AlbumTracksAdapter mTracksAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_album_tracks, container, false);

        // Get the main ListView of this fragment
        mListView = (ListView)rootView.findViewById(R.id.album_tracks_listview);

        /* Check if an artistname/albumame was given in the extras */
        Bundle args = getArguments();
        if ( null != args ) {
            mArtistName = args.getString(BUNDLE_STRING_EXTRA_ARTISTNAME);
            mAlbumName = args.getString(BUNDLE_STRING_EXTRA_ALBUMNAME);
        }

        // Create the needed adapter for the ListView
        mTracksAdapter = new AlbumTracksAdapter(getActivity());

        // Combine the two to a happy couple
        mListView.setAdapter(mTracksAdapter);

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

    }

    /**
     * Creates a new Loader that retrieves the list of album tracks
     * @param id
     * @param args
     * @return Newly created loader
     */
    @Override
    public Loader<List<MPDFile>> onCreateLoader(int id, Bundle args) {
        return new AlbumTracksLoader(getActivity(),mAlbumName,mArtistName);
    }

    /**
     * When the loader finished its loading of the data it is transferred to the adapter.
     * @param loader Loader that finished its loading
     * @param data Data that was retrieved by the laoder
     */
    @Override
    public void onLoadFinished(Loader<List<MPDFile>> loader, List<MPDFile> data) {
        // Give the adapter the new retrieved data set
        mTracksAdapter.swapModel(data);
    }

    /**
     * Resets the loader and clears the model data set
     * @param loader The loader that gets cleared.
     */
    @Override
    public void onLoaderReset(Loader<List<MPDFile>> loader) {
        // Clear the model data of the used adapter
        mTracksAdapter.swapModel(null);
    }
}
