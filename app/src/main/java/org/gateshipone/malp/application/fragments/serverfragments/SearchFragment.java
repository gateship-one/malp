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

package org.gateshipone.malp.application.fragments.serverfragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
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

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.FileAdapter;
import org.gateshipone.malp.application.callbacks.AddPathToPlaylist;
import org.gateshipone.malp.application.callbacks.FABFragmentCallback;
import org.gateshipone.malp.application.loaders.SearchResultLoader;
import org.gateshipone.malp.application.utils.PreferenceHelper;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDCommands;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;

public class SearchFragment extends GenericMPDFragment<List<MPDFileEntry>> implements AdapterView.OnItemClickListener, View.OnFocusChangeListener {
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

    private MPDAlbum.MPD_ALBUM_SORT_ORDER mAlbumSortOrder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_server_search, container, false);

        // Get the main ListView of this fragment
        mListView = (ListView) rootView.findViewById(R.id.main_listview);

        // Create the needed adapter for the ListView
        mFileAdapter = new FileAdapter(getActivity(), false, true);

        // Combine the two to a happy couple
        mListView.setAdapter(mFileAdapter);
        mListView.setOnItemClickListener(this);
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
        mSearchView.setOnFocusChangeListener(this);


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

        // Get album sort order
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        mAlbumSortOrder = PreferenceHelper.getMPDAlbumSortOrder(sharedPref, getContext());

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

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String searchTypeSetting = sharedPref.getString(getString(R.string.pref_search_type_key), getString(R.string.pref_search_type_default));

        if (searchTypeSetting.equals(getString(R.string.pref_search_type_track_key))) {
            mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_TRACK;
            mSelectSpinner.setSelection(0);
        } else if (searchTypeSetting.equals(getString(R.string.pref_search_type_album_key))) {
            mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_ALBUM;
            mSelectSpinner.setSelection(1);
        } else if (searchTypeSetting.equals(getString(R.string.pref_search_type_artist_key))) {
            mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_ARTIST;
            mSelectSpinner.setSelection(2);
        } else if (searchTypeSetting.equals(getString(R.string.pref_search_type_file_key))) {
            mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_FILE;
            mSelectSpinner.setSelection(3);
        } else if (searchTypeSetting.equals(getString(R.string.pref_search_type_any_key))) {
            mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_ANY;
            mSelectSpinner.setSelection(4);
        }
    }

    @Override
    public Loader<List<MPDFileEntry>> onCreateLoader(int id, Bundle args) {
        return new SearchResultLoader(getActivity(), mSearchText, mSearchType);
    }

    @Override
    public void onLoadFinished(Loader<List<MPDFileEntry>> loader, List<MPDFileEntry> data) {
        super.onLoadFinished(loader, data);
        mFileAdapter.swapModel(data);
        if (null != data && !data.isEmpty()) {
            showFAB(true);
        } else {
            showFAB(false);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<MPDFileEntry>> loader) {
        super.onLoaderReset(loader);
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

    @Override
    public void onPause() {
        super.onPause();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getView();
        if (null != view) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
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


        MPDTrack track = (MPDTrack) mFileAdapter.getItem(info.position);

        mListView.requestFocus();


        switch (item.getItemId()) {
            case R.id.action_song_play:
                MPDQueryHandler.playSong(track.getPath());
                return true;
            case R.id.action_song_enqueue:
                MPDQueryHandler.addPath(track.getPath());
                return true;
            case R.id.action_song_play_next:
                MPDQueryHandler.playSongNext(track.getPath());
                return true;
            case R.id.action_add_to_saved_playlist:
                // open dialog in order to save the current playlist as a playlist in the mediastore
                ChoosePlaylistDialog choosePlaylistDialog = new ChoosePlaylistDialog();
                Bundle args = new Bundle();
                args.putBoolean(ChoosePlaylistDialog.EXTRA_SHOW_NEW_ENTRY, true);
                choosePlaylistDialog.setCallback(new AddPathToPlaylist((MPDFileEntry) mFileAdapter.getItem(info.position), getContext()));
                choosePlaylistDialog.setArguments(args);
                choosePlaylistDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "ChoosePlaylistDialog");
                return true;
            case R.id.action_add_album:
                MPDQueryHandler.addArtistAlbum(track.getTrackAlbum(), "", track.getTrackAlbumMBID());
                return true;
            case R.id.action_play_album:
                MPDQueryHandler.playArtistAlbum(track.getTrackAlbum(), "", track.getTrackAlbumMBID());
                return true;
            case R.id.action_add_artist:
                MPDQueryHandler.addArtist(track.getTrackArtist(),mAlbumSortOrder);
                return true;
            case R.id.action_play_artist:
                MPDQueryHandler.playArtist(track.getTrackArtist(),mAlbumSortOrder);
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
        int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_accent);

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
                MPDQueryHandler.searchAddFiles(mSearchText, mSearchType);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Open song details dialog
        SongDetailsDialog songDetailsDialog = new SongDetailsDialog();
        Bundle args = new Bundle();
        args.putParcelable(SongDetailsDialog.EXTRA_FILE, (MPDTrack) mFileAdapter.getItem(position));
        songDetailsDialog.setArguments(args);
        songDetailsDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "SongDetails");
    }

    private void showFAB(boolean active) {
        if (null != mFABCallback) {
            mFABCallback.setupFAB(active, active ? new FABOnClickListener() : null);
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v.equals(mSearchView) && !hasFocus) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
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
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor prefEditor = sharedPref.edit();
            switch (position) {
                case 0:
                    mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_TRACK;
                    prefEditor.putString(getString(R.string.pref_search_type_key), getString(R.string.pref_search_type_track_key));
                    break;
                case 1:
                    mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_ALBUM;
                    prefEditor.putString(getString(R.string.pref_search_type_key), getString(R.string.pref_search_type_album_key));
                    break;
                case 2:
                    mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_ARTIST;
                    prefEditor.putString(getString(R.string.pref_search_type_key), getString(R.string.pref_search_type_artist_key));
                    break;
                case 3:
                    mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_FILE;
                    prefEditor.putString(getString(R.string.pref_search_type_key), getString(R.string.pref_search_type_file_key));
                    break;
                case 4:
                    mSearchType = MPDCommands.MPD_SEARCH_TYPE.MPD_SEARCH_ANY;
                    prefEditor.putString(getString(R.string.pref_search_type_key), getString(R.string.pref_search_type_any_key));
                    break;
            }

            // Write settings values
            prefEditor.apply();

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
