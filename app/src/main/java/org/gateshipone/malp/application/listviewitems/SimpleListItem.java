/*
 * Copyright (C) 2016 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gateshipone.malp.application.listviewitems;


import android.content.Context;
import android.view.LayoutInflater;

import android.widget.LinearLayout;
import android.widget.TextView;

import org.gateshipone.malp.R;

public class SimpleListItem extends LinearLayout {

    TextView mMainView;
    TextView mDetailsView;

    public SimpleListItem(Context context, String text, String details) {
        super(context);
        if (null == details) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.listview_item_simple, this, true);
        } else {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.listview_item_details, this, true);

            mDetailsView = (TextView) findViewById(R.id.item_details);
            if ( !details.isEmpty()) {
                mDetailsView.setText(details);
            } else {
                mDetailsView.setVisibility(GONE);
            }
        }

        mMainView = (TextView) findViewById(R.id.item_text);
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
