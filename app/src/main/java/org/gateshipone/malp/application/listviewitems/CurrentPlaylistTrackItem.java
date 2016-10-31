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
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.utils.ThemeUtils;

public class CurrentPlaylistTrackItem extends TrackListViewItem {

    public CurrentPlaylistTrackItem(Context context) {
        super(context,"","","","",false);

        mTitleView.setText(getResources().getText(R.string.track_item_loading));

        // Hide separator
        mSeparator.setVisibility(GONE);
    }


    /**
     * Constructor that already sets the values for each view.
     */
    public CurrentPlaylistTrackItem(Context context, String number, String title, String information, String duration) {
        super(context, number, title, information, duration, false);

    }

    /**
     * Method that tint the title, number and separator view according to the state.
     * @param state flag indicates if the representing track is currently marked as played by the playbackservice
     */
    public void setPlaying(boolean state) {
        if(state) {
            int color = ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent);
            mTitleView.setTextColor(color);
            mNumberView.setTextColor(color);
            mSeparator.setTextColor(color);
        } else {
            int color = ThemeUtils.getThemeColor(getContext(),R.attr.malp_color_text_background_primary);
            mTitleView.setTextColor(color);
            mNumberView.setTextColor(color);
            mSeparator.setTextColor(color);
        }

    }
}
