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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.gateshipone.malp.R;

public class ArtistGridViewItem extends GenericGridItem {
    public ArtistGridViewItem(Context context, String title, String imageURL, ViewGroup.LayoutParams layoutParams) {
        super(context, imageURL, layoutParams);

        mTitleView.setText(title);
    }

    @Override
    TextView provideTitleView() {
        return (TextView) this.findViewById(R.id.item_artists_title);
    }

    @Override
    ImageView provideImageView() {
        return (ImageView) this.findViewById(R.id.item_artists_cover_image);
    }

    @Override
    ViewSwitcher provideViewSwitcher() {
        return (ViewSwitcher) this.findViewById(R.id.item_artists_view_switcher);
    }

    @Override
    int provideLayout() {
        return R.layout.gridview_item_artist;
    }
}
