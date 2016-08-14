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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.adapters.OutputAdapter;
import andrompd.org.andrompd.application.callbacks.FABFragmentCallback;
import andrompd.org.andrompd.application.loaders.OutputsLoader;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDCommandHandler;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDOutput;

public class OutputsFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<MPDOutput>>, AbsListView.OnItemClickListener{
    public final static String TAG = OutputsFragment.class.getSimpleName();
    /**
     * Main ListView of this fragment
     */
    private ListView mListView;

    private OutputAdapter mAdapter;


    private FABFragmentCallback mFABCallback = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.listview_layout, container, false);

        // Get the main ListView of this fragment
        mListView = (ListView)rootView.findViewById(R.id.main_listview);


        // Create the needed adapter for the ListView
        mAdapter = new OutputAdapter(getActivity());

        // Combine the two to a happy couple
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);

        setHasOptionsMenu(true);

        // Return the ready inflated and configured fragment view.
        return rootView;
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
            mFABCallback = (FABFragmentCallback) context;
        } catch (ClassCastException e) {
            mFABCallback = null;
        }
    }

    /**
     * Called when the fragment resumes.
     * Reload the data, setup the toolbar and create the PBS connection.
     */
    @Override
    public void onResume() {
        super.onResume();

        // Prepare loader ( start new one or reuse old )
        getLoaderManager().initLoader(0, getArguments(), this);

        if ( null != mFABCallback ) {
            mFABCallback.setupFAB(false,null);
            mFABCallback.setupToolbar(getString(R.string.menu_outputs), false, true, false);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MPDOutput output = (MPDOutput)mAdapter.getItem(position);
        MPDCommandHandler.toggleOutput(output.getID());
        mAdapter.setOutputActive(position,!output.getOutputState());
    }

    @Override
    public Loader<List<MPDOutput>> onCreateLoader(int id, Bundle args) {
        return new OutputsLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<MPDOutput>> loader, List<MPDOutput> data) {
        mAdapter.swapModel(data);
    }

    @Override
    public void onLoaderReset(Loader<List<MPDOutput>> loader) {
        mAdapter.swapModel(null);
    }

}
