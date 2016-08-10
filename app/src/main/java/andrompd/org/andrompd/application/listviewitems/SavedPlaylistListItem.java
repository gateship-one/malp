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
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import andrompd.org.andrompd.R;

public class SavedPlaylistListItem extends LinearLayout {
    TextView mPlaylistNameView;
    TextView mLastModifiedView;

    public SavedPlaylistListItem(Context context, String playlistname, String lastmodified) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_saved_playlist, this, true);

        mPlaylistNameView = (TextView) findViewById(R.id.item_saved_playlist_name);
        mPlaylistNameView.setText(playlistname);

        mLastModifiedView = (TextView) findViewById(R.id.item_saved_playlist_last_modified);
        mLastModifiedView.setText(lastmodified);

    }

    public void setPlaylistName(String playlistName) {
        mPlaylistNameView.setText(playlistName);
    }

    public void setLastModified(String lastModified) {
        mLastModifiedView.setText(lastModified);
    }
}
