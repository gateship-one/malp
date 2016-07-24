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
import android.widget.GridView;

import java.util.List;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.adapters.AlbumsGridAdapter;
import andrompd.org.andrompd.application.loaders.AlbumsLoader;
import andrompd.org.andrompd.application.utils.ScrollSpeedListener;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDAlbum;

public class AlbumsFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<MPDAlbum>>{

    /**
     * Definition of bundled extras
     */
    public static final String BUNDLE_STRING_EXTRA_ARTISTNAME = "artistname";

    /**
     * GridView adapter object used for this GridView
     */
    private AlbumsGridAdapter mAlbumsAdapter;

    /**
     * Save the root GridView for later usage.
     */
    private GridView mRootGrid;

    /**
     * Save the last position here. Gets reused when the user returns to this view after selecting sme
     * albums.
     */
    private int mLastPosition;


    private String mArtistName;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_artists, container, false);

        // get gridview
        mRootGrid = (GridView) rootView.findViewById(R.id.artists_gridview);

        // add progressbar
        mRootGrid.setEmptyView(rootView.findViewById(R.id.artists_progressbar));

        mAlbumsAdapter = new AlbumsGridAdapter(getActivity(), mRootGrid);

        /* Check if an artistname was given in the extras */
        Bundle args = getArguments();
        if ( null != args ) {
            mArtistName = args.getString(BUNDLE_STRING_EXTRA_ARTISTNAME);
        }

        mRootGrid.setAdapter(mAlbumsAdapter);
        mRootGrid.setOnScrollListener(new ScrollSpeedListener(mAlbumsAdapter, mRootGrid));
        //mRootGrid.setOnItemClickListener(this);

        // register for context menu
        registerForContextMenu(mRootGrid);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);

    }

    /**
     * This method creates a new loader for this fragment.
     * @param id
     * @param args
     * @return
     */
    @Override
    public Loader<List<MPDAlbum>> onCreateLoader(int id, Bundle args) {
        return new AlbumsLoader(getActivity(),mArtistName);
    }

    /**
     * Called when the loader finished loading its data.
     * @param loader The used loader itself
     * @param data Data of the loader
     */
    @Override
    public void onLoadFinished(Loader<List<MPDAlbum>> loader, List<MPDAlbum> data) {
        // Set the actual data to the adapter.
        mAlbumsAdapter.swapModel(data);

        // Reset old scroll position
        if (mLastPosition >= 0) {
            mRootGrid.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    /**
     * If a loader is reset the model data should be cleared.
     * @param loader Loader that was resetted.
     */
    @Override
    public void onLoaderReset(Loader<List<MPDAlbum>> loader) {
        // Clear the model data of the adapter.
        mAlbumsAdapter.swapModel(null);
    }
}
