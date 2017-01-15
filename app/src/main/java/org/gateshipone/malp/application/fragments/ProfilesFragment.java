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

package org.gateshipone.malp.application.fragments;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.ProfileAdapter;
import org.gateshipone.malp.application.callbacks.FABFragmentCallback;
import org.gateshipone.malp.application.callbacks.ProfileManageCallbacks;
import org.gateshipone.malp.application.loaders.ProfilesLoader;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.mpdservice.profilemanagement.MPDServerProfile;

public class ProfilesFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<MPDServerProfile>>, AbsListView.OnItemClickListener{
    public final static String TAG = ProfilesFragment.class.getSimpleName();
    /**
     * Main ListView of this fragment
     */
    private ListView mListView;

    private ProfileAdapter mAdapter;

    private ProfileManageCallbacks mCallback;

    private FABFragmentCallback mFABCallback = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.listview_layout, container, false);

        // Get the main ListView of this fragment
        mListView = (ListView)rootView.findViewById(R.id.main_listview);


        // Create the needed adapter for the ListView
        mAdapter = new ProfileAdapter(getActivity());

        // Combine the two to a happy couple
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);

        setHasOptionsMenu(true);

        // Return the ready inflated and configured fragment view.
        return rootView;
    }


    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_profile, menu);
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

        switch (item.getItemId()) {
            case R.id.action_profile_connect:
                connectProfile(info.position);
                return true;
            case R.id.action_profile_edit:
                editProfile(info.position);
                return true;
            case R.id.action_profile_remove:
                removeProfile(info.position);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Initialize the options menu.
     * Be sure to call {@link #setHasOptionsMenu} before.
     *
     * @param menu         The container for the custom options menu.
     * @param menuInflater The inflater to instantiate the layout.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.fragment_menu_profiles, menu);

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_accent);

        Drawable drawable = menu.findItem(R.id.action_add).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_add).setIcon(drawable);

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * Hook called when an menu item in the options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                mCallback.editProfile(null);
                return true;
        }

        return super.onOptionsItemSelected(item);
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
            mCallback = (ProfileManageCallbacks) context;
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
            mFABCallback.setupToolbar(getString(R.string.menu_profiles), false, true, false);
        }
    }

    @Override
    public Loader<List<MPDServerProfile>> onCreateLoader(int id, Bundle args) {
        return new ProfilesLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<MPDServerProfile>> loader, List<MPDServerProfile> data) {
        mAdapter.swapModel(data);
    }

    @Override
    public void onLoaderReset(Loader<List<MPDServerProfile>> loader) {
        mAdapter.swapModel(null);
    }


    private void connectProfile(int index) {
        if ( null != mCallback ) {
            mCallback.connectProfile((MPDServerProfile)mAdapter.getItem(index));
        }
    }

    private void editProfile(int index) {
        if ( null != mCallback ) {
            mCallback.editProfile((MPDServerProfile)mAdapter.getItem(index));
        }
    }

    private void removeProfile(int index) {
        if ( null != mCallback ) {
            mCallback.removeProfile((MPDServerProfile)mAdapter.getItem(index));
            mAdapter.swapModel(null);
            // Prepare loader ( start new one or reuse old )
            getLoaderManager().restartLoader(0, getArguments(), this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if ( null != mCallback ) {
            mCallback.connectProfile((MPDServerProfile)mAdapter.getItem(position));
        }
    }
}
