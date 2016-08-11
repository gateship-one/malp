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
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.utils.ThemeUtils;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDDirectory;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDFileEntry;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDPlaylist;

public class GenericFileListItem extends LinearLayout {
    TextView mNameView;
    TextView mLastModifiedView;

    public GenericFileListItem(Context context, MPDFileEntry file, boolean showIcon) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_generic_file_entry, this, true);

        mNameView = (TextView) findViewById(R.id.item_filename);
        mNameView.setText(file.getPath());

        mLastModifiedView = (TextView) findViewById(R.id.item_last_modified);
        mLastModifiedView.setText(file.getLastModified());



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
        } else {
            imageView.setVisibility(GONE);
        }

    }

}
