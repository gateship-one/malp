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

package org.gateshipone.malp.application.listviewitems;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.utils.AsyncLoader;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDGenericItem;


/**
 * Class that can be used for all track type items (albumtracks, playlist tracks, etc)
 */
public class TrackListViewItem extends LinearLayout implements CoverLoadable{
    protected TextView mTitleView;
    protected TextView mSeparator;
    protected TextView mAdditionalInfoView;
    protected TextView mNumberView;
    protected TextView mDurationView;
    protected TextView mSectionHeader;
    protected ImageView mSectionImage;
    protected LinearLayout mSectionHeaderLayout;

    protected final ViewSwitcher mSwitcher;

    private AsyncLoader mLoaderTask;
    protected final AsyncLoader.CoverViewHolder mHolder;
    protected boolean mCoverDone = false;


    /**
     * Constructor with basic properties
     * @param context Context used for inflating the layout in this view.
     * @param number Tracknumber of this item
     * @param title Track title of this item
     * @param information Additional bottom line information of this item (e.g. Artistname - Albumname)
     * @param duration String of formatted duration of this track (eg.: 3:21 )
     */
    public TrackListViewItem(Context context, String number, String title, String information, String duration, boolean showIcon, String sectionTitle) {
        super(context);

        // Inflate the view with the given layout
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if ( sectionTitle != null && !sectionTitle.isEmpty()) {
            inflater.inflate(R.layout.listview_item_playlist_track, this, true);
            mSectionHeader = (TextView)findViewById(R.id.section_header_text);
            mSectionHeaderLayout = (LinearLayout)findViewById(R.id.section_header);
            mSectionImage = (ImageView)findViewById(R.id.section_header_image);
            mSwitcher = (ViewSwitcher)findViewById(R.id.section_header_image_switcher);
            setSectionHeader(sectionTitle);

            mHolder = new AsyncLoader.CoverViewHolder();
            mHolder.coverLoadable = this;
            mHolder.mAdapter = null;
            mHolder.imageDimension = new Pair<>(mSectionImage.getWidth(), mSectionImage.getHeight());
        } else {
            inflater.inflate(R.layout.listview_item_track, this, true);
            mHolder = null;
            mSwitcher = null;
        }


        mTitleView = (TextView)findViewById(R.id.track_title);
        mAdditionalInfoView = (TextView)findViewById(R.id.track_additional_information);
        mSeparator = (TextView)findViewById(R.id.track_separator);
        mDurationView = (TextView) findViewById(R.id.track_duration);
        mNumberView = (TextView) findViewById(R.id.track_number);

        // Call the functions to set the initial information
        setTitle(title);
        setTrackNumber(number);
        setAdditionalInformation(information);
        setDuration(duration);

        LinearLayout textLayout = (LinearLayout)findViewById(R.id.item_track_text_layout);

        ImageView imageView = (ImageView) findViewById(R.id.item_icon);
        if ( showIcon ) {
            imageView.setVisibility(VISIBLE);
            Drawable icon = context.getDrawable(R.drawable.ic_file_48dp);

            if (icon != null) {
                // get tint color
                int tintColor = ThemeUtils.getThemeColor(context, android.R.attr.textColor);
                // tint the icon
                DrawableCompat.setTint(icon, tintColor);
            }
            textLayout.setPadding(0,textLayout.getPaddingTop(),textLayout.getPaddingRight(),textLayout.getBottom());
            imageView.setImageDrawable(icon);
        } else {
            imageView.setVisibility(GONE);
        }
    }

    /**
     * Starts the image retrieval task
     */
    public void startCoverImageTask() {
        if (mLoaderTask == null && mHolder.artworkManager != null && mHolder.modelItem != null && !mCoverDone) {
            mLoaderTask = new AsyncLoader();
            mLoaderTask.execute(mHolder);
        }
    }


    public void prepareArtworkFetching(ArtworkManager artworkManager, MPDGenericItem modelItem) {
        if (!modelItem.equals(mHolder.modelItem) || !mCoverDone) {
            setImage(null);
        }
        mHolder.artworkManager = artworkManager;
        mHolder.modelItem = modelItem;
    }

    /**
     * Simple setter for the title (top line)
     * @param title Title to use
     */
    public void setTitle(String title) {
        mTitleView.setText(title);
    }

    /**
     * Sets the duration of a pre-formatted string (right side)
     * @param duration String of the length
     */
    public void setDuration(String duration) {
        mDurationView.setText(duration);
    }

    /**
     * Sets the track number of this item. (left side)
     * @param number Number of this track
     */
    public void setTrackNumber(String number) {
        mNumberView.setText(number);
    }

    public void setSectionHeader(String header) {
        if ( mSectionHeader != null ) {
            if ( header != null && !header.isEmpty()) {
                mSectionHeader.setText(header);
            } else {
                // Hide away old header
                mSectionHeaderLayout.setVisibility(GONE);
            }
        }
    }

    /**
     * Sets additional information for this track. (Bottom line for eg.: artistname - albumname)
     * @param information Information string (use R.string.track_item_separator) to separate information
     */
    public void setAdditionalInformation(String information) {
        mSeparator.setVisibility(VISIBLE);
        mAdditionalInfoView.setText(information);
    }

    public boolean isSectionView() {
        return mSectionHeaderLayout != null;
    }

    /**
     * If this ListItem gets detached from the parent it makes no sense to let
     * the task for image retrieval running. (non-Javadoc)
     *
     * @see android.view.View#onDetachedFromWindow()
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mLoaderTask != null) {
            mLoaderTask.cancel(true);
            mLoaderTask = null;
        }
    }

    @Override
    public void setImage(Bitmap image) {
        if (null != image) {
            mCoverDone = true;

            mSectionImage.setImageBitmap(image);
            mSwitcher.setDisplayedChild(1);
        } else {
            // Cancel old task
            if (mLoaderTask != null) {
                mLoaderTask.cancel(true);
            }
            mLoaderTask = null;

            mCoverDone = false;
            mSwitcher.setOutAnimation(null);
            mSwitcher.setInAnimation(null);
            mSectionImage.setImageDrawable(null);
            mSwitcher.setDisplayedChild(0);
            mSwitcher.setOutAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
            mSwitcher.setInAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
        }
    }
}
