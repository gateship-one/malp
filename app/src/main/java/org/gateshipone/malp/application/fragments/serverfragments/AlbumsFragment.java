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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.AlbumsAdapter;
import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.callbacks.FABFragmentCallback;
import org.gateshipone.malp.application.listviewitems.AbsImageListViewItem;
import org.gateshipone.malp.application.loaders.AlbumsLoader;
import org.gateshipone.malp.application.utils.CoverBitmapLoader;
import org.gateshipone.malp.application.utils.PreferenceHelper;
import org.gateshipone.malp.application.utils.ScrollSpeedListener;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;

import java.util.List;

public class AlbumsFragment extends GenericMPDFragment<List<MPDAlbum>> implements AdapterView.OnItemClickListener, CoverBitmapLoader.CoverBitmapListener, ArtworkManager.onNewArtistImageListener {
    public final static String TAG = AlbumsFragment.class.getSimpleName();

    /**
     * Definition of bundled extras
     */
    public static final String BUNDLE_STRING_EXTRA_ARTISTNAME = "artistname";

    public static final String BUNDLE_STRING_EXTRA_ARTIST = "artist";

    public static final String BUNDLE_STRING_EXTRA_PATH = "album_path";

    public static final String BUNDLE_STRING_EXTRA_BITMAP = "bitmap";

    /**
     * GridView adapter object used for this GridView
     */
    private AlbumsAdapter mAlbumsAdapter;

    /**
     * Save the root GridView for later usage.
     */
    private AbsListView mAdapterView;

    /**
     * Save the last position here. Gets reused when the user returns to this view after selecting sme
     * albums.
     */
    private int mLastPosition = -1;

    private String mAlbumsPath;

    private MPDArtist mArtist;

    private AlbumSelectedCallback mAlbumSelectCallback;

    private FABFragmentCallback mFABCallback = null;

    private boolean mUseList = false;

    private Bitmap mBitmap;

    private CoverBitmapLoader mBitmapLoader;

    private MPDAlbum.MPD_ALBUM_SORT_ORDER mSortOrder;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String libraryView = sharedPref.getString(getString(R.string.pref_library_view_key), getString(R.string.pref_library_view_default));

        if (libraryView.equals(getString(R.string.pref_library_view_list_key))) {
            mUseList = true;
        }

        View rootView;
        // get gridview
        if (mUseList) {
            rootView = inflater.inflate(R.layout.listview_layout_refreshable, container, false);
            mAdapterView = (ListView) rootView.findViewById(R.id.main_listview);
        } else {
            // Inflate the layout for this fragment
            rootView = inflater.inflate(R.layout.fragment_gridview, container, false);
            mAdapterView = (GridView) rootView.findViewById(R.id.grid_refresh_gridview);
        }

        mSortOrder = PreferenceHelper.getMPDAlbumSortOrder(sharedPref, getContext());

        mAlbumsAdapter = new AlbumsAdapter(getActivity(), mAdapterView, mUseList);


        /* Check if an artistname was given in the extras */
        Bundle args = getArguments();
        if (null != args) {
            mAlbumsPath = args.getString(BUNDLE_STRING_EXTRA_PATH);
            mArtist = args.getParcelable(BUNDLE_STRING_EXTRA_ARTIST);
            mBitmap = args.getParcelable(BUNDLE_STRING_EXTRA_BITMAP);
        } else {
            mAlbumsPath = "";
            // Create dummy album
            mArtist = new MPDArtist("");
        }

        mAdapterView.setAdapter(mAlbumsAdapter);
        mAdapterView.setOnItemClickListener(this);


        mAdapterView.setOnScrollListener(new ScrollSpeedListener(mAlbumsAdapter, mAdapterView));

        // register for context menu
        registerForContextMenu(mAdapterView);


        setHasOptionsMenu(true);

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

        mBitmapLoader = new CoverBitmapLoader(getContext(), this);


        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        setupToolbarAndStuff();


        ArtworkManager.getInstance(getContext()).registerOnNewArtistImageListener(this);
        ArtworkManager.getInstance(getContext()).registerOnNewAlbumImageListener((AlbumsAdapter) mAlbumsAdapter);
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
            mAlbumSelectCallback = (AlbumSelectedCallback) context;
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

    @Override
    public void onPause() {
        super.onPause();

        ArtworkManager.getInstance(getContext()).unregisterOnNewArtistImageListener(this);
        ArtworkManager.getInstance(getContext()).unregisterOnNewAlbumImageListener((AlbumsAdapter) mAlbumsAdapter);
    }


    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_album, menu);
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
            case R.id.fragment_albums_action_enqueue:
                enqueueAlbum(info.position);
                return true;
            case R.id.fragment_albums_action_play:
                playAlbum(info.position);
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
        if (null != mArtist && !mArtist.getArtistName().equals("")) {
            menuInflater.inflate(R.menu.fragment_menu_albums, menu);

            // get tint color
            int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_accent);

            Drawable drawable = menu.findItem(R.id.action_add_artist).getIcon();
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, tintColor);
            menu.findItem(R.id.action_add_artist).setIcon(drawable);

            menu.findItem(R.id.action_reset_artwork).setVisible(true);
        }
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
            case R.id.action_reset_artwork:
                if (null != mArtist && !mArtist.getArtistName().equals("")) {
                    mFABCallback.setupFAB(true, new FABOnClickListener());
                    mFABCallback.setupToolbar(mArtist.getArtistName(), false, false, false);
                }
                ArtworkManager.getInstance(getContext()).resetArtistImage(mArtist);
                return true;
            case R.id.action_add_artist:
                enqueueArtist();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * This method creates a new loader for this fragment.
     *
     * @param id
     * @param args
     * @return
     */
    @Override
    public Loader<List<MPDAlbum>> onCreateLoader(int id, Bundle args) {
        return new AlbumsLoader(getActivity(), mArtist == null ? "" : mArtist.getArtistName(), mAlbumsPath);
    }

    /**
     * Called when the loader finished loading its data.
     *
     * @param loader The used loader itself
     * @param data   Data of the loader
     */
    @Override
    public void onLoadFinished(Loader<List<MPDAlbum>> loader, List<MPDAlbum> data) {
        super.onLoadFinished(loader, data);
        // Set the actual data to the adapter.
        mAlbumsAdapter.swapModel(data);

        // Reset old scroll position
        if (mLastPosition >= 0) {
            mAdapterView.setSelection(mLastPosition);
            mLastPosition = -1;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Do not save the bitmap for later use (too big for binder)
        Bundle args = getArguments();
        if (args != null) {
            getArguments().remove(BUNDLE_STRING_EXTRA_BITMAP);
        }
        super.onSaveInstanceState(savedInstanceState);
    }


    /**
     * If a loader is reset the model data should be cleared.
     *
     * @param loader Loader that was resetted.
     */
    @Override
    public void onLoaderReset(Loader<List<MPDAlbum>> loader) {
        // Clear the model data of the adapter.
        mAlbumsAdapter.swapModel(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mLastPosition = position;

        MPDAlbum album = (MPDAlbum) mAlbumsAdapter.getItem(position);
        Bitmap bitmap = null;

        // Check if correct view type, to be safe
        if (view instanceof AbsImageListViewItem ) {
            bitmap = ((AbsImageListViewItem) view).getBitmap();
        }

        if (mArtist != null) {
            if (!mArtist.getArtistName().equals(album.getArtistName())) {
                album.setArtistName(mArtist.getArtistName());
            }
        }

        // Check if the album already has an artist set. If not use the artist of the fragment
        mAlbumSelectCallback.onAlbumSelected(album, bitmap);
    }

    @Override
    public void receiveBitmap(final Bitmap bm, final CoverBitmapLoader.IMAGE_TYPE type) {
        if (type == CoverBitmapLoader.IMAGE_TYPE.ARTIST_IMAGE && null != mFABCallback && bm != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFABCallback.setupToolbar(mArtist.getArtistName(), false, false, true);
                    mFABCallback.setupToolbarImage(bm);
                    getArguments().putParcelable(BUNDLE_STRING_EXTRA_BITMAP, bm);
                }
            });
        }
    }

    private void setupToolbarAndStuff() {
        if (null != mFABCallback) {
            if (null != mArtist && !mArtist.getArtistName().isEmpty()) {
                mFABCallback.setupFAB(true, new FABOnClickListener());
                if (mBitmap == null) {
                    mBitmapLoader.getArtistImage(mArtist, true);
                    mFABCallback.setupToolbar(mArtist.getArtistName(), false, false, false);
                } else {
                    // Reuse image
                    mFABCallback.setupToolbar(mArtist.getArtistName(), false, false, true);
                    mFABCallback.setupToolbarImage(mBitmap);
                }
            } else if (null != mAlbumsPath && !mAlbumsPath.equals("")) {
                String lastPath = mAlbumsPath;
                String pathSplit[] = mAlbumsPath.split("/");
                if (pathSplit.length > 0) {
                    lastPath = pathSplit[pathSplit.length - 1];
                }
                mFABCallback.setupFAB(true, new FABOnClickListener());
                mFABCallback.setupToolbar(lastPath, false, false, false);
            } else {
                mFABCallback.setupFAB(false, null);
                mFABCallback.setupToolbar(getString(R.string.app_name), true, true, false);

            }
        }
    }

    /**
     * Callback for asynchronous image fetching
     * @param artist Artist for which a new image is received
     */
    @Override
    public void newArtistImage(MPDArtist artist) {
        if (artist.equals(mArtist)) {
            setupToolbarAndStuff();
        }
    }

    /**
     * Interface to implement for the activity containing this fragment
     */
    public interface AlbumSelectedCallback {
        void onAlbumSelected(MPDAlbum album, Bitmap bitmap);
    }


    /**
     * Enqueues the album selected by the user
     * @param index Index of the selected album
     */
    private void enqueueAlbum(int index) {
        MPDAlbum album = (MPDAlbum) mAlbumsAdapter.getItem(index);

        MPDQueryHandler.addArtistAlbum(album.getName(), album.getArtistName(), album.getMBID());
    }

    /**
     * Plays the album selected by the user
     * @param index Index of the selected album
     */
    private void playAlbum(int index) {
        MPDAlbum album = (MPDAlbum) mAlbumsAdapter.getItem(index);

        MPDQueryHandler.playArtistAlbum(album.getName(), album.getArtistName(), album.getMBID());
    }

    /**
     * Enqueues the artist that is currently shown (if the fragment is not shown for all albums)
     */
    private void enqueueArtist() {
        MPDQueryHandler.addArtist(mArtist.getArtistName(), mSortOrder);
    }

    private class FABOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            MPDQueryHandler.playArtist(mArtist.getArtistName(), mSortOrder);
        }
    }

    public void applyFilter(String name) {
        mAlbumsAdapter.applyFilter(name);
    }

    public void removeFilter() {
        mAlbumsAdapter.removeFilter();
    }


}
