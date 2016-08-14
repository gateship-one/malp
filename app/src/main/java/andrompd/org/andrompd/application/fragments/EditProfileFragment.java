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


import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.callbacks.FABFragmentCallback;
import andrompd.org.andrompd.application.callbacks.ProfileManageCallbacks;
import andrompd.org.andrompd.mpdservice.profilemanagement.MPDServerProfile;

public class EditProfileFragment extends Fragment {
    public final static String TAG = EditProfileFragment.class.getSimpleName();
    public static final String EXTRA_PROFILE = "profile";

    

    private String mProfilename;
    private String mHostname;
    private String mPassword;
    private int mPort;


    private TextInputEditText mProfilenameView;
    private TextInputEditText mHostnameView;
    private TextInputEditText mPasswordView;
    private TextInputEditText mPortView;


    private MPDServerProfile mOldProfile;

    private ProfileManageCallbacks mCallback;

    private FABFragmentCallback mFABCallback = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        /* Check if an artistname/albumame was given in the extras */
        Bundle args = getArguments();
        if (null != args) {
            mOldProfile = args.getParcelable(EXTRA_PROFILE);
            if ( mOldProfile != null ) {
                mProfilename = mOldProfile.getProfileName();
                mHostname = mOldProfile.getHostname();
                mPassword = mOldProfile.getPassword();
                mPort = mOldProfile.getPort();
            } else {
                mHostname = "";
                mProfilename = "";
                mPassword = "";
                mPort = 6600;
            }
        }

        mProfilenameView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_profilename);
        mHostnameView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_hostname);
        mPasswordView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_password);
        mPortView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_port);

        mProfilenameView.setText(mProfilename);
        mHostnameView.setText(mHostname);
        mPasswordView.setText(mPassword);
        mPortView.setText(String.valueOf(mPort));


        // Return the ready inflated and configured fragment view.
        return rootView;
    }

    /**
     * Called when the fragment is first attached to its context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (ProfileManageCallbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnArtistSelectedListener");
        }
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mFABCallback = (FABFragmentCallback) context;
        } catch (ClassCastException e) {
            mFABCallback = null;
        }
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

        if ( profileChanged ) {
            if ( null != mOldProfile ) {
                mCallback.removeProfile(mOldProfile);
            }
            MPDServerProfile profile = new MPDServerProfile(mProfilename,true);
            profile.setHostname(mHostname);
            profile.setPassword(mPassword);
            profile.setPort(mPort);
            mCallback.addProfile(profile);
        }

    }


    @Override
    public void onResume() {
        super.onResume();

        if ( null != mFABCallback ) {
            mFABCallback.setupFAB(false,null);
            mFABCallback.setupToolbar(getString(R.string.fragment_profile_title),false, false, false);
        }
    }

}
