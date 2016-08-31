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
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDDirectory;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDPlaylist;

public class GenericFileListItem extends LinearLayout {
    TextView mNameView;
    TextView mLastModifiedView;

    public GenericFileListItem(Context context, MPDFileEntry file, boolean showIcon) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_generic_file_entry, this, true);

        String path = file.getPath();
        String[] pathSplit = path.split("/");
        if ( pathSplit.length > 0 ) {
            path = pathSplit[pathSplit.length - 1];
        }

        mNameView = (TextView) findViewById(R.id.item_filename);
        mNameView.setText(path);

        mLastModifiedView = (TextView) findViewById(R.id.item_last_modified);
        if ( null != file.getLastModified() && !file.getLastModified().isEmpty()) {
            mLastModifiedView.setVisibility(VISIBLE);
            mLastModifiedView.setText(file.getLastModified());
        } else {
            mLastModifiedView.setVisibility(GONE);
        }

        LinearLayout textLayout = (LinearLayout)findViewById(R.id.item_text_layout);

        ImageView imageView = (ImageView) findViewById(R.id.item_icon);
        if ( showIcon ) {
            imageView.setVisibility(VISIBLE);
            Drawable icon;
            if ( file instanceof MPDPlaylist ) {
                icon = context.getDrawable(R.drawable.ic_queue_music_black_48dp);
            } else if ( file instanceof MPDDirectory ) {
                icon = context.getDrawable(R.drawable.ic_folder_48dp);
            } else {
                icon = context.getDrawable(R.drawable.ic_file_48dp);
            }

            if (icon != null) {
                // get tint color
                int tintColor = ThemeUtils.getThemeColor(context, android.R.attr.textColor);
                // tint the icon
                DrawableCompat.setTint(icon, tintColor);
            }
            imageView.setImageDrawable(icon);
            textLayout.setPadding(0,textLayout.getPaddingTop(),textLayout.getPaddingRight(),textLayout.getBottom());
        } else {
            imageView.setVisibility(GONE);
        }

    }

}
