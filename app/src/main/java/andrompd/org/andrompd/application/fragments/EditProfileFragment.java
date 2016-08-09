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
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.adapters.TracksAdapter;
import andrompd.org.andrompd.mpdservice.profilemanagement.MPDProfileManager;

public class EditProfileFragment extends Fragment {
    private static final String TAG = "EditProfileFragment";
    public static final String EXTRA_PROFILENAME = "profilename";
    public static final String EXTRA_HOSTNAME = "hostname";
    public static final String EXTRA_PASSWORD = "password";
    public static final String EXTRA_PORT = "port";
    public static final String EXTRA_AUTOCONNECT = "autoconnect";
    

    private String mProfilename;
    private String mHostname;
    private String mPassword;
    private int mPort;
    private boolean mAutoconnect;


    private TextInputEditText mProfilenameView;
    private TextInputEditText mHostnameView;
    private TextInputEditText mPasswordView;
    private TextInputEditText mPortView;

    private Switch mAutoConnectView;

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
            mAutoconnect = args.getBoolean(EXTRA_AUTOCONNECT);
        }

        mProfilenameView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_profilename);
        mHostnameView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_hostname);
        mPasswordView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_password);
        mPortView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_port);
        mAutoConnectView = (Switch) rootView.findViewById(R.id.fragment_profile_autoconnect);

        mProfilenameView.setText(mProfilename);
        mHostnameView.setText(mHostname);
        mPasswordView.setText(mPassword);
        mPortView.setText(String.valueOf(mPort));
        mAutoConnectView.setChecked(mAutoconnect);


        // Return the ready inflated and configured fragment view.
        return rootView;
    }


    @Override
    public void onPause() {
        super.onPause();

        boolean profileChanged = false;
        if ( !mProfilenameView.getText().toString().equals(mProfilenameView) ) {
            profileChanged = true;
            mProfilename = mProfilenameView.getText().toString();
        }
        if ( !mHostnameView.getText().toString().equals(mHostnameView) ) {
            profileChanged = true;
            mHostname = mHostnameView.getText().toString();
        }
        if ( !mPasswordView.getText().toString().equals(mPasswordView) ) {
            profileChanged = true;
            mPassword = mPasswordView.getText().toString();
        }
        if ( !mPortView.getText().toString().equals(String.valueOf(mPort)) ) {
            profileChanged = true;
            mPort = Integer.valueOf(mPortView.getText().toString());
        }
        if ( !mAutoConnectView.isChecked() == mAutoconnect ) {
            profileChanged = true;
            mAutoconnect = mAutoConnectView.isChecked();
        }

        if ( profileChanged ) {
            Log.v(TAG,"Profile changed: " + mProfilename + ':' + mHostname + ':' + String.valueOf(mPort) + ':' + String.valueOf(mAutoconnect));
        }

    }

}
