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

package andrompd.org.andrompd.application.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.adapters.TracksAdapter;

public class EditProfileFragment extends Fragment {
    public static final String EXTRA_PROFILENAME = "profilename";
    public static final String EXTRA_HOSTNAME = "hostname";
    public static final String EXTRA_PASSWORD = "password";
    public static final String EXTRA_PORT = "port";


    private String mProfilename;
    private String mHostname;
    private String mPassword;
    private int mPort;


    private EditText mProfilenameView;
    private EditText mHostnameView;
    private EditText mPasswordView;
    private EditText mPortView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        /* Check if an artistname/albumame was given in the extras */
        Bundle args = getArguments();
        if (null != args) {
            mProfilename = args.getString(EXTRA_PROFILENAME);
            mHostname = args.getString(EXTRA_HOSTNAME);
            mPassword = args.getString(EXTRA_PASSWORD);
            mPort = args.getInt(EXTRA_PORT);
        }

        mProfilenameView = (EditText) rootView.findViewById(R.id.fragment_profile_profilename);
        mHostnameView = (EditText) rootView.findViewById(R.id.fragment_profile_hostname);
        mPasswordView = (EditText) rootView.findViewById(R.id.fragment_profile_password);
        mPortView = (EditText) rootView.findViewById(R.id.fragment_profile_port);

        mProfilenameView.setText(mProfilename);
        mHostnameView.setText(mHostname);
        mPasswordView.setText(mPassword);
        mPortView.setText(String.valueOf(mPort));


        // Return the ready inflated and configured fragment view.
        return rootView;
    }
}
