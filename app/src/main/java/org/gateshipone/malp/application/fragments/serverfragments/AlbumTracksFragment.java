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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.FileAdapter;
import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.callbacks.AddPathToPlaylist;
import org.gateshipone.malp.application.callbacks.FABFragmentCallback;
import org.gateshipone.malp.application.loaders.AlbumTracksLoader;
import org.gateshipone.malp.application.utils.CoverBitmapLoader;
import org.gateshipone.malp.application.utils.PreferenceHelper;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;

import java.util.List;

public class AlbumTracksFragment extends GenericMPDFragment<List<MPDFileEntry>> implements AdapterView.OnItemClickListener, CoverBitmapLoader.CoverBitmapListener, ArtworkManager.onNewAlbumImageListener {
    public final static String TAG = AlbumTracksFragment.class.getSimpleName();
    /**
     * Parameters for bundled extra arguments for this fragment. Necessary to define which album to
     * retrieve from the MPD server.
     */
    public static final String BUNDLE_STRING_EXTRA_ALBUM = "album";
    public static final String BUNDLE_STRING_EXTRA_BITMAP = "bitmap";

    /**
     * Album definition variables
     */
    private MPDAlbum mAlbum;

    /**
     * Main ListView of this fragment
     */
    private ListView mListView;

    private FABFragmentCallback mFABCallback = null;

    /**
     * Adapter used by the ListView
     */
    private FileAdapter mFileAdapter;

    private Bitmap mBitmap;

    private CoverBitmapLoader mBitmapLoader;

    private PreferenceHelper.LIBRARY_TRACK_CLICK_ACTION mClickAction;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.listview_layout_refreshable, container, false);

        // Get the main ListView of this fragment
        mListView = (ListView) rootView.findViewById(R.id.main_listview);

        /* Check if an artistname/albumame was given in the extras */
        Bundle args = getArguments();
        if (null != args) {
            mAlbum = args.getParcelable(BUNDLE_STRING_EXTRA_ALBUM);

            mBitmap = args.getParcelable(BUNDLE_STRING_EXTRA_BITMAP);
        }

        // Create the needed adapter for the ListView
        mFileAdapter = new FileAdapter(getActivity(), false, true);

        // Combine the two to a happy couple
        mListView.setAdapter(mFileAdapter);
        registerForContextMenu(mListView);
        mListView.setOnItemClickListener(this);

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

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());

        mClickAction = PreferenceHelper.getClickAction(sharedPref, getContext());

        setHasOptionsMenu(true);

        mBitmapLoader = new CoverBitmapLoader(getContext(), this);

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
            mFABCallback.setupFAB(true, new FABOnClickListener());
            mFABCallback.setupToolbar(mAlbum.getName(), false, false, false);
        }

        if (mAlbum != null && mBitmap == null) {
            final View rootView = getView();
            if (rootView != null) {
                getView().post(new Runnable() {
                    @Override
                    public void run() {
                        int width = rootView.getWidth();
                        mBitmapLoader.getAlbumImage(mAlbum, false, width, width);
                    }
                });
            }
        } else if (mAlbum != null) {
            // Reuse the image passed from the previous fragment
            mFABCallback.setupToolbar(mAlbum.getName(), false, false, true);
            mFABCallback.setupToolbarImage(mBitmap);
            final View rootView = getView();
            if (rootView != null) {
                getView().post(new Runnable() {
                    @Override
                    public void run() {
                        int width = rootView.getWidth();
                        // Image too small
                        if(mBitmap.getWidth() < width) {
                            mBitmapLoader.getAlbumImage(mAlbum, false, width, width);
                        }
                    }
                });
            }
        }

        ArtworkManager.getInstance(getContext()).registerOnNewAlbumImageListener(this);
    }

    /**
     * Called when the fragment is hidden. This unregisters the listener for a new album image
     */
    @Override
    public void onPause() {
        super.onPause();
        ArtworkManager.getInstance(getContext()).unregisterOnNewAlbumImageListener(this);
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
     * Creates a new Loader that retrieves the list of album tracks
     *
     * @param id
     * @param args
     * @return Newly created loader
     */
    @Override
    public Loader<List<MPDFileEntry>> onCreateLoader(int id, Bundle args) {
        return new AlbumTracksLoader(getActivity(), mAlbum.getName(), mAlbum.getArtistName(), mAlbum.getMBID());
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
        // Give the adapter the new retrieved data set
        mFileAdapter.swapModel(data);
    }

    /**
     * Resets the loader and clears the model data set
     *
     * @param loader The loader that gets cleared.
     */
    @Override
    public void onLoaderReset(Loader<List<MPDFileEntry>> loader) {
        // Clear the model data of the used adapter
        mFileAdapter.swapModel(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (mClickAction) {
            case ACTION_SHOW_DETAILS: {
                // Open song details dialog
                SongDetailsDialog songDetailsDialog = new SongDetailsDialog();
                Bundle args = new Bundle();
                args.putParcelable(SongDetailsDialog.EXTRA_FILE, (MPDTrack) mFileAdapter.getItem(position));
                songDetailsDialog.setArguments(args);
                songDetailsDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "SongDetails");
                return;
            }
            case ACTION_ADD_SONG:{
                enqueueTrack(position);
                return;
            }
            case ACTION_PLAY_SONG: {
                play(position);
                return;
            }
            case ACTION_PLAY_SONG_NEXT: {
                playNext(position);
                return;
            }
        }
    }

    /**
     * Create the context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_track, menu);
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
            case R.id.action_song_enqueue:
                enqueueTrack(info.position);
                return true;
            case R.id.action_song_play:
                play(info.position);
                return true;
            case R.id.action_song_play_next:
                playNext(info.position);
                return true;
            case R.id.action_add_to_saved_playlist: {
                // open dialog in order to save the current playlist as a playlist in the mediastore
                ChoosePlaylistDialog choosePlaylistDialog = new ChoosePlaylistDialog();
                Bundle args = new Bundle();
                args.putBoolean(ChoosePlaylistDialog.EXTRA_SHOW_NEW_ENTRY, true);
                choosePlaylistDialog.setCallback(new AddPathToPlaylist((MPDFileEntry) mFileAdapter.getItem(info.position), getActivity()));
                choosePlaylistDialog.setArguments(args);
                choosePlaylistDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "ChoosePlaylistDialog");
                return true;
            }
            case R.id.action_show_details: {
                // Open song details dialog
                SongDetailsDialog songDetailsDialog = new SongDetailsDialog();
                Bundle args = new Bundle();
                args.putParcelable(SongDetailsDialog.EXTRA_FILE, (MPDTrack) mFileAdapter.getItem(info.position));
                songDetailsDialog.setArguments(args);
                songDetailsDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "SongDetails");
                return true;
            }
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
        menuInflater.inflate(R.menu.fragment_menu_album_tracks, menu);

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_accent);

        Drawable drawable = menu.findItem(R.id.action_add_album).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_add_album).setIcon(drawable);

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
                if (null != mFABCallback) {
                    mFABCallback.setupToolbar(mAlbum.getName(), false, false, false);
                }
                ArtworkManager.getInstance(getContext()).resetAlbumImage(mAlbum);
                return true;
            case R.id.action_add_album:
                enqueueAlbum();
                return true;
            case R.id.action_show_all_tracks:
                mAlbum.setMBID("");
                mAlbum.setArtistName("");
                getLoaderManager().destroyLoader(0);

                getLoaderManager().initLoader(0, getArguments(), this);
                return true;
        }

        return super.onOptionsItemSelected(item);
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


    private void enqueueTrack(int index) {
        MPDTrack track = (MPDTrack) mFileAdapter.getItem(index);

        MPDQueryHandler.addPath(track.getPath());
    }

    private void play(int index) {
        MPDTrack track = (MPDTrack) mFileAdapter.getItem(index);

        MPDQueryHandler.playSong(track.getPath());
    }


    private void playNext(int index) {
        MPDTrack track = (MPDTrack) mFileAdapter.getItem(index);

        MPDQueryHandler.playSongNext(track.getPath());
    }

    private void enqueueAlbum() {
        MPDQueryHandler.addArtistAlbum(mAlbum.getName(), mAlbum.getArtistName(), mAlbum.getMBID());
    }

    @Override
    public void receiveBitmap(final Bitmap bm, final CoverBitmapLoader.IMAGE_TYPE type) {
        if (type == CoverBitmapLoader.IMAGE_TYPE.ALBUM_IMAGE && null != mFABCallback && bm != null) {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFABCallback.setupToolbar(mAlbum.getName(), false, false, true);
                        mFABCallback.setupToolbarImage(bm);
                        getArguments().putParcelable(BUNDLE_STRING_EXTRA_BITMAP, bm);
                    }
                });
            }
        }
    }

    @Override
    public void newAlbumImage(MPDAlbum album) {
        if (album.equals(mAlbum)) {
            int width = getView().getWidth();
            mBitmapLoader.getAlbumImage(mAlbum, false, width, width);
        }
    }

    private class FABOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            MPDCommandHandler.setRandom(false);
            MPDCommandHandler.setRepeat(false);
            MPDQueryHandler.playArtistAlbum(mAlbum.getName(), mAlbum.getArtistName(), mAlbum.getMBID());
        }
    }

}
