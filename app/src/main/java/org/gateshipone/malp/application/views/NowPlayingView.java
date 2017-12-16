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

package org.gateshipone.malp.application.views;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;


import org.gateshipone.malp.R;
import org.gateshipone.malp.application.activities.FanartActivity;
import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.background.BackgroundService;
import org.gateshipone.malp.application.background.BackgroundServiceConnection;
import org.gateshipone.malp.application.callbacks.OnSaveDialogListener;
import org.gateshipone.malp.application.callbacks.TextDialogCallback;
import org.gateshipone.malp.application.fragments.TextDialog;
import org.gateshipone.malp.application.fragments.serverfragments.ChoosePlaylistDialog;
import org.gateshipone.malp.application.utils.CoverBitmapLoader;
import org.gateshipone.malp.application.utils.FormatHelper;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.application.utils.VolumeButtonLongClickListener;
import org.gateshipone.malp.mpdservice.ConnectionManager;
import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.MPDStatusChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseOutputList;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDOutput;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class NowPlayingView extends RelativeLayout implements PopupMenu.OnMenuItemClickListener, ArtworkManager.onNewAlbumImageListener, ArtworkManager.onNewArtistImageListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = NowPlayingView.class.getSimpleName();

    private final ViewDragHelper mDragHelper;

    private ServerStatusListener mStateListener;

    private ServerConnectionListener mConnectionStateListener;

    /**
     * Upper view part which is dragged up & down
     */
    private View mHeaderView;

    /**
     * Main view of draggable part
     */
    private View mMainView;

    private LinearLayout mDraggedUpButtons;
    private LinearLayout mDraggedDownButtons;

    /**
     * Absolute pixel position of upper layout bound
     */
    private int mTopPosition;

    /**
     * relative dragposition
     */
    private float mDragOffset;

    /**
     * Height of non-draggable part.
     * (Layout height - draggable part)
     */
    private int mDragRange;

    /**
     * Flag whether the views switches between album cover and artist image
     */
    private boolean mShowArtistImage = false;

    private BackgroundService.STREAMING_STATUS mStreamingStatus;

    /**
     * Main cover imageview
     */
    private AlbumArtistView mCoverImage;

    /**
     * Small cover image, part of the draggable header
     */
    private ImageView mTopCoverImage;

    /**
     * View that contains the playlist ListVIew
     */
    private CurrentPlaylistView mPlaylistView;

    /**
     * ViewSwitcher used for switching between the main cover image and the playlist
     */
    private ViewSwitcher mViewSwitcher;

    /**
     * Asynchronous loader for coverimages for TrackItems.
     */
    private CoverBitmapLoader mCoverLoader = null;

    /**
     * Observer for information about the state of the draggable part of this view.
     * This is probably the Activity of which this view is part of.
     * (Used for smooth statusbar transition and state resuming)
     */
    private NowPlayingDragStatusReceiver mDragStatusReceiver = null;

    private StreamingStatusReceiver mStreamingStatusReceiver;

    private BackgroundServiceConnection mBackgroundServiceConnection;

    /**
     * Top buttons in the draggable header part.
     */
    private ImageButton mTopPlayPauseButton;
    private ImageButton mTopPlaylistButton;
    private ImageButton mTopMenuButton;

    /**
     * Buttons in the bottom part of the view
     */
    private ImageButton mBottomRepeatButton;
    private ImageButton mBottomPreviousButton;
    private ImageButton mBottomPlayPauseButton;
    private ImageButton mBottomStopButton;
    private ImageButton mBottomNextButton;
    private ImageButton mBottomRandomButton;

    /**
     * Seekbar used for seeking and informing the user of the current playback position.
     */
    private SeekBar mPositionSeekbar;

    /**
     * Seekbar used for volume control of host
     */
    private SeekBar mVolumeSeekbar;
    private ImageView mVolumeIcon;
    private ImageView mVolumeIconButtons;

    private TextView mVolumeText;

    private ImageButton mVolumeMinus;
    private ImageButton mVolumePlus;

    private LinearLayout mHeaderTextLayout;

    private LinearLayout mVolumeSeekbarLayout;
    private LinearLayout mVolumeButtonLayout;

    /**
     * Various textviews for track information
     */
    private TextView mTrackName;
    private TextView mTrackAdditionalInfo;
    private TextView mElapsedTime;
    private TextView mDuration;

    private TextView mTrackNo;
    private TextView mPlaylistNo;
    private TextView mBitrate;
    private TextView mAudioProperties;
    private TextView mTrackURI;


    private MPDCurrentStatus mLastStatus;
    private MPDTrack mLastTrack;

    private boolean mUseEnglishWikipedia;

    public NowPlayingView(Context context) {
        this(context, null, 0);
    }

    public NowPlayingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NowPlayingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDragHelper = ViewDragHelper.create(this, 1f, new BottomDragCallbackHelper());
        mStateListener = new ServerStatusListener();
        mConnectionStateListener = new ServerConnectionListener();
        mLastStatus = new MPDCurrentStatus();
        mLastTrack = new MPDTrack("");
    }

    /**
     * Maximizes this view with an animation.
     */
    public void maximize() {
        smoothSlideTo(0f);
    }

    /**
     * Minimizes the view with an animation.
     */
    public void minimize() {
        smoothSlideTo(1f);
    }

    /**
     * Slides the view to the given position.
     *
     * @param slideOffset 0.0 - 1.0 (0.0 is dragged down, 1.0 is dragged up)
     * @return If the move was successful
     */
    boolean smoothSlideTo(float slideOffset) {
        final int topBound = getPaddingTop();
        int y = (int) (topBound + slideOffset * mDragRange);

        if (mDragHelper.smoothSlideViewTo(mHeaderView, mHeaderView.getLeft(), y)) {
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }


    /**
     * Set the position of the draggable view to the given offset. This is done without an animation.
     * Can be used to resume a certain state of the view (e.g. on resuming an activity)
     *
     * @param offset Offset to position the view to from 0.0 - 1.0 (0.0 dragged up, 1.0 dragged down)
     */
    public void setDragOffset(float offset) {
        if (offset > 1.0f || offset < 0.0f) {
            mDragOffset = 1.0f;
        }
        mDragOffset = offset;

        invalidate();
        requestLayout();


        // Set inverse alpha values for smooth layout transition.
        // Visibility still needs to be set otherwise parts of the buttons
        // are not clickable.
        mDraggedDownButtons.setAlpha(mDragOffset);
        mDraggedUpButtons.setAlpha(1.0f - mDragOffset);

        // Calculate the margin to smoothly resize text field
        LayoutParams layoutParams = (LayoutParams) mHeaderTextLayout.getLayoutParams();
        layoutParams.setMarginEnd((int) (mTopPlaylistButton.getWidth() * (1.0 - mDragOffset)));
        mHeaderTextLayout.setLayoutParams(layoutParams);

        // Notify the observers about the change
        if (mDragStatusReceiver != null) {
            mDragStatusReceiver.onDragPositionChanged(offset);
        }

        if (mDragOffset == 0.0f) {
            // top
            mDraggedDownButtons.setVisibility(INVISIBLE);
            mDraggedUpButtons.setVisibility(VISIBLE);
            mCoverImage.setVisibility(VISIBLE);
            if (mDragStatusReceiver != null) {
                mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_UP);
            }
        } else {
            // bottom
            mDraggedDownButtons.setVisibility(VISIBLE);
            mDraggedUpButtons.setVisibility(INVISIBLE);
            mCoverImage.setVisibility(INVISIBLE);
            if (mDragStatusReceiver != null) {
                mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_DOWN);
            }
        }
    }

    /**
     * Menu click listener. This method gets called when the user selects an item of the popup menu (right top corner).
     *
     * @param item MenuItem that was clicked.
     * @return Returns true if the item was handled by this method. False otherwise.
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_playlist:
                final AlertDialog.Builder removeListBuilder = new AlertDialog.Builder(getContext());
                removeListBuilder.setTitle(getContext().getString(R.string.action_delete_playlist));
                removeListBuilder.setMessage(getContext().getString(R.string.dialog_message_delete_current_playlist));
                removeListBuilder.setPositiveButton(R.string.dialog_action_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MPDQueryHandler.clearPlaylist();
                    }
                });
                removeListBuilder.setNegativeButton(R.string.dialog_action_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                removeListBuilder.create().show();
                break;
            case R.id.action_shuffle_playlist:
                MPDQueryHandler.shufflePlaylist();
                return true;
            case R.id.action_save_playlist:
                OnSaveDialogListener plDialogCallback = new OnSaveDialogListener() {
                    @Override
                    public void onSaveObject(final String title) {
                        AlertDialog.Builder overWriteBuilder = new AlertDialog.Builder(getContext());
                        overWriteBuilder.setTitle(getContext().getString(R.string.action_overwrite_playlist));
                        overWriteBuilder.setMessage(getContext().getString(R.string.dialog_message_overwrite_playlist) + ' ' + title + '?');
                        overWriteBuilder.setPositiveButton(R.string.dialog_action_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MPDQueryHandler.removePlaylist(title);
                                MPDQueryHandler.savePlaylist(title);
                            }
                        });
                        overWriteBuilder.setNegativeButton(R.string.dialog_action_no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                        overWriteBuilder.create().show();

                    }

                    @Override
                    public void onCreateNewObject() {
                        // open dialog in order to save the current playlist as a playlist in the mediastore
                        TextDialog textDialog = new TextDialog();
                        Bundle args = new Bundle();
                        args.putString(TextDialog.EXTRA_DIALOG_TITLE, getResources().getString(R.string.dialog_save_playlist));
                        args.putString(TextDialog.EXTRA_DIALOG_TEXT, getResources().getString(R.string.default_playlist_title));

                        textDialog.setCallback(new TextDialogCallback() {
                            @Override
                            public void onFinished(String text) {
                                MPDQueryHandler.savePlaylist(text);
                            }
                        });
                        textDialog.setArguments(args);
                        textDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "SavePLTextDialog");
                    }
                };

                // open dialog in order to save the current playlist as a playlist in the mediastore
                ChoosePlaylistDialog choosePlaylistDialog = new ChoosePlaylistDialog();
                Bundle args = new Bundle();
                args.putBoolean(ChoosePlaylistDialog.EXTRA_SHOW_NEW_ENTRY, true);

                choosePlaylistDialog.setCallback(plDialogCallback);
                choosePlaylistDialog.setArguments(args);
                choosePlaylistDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "ChoosePlaylistDialog");
                break;
            case R.id.action_add_url:
                TextDialog addURLDialog = new TextDialog();
                addURLDialog.setCallback(new TextDialogCallback() {
                    @Override
                    public void onFinished(String text) {
                        MPDQueryHandler.addPath(text);
                    }
                });
                Bundle textDialogArgs = new Bundle();
                textDialogArgs.putString(TextDialog.EXTRA_DIALOG_TEXT, "http://...");
                textDialogArgs.putString(TextDialog.EXTRA_DIALOG_TITLE, getResources().getString(R.string.action_add_url));
                addURLDialog.setArguments(textDialogArgs);
                addURLDialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "AddURLDialog");
                break;
            case R.id.action_jump_to_current:
                mPlaylistView.jumpToCurrentSong();
                break;
            case R.id.action_toggle_single_mode:
                if (null != mLastStatus) {
                    if (mLastStatus.getSinglePlayback() == 0) {
                        MPDCommandHandler.setSingle(true);
                    } else {
                        MPDCommandHandler.setSingle(false);
                    }
                }
                break;
            case R.id.action_toggle_consume_mode:
                if (null != mLastStatus) {
                    if (mLastStatus.getConsume() == 0) {
                        MPDCommandHandler.setConsume(true);
                    } else {
                        MPDCommandHandler.setConsume(false);
                    }
                }
                break;
            case R.id.action_open_fanart:
                Intent intent = new Intent(getContext(), FanartActivity.class);
                getContext().startActivity(intent);
                return true;
            case R.id.action_wikipedia_album:
                Intent albumIntent = new Intent(Intent.ACTION_VIEW);
                //albumIntent.setData(Uri.parse("https://" + Locale.getDefault().getLanguage() + ".wikipedia.org/wiki/index.php?search=" + mLastTrack.getTrackAlbum() + "&title=Special:Search&go=Go"));
                if (mUseEnglishWikipedia) {
                    albumIntent.setData(Uri.parse("https://en.wikipedia.org/wiki/" + mLastTrack.getTrackAlbum()));
                } else {
                    albumIntent.setData(Uri.parse("https://" + Locale.getDefault().getLanguage() + ".wikipedia.org/wiki/" + mLastTrack.getTrackAlbum()));
                }
                getContext().startActivity(albumIntent);
                return true;
            case R.id.action_wikipedia_artist:
                Intent artistIntent = new Intent(Intent.ACTION_VIEW);
                //artistIntent.setData(Uri.parse("https://" + Locale.getDefault().getLanguage() + ".wikipedia.org/wiki/index.php?search=" + mLastTrack.getTrackAlbumArtist() + "&title=Special:Search&go=Go"));
                if (mUseEnglishWikipedia) {
                    artistIntent.setData(Uri.parse("https://en.wikipedia.org/wiki/" + mLastTrack.getTrackArtist()));
                } else {
                    artistIntent.setData(Uri.parse("https://" + Locale.getDefault().getLanguage() + ".wikipedia.org/wiki/" + mLastTrack.getTrackArtist()));
                }
                getContext().startActivity(artistIntent);
                return true;
            case R.id.action_start_streaming: {
                if (mStreamingStatus == BackgroundService.STREAMING_STATUS.PLAYING || mStreamingStatus == BackgroundService.STREAMING_STATUS.BUFFERING) {
                    try {
                        mBackgroundServiceConnection.getService().stopStreamingPlayback();
                    } catch (RemoteException e) {

                    }
                } else {
                    try {
                        mBackgroundServiceConnection.getService().startStreamingPlayback();
                    } catch (RemoteException e) {

                    }
                }
                return true;
            }
            default:
                return false;
        }
        return false;
    }


    @Override
    public void newAlbumImage(MPDAlbum album) {
        if (mLastTrack.getTrackAlbum().equals(album.getName())) {
            mCoverLoader.getImage(mLastTrack, true);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getContext().getString(R.string.pref_volume_controls_key))) {
            setVolumeControlSetting();
        } else if (key.equals(getContext().getString(R.string.pref_use_english_wikipedia_key))) {
            mUseEnglishWikipedia = sharedPreferences.getBoolean(key, getContext().getResources().getBoolean(R.bool.pref_use_english_wikipedia_default));
        } else if (key.equals(getContext().getString(R.string.pref_show_npv_artist_image_key))) {
            mShowArtistImage = sharedPreferences.getBoolean(key, getContext().getResources().getBoolean(R.bool.pref_show_npv_artist_image_default));

            // Show artist image if artwork is requested
            if (mShowArtistImage) {
                mCoverLoader.getArtistImage(mLastTrack, true);
            } else {
                // Hide artist image
                mCoverImage.clearArtistImage();
            }
        }
    }


    private void setVolumeControlSetting() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String volumeControlView = sharedPref.getString(getContext().getString(R.string.pref_volume_controls_key), getContext().getString(R.string.pref_volume_control_view_default));

        LinearLayout volLayout = (LinearLayout) findViewById(R.id.volume_control_layout);

        if (volumeControlView.equals(getContext().getString(R.string.pref_volume_control_view_off_key))) {
            if (volLayout != null) {
                volLayout.setVisibility(GONE);
            }
            mVolumeSeekbarLayout.setVisibility(GONE);
            mVolumeButtonLayout.setVisibility(GONE);
        } else if (volumeControlView.equals(getContext().getString(R.string.pref_volume_control_view_seekbar_key))) {
            if (volLayout != null) {
                volLayout.setVisibility(VISIBLE);
            }
            mVolumeSeekbarLayout.setVisibility(VISIBLE);
            mVolumeButtonLayout.setVisibility(GONE);
        } else if (volumeControlView.equals(getContext().getString(R.string.pref_volume_control_view_buttons_key))) {
            if (volLayout != null) {
                volLayout.setVisibility(VISIBLE);
            }
            mVolumeSeekbarLayout.setVisibility(GONE);
            mVolumeButtonLayout.setVisibility(VISIBLE);
        }
    }

    @Override
    public void newArtistImage(MPDArtist artist) {
        if (mShowArtistImage && mLastTrack.getTrackArtist().equals(artist.getArtistName())) {
            mCoverLoader.getArtistImage(artist, false);
        }
    }

    /**
     * Observer class for changes of the drag status.
     */
    private class BottomDragCallbackHelper extends ViewDragHelper.Callback {

        /**
         * Checks if a given child view should act as part of the drag. This is only true for the header
         * element of this View-class.
         *
         * @param child     Child that was touched by the user
         * @param pointerId Id of the pointer used for touching the view.
         * @return True if the view should be allowed to be used as dragging part, false otheriwse.
         */
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mHeaderView;
        }

        /**
         * Called if the position of the draggable view is changed. This rerequests the layout of the view.
         *
         * @param changedView The view that was changed.
         * @param left        Left position of the view (should stay constant in this case)
         * @param top         Top position of the view
         * @param dx          Dimension of the width
         * @param dy          Dimension of the height
         */
        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            // Save the heighest top position of this view.
            mTopPosition = top;

            // Calculate the new drag offset
            mDragOffset = (float) top / mDragRange;

            // Relayout this view
            requestLayout();

            // Set inverse alpha values for smooth layout transition.
            // Visibility still needs to be set otherwise parts of the buttons
            // are not clickable.
            mDraggedDownButtons.setAlpha(mDragOffset);
            mDraggedUpButtons.setAlpha(1.0f - mDragOffset);

            // Calculate the margin to smoothly resize text field
            LayoutParams layoutParams = (LayoutParams) mHeaderTextLayout.getLayoutParams();
            layoutParams.setMarginEnd((int) (mTopPlaylistButton.getWidth() * (1.0 - mDragOffset)));
            mHeaderTextLayout.setLayoutParams(layoutParams);

            if (mDragStatusReceiver != null) {
                mDragStatusReceiver.onDragPositionChanged(mDragOffset);
            }

        }

        /**
         * Called if the user lifts the finger(release the view) with a velocity
         *
         * @param releasedChild View that was released
         * @param xvel          x position of the view
         * @param yvel          y position of the view
         */
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int top = getPaddingTop();
            if (yvel > 0 || (yvel == 0 && mDragOffset > 0.5f)) {
                top += mDragRange;
            }
            // Snap the view to top/bottom position
            mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), top);
            invalidate();
        }

        /**
         * Returns the range within a view is allowed to be dragged.
         *
         * @param child Child to get the dragrange for
         * @return Dragging range
         */
        @Override
        public int getViewVerticalDragRange(View child) {
            return mDragRange;
        }


        /**
         * Clamps (limits) the view during dragging to the top or bottom(plus header height)
         *
         * @param child Child that is being dragged
         * @param top   Top position of the dragged view
         * @param dy    Delta value of the height
         * @return The limited height value (or valid position inside the clamped range).
         */
        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            final int topBound = getPaddingTop();
            int bottomBound = getHeight() - mHeaderView.getHeight() - mHeaderView.getPaddingBottom();

            final int newTop = Math.min(Math.max(top, topBound), bottomBound);

            return newTop;
        }

        /**
         * Called when the drag state changed. Informs observers that it is either dragged up or down.
         * Also sets the visibility of button groups in the header
         *
         * @param state New drag state
         */
        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);

            // Check if the new state is the idle state. If then notify the observer (if one is registered)
            if (state == ViewDragHelper.STATE_IDLE) {
                // Enable scrolling of the text views
                mTrackName.setSelected(true);
                mTrackAdditionalInfo.setSelected(true);

                if (mDragOffset == 0.0f) {
                    // Called when dragged up
                    mDraggedDownButtons.setVisibility(INVISIBLE);
                    mDraggedUpButtons.setVisibility(VISIBLE);
                    if (mDragStatusReceiver != null) {
                        mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_UP);
                    }
                } else {
                    // Called when dragged down
                    mDraggedDownButtons.setVisibility(VISIBLE);
                    mDraggedUpButtons.setVisibility(INVISIBLE);
                    mCoverImage.setVisibility(INVISIBLE);
                    if (mDragStatusReceiver != null) {
                        mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_DOWN);
                    }

                }
            } else if (state == ViewDragHelper.STATE_DRAGGING) {
                /*
                 * Show both layouts to enable a smooth transition via
                 * alpha values of the layouts.
                 */
                mDraggedDownButtons.setVisibility(VISIBLE);
                mDraggedUpButtons.setVisibility(VISIBLE);
                mCoverImage.setVisibility(VISIBLE);
                // report the change of the view
                if (mDragStatusReceiver != null) {
                    // Disable scrolling of the text views
                    mTrackName.setSelected(false);
                    mTrackAdditionalInfo.setSelected(false);

                    mDragStatusReceiver.onStartDrag();

                    if (mViewSwitcher.getCurrentView() == mPlaylistView && mDragOffset == 1.0f) {
                        mPlaylistView.jumpToCurrentSong();
                    }
                }

            }
        }
    }

    /**
     * Informs the dragHelper about a scroll movement.
     */
    @Override
    public void computeScroll() {
        // Continues the movement of the View Drag Helper and sets the invalidation for this View
        // if the animation is not finished and needs continuation
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * Handles touch inputs to some views, to make sure, the ViewDragHelper is called.
     *
     * @param ev Touch input event
     * @return True if handled by this view or false otherwise
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Call the drag helper
        mDragHelper.processTouchEvent(ev);

        // Get the position of the new touch event
        final float x = ev.getX();
        final float y = ev.getY();

        // Check if the position lies in the bounding box of the header view (which is draggable)
        boolean isHeaderViewUnder = mDragHelper.isViewUnder(mHeaderView, (int) x, (int) y);

        // Check if drag is handled by the helper, or the header or mainview. If not notify the system that input is not yet handled.
        return isHeaderViewUnder && isViewHit(mHeaderView, (int) x, (int) y) || isViewHit(mMainView, (int) x, (int) y);
    }


    /**
     * Checks if an input to coordinates lay within a View
     *
     * @param view View to check with
     * @param x    x value of the input
     * @param y    y value of the input
     * @return
     */
    private boolean isViewHit(View view, int x, int y) {
        int[] viewLocation = new int[2];
        view.getLocationOnScreen(viewLocation);
        int[] parentLocation = new int[2];
        this.getLocationOnScreen(parentLocation);
        int screenX = parentLocation[0] + x;
        int screenY = parentLocation[1] + y;
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + view.getWidth() &&
                screenY >= viewLocation[1] && screenY < viewLocation[1] + view.getHeight();
    }

    /**
     * Asks the ViewGroup about the size of all its children and paddings around.
     *
     * @param widthMeasureSpec  The width requirements for this view
     * @param heightMeasureSpec The height requirements for this view
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // FIXME check why super.onMeasure(widthMeasureSpec, heightMeasureSpec); causes
        // problems with scrolling header view.
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));

        ViewGroup.LayoutParams imageParams = mCoverImage.getLayoutParams();
        imageParams.height = mViewSwitcher.getHeight();
        mCoverImage.setLayoutParams(imageParams);
        mCoverImage.requestLayout();


        // Calculate the margin to smoothly resize text field
        LayoutParams layoutParams = (LayoutParams) mHeaderTextLayout.getLayoutParams();
        layoutParams.setMarginEnd((int) (mTopPlaylistButton.getMeasuredHeight() * (1.0 - mDragOffset)));
        mHeaderTextLayout.setLayoutParams(layoutParams);
    }


    /**
     * Called after the layout inflater is finished.
     * Sets all global view variables to the ones inflated.
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Get both main views (header and bottom part)
        mHeaderView = findViewById(R.id.now_playing_headerLayout);
        mMainView = findViewById(R.id.now_playing_bodyLayout);

        // header buttons
        mTopPlayPauseButton = (ImageButton) findViewById(R.id.now_playing_topPlayPauseButton);
        mTopPlaylistButton = (ImageButton) findViewById(R.id.now_playing_topPlaylistButton);
        mTopMenuButton = (ImageButton) findViewById(R.id.now_playing_topMenuButton);

        // bottom buttons
        mBottomRepeatButton = (ImageButton) findViewById(R.id.now_playing_bottomRepeatButton);
        mBottomPreviousButton = (ImageButton) findViewById(R.id.now_playing_bottomPreviousButton);
        mBottomPlayPauseButton = (ImageButton) findViewById(R.id.now_playing_bottomPlayPauseButton);
        mBottomStopButton = (ImageButton) findViewById(R.id.now_playing_bottomStopButton);
        mBottomNextButton = (ImageButton) findViewById(R.id.now_playing_bottomNextButton);
        mBottomRandomButton = (ImageButton) findViewById(R.id.now_playing_bottomRandomButton);

        // Main cover image
        mCoverImage = (AlbumArtistView) findViewById(R.id.now_playing_cover);
        // Small header cover image
        mTopCoverImage = (ImageView) findViewById(R.id.now_playing_topCover);

        // View with the ListView of the playlist
        mPlaylistView = (CurrentPlaylistView) findViewById(R.id.now_playing_playlist);

        // view switcher for cover and playlist view
        mViewSwitcher = (ViewSwitcher) findViewById(R.id.now_playing_view_switcher);

        // Button container for the buttons shown if dragged up
        mDraggedUpButtons = (LinearLayout) findViewById(R.id.now_playing_layout_dragged_up);
        // Button container for the buttons shown if dragged down
        mDraggedDownButtons = (LinearLayout) findViewById(R.id.now_playing_layout_dragged_down);

        // textviews
        mTrackName = (TextView) findViewById(R.id.now_playing_trackName);
        // For marquee scrolling the TextView need selected == true
        mTrackName.setSelected(true);
        mTrackAdditionalInfo = (TextView) findViewById(R.id.now_playing_track_additional_info);
        // For marquee scrolling the TextView need selected == true
        mTrackAdditionalInfo.setSelected(true);

        mTrackNo = (TextView) findViewById(R.id.now_playing_text_track_no);
        mPlaylistNo = (TextView) findViewById(R.id.now_playing_text_playlist_no);
        mBitrate = (TextView) findViewById(R.id.now_playing_text_bitrate);
        mAudioProperties = (TextView) findViewById(R.id.now_playing_text_audio_properties);
        mTrackURI = (TextView) findViewById(R.id.now_playing_text_track_uri);

        // Textviews directly under the seekbar
        mElapsedTime = (TextView) findViewById(R.id.now_playing_elapsedTime);
        mDuration = (TextView) findViewById(R.id.now_playing_duration);

        mHeaderTextLayout = (LinearLayout) findViewById(R.id.now_playing_header_textLayout);

        // seekbar (position)
        mPositionSeekbar = (SeekBar) findViewById(R.id.now_playing_seekBar);
        mPositionSeekbar.setOnSeekBarChangeListener(new PositionSeekbarListener());

        mVolumeSeekbar = (SeekBar) findViewById(R.id.volume_seekbar);
        mVolumeIcon = (ImageView) findViewById(R.id.volume_icon);
        mVolumeIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MPDCommandHandler.setVolume(0);
            }
        });

        mVolumeIcon.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(final View view) {

                MPDQueryHandler.getOutputs(new OutputResponseMenuHandler(NowPlayingView.this, view));

                return true;
            }
        });

        mVolumeSeekbar.setMax(100);
        mVolumeSeekbar.setOnSeekBarChangeListener(new VolumeSeekBarListener());


        /* Volume control buttons */
        mVolumeIconButtons = (ImageView) findViewById(R.id.volume_icon_buttons);
        mVolumeIconButtons.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MPDCommandHandler.setVolume(0);
            }
        });

        mVolumeText = (TextView) findViewById(R.id.volume_button_text);

        mVolumeMinus = (ImageButton) findViewById(R.id.volume_button_minus);

        mVolumeMinus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MPDCommandHandler.decreaseVolume();
            }
        });

        mVolumePlus = (ImageButton) findViewById(R.id.volume_button_plus);
        mVolumePlus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                MPDCommandHandler.increaseVolume();
            }
        });

        /* Create two listeners that start a repeating timer task to repeat the volume plus/minus action */
        VolumeButtonLongClickListener plusListener = new VolumeButtonLongClickListener(VolumeButtonLongClickListener.LISTENER_ACTION.VOLUME_UP);
        VolumeButtonLongClickListener minusListener = new VolumeButtonLongClickListener(VolumeButtonLongClickListener.LISTENER_ACTION.VOLUME_DOWN);

        /* Set the listener to the plus/minus button */
        mVolumeMinus.setOnLongClickListener(minusListener);
        mVolumeMinus.setOnTouchListener(minusListener);

        mVolumePlus.setOnLongClickListener(plusListener);
        mVolumePlus.setOnTouchListener(plusListener);

        mVolumeSeekbarLayout = (LinearLayout) findViewById(R.id.volume_seekbar_layout);
        mVolumeButtonLayout = (LinearLayout) findViewById(R.id.volume_button_layout);

        // set dragging part default to bottom
        mDragOffset = 1.0f;
        mDraggedUpButtons.setVisibility(INVISIBLE);
        mDraggedDownButtons.setVisibility(VISIBLE);
        mDraggedUpButtons.setAlpha(0.0f);

        // add listener to top playpause button
        mTopPlayPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                MPDCommandHandler.togglePause();
            }
        });

        // Add listeners to top playlist button
        mTopPlaylistButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // get color for playlist button
                int color;
                if (mViewSwitcher.getCurrentView() != mPlaylistView) {
                    color = ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent);
                } else {
                    color = ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_accent);
                }

                // tint the button
                mTopPlaylistButton.setImageTintList(ColorStateList.valueOf(color));

                // toggle between cover and playlistview
                mViewSwitcher.showNext();

                // report the change of the view
                if (mDragStatusReceiver != null) {
                    // set view status
                    if (mViewSwitcher.getDisplayedChild() == 0) {
                        // cover image is shown
                        mDragStatusReceiver.onSwitchedViews(NowPlayingDragStatusReceiver.VIEW_SWITCHER_STATUS.COVER_VIEW);
                    } else {
                        // playlist view is shown
                        mDragStatusReceiver.onSwitchedViews(NowPlayingDragStatusReceiver.VIEW_SWITCHER_STATUS.PLAYLIST_VIEW);
                        mPlaylistView.jumpToCurrentSong();
                    }
                }
            }
        });

        // Add listener to top menu button
        mTopMenuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdditionalOptionsMenu(v);
            }
        });

        // Add listener to bottom repeat button
        mBottomRepeatButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (null != mLastStatus) {
                    if (mLastStatus.getRepeat() == 0) {
                        MPDCommandHandler.setRepeat(true);
                    } else {
                        MPDCommandHandler.setRepeat(false);
                    }
                }
            }
        });

        // Add listener to bottom previous button
        mBottomPreviousButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                MPDCommandHandler.previousSong();

            }
        });

        // Add listener to bottom playpause button
        mBottomPlayPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                MPDCommandHandler.togglePause();
            }
        });

        mBottomStopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MPDCommandHandler.stop();
            }
        });

        // Add listener to bottom next button
        mBottomNextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                MPDCommandHandler.nextSong();
            }
        });

        // Add listener to bottom random button
        mBottomRandomButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (null != mLastStatus) {
                    if (mLastStatus.getRandom() == 0) {
                        MPDCommandHandler.setRandom(true);
                    } else {
                        MPDCommandHandler.setRandom(false);
                    }
                }
            }
        });

        mCoverImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), FanartActivity.class);
                getContext().startActivity(intent);
            }
        });
        mCoverImage.setVisibility(INVISIBLE);

        mCoverLoader = new CoverBitmapLoader(getContext(), new CoverReceiverClass());
    }

    /**
     * Called to open the popup menu on the top right corner.
     *
     * @param v
     */
    private void showAdditionalOptionsMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(getContext(), v);
        // Inflate the menu from a menu xml file
        popupMenu.inflate(R.menu.popup_menu_nowplaying);
        // Set the main NowPlayingView as a listener (directly implements callback)
        popupMenu.setOnMenuItemClickListener(this);
        // Real menu
        Menu menu = popupMenu.getMenu();

        // Set the checked menu item state if a MPDCurrentStatus is available
        if (null != mLastStatus) {
            MenuItem singlePlaybackItem = menu.findItem(R.id.action_toggle_single_mode);
            singlePlaybackItem.setChecked(mLastStatus.getSinglePlayback() == 1);

            MenuItem consumeItem = menu.findItem(R.id.action_toggle_consume_mode);
            consumeItem.setChecked(mLastStatus.getConsume() == 1);
        }

        // Check if the current view is the cover or the playlist. If it is the playlist hide its actions.
        // If the viewswitcher only has one child the dual pane layout is used
        if (mViewSwitcher.getDisplayedChild() == 0 && (mViewSwitcher.getChildCount() > 1)) {
            menu.setGroupEnabled(R.id.group_playlist_actions, false);
            menu.setGroupVisible(R.id.group_playlist_actions, false);
        }

        // Check if streaming is configured for the current server
        boolean streamingEnabled = ConnectionManager.getInstance(getContext().getApplicationContext()).getStreamingEnabled();
        MenuItem streamingStartStopItem = menu.findItem(R.id.action_start_streaming);

        if (!streamingEnabled) {
            streamingStartStopItem.setVisible(false);
        } else {
            if (mStreamingStatus == BackgroundService.STREAMING_STATUS.PLAYING || mStreamingStatus == BackgroundService.STREAMING_STATUS.BUFFERING) {
                streamingStartStopItem.setTitle(getResources().getString(R.string.action_stop_streaming));
            } else {
                streamingStartStopItem.setTitle(getResources().getString(R.string.action_start_streaming));
            }
        }

        // Open the menu itself
        popupMenu.show();
    }


    /**
     * Called when a layout is requested from the graphics system.
     *
     * @param changed If the layout is changed (size, ...)
     * @param l       Left position
     * @param t       Top position
     * @param r       Right position
     * @param b       Bottom position
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Calculate the maximal range that the view is allowed to be dragged
        mDragRange = (getMeasuredHeight() - mHeaderView.getMeasuredHeight());

        // New temporary top position, to fix the view at top or bottom later if state is idle.
        int newTop = mTopPosition;

        // fix height at top or bottom if state idle
        if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
            newTop = (int) (mDragRange * mDragOffset);
        }

        // Request the upper part of the NowPlayingView (header)
        mHeaderView.layout(
                0,
                newTop,
                r,
                newTop + mHeaderView.getMeasuredHeight());

        // Request the lower part of the NowPlayingView (main part)
        mMainView.layout(
                0,
                newTop + mHeaderView.getMeasuredHeight(),
                r,
                newTop + b);
    }

    /**
     * Stop the refresh timer when the view is not visible to the user anymore.
     * Unregister the receiver for NowPlayingInformation intends, not needed anylonger.
     */
    public void onPause() {
        // Unregister listener
        MPDStateMonitoringHandler.unregisterStatusListener(mStateListener);
        MPDStateMonitoringHandler.unregisterConnectionStateListener(mConnectionStateListener);
        mPlaylistView.onPause();

        if (null != mBackgroundServiceConnection) {
            mBackgroundServiceConnection.closeConnection();
            mBackgroundServiceConnection = null;
        }

        getContext().getApplicationContext().unregisterReceiver(mStreamingStatusReceiver);

        ArtworkManager.getInstance(getContext().getApplicationContext()).unregisterOnNewAlbumImageListener(this);
        ArtworkManager.getInstance(getContext().getApplicationContext()).unregisterOnNewArtistImageListener(this);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Resumes refreshing operation because the view is visible to the user again.
     * Also registers to the NowPlayingInformation intends again.
     */
    public void onResume() {

        // get the playbackservice, when the connection is successfully established the timer gets restarted

        // Reenable scrolling views after resuming
        if (mTrackName != null) {
            mTrackName.setSelected(true);
        }

        if (mTrackAdditionalInfo != null) {
            mTrackAdditionalInfo.setSelected(true);
        }

        if (mStreamingStatusReceiver == null) {
            mStreamingStatusReceiver = new StreamingStatusReceiver();
        }

        if (null == mBackgroundServiceConnection) {
            mBackgroundServiceConnection = new BackgroundServiceConnection(getContext().getApplicationContext(), new BackgroundServiceConnectionListener());
        }
        mBackgroundServiceConnection.openConnection();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BackgroundService.ACTION_STREAMING_STATUS_CHANGED);
        getContext().getApplicationContext().registerReceiver(mStreamingStatusReceiver, filter);

        // Register with MPDStateMonitoring system
        MPDStateMonitoringHandler.registerStatusListener(mStateListener);
        MPDStateMonitoringHandler.registerConnectionStateListener(mConnectionStateListener);

        mPlaylistView.onResume();
        ArtworkManager.getInstance(getContext().getApplicationContext()).registerOnNewAlbumImageListener(this);
        ArtworkManager.getInstance(getContext().getApplicationContext()).registerOnNewArtistImageListener(this);


        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        setVolumeControlSetting();

        mUseEnglishWikipedia = sharedPref.getBoolean(getContext().getString(R.string.pref_use_english_wikipedia_key), getContext().getResources().getBoolean(R.bool.pref_use_english_wikipedia_default));

        mShowArtistImage = sharedPref.getBoolean(getContext().getString(R.string.pref_show_npv_artist_image_key), getContext().getResources().getBoolean(R.bool.pref_show_npv_artist_image_default));
    }


    private void updateMPDStatus(MPDCurrentStatus status) {
        MPDCurrentStatus.MPD_PLAYBACK_STATE state = status.getPlaybackState();

        // update play buttons
        switch (state) {
            case MPD_PLAYING:
                mTopPlayPauseButton.setImageResource(R.drawable.ic_pause_48dp);
                mBottomPlayPauseButton.setImageResource(R.drawable.ic_pause_circle_fill_48dp);


                break;
            case MPD_PAUSING:
            case MPD_STOPPED:
                mTopPlayPauseButton.setImageResource(R.drawable.ic_play_arrow_48dp);
                mBottomPlayPauseButton.setImageResource(R.drawable.ic_play_circle_fill_48dp);


                break;
        }

        // update repeat button
        // FIXME with single playback
        switch (status.getRepeat()) {
            case 0:
                mBottomRepeatButton.setImageResource(R.drawable.ic_repeat_24dp);
                mBottomRepeatButton.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_accent)));
                break;
            case 1:
                mBottomRepeatButton.setImageResource(R.drawable.ic_repeat_24dp);
                mBottomRepeatButton.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(getContext(), android.R.attr.colorAccent)));
                break;
        }

        // update random button
        switch (status.getRandom()) {
            case 0:
                mBottomRandomButton.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_accent)));
                break;
            case 1:
                mBottomRandomButton.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(getContext(), android.R.attr.colorAccent)));
                break;
        }

        // Update position seekbar & textviews
        mPositionSeekbar.setMax(status.getTrackLength());
        mPositionSeekbar.setProgress(status.getElapsedTime());

        mElapsedTime.setText(FormatHelper.formatTracktimeFromS(status.getElapsedTime()));
        mDuration.setText(FormatHelper.formatTracktimeFromS(status.getTrackLength()));

        // Update volume seekbar
        int volume = status.getVolume();
        mVolumeSeekbar.setProgress(volume);

        if (volume >= 70) {
            mVolumeIcon.setImageResource(R.drawable.ic_volume_high_black_48dp);
            mVolumeIconButtons.setImageResource(R.drawable.ic_volume_high_black_48dp);
        } else if (volume >= 30 && volume < 70) {
            mVolumeIcon.setImageResource(R.drawable.ic_volume_medium_black_48dp);
            mVolumeIconButtons.setImageResource(R.drawable.ic_volume_medium_black_48dp);
        } else if (volume > 0 && volume < 30) {
            mVolumeIcon.setImageResource(R.drawable.ic_volume_low_black_48dp);
            mVolumeIconButtons.setImageResource(R.drawable.ic_volume_low_black_48dp);
        } else {
            mVolumeIcon.setImageResource(R.drawable.ic_volume_mute_black_48dp);
            mVolumeIconButtons.setImageResource(R.drawable.ic_volume_mute_black_48dp);
        }
        mVolumeIcon.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_accent)));
        mVolumeIconButtons.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_accent)));

        mVolumeText.setText(String.valueOf(volume) + '%');

        mPlaylistNo.setText(String.valueOf(status.getCurrentSongIndex() + 1) + getResources().getString(R.string.track_number_album_count_separator) +
                String.valueOf(status.getPlaylistLength()));

        mLastStatus = status;

        mBitrate.setText(status.getBitrate() + getResources().getString(R.string.bitrate_unit_kilo_bits));

        // Set audio properties string
        String properties = status.getSamplerate() + getResources().getString(R.string.samplerate_unit_hertz) + ' ';

        // Check for fancy new formats here (dsd, float = f)
        String sampleFormat = status.getBitDepth();

        // 16bit is the most probable sample format
        if (sampleFormat.equals("16") || sampleFormat.equals("24")  || sampleFormat.equals("8") || sampleFormat.equals("32")) {
            properties += sampleFormat + getResources().getString(R.string.bitcount_unit) + ' ';
        } else if (sampleFormat.equals("f")) {
            properties += "float ";
        } else {
            properties += sampleFormat + ' ';
        }


        properties += status.getChannelCount() + getResources().getString(R.string.channel_count_unit);
        mAudioProperties.setText(properties);
    }

    private void updateMPDCurrentTrack(MPDTrack track) {
        // Check if track title is set, otherwise use track name, otherwise path
        String title;
        if(!(title = track.getTrackTitle()).isEmpty()) {

        } else if (!(title = track.getTrackName()).isEmpty()) {

        } else if (!track.getPath().isEmpty()) {
            title = FormatHelper.getFilenameFromPath(track.getPath());
        } else {
            title = "";
        }
        mTrackName.setText(title);


        if (!track.getTrackArtist().isEmpty() && !track.getTrackAlbum().isEmpty()) {
            mTrackAdditionalInfo.setText(track.getTrackArtist() + getResources().getString(R.string.track_item_separator) + track.getTrackAlbum());
        } else if (track.getTrackArtist().isEmpty() && !track.getTrackAlbum().isEmpty()) {
            mTrackAdditionalInfo.setText(track.getTrackAlbum());
        } else if (track.getTrackAlbum().isEmpty() && !track.getTrackArtist().isEmpty()) {
            mTrackAdditionalInfo.setText(track.getTrackArtist());
        } else {
            mTrackAdditionalInfo.setText(track.getPath());
        }

        if (null == mLastTrack || !track.getTrackAlbum().equals(mLastTrack.getTrackAlbum())) {
            // get tint color
            int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_background_primary);

            Drawable drawable = getResources().getDrawable(R.drawable.cover_placeholder, null);
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, tintColor);

            // Show the placeholder image until the cover fetch process finishes
            mCoverImage.clearAlbumImage();

            tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_accent);

            drawable = getResources().getDrawable(R.drawable.cover_placeholder_128dp, null);
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, tintColor);


            // The same for the small header image
            mTopCoverImage.setImageDrawable(drawable);
            // Start the cover loader
            mCoverLoader.getImage(track, true);
        }

        if (mShowArtistImage && (null == mLastTrack || !track.getTrackArtist().equals(mLastTrack.getTrackArtist()))) {
            mCoverImage.clearArtistImage();

            mCoverLoader.getArtistImage(track, true);
        }

        // Calculate the margin to avoid cut off textviews
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mHeaderTextLayout.getLayoutParams();
        layoutParams.setMarginEnd((int) (mTopPlaylistButton.getWidth() * (1.0 - mDragOffset)));
        mHeaderTextLayout.setLayoutParams(layoutParams);

        mTrackURI.setText(track.getPath());
        if (track.getAlbumTrackCount() != 0) {
            mTrackNo.setText(String.valueOf(track.getTrackNumber()) + getResources().getString(R.string.track_number_album_count_separator) +
                    String.valueOf(track.getAlbumTrackCount()));
        } else {
            mTrackNo.setText(String.valueOf(track.getTrackNumber()));
        }

        mLastTrack = track;

    }


    /**
     * Can be used to register an observer to this view, that is notified when a change of the dragstatus,offset happens.
     *
     * @param receiver Observer to register, only one observer at a time is possible.
     */
    public void registerDragStatusReceiver(NowPlayingDragStatusReceiver receiver) {
        mDragStatusReceiver = receiver;
        // Initial status notification
        if (mDragStatusReceiver != null) {

            // set drag status
            if (mDragOffset == 0.0f) {
                // top
                mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_UP);
            } else {
                // bottom
                mDragStatusReceiver.onStatusChanged(NowPlayingDragStatusReceiver.DRAG_STATUS.DRAGGED_DOWN);
            }

            // set view status
            if (mViewSwitcher.getDisplayedChild() == 0) {
                // cover image is shown
                mDragStatusReceiver.onSwitchedViews(NowPlayingDragStatusReceiver.VIEW_SWITCHER_STATUS.COVER_VIEW);
            } else {
                // playlist view is shown
                mDragStatusReceiver.onSwitchedViews(NowPlayingDragStatusReceiver.VIEW_SWITCHER_STATUS.PLAYLIST_VIEW);
            }
        }
    }


    /**
     * Set the viewswitcher of cover/playlist view to the requested state.
     *
     * @param view the view which should be displayed.
     */
    public void setViewSwitcherStatus(NowPlayingDragStatusReceiver.VIEW_SWITCHER_STATUS view) {
        int color = 0;

        switch (view) {
            case COVER_VIEW:
                // change the view only if the requested view is not displayed
                mViewSwitcher.setDisplayedChild(0);
                color = ThemeUtils.getThemeColor(getContext(), android.R.attr.textColor);
                break;
            case PLAYLIST_VIEW:
                // change the view only if the requested view is not displayed
                mViewSwitcher.setDisplayedChild(1);
                color = ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent);
                break;
        }

        // tint the button according to the requested view
        mTopPlaylistButton.setImageTintList(ColorStateList.valueOf(color));
    }


    /**
     * Public interface used by observers to be notified about a change in drag state or drag position.
     */
    public interface NowPlayingDragStatusReceiver {
        // Possible values for DRAG_STATUS (up,down)
        enum DRAG_STATUS {
            DRAGGED_UP, DRAGGED_DOWN
        }

        // Possible values for the view in the viewswitcher (cover, playlist)
        enum VIEW_SWITCHER_STATUS {
            COVER_VIEW, PLAYLIST_VIEW
        }

        // Called when the whole view is either completely dragged up or down
        void onStatusChanged(DRAG_STATUS status);

        // Called continuously during dragging.
        void onDragPositionChanged(float pos);

        // Called when the view switcher switches between cover and playlist view
        void onSwitchedViews(VIEW_SWITCHER_STATUS view);

        // Called when the user starts the drag
        void onStartDrag();
    }

    private class ServerStatusListener extends MPDStatusChangeHandler {

        @Override
        protected void onNewStatusReady(MPDCurrentStatus status) {
            updateMPDStatus(status);
        }

        @Override
        protected void onNewTrackReady(MPDTrack track) {
            updateMPDCurrentTrack(track);
        }
    }

    private class ServerConnectionListener extends MPDConnectionStateChangeHandler {

        @Override
        public void onConnected() {
            updateMPDStatus(MPDStateMonitoringHandler.getLastStatus());
        }

        @Override
        public void onDisconnected() {
            updateMPDStatus(new MPDCurrentStatus());
            updateMPDCurrentTrack(new MPDTrack(""));
        }
    }

    private class PositionSeekbarListener implements SeekBar.OnSeekBarChangeListener {
        /**
         * Called if the user drags the seekbar to a new position or the seekbar is altered from
         * outside. Just do some seeking, if the action is done by the user.
         *
         * @param seekBar  Seekbar of which the progress was changed.
         * @param progress The new position of the seekbar.
         * @param fromUser If the action was initiated by the user.
         */
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                // FIXME Check if it is better to just update if user releases the seekbar
                // (network stress)
                MPDCommandHandler.seekSeconds(progress);
            }
        }

        /**
         * Called if the user starts moving the seekbar. We do not handle this for now.
         *
         * @param seekBar SeekBar that is used for dragging.
         */
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
        }

        /**
         * Called if the user ends moving the seekbar. We do not handle this for now.
         *
         * @param seekBar SeekBar that is used for dragging.
         */
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
        }
    }

    private class VolumeSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        /**
         * Called if the user drags the seekbar to a new position or the seekbar is altered from
         * outside. Just do some seeking, if the action is done by the user.
         *
         * @param seekBar  Seekbar of which the progress was changed.
         * @param progress The new position of the seekbar.
         * @param fromUser If the action was initiated by the user.
         */
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                MPDCommandHandler.setVolume(progress);

                if (progress >= 70) {
                    mVolumeIcon.setImageResource(R.drawable.ic_volume_high_black_48dp);
                } else if (progress >= 30 && progress < 70) {
                    mVolumeIcon.setImageResource(R.drawable.ic_volume_medium_black_48dp);
                } else if (progress > 0 && progress < 30) {
                    mVolumeIcon.setImageResource(R.drawable.ic_volume_low_black_48dp);
                } else {
                    mVolumeIcon.setImageResource(R.drawable.ic_volume_mute_black_48dp);
                }
            }
        }

        /**
         * Called if the user starts moving the seekbar. We do not handle this for now.
         *
         * @param seekBar SeekBar that is used for dragging.
         */
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
        }

        /**
         * Called if the user ends moving the seekbar. We do not handle this for now.
         *
         * @param seekBar SeekBar that is used for dragging.
         */
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * Private class that handles when the CoverGenerator finishes its fetching of cover images.
     */
    private class CoverReceiverClass implements CoverBitmapLoader.CoverBitmapListener {

        /**
         * Called when a bitmap is created
         *
         * @param bm Bitmap ready for use in the UI
         */
        @Override
        public void receiveBitmap(final Bitmap bm, final CoverBitmapLoader.IMAGE_TYPE type) {
            if (bm != null) {
                Activity activity = (Activity) getContext();
                if (activity != null) {
                    // Run on the UI thread of the activity because we are modifying gui elements.
                    activity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (type == CoverBitmapLoader.IMAGE_TYPE.ALBUM_IMAGE) {
                                // Set the main cover image
                                mCoverImage.setAlbumImage(bm);
                                // Set the small header image
                                mTopCoverImage.setImageBitmap(bm);
                            } else if (type == CoverBitmapLoader.IMAGE_TYPE.ARTIST_IMAGE) {
                                mCoverImage.setArtistImage(bm);
                            }
                        }
                    });
                }
            }
        }
    }

    /**
     * Receives stream playback status updates. When stream playback is started the status
     * is necessary to show the right menu item.
     */
    private class StreamingStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BackgroundService.ACTION_STREAMING_STATUS_CHANGED)) {
                mStreamingStatus = BackgroundService.STREAMING_STATUS.values()[intent.getIntExtra(BackgroundService.INTENT_EXTRA_STREAMING_STATUS, 0)];
            }
        }
    }

    /**
     * Private class to handle when a {@link android.content.ServiceConnection} to the {@link BackgroundService}
     * is established. When the connection is established, the stream playback status is retrieved.
     */
    private class BackgroundServiceConnectionListener implements BackgroundServiceConnection.OnConnectionStatusChangedListener {

        @Override
        public void onConnected() {
            try {
                mStreamingStatus = BackgroundService.STREAMING_STATUS.values()[mBackgroundServiceConnection.getService().getStreamingStatus()];
            } catch (RemoteException e) {

            }
        }

        @Override
        public void onDisconnected() {

        }
    }

    private static class OutputResponseMenuHandler extends MPDResponseOutputList {

        private WeakReference<NowPlayingView> mNPV;

        private WeakReference<View> mView;

        public OutputResponseMenuHandler(NowPlayingView npv, View view) {
            mNPV = new WeakReference<NowPlayingView>(npv);
            mView = new WeakReference<View>(view);
        }

        @Override
        public void handleOutputs(final List<MPDOutput> outputList) {
            // we need at least 2 output plugins configured
            if (outputList != null && outputList.size() > 1) {
                PopupMenu popup = new PopupMenu((AppCompatActivity)mNPV.get().getContext(), mView.get());
                Menu menu = popup.getMenu();
                SubMenu menuSwitch =  menu.addSubMenu(R.string.action_switch_to_output);
                SubMenu menuToggle = menu.addSubMenu(R.string.action_toggle_outputs);

                int menuId = 0;
                for (final MPDOutput output : outputList) {
                    MenuItem subMenuItem = menuToggle.add(0, menuId, 0, output.getOutputName())
                            .setCheckable(true)
                            .setChecked(output.getOutputState());

                    subMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            MPDOutput out = outputList.get(item.getItemId());

                            if (out.getOutputState()) {
                                MPDCommandHandler.disableOutput(out.getID());
                            } else {
                                MPDCommandHandler.enableOutput(out.getID());
                            }
                            out.setOutputState(!out.getOutputState());

                            item.setChecked(out.getOutputState());
                            // Keep the popup menu open
                            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                            item.setActionView(new View(mNPV.get().getContext()));
                            item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                                @Override
                                public boolean onMenuItemActionExpand(MenuItem item) {
                                    return false;
                                }

                                @Override
                                public boolean onMenuItemActionCollapse(MenuItem item) {
                                    return false;
                                }
                            });
                            return false;
                        }
                    });

                    subMenuItem = menuSwitch.add(0, menuId, 0, output.getOutputName());
                    subMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            MPDOutput selectedOut = outputList.get(item.getItemId());

                            // first enable the selected output so we have always an active ones
                            MPDCommandHandler.enableOutput(selectedOut.getID());
                            selectedOut.setOutputState(true);

                            for(MPDOutput current: outputList) {
                                if (current != selectedOut) {
                                    MPDCommandHandler.disableOutput(current.getID());
                                    current.setOutputState(false);
                                }
                            }
                            return false;
                        }
                    });
                    menuId++;
                }
                popup.show();
            } else {
                // Only one output, show toggle menu
                PopupMenu popup = new PopupMenu((AppCompatActivity)mNPV.get().getContext(), mView.get());

                Menu menu = popup.getMenu();

                for (final MPDOutput output : outputList) {
                    MenuItem subMenuItem = menu.add(0, 0, 0, output.getOutputName())
                            .setCheckable(true)
                            .setChecked(output.getOutputState());

                    subMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            MPDOutput out = outputList.get(item.getItemId());

                            if (out.getOutputState() == true) {
                                MPDCommandHandler.disableOutput(out.getID());
                            } else {
                                MPDCommandHandler.enableOutput(out.getID());
                            }
                            out.setOutputState(!out.getOutputState());

                            item.setChecked(out.getOutputState());
                            // Keep the popup menu open
                            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                            item.setActionView(new View(mNPV.get().getContext()));
                            item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                                @Override
                                public boolean onMenuItemActionExpand(MenuItem item) {
                                    return false;
                                }

                                @Override
                                public boolean onMenuItemActionCollapse(MenuItem item) {
                                    return false;
                                }
                            });
                            return false;
                        }
                    });

                }
                popup.show();
            }
        }
    }
}
