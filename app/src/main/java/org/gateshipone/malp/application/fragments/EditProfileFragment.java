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

package org.gateshipone.malp.application.fragments;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Switch;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.callbacks.FABFragmentCallback;
import org.gateshipone.malp.application.callbacks.ProfileManageCallbacks;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.mpdservice.ConnectionManager;
import org.gateshipone.malp.mpdservice.profilemanagement.MPDServerProfile;

public class EditProfileFragment extends Fragment {
    public final static String TAG = EditProfileFragment.class.getSimpleName();
    public static final String EXTRA_PROFILE = "profile";


    private String mProfilename;
    private String mHostname;
    private String mPassword;
    private int mPort;

    private String mStreamingURL;
    private boolean mStreamingEnabled;

    private String mHTTPCoverRegex;
    private boolean mHTTPCoverEnabled;

    private TextInputEditText mProfilenameView;
    private TextInputEditText mHostnameView;
    private TextInputEditText mPasswordView;
    private TextInputEditText mPortView;

    private Switch mStreamingEnabledView;
    private TextInputEditText mStreamingURLView;

    private Switch mHTTPCoverEnabledView;
    private TextInputEditText mHTTPCoverRegexView;

    private MPDServerProfile mOldProfile;

    private ProfileManageCallbacks mCallback;

    private FABFragmentCallback mFABCallback = null;

    private boolean mOptionsMenuHandled = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_edit_profile, container, false);


        mProfilenameView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_profilename);
        mHostnameView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_hostname);
        mPasswordView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_password);
        mPortView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_port);

        mStreamingURLView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_streaming_url);
        mStreamingEnabledView = (Switch) rootView.findViewById(R.id.fragment_profile_streaming_enabled);

        mHTTPCoverRegexView = (TextInputEditText) rootView.findViewById(R.id.fragment_profile_cover_regex);
        mHTTPCoverEnabledView = (Switch) rootView.findViewById(R.id.fragment_profile_http_covers_enabled);


        // Set to maximum tcp port
        InputFilter portFilter = new PortNumberFilter();

        mPortView.setFilters(new InputFilter[]{portFilter});


        /* Check if an artistname/albumame was given in the extras */
        Bundle args = getArguments();
        if (null != args) {
            mOldProfile = args.getParcelable(EXTRA_PROFILE);
            if (mOldProfile != null) {
                mProfilename = mOldProfile.getProfileName();
                mHostname = mOldProfile.getHostname();
                mPassword = mOldProfile.getPassword();
                mPort = mOldProfile.getPort();

                mStreamingURL = mOldProfile.getStreamingURL();
                mStreamingEnabled = mOldProfile.getStreamingEnabled();

                mHTTPCoverRegex = mOldProfile.getHTTPRegex();
                mHTTPCoverEnabled = mOldProfile.getHTTPCoverEnabled();

                mProfilenameView.setText(mProfilename);
            } else {
                mHostname = "";
                mProfilename = "";
                mPassword = "";
                mPort = 6600;

                mStreamingEnabled = false;
                mStreamingURL = "";

                mHTTPCoverEnabled = false;
                mHTTPCoverRegex = "";

                mProfilenameView.setText(getString(R.string.fragment_profile_default_name));
            }
        }

        mHostnameView.setText(mHostname);
        mPasswordView.setText(mPassword);
        mPortView.setText(String.valueOf(mPort));

        // Show/Hide streaming url view depending on state
        mStreamingEnabledView.setChecked(mStreamingEnabled);
        mStreamingEnabledView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if ( mStreamingURLView.getText().toString().isEmpty()) {
                    // Check if a text was already set otherwise show an example
                    mStreamingURL = "http://" + mHostnameView.getText().toString() + ":8080";
                    mStreamingURLView.setText(mStreamingURL);
                }
                mStreamingURLView.setVisibility(View.VISIBLE);
            } else {
                mStreamingURLView.setVisibility(View.GONE);
            }

        });

        if (!mStreamingEnabled) {
            mStreamingURLView.setVisibility(View.GONE);
        }
        mStreamingURLView.setText(mStreamingURL);

        // Show/Hide HTTP cover regex view depending on state
        mHTTPCoverEnabledView.setChecked(mHTTPCoverEnabled);
        mHTTPCoverEnabledView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mHTTPCoverRegexView.setText(mHTTPCoverRegex);
                mHTTPCoverRegexView.setVisibility(View.VISIBLE);
            } else {
                mHTTPCoverRegexView.setVisibility(View.GONE);
            }

        });
        if (!mHTTPCoverEnabled) {
            mHTTPCoverRegexView.setVisibility(View.GONE);
        }
        mHTTPCoverRegexView.setText(mHTTPCoverRegex);


        mProfilenameView.setSelectAllOnFocus(true);

        setHasOptionsMenu(true);

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

        if (!mOptionsMenuHandled) {
            checkChanged();
        }

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getView();
        if (null != view) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    private void checkChanged() {
        boolean profileChanged = false;
        if (!mProfilenameView.getText().toString().equals(mProfilename)) {
            profileChanged = true;
            mProfilename = mProfilenameView.getText().toString();
        }
        if (!mHostnameView.getText().toString().equals(mHostname)) {
            profileChanged = true;
            mHostname = mHostnameView.getText().toString();
        }
        if (!mPasswordView.getText().toString().equals(mPassword)) {
            profileChanged = true;
            mPassword = mPasswordView.getText().toString();
        }
        if (!mPortView.getText().toString().isEmpty() && Integer.parseInt(mPortView.getText().toString()) != mPort) {
            profileChanged = true;
            mPort = Integer.parseInt(mPortView.getText().toString());
        }
        if (!mStreamingURLView.getText().toString().equals(mStreamingURL)) {
            profileChanged = true;
            mStreamingURL = mStreamingURLView.getText().toString();
        }
        if (mStreamingEnabledView.isChecked() != mStreamingEnabled) {
            profileChanged = true;
            mStreamingEnabled = mStreamingEnabledView.isChecked();
        }
        if (!mHTTPCoverRegexView.getText().toString().equals(mHTTPCoverRegex)) {
            profileChanged = true;
            mHTTPCoverRegex = mHTTPCoverRegexView.getText().toString();
        }
        if (mHTTPCoverEnabledView.isChecked() != mHTTPCoverEnabled) {
            profileChanged = true;
            mHTTPCoverEnabled = mHTTPCoverEnabledView.isChecked();
        }

        if (profileChanged) {
            if (null != mOldProfile) {
                ConnectionManager.getInstance(getContext().getApplicationContext()).removeProfile(mOldProfile,getActivity());
            } else {
                mOldProfile = new MPDServerProfile(mProfilename, true);
            }
            mOldProfile.setProfileName(mProfilename);
            mOldProfile.setHostname(mHostname);
            mOldProfile.setPassword(mPassword);
            mOldProfile.setPort(mPort);
            mOldProfile.setStreamingURL(mStreamingURL);
            mOldProfile.setStreamingEnabled(mStreamingEnabled);
            mOldProfile.setHTTPCoverEnabled(mHTTPCoverEnabled);
            mOldProfile.setHTTPRegex(mHTTPCoverRegex);
            ConnectionManager.getInstance(getContext().getApplicationContext()).addProfile(mOldProfile, getContext());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (null != mFABCallback) {
            mFABCallback.setupFAB(false, null);
            mFABCallback.setupToolbar(getString(R.string.fragment_profile_title), false, false, false);
        }
    }

    private class PortNumberFilter implements InputFilter {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            if (end > start) {
                String destTxt = dest.toString();
                String resultingTxt = destTxt.substring(0, dstart) +
                        source.subSequence(start, end) +
                        destTxt.substring(dend);
                try {
                    int port = Integer.parseInt(resultingTxt);
                    if (port > 65535) {
                        return "";
                    }
                    if (port < 1) {
                        return "";
                    }
                } catch (NumberFormatException e) {
                    return "";
                }
            }
            return null;
        }
    }

    /**
     * Initialize the options menu.
     * Be sure to call {@link #setHasOptionsMenu} before.
     *
     * @param menu         The container for the custom options menu.
     * @param menuInflater The inflater to instantiate the layout.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.fragment_menu_edit_profile, menu);

        // get tint color
        int tintColor = ThemeUtils.getThemeColor(getContext(), R.attr.malp_color_text_accent);

        Drawable drawable = menu.findItem(R.id.action_save).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_save).setIcon(drawable);

        drawable = menu.findItem(R.id.action_delete).getIcon();
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, tintColor);
        menu.findItem(R.id.action_delete).setIcon(drawable);

        super.onCreateOptionsMenu(menu, menuInflater);
    }

    /**
     * Hook called when an menu item in the options menu is selected.
     *
     * @param item The menu item that was selected.
     * @return True if the hook was consumed here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                checkChanged();
                mOptionsMenuHandled = true;
                getActivity().onBackPressed();
                return true;
            case R.id.action_delete:
                ConnectionManager.getInstance(getContext().getApplicationContext()).removeProfile(mOldProfile,getContext());
                mOptionsMenuHandled = true;
                getActivity().onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
