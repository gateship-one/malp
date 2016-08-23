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
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.utils.ThemeUtils;


/**
 * Class that can be used for all track type items (albumtracks, playlist tracks, etc)
 */
public class TrackListViewItem extends LinearLayout {
    protected TextView mTitleView;
    protected TextView mSeparator;
    protected TextView mAdditionalInfoView;
    protected TextView mNumberView;
    protected TextView mDurationView;

    /**
     * Constructor with basic properties
     * @param context Context used for inflating the layout in this view.
     * @param number Tracknumber of this item
     * @param title Track title of this item
     * @param information Additional bottom line information of this item (e.g. Artistname - Albumname)
     * @param duration String of formatted duration of this track (eg.: 3:21 )
     */
    public TrackListViewItem(Context context, String number, String title, String information, String duration, boolean showIcon) {
        super(context);

        // Inflate the view with the given layout
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_track, this, true);

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

    /**
     * Sets additional information for this track. (Bottom line for eg.: artistname - albumname)
     * @param information Information string (use R.string.track_item_separator) to separate information
     */
    public void setAdditionalInformation(String information) {
        mAdditionalInfoView.setText(information);
    }
}
