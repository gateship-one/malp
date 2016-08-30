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
import android.view.LayoutInflater;

import android.widget.LinearLayout;
import android.widget.TextView;

import org.gateshipone.malp.R;

public class SimpleListItem extends LinearLayout {

    TextView mMainView;

    public SimpleListItem(Context context, String text) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_simple, this, true);

        mMainView = (TextView) findViewById(R.id.item_text);
        mMainView.setText(text);

    }

    public void setText(String text) {
        mMainView.setText(text);
    }

}
