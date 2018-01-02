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
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import org.gateshipone.malp.R;

public class ProfileListItem extends LinearLayout {
    TextView mProfileNameView;
    TextView mHostnameView;
    TextView mPortView;

    RadioButton mRadioButton;

    public ProfileListItem(Context context, String profilename, String hostname, String port, boolean checked) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.listview_item_profile, this, true);

        mProfileNameView = (TextView) findViewById(R.id.item_profile_name);
        mProfileNameView.setText(profilename);

        mHostnameView = (TextView) findViewById(R.id.item_profile_hostname);
        mHostnameView.setText(hostname);

        mPortView = (TextView) findViewById(R.id.item_profile_port);
        mPortView.setText(port);

        mRadioButton = (RadioButton)findViewById(R.id.item_profile_radiobtn);
        mRadioButton.setChecked(checked);
    }

    public void setProfileName(String profilename) {
        mProfileNameView.setText(profilename);
    }

    public void setHostname(String hostname) {
        mHostnameView.setText(hostname);
    }

    public void setPort(String port) {
        mPortView.setText(port);
    }

    public void setChecked(boolean checked) {
        mRadioButton.setChecked(checked);
    }
}
