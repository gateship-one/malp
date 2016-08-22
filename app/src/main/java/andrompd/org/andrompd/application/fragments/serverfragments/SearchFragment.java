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


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;

import java.util.List;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.adapters.FileAdapter;
import andrompd.org.andrompd.application.callbacks.FABFragmentCallback;
import andrompd.org.andrompd.application.loaders.SearchResultLoader;
import andrompd.org.andrompd.application.utils.ThemeUtils;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDCommands;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDFile;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;

public class SearchFragment extends GenericMPDFragment<List<MPDFileEntry>> {
    public static final String TAG = SearchFragment.class.getSimpleName();

    /**
     * Adapter used by the ListView
     */
    private FileAdapter mFileAdapter;

    /**
     * Main ListView of this fragment
     */
    private ListView mListView;

    /**
     * Name of the playlist to load
     */
    private String mPath;

    private FABFragmentCallback mFABCallback = null;

    private Spinner mSelectSpinner;

    private SearchView mSearchView;

    private String mSearchText;

    private MPDCommands.MPD_SEARCH_TYPE mSearchType;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_server_search, container, false);

        // Get the main ListView of this fragment
        mListView = (ListView) rootView.findViewById(R.id.main_listview);


        // Create the needed adapter for the ListView
        mFileAdapter = new FileAdapter(getActivity(), false);

        // Combine the two to a happy couple
        mListView.setAdapter(mFileAdapter);
        registerForContextMenu(mListView);

        mSelectSpinner = (Spinner) rootView.findViewById(R.id.search_criteria);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.server_search_choices, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSelectSpinner.setAdapter(adapter);
        mSelectSpinner.setOnItemSelectedListener(new SpinnerSelectListener());

        mSearchView = (SearchView) rootView.findViewById(R.id.search_text);
        mSearchView.setOnQueryTextListener(new SearchViewQueryListener());


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

        setHasOptionsMenu(true);

        mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_TRACK;

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
            showFAB(false);
        } catch (ClassCastException e) {
            mFABCallback = null;
        }
    }

    /**
     * Starts the loader to make sure the data is up-to-date after resuming the fragment (from background)
     */
    @Override
    public void onResume() {
        super.onResume();

        if (null != mFABCallback) {
            mFABCallback.setupFAB(true, new FABOnClickListener());
            mFABCallback.setupToolbar(getResources().getString(R.string.action_search), false, true, false);
        }
        finishedLoading();



    }

    @Override
    public Loader<List<MPDFileEntry>> onCreateLoader(int id, Bundle args) {
        return new SearchResultLoader(getActivity(), mSearchText, mSearchType);
    }

    @Override
    public void onLoadFinished(Loader<List<MPDFileEntry>> loader, List<MPDFileEntry> data) {
        finishedLoading();
        mFileAdapter.swapModel(data);
        if ( null != data && !data.isEmpty()) {
            showFAB(true);
        } else {
            showFAB(false);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<MPDFileEntry>> loader) {
        finishedLoading();
        mFileAdapter.swapModel(null);
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_search_track, menu);
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


        MPDFile track = (MPDFile)mFileAdapter.getItem(info.position);

        mListView.setActivated(true);
        mListView.requestFocus();


        switch (item.getItemId()) {
            case R.id.action_song_play:
                MPDQueryHandler.playSong(track.getPath());
                return true;
            case R.id.action_song_enqueue:
                MPDQueryHandler.addSong(track.getPath());
                return true;
            case R.id.action_song_play_next:
                MPDQueryHandler.playSongNext(track.getPath());
                return true;
            case R.id.action_add_album:
                MPDQueryHandler.addArtistAlbum(track.getTrackAlbum(),"");
                return true;
            case R.id.action_play_album:
                MPDQueryHandler.playArtistAlbum(track.getTrackAlbum(),"");
                return true;
            case R.id.action_add_artist:
                MPDQueryHandler.addArtist(track.getTrackArtist());
                return true;
            case R.id.action_play_artist:
                MPDQueryHandler.playArtist(track.getTrackArtist());
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
        menuInflater.inflate(R.menu.fragment_menu_search_tracks, menu);

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), android.R.attr.textColor);

        Drawable drawable = menu.findItem(R.id.action_add_search_result).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_add_search_result).setIcon(drawable);

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
            case R.id.action_add_search_result:
                MPDQueryHandler.searchAddFiles(mSearchText,mSearchType);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showFAB(boolean active) {
        if (null != mFABCallback) {
            mFABCallback.setupFAB(active, active ?  new FABOnClickListener() : null);
        }
    }

    private class FABOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            MPDQueryHandler.searchPlayFiles(mSearchText, mSearchType);
        }
    }

    private class SpinnerSelectListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            switch (position) {
                case 0:
                    mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_TRACK;
                    break;
                case 1:
                    mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_ALBUM;
                    break;
                case 2:
                    mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_ARTIST;
                    break;
                case 3:
                    mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_FILE;
                    break;
                case 4:
                    mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_ANY;
                    break;
            }

            mSearchView.setActivated(true);
            mSearchView.requestFocus();

            // Open the keyboard again
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.toggleSoftInput(0, InputMethodManager.SHOW_IMPLICIT);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    private class SearchViewQueryListener implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            mSearchText = query;
            refreshContent();
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    }
}
