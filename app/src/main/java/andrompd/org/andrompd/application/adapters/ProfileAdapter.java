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

package andrompd.org.andrompd.application.adapters;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import andrompd.org.andrompd.application.listviewitems.ProfileListItem;
import andrompd.org.andrompd.mpdservice.profilemanagement.MPDServerProfile;

public class ProfileAdapter extends GenericSectionAdapter<MPDServerProfile> {
    private Context mContext;

    public ProfileAdapter(Context context) {
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MPDServerProfile profile = mModelData.get(position);

        // title
        String profileName = profile.getProfileName();

        // number of tracks
        int port = profile.getPort();
        String portString = String.valueOf(port);

        String hostname = profile.getHostname();

        if (convertView != null) {
            ProfileListItem bookmarksListViewItem = (ProfileListItem) convertView;

            bookmarksListViewItem.setProfileName(profileName);
            bookmarksListViewItem.setHostname(hostname);
            bookmarksListViewItem.setPort(portString);
        } else {
            convertView = new ProfileListItem(mContext, profileName, hostname, portString);
        }

        return convertView;
    }
}
