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

package org.gateshipone.malp.application.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.transition.Slide;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;


import org.gateshipone.malp.R;
import org.gateshipone.malp.application.fragments.ArtworkSettingsFragment;
import org.gateshipone.malp.application.fragments.serverfragments.ServerPropertiesFragment;
import org.gateshipone.malp.mpdservice.ConnectionManager;
import org.gateshipone.malp.application.callbacks.AddPathToPlaylist;
import org.gateshipone.malp.application.callbacks.FABFragmentCallback;
import org.gateshipone.malp.application.callbacks.PlaylistCallback;
import org.gateshipone.malp.application.callbacks.ProfileManageCallbacks;
import org.gateshipone.malp.application.fragments.EditProfileFragment;
import org.gateshipone.malp.application.fragments.ProfilesFragment;
import org.gateshipone.malp.application.fragments.SettingsFragment;
import org.gateshipone.malp.application.fragments.serverfragments.AlbumTracksFragment;
import org.gateshipone.malp.application.fragments.serverfragments.AlbumsFragment;
import org.gateshipone.malp.application.fragments.serverfragments.ArtistsFragment;
import org.gateshipone.malp.application.fragments.serverfragments.ChoosePlaylistDialog;
import org.gateshipone.malp.application.fragments.serverfragments.FilesFragment;
import org.gateshipone.malp.application.fragments.serverfragments.MyMusicTabsFragment;
import org.gateshipone.malp.application.fragments.serverfragments.OutputsFragment;
import org.gateshipone.malp.application.fragments.serverfragments.PlaylistTracksFragment;
import org.gateshipone.malp.application.fragments.serverfragments.SavedPlaylistsFragment;
import org.gateshipone.malp.application.fragments.serverfragments.SearchFragment;
import org.gateshipone.malp.application.fragments.serverfragments.ServerStatisticFragment;
import org.gateshipone.malp.application.fragments.serverfragments.SongDetailsDialog;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.application.views.CurrentPlaylistView;
import org.gateshipone.malp.application.views.NowPlayingView;
import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFile;
import org.gateshipone.malp.mpdservice.profilemanagement.MPDProfileManager;
import org.gateshipone.malp.mpdservice.profilemanagement.MPDServerProfile;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AlbumsFragment.AlbumSelectedCallback, ArtistsFragment.ArtistSelectedCallback,
        ProfileManageCallbacks, PlaylistCallback,
        NowPlayingView.NowPlayingDragStatusReceiver, FilesFragment.FilesCallback,
        FABFragmentCallback, SettingsFragment.OnArtworkSettingsRequestedCallback {


    private static final String TAG = "MainActivity";

    private final static String MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW = "org.malp.requestedview";
    private final static String MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW = "org.malp.requestedview.nowplaying";

    private final static String MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_DRAG_STATUS = "MainActivity.NowPlayingDragStatus";
    private final static String MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_VIEW_SWITCHER_CURRENT_VIEW = "MainActivity.NowPlayingViewSwitcherCurrentView";

    private DRAG_STATUS mNowPlayingDragStatus;
    private DRAG_STATUS mSavedNowPlayingDragStatus = null;

    private ActionBarDrawerToggle mDrawerToggle;

    private VIEW_SWITCHER_STATUS mNowPlayingViewSwitcherStatus;
    private VIEW_SWITCHER_STATUS mSavedNowPlayingViewSwitcherStatus;

    private MPDProfileManager mProfileManager;


    private FloatingActionButton mFAB;

    private ConnectionStateListener mConnectionStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // restore drag state
        if (savedInstanceState != null) {
            mSavedNowPlayingDragStatus = DRAG_STATUS.values()[savedInstanceState.getInt(MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_DRAG_STATUS)];
            mSavedNowPlayingViewSwitcherStatus = VIEW_SWITCHER_STATUS.values()[savedInstanceState.getInt(MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_VIEW_SWITCHER_CURRENT_VIEW)];
        }

        // Read theme preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String themePref = sharedPref.getString("pref_theme", "orange");

        switch (themePref) {
            case "indigo":
                setTheme(R.style.AppTheme_indigo);
                break;
            case "orange":
                setTheme(R.style.AppTheme_orange);
                break;
            case "deeporange":
                setTheme(R.style.AppTheme_deepOrange);
                break;
            case "blue":
                setTheme(R.style.AppTheme_blue);
                break;
            case "darkgrey":
                setTheme(R.style.AppTheme_darkGrey);
                break;
            case "brown":
                setTheme(R.style.AppTheme_brown);
                break;
            case "lightgreen":
                setTheme(R.style.AppTheme_lightGreen);
                break;
        }

        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // enable back navigation
        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            mDrawerToggle = new ActionBarDrawerToggle(this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(mDrawerToggle);
            mDrawerToggle.syncState();
        }

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            navigationView.setCheckedItem(R.id.nav_library);
        }


        mFAB = (FloatingActionButton) findViewById(R.id.andrompd_play_button);


        mProfileManager = new MPDProfileManager(getApplicationContext());

        registerForContextMenu(findViewById(R.id.main_listview));

        // Read default view preference
        String defaultView = sharedPref.getString("pref_default_view", "my_music_albums");

        // the default tab for mymusic
        MyMusicTabsFragment.DEFAULTTAB defaultTab = null;
        // the nav ressource id to mark the right item in the nav drawer
        int navId = R.id.nav_library;

        switch (defaultView) {
            case "my_music_artists":
                defaultTab = MyMusicTabsFragment.DEFAULTTAB.ARTISTS;
                break;
            case "my_music_albums":
                defaultTab = MyMusicTabsFragment.DEFAULTTAB.ALBUMS;
                break;
            case "playlists":
                navId = R.id.nav_saved_playlists;
                break;
            case "files":
                navId = R.id.nav_files;
                break;
        }

        if ( mProfileManager.getProfiles().size() == 0 ) {
            navId = R.id.nav_profiles;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.welcome_dialog_title));
            builder.setMessage(getResources().getString(R.string.welcome_dialog_text));


            builder.setPositiveButton(R.string.dialog_action_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        navigationView.setCheckedItem(navId);

        if ((findViewById(R.id.fragment_container) != null) && (savedInstanceState == null)) {

            Fragment fragment = null;

            if (navId == R.id.nav_library) {
                fragment = new MyMusicTabsFragment();

                Bundle args = new Bundle();
                args.putInt(MyMusicTabsFragment.MY_MUSIC_REQUESTED_TAB, defaultTab.ordinal());

                fragment.setArguments(args);
            } else if (navId == R.id.nav_saved_playlists) {
                fragment = new SavedPlaylistsFragment();
            } else if (navId == R.id.nav_files) {
                fragment = new FilesFragment();
            } else if ( navId == R.id.nav_profiles) {
                fragment = new ProfilesFragment();
            }

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
        mConnectionStateListener = new ConnectionStateListener();



    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        FragmentManager fragmentManager = getSupportFragmentManager();

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (mNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
            NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
            if (nowPlayingView != null) {
                View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
                coordinatorLayout.setVisibility(View.VISIBLE);
                nowPlayingView.minimize();
            }
        } else {
            super.onBackPressed();

            // enable navigation bar when backstack empty
            if (fragmentManager.getBackStackEntryCount() == 0) {
                mDrawerToggle.setDrawerIndicatorEnabled(true);
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        FragmentManager fragmentManager = getSupportFragmentManager();

        switch (item.getItemId()) {
            case android.R.id.home:
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    onBackPressed();
                } else {
                    // back stack empty so enable navigation drawer

                    mDrawerToggle.setDrawerIndicatorEnabled(true);

                    if (mDrawerToggle.onOptionsItemSelected(item)) {
                        return true;
                    }
                }
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.main_listview && mNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.context_menu_current_playlist_track, menu);

            // Check if the menu is created for the currently playing song. If this is the case, do not show play as next item.
            MPDCurrentStatus status = MPDStateMonitoringHandler.getLastStatus();
            if (status != null &&  ((AdapterView.AdapterContextMenuInfo)menuInfo).position == status.getCurrentSongIndex()) {
                menu.findItem(R.id.action_song_play_next).setVisible(false);
            }
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            return super.onContextItemSelected(item);
        }

        CurrentPlaylistView currentPlaylistView = (CurrentPlaylistView) findViewById(R.id.now_playing_playlist);

        if (currentPlaylistView != null && mNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {

            MPDFile track = (MPDFile) currentPlaylistView.getItem(info.position);

            switch (item.getItemId()) {

                case R.id.action_song_play_next:
                    MPDQueryHandler.playIndexAsNext(info.position);
                    return true;
                case R.id.action_add_to_saved_playlist:
                    // open dialog in order to save the current playlist as a playlist in the mediastore
                    ChoosePlaylistDialog choosePlaylistDialog = new ChoosePlaylistDialog();
                    Bundle args = new Bundle();
                    args.putBoolean(ChoosePlaylistDialog.EXTRA_SHOW_NEW_ENTRY, true);
                    choosePlaylistDialog.setCallback(new AddPathToPlaylist(track, this));
                    choosePlaylistDialog.setArguments(args);
                    choosePlaylistDialog.show(getSupportFragmentManager(), "ChoosePlaylistDialog");
                    return true;
                case R.id.action_remove_song:
                    MPDQueryHandler.removeSongFromCurrentPlaylist(info.position);
                    return true;
                case R.id.action_show_artist:
                    onArtistSelected(new MPDArtist(track.getTrackArtist()));
                    return true;
                case R.id.action_show_album:
                    MPDAlbum tmpAlbum = new MPDAlbum(track.getTrackAlbum());
                    if (!track.getTrackAlbumArtist().isEmpty()) {
                        tmpAlbum.setArtistName(track.getTrackAlbumArtist());
                    } else {
                        tmpAlbum.setArtistName(track.getTrackArtist());
                    }
                    tmpAlbum.setMBID(track.getTrackAlbumMBID());
                    onAlbumSelected(tmpAlbum);
                    return true;
                case R.id.action_show_details:
                    // Open song details dialog
                    SongDetailsDialog songDetailsDialog = new SongDetailsDialog();
                    Bundle songArgs = new Bundle();
                    songArgs.putParcelable(SongDetailsDialog.EXTRA_FILE, (MPDFile)track);
                    songDetailsDialog.setArguments(songArgs);
                    songDetailsDialog.show(getSupportFragmentManager(), "SongDetails");
                    return true;
            }
        }
        return false;
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Log.v(TAG, "Navdrawer item selected");
        View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
        coordinatorLayout.setVisibility(View.VISIBLE);

        NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
        if (nowPlayingView != null) {
            nowPlayingView.minimize();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();

        // clear backstack
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Fragment fragment = null;
        String fragmentTag = "";

        if (id == R.id.nav_library) {
            // Handle the camera action
            fragment = new MyMusicTabsFragment();
            fragmentTag = MyMusicTabsFragment.TAG;
        } else if (id == R.id.nav_saved_playlists) {
            fragment = new SavedPlaylistsFragment();
            fragmentTag = SavedPlaylistsFragment.TAG;
        } else if (id == R.id.nav_files) {
            fragment = new FilesFragment();
            fragmentTag = FilesFragment.TAG;

            Bundle args = new Bundle();
            args.putString(FilesFragment.EXTRA_FILENAME, "");

        } else if (id == R.id.nav_search) {
            fragment = new SearchFragment();
            fragmentTag = SearchFragment.TAG;
        } else if (id == R.id.nav_profiles) {
            fragment = new ProfilesFragment();
            fragmentTag = ProfilesFragment.TAG;
        } else if (id == R.id.nav_app_settings) {
            fragment = new SettingsFragment();
            fragmentTag = SettingsFragment.TAG;
        } else if (id == R.id.nav_server_properties) {
            fragment = new ServerPropertiesFragment();
            fragmentTag = ServerPropertiesFragment.TAG;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);


        // Do the actual fragment transaction
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, fragmentTag);
        transaction.commit();

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
        NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
        if (nowPlayingView != null) {


            nowPlayingView.registerDragStatusReceiver(this);

            /*
             * Check if the activity got an extra in its intend to show the nowplayingview directly.
             * If yes then pre set the dragoffset of the draggable helper.
             */
            Intent resumeIntent = getIntent();
            if (resumeIntent != null && resumeIntent.getExtras() != null && resumeIntent.getExtras().getString(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW) != null &&
                    resumeIntent.getExtras().getString(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW).equals(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW)) {
                nowPlayingView.setDragOffset(0.0f);
                getIntent().removeExtra(MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW);
            } else {
                // set drag status
                if (mSavedNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
                    nowPlayingView.setDragOffset(0.0f);
                } else if (mSavedNowPlayingDragStatus == DRAG_STATUS.DRAGGED_DOWN) {
                    nowPlayingView.setDragOffset(1.0f);
                }
                mSavedNowPlayingDragStatus = null;

                // set view switcher status
                if (mSavedNowPlayingViewSwitcherStatus != null) {
                    Log.v(TAG,"Restoring switcher status: " + mSavedNowPlayingViewSwitcherStatus);
                    nowPlayingView.setViewSwitcherStatus(mSavedNowPlayingViewSwitcherStatus);
                    mNowPlayingViewSwitcherStatus = mSavedNowPlayingViewSwitcherStatus;
                }
                mSavedNowPlayingViewSwitcherStatus = null;
            }
            nowPlayingView.onResume();
        }
        ConnectionManager.reconnectLastServer(getApplicationContext());

        MPDStateMonitoringHandler.registerConnectionStateListener(mConnectionStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");

        NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
        if (nowPlayingView != null) {
            nowPlayingView.registerDragStatusReceiver(null);

            nowPlayingView.onPause();
        }

        if (!isChangingConfigurations()) {
            // Disconnect from MPD server
            ConnectionManager.disconnectFromServer();
        }
        MPDStateMonitoringHandler.unregisterConnectionStateListener(mConnectionStateListener);
    }

    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        // save drag status of the nowplayingview
        savedInstanceState.putInt(MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_DRAG_STATUS, mNowPlayingDragStatus.ordinal());

        // save the cover/playlist view status of the nowplayingview
        savedInstanceState.putInt(MAINACTIVITY_SAVED_INSTANCE_NOW_PLAYING_VIEW_SWITCHER_CURRENT_VIEW, mNowPlayingViewSwitcherStatus.ordinal());
    }

    @Override
    public void onAlbumSelected(MPDAlbum album) {
        if (mNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
            NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
            if (nowPlayingView != null) {
                View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
                coordinatorLayout.setVisibility(View.VISIBLE);
                nowPlayingView.minimize();
            }
        }

        // Create fragment and give it an argument for the selected article
        AlbumTracksFragment newFragment = new AlbumTracksFragment();
        Bundle args = new Bundle();
        args.putParcelable(AlbumTracksFragment.BUNDLE_STRING_EXTRA_ALBUM, album);

        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        newFragment.setEnterTransition(new Slide(GravityCompat.START));
        newFragment.setExitTransition(new Slide(GravityCompat.END));
        transaction.replace(R.id.fragment_container, newFragment, AlbumTracksFragment.TAG);
        transaction.addToBackStack("AlbumTracksFragment");

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void onArtistSelected(MPDArtist artist) {
        Log.v(TAG, "Artist selected: " + artist);

        if (mNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
            NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
            if (nowPlayingView != null) {
                View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
                coordinatorLayout.setVisibility(View.VISIBLE);
                nowPlayingView.minimize();
            }
        }
        // Create fragment and give it an argument for the selected article
        AlbumsFragment newFragment = new AlbumsFragment();
        Bundle args = new Bundle();
        args.putString(AlbumsFragment.BUNDLE_STRING_EXTRA_ARTISTNAME, artist.getArtistName());
        args.putParcelable(AlbumsFragment.BUNDLE_STRING_EXTRA_ARTIST, artist);


        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        newFragment.setEnterTransition(new Slide(GravityCompat.START));
        newFragment.setExitTransition(new Slide(GravityCompat.END));        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragment_container, newFragment, AlbumsFragment.TAG);
        transaction.addToBackStack("ArtistAlbumsFragment");

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setCheckedItem(R.id.nav_library);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void onStatusChanged(DRAG_STATUS status) {
        mNowPlayingDragStatus = status;
        if (status == DRAG_STATUS.DRAGGED_UP) {
            View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
            coordinatorLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDragPositionChanged(float pos) {
        // Get the primary color of the active theme from the helper.
        int newColor = ThemeUtils.getThemeColor(this, R.attr.colorPrimary);

        // Calculate the offset depending on the floating point position (0.0-1.0 of the view)
        // Shift by 24 bit to set it as the A from ARGB and set all remaining 24 bits to 1 to
        int alphaOffset = (((255 - (int) (255.0 * pos)) << 24) | 0xFFFFFF);
        // and with this mask to set the new alpha value.
        newColor &= (alphaOffset);
        getWindow().setStatusBarColor(newColor);
    }

    @Override
    public void onSwitchedViews(VIEW_SWITCHER_STATUS view) {
        mNowPlayingViewSwitcherStatus = view;
        Log.v(TAG,"onSwitchedViews(" + view);
    }

    @Override
    public void onStartDrag() {
        View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
        coordinatorLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void editProfile(MPDServerProfile profile) {
        // Create fragment and give it an argument for the selected article
        EditProfileFragment newFragment = new EditProfileFragment();
        Bundle args = new Bundle();
        if (null != profile) {
            args.putParcelable(EditProfileFragment.EXTRA_PROFILE, profile);
        }


        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        newFragment.setEnterTransition(new Slide(GravityCompat.START));
        newFragment.setExitTransition(new Slide(GravityCompat.END));        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragment_container, newFragment, EditProfileFragment.TAG);
        transaction.addToBackStack("EditProfileFragment");


        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void connectProfile(MPDServerProfile profile) {
        ConnectionManager.disconnectFromServer();
        ConnectionManager.setParameters(profile, this);
        ConnectionManager.reconnectLastServer(getApplicationContext());
    }

    @Override
    public void addProfile(MPDServerProfile profile) {
        mProfileManager.addProfile(profile);

        // Try connecting to the new profile
        ConnectionManager.setParameters(profile, this);
        ConnectionManager.reconnectLastServer(getApplicationContext());
    }

    @Override
    public void removeProfile(MPDServerProfile profile) {
        mProfileManager.deleteProfile(profile);
    }

    @Override
    public void openPlaylist(String name) {
        // Create fragment and give it an argument for the selected article
        PlaylistTracksFragment newFragment = new PlaylistTracksFragment();
        Bundle args = new Bundle();
        args.putString(PlaylistTracksFragment.EXTRA_PLAYLIST_NAME, name);


        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        newFragment.setEnterTransition(new Slide(GravityCompat.START));
        newFragment.setExitTransition(new Slide(GravityCompat.END));
        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack("PlaylistTracksFragment");

        // Commit the transaction
        transaction.commit();

    }


    @Override
    public void setupFAB(boolean active, View.OnClickListener listener) {
        mFAB = (FloatingActionButton) findViewById(R.id.andrompd_play_button);
        if (null == mFAB) {
            return;
        }
        if (active) {
            mFAB.show();
        } else {
            mFAB.hide();
        }
        mFAB.setOnClickListener(listener);
    }

    @Override
    public void setupToolbar(String title, boolean scrollingEnabled, boolean drawerIndicatorEnabled, boolean showImage) {
        // set drawer state
        mDrawerToggle.setDrawerIndicatorEnabled(drawerIndicatorEnabled);

        ImageView collapsingImage = (ImageView) findViewById(R.id.collapsing_image);
        View collapsingImageGradientTop = findViewById(R.id.collapsing_image_gradient_top);
        View collapsingImageGradientBottom = findViewById(R.id.collapsing_image_gradient_bottom);
        if (collapsingImage != null && collapsingImageGradientTop != null && collapsingImageGradientBottom != null) {
            if (showImage) {
                collapsingImage.setVisibility(View.VISIBLE);
                collapsingImageGradientTop.setVisibility(View.VISIBLE);
                collapsingImageGradientBottom.setVisibility(View.VISIBLE);
            } else {
                collapsingImage.setVisibility(View.GONE);
                collapsingImageGradientTop.setVisibility(View.GONE);
                collapsingImageGradientBottom.setVisibility(View.GONE);
            }
        }
        // set scrolling behaviour
        CollapsingToolbarLayout toolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);

        // set title for both the activity and the collapsingToolbarlayout for both cases
        // where and image is shown and not.
        if (toolbar != null) {
            toolbar.setTitle(title);

            setTitle(title);


            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
            AppBarLayout layout = (AppBarLayout) findViewById(R.id.appbar);
            if (layout != null) {
                layout.setExpanded(true, false);
            }

            if (scrollingEnabled) {
                params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
            } else {
                params.setScrollFlags(0);
            }

            if (showImage && collapsingImage != null) {
                // Enable title of collapsingToolbarlayout for smooth transition
                toolbar.setTitleEnabled(true);
                setToolbarImage(getResources().getDrawable(R.drawable.cover_placeholder, null));
                params.setScrollFlags(params.getScrollFlags() | AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED);

                // Reset the previously added padding again.
                toolbar.setPadding(0, 0, 0, 0);
            } else {
                // Disable title for collapsingToolbarLayout and show normal title
                toolbar.setTitleEnabled(false);
                // Set the padding to match the statusbar height if a picture is shown.
                toolbar.setPadding(0, getStatusBarHeight(), 0, 0);
            }

        }
    }

    public void setupToolbarImage(Bitmap bm) {
        ImageView collapsingImage = (ImageView) findViewById(R.id.collapsing_image);
        if (collapsingImage != null) {
            collapsingImage.setImageBitmap(bm);
        }
    }


    /**
     * Method to retrieve the height of the statusbar to compensate in non-transparent cases.
     *
     * @return The Dimension of the statusbar. Used to compensate the padding.
     */
    private int getStatusBarHeight() {
        int resHeight = 0;
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            resHeight = getResources().getDimensionPixelSize(resId);
        }
        return resHeight;
    }


    public void setToolbarImage(Drawable drawable) {
        ImageView collapsingImage = (ImageView) findViewById(R.id.collapsing_image);
        if (collapsingImage != null) {
            collapsingImage.setImageDrawable(drawable);
        }
    }

    @Override
    public void openPath(String path) {
        // Create fragment and give it an argument for the selected directory
        FilesFragment newFragment = new FilesFragment();
        Bundle args = new Bundle();
        args.putString(FilesFragment.EXTRA_FILENAME, path);

        newFragment.setArguments(args);

        FragmentManager fragmentManager = getSupportFragmentManager();

        android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();

        newFragment.setEnterTransition(new Slide(GravityCompat.START));
        newFragment.setExitTransition(new Slide(GravityCompat.END));

        transaction.addToBackStack("FilesFragment" + path);
        transaction.replace(R.id.fragment_container, newFragment);

        // Commit the transaction
        transaction.commit();

    }

    @Override
    public void showAlbumsForPath(String path) {
        if (mNowPlayingDragStatus == DRAG_STATUS.DRAGGED_UP) {
            NowPlayingView nowPlayingView = (NowPlayingView) findViewById(R.id.now_playing_layout);
            if (nowPlayingView != null) {
                View coordinatorLayout = findViewById(R.id.main_coordinator_layout);
                coordinatorLayout.setVisibility(View.VISIBLE);
                nowPlayingView.minimize();
            }
        }
        // Create fragment and give it an argument for the selected article
        AlbumsFragment newFragment = new AlbumsFragment();
        Bundle args = new Bundle();
        args.putString(AlbumsFragment.BUNDLE_STRING_EXTRA_PATH, path);


        newFragment.setArguments(args);

        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        newFragment.setEnterTransition(new Slide(GravityCompat.START));
        newFragment.setExitTransition(new Slide(GravityCompat.END));        // Replace whatever is in the fragment_container view with this
        // fragment,
        // and add the transaction to the back stack so the user can navigate
        // back
        transaction.replace(R.id.fragment_container, newFragment, AlbumsFragment.TAG);
        transaction.addToBackStack("DirectoryAlbumsFragment");

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setCheckedItem(R.id.nav_library);

        // Commit the transaction
        transaction.commit();
    }

    @Override
    public void openArtworkSettings() {
        // Create fragment and give it an argument for the selected directory
        ArtworkSettingsFragment newFragment = new ArtworkSettingsFragment();


        FragmentManager fragmentManager = getSupportFragmentManager();

        android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();

        newFragment.setEnterTransition(new Slide(GravityCompat.START));
        newFragment.setExitTransition(new Slide(GravityCompat.END));

        transaction.addToBackStack("ArtworkSettingsFragment");
        transaction.replace(R.id.fragment_container, newFragment);

        // Commit the transaction
        transaction.commit();
    }

    private class ConnectionStateListener extends MPDConnectionStateChangeHandler {
        @Override
        public void onConnected() {
            Snackbar sb = Snackbar.make(findViewById(R.id.drawer_layout), getResources().getString(R.string.main_activity_connected), Snackbar.LENGTH_SHORT);
            sb.show();
        }

        @Override
        public void onDisconnected() {
            Snackbar sb = Snackbar.make(findViewById(R.id.drawer_layout), getResources().getString(R.string.main_activity_disconnected), Snackbar.LENGTH_SHORT);
            sb.show();
        }
    }


}
