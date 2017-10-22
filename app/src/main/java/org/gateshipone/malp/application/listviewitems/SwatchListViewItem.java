/*
 *  Copyright (C) 2017 Team Gateship-One
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
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

import org.gateshipone.malp.R;

public class SwatchListViewItem extends LinearLayout {
    public SwatchListViewItem(Context context, Palette.Swatch swatch) {
        super(context);


        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.swatch_test_item, this, true);

        findViewById(R.id.swatch_rgb).setBackgroundColor(swatch.getRgb());
        findViewById(R.id.swatch_item).setBackgroundColor(swatch.getTitleTextColor());
        findViewById(R.id.swatch_body).setBackgroundColor(swatch.getBodyTextColor());

    }
}
