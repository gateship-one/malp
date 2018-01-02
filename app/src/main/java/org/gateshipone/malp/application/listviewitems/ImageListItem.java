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

package org.gateshipone.malp.application.listviewitems;


import android.content.Context;
import android.view.LayoutInflater;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.ScrollSpeedAdapter;

public class ImageListItem extends AbsImageListViewItem {

    TextView mMainView;
    TextView mDetailsView;


    public ImageListItem(Context context, String text, String details, ScrollSpeedAdapter adapter) {
        super(context, R.layout.listview_item_image, R.id.item_image, R.id.item_image_viewswitcher, adapter);

        mMainView = (TextView)findViewById(R.id.item_text);
        mDetailsView = (TextView)findViewById(R.id.item_details);

        if ( details == null || details.isEmpty() ) {
            mDetailsView.setVisibility(GONE);
        } else {
            mDetailsView.setText(details);
        }

        mMainView.setText(text);

    }

    public void setText(String text) {
        mMainView.setText(text);
    }

    public void setDetails(String text) {
        if (null != mDetailsView && !text.isEmpty()) {
            mDetailsView.setText(text);
            mDetailsView.setVisibility(VISIBLE);
        } else if ( null != mDetailsView ) {
            mDetailsView.setText("");
            mDetailsView.setVisibility(GONE);
        }
    }

}
