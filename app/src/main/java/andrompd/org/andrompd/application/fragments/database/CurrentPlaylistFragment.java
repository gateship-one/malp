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

package andrompd.org.andrompd.application.fragments.database;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.application.adapters.CurrentPlaylistAdapter;
import andrompd.org.andrompd.application.adapters.TracksAdapter;

public class CurrentPlaylistFragment extends Fragment {
    /**
     * Parameters for bundled extra arguments for this fragment. Necessary to define which playlist to
     * retrieve from the MPD server.
     */
    public static final String BUNDLE_STRING_EXTRA_PLAYLISTNAME = "playlistname";

    private String mPlaylistPath;

    /**
     * Main ListView of this fragment
     */
    private ListView mListView;


    /**
     * Adapter used by the ListView
     */
    private CurrentPlaylistAdapter mPlaylistAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.current_playlist, container, false);

        // Get the main ListView of this fragment
        mListView = (ListView) rootView.findViewById(R.id.current_playlist_listview);

        /* Check if an artistname/albumame was given in the extras */
        Bundle args = getArguments();
        if (null != args) {
            mPlaylistPath = args.getString(BUNDLE_STRING_EXTRA_PLAYLISTNAME);
        }

        // Create the needed adapter for the ListView
        mPlaylistAdapter = new CurrentPlaylistAdapter(getActivity());

        // Combine the two to a happy couple
        mListView.setAdapter(mPlaylistAdapter);

        // Return the ready inflated and configured fragment view.
        return rootView;
    }


    /**
     * Starts the loader to make sure the data is up-to-date after resuming the fragment (from background)
     */
    @Override
    public void onResume() {
        super.onResume();
    }


}
