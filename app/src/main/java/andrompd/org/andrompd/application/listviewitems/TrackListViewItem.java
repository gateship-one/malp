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

package andrompd.org.andrompd.application.listviewitems;


import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import andrompd.org.andrompd.R;


/**
 * Class that can be used for all track type items (albumtracks, playlist tracks, etc)
 */
public class TrackListViewItem extends LinearLayout {

    /**
     * Constructor with basic properties
     * @param context Context used for inflating the layout in this view.
     * @param number Tracknumber of this item
     * @param title Track title of this item
     * @param information Additional bottom line information of this item (e.g. Artistname - Albumname)
     * @param duration String of formatted duration of this track (eg.: 3:21 )
     */
    public TrackListViewItem(Context context, String number, String title, String information, String duration) {
        super(context);

        // Inflate the view with the given layout
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_track, this, true);

        // Call the functions to set the initial information
        setTitle(title);
        setTrackNumber(number);
        setAdditionalInformation(information);
        setDuration(duration);
    }

    /**
     * Simple setter for the title (top line)
     * @param title Title to use
     */
    public void setTitle(String title) {
        TextView textView = (TextView)findViewById(R.id.track_title);
        textView.setText(title);
    }

    /**
     * Sets the duration of a pre-formatted string (right side)
     * @param duration String of the length
     */
    public void setDuration(String duration) {
        TextView textView = (TextView)findViewById(R.id.track_duration);
        textView.setText(duration);
    }

    /**
     * Sets the track number of this item. (left side)
     * @param number Number of this track
     */
    public void setTrackNumber(String number) {
        TextView textView = (TextView)findViewById(R.id.track_number);
        textView.setText(number);
    }

    /**
     * Sets additional information for this track. (Bottom line for eg.: artistname - albumname)
     * @param information Information string (use R.string.track_item_separator) to separate information
     */
    public void setAdditionalInformation(String information) {
        TextView textView = (TextView)findViewById(R.id.track_additional_information);
        textView.setText(information);
    }
}
