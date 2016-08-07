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
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.utils.ThemeUtils;

public class CurrentPlaylistTrackItem extends TrackListViewItem {



    /**
     * Constructor that already sets the values for each view.
     */
    public CurrentPlaylistTrackItem(Context context, String number, String title, String information, String duration) {
        super(context, number, title, information, duration);

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
            int color = ContextCompat.getColor(getContext(), R.color.colorTextLight);
            mTitleView.setTextColor(color);
            mNumberView.setTextColor(color);
            mSeparator.setTextColor(color);
        }

    }
}
