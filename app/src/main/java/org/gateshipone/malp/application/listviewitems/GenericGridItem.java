/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
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
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.GenericSectionAdapter;
import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.utils.AsyncLoader;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDGenericItem;

public class GenericGridItem extends AbsImageListViewItem {

    protected final TextView mTitleView;



    public GenericGridItem(Context context, String labelText, GenericSectionAdapter adapter) {
        super(context,R.layout.gridview_item, R.id.item_artists_cover_image, R.id.item_grid_viewswitcher, adapter);

        mTitleView = (TextView) findViewById(R.id.item_grid_text);

        mTitleView.setText(labelText);
    }


    /*
    * Sets the title for the GridItem
     */
    public void setTitle(String text) {
        mTitleView.setText(text);
    }
}
