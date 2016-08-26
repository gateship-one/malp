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

package org.gateshipone.malp.application.fragments.serverfragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.FileAdapter;
import org.gateshipone.malp.application.callbacks.OnSaveDialogListener;
import org.gateshipone.malp.application.loaders.PlaylistsLoader;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFile;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDPlaylist;
import org.w3c.dom.Text;

import java.util.List;

public class SongDetailsDialog extends DialogFragment {

    public static final String EXTRA_FILE = "file";


    private MPDFile mFile;

    private TextView mTrackTitle;
    private TextView mTrackAlbum;
    private TextView mTrackArtist;
    private TextView mTrackAlbumArtist;

    private TextView mTrackNo;
    private TextView mTrackDisc;
    private TextView mTrackDate;

    private TextView mTrackTitleMBID;
    private TextView mTrackAlbumMBID;
    private TextView mTrackArtistMBID;
    private TextView mTrackAlbumArtistMBID;

    private TextView mTrackURI;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_song_details, container, false);

        /* Check if an artistname/albumame was given in the extras */
        Bundle args = getArguments();
        if (null != args) {
            mFile = args.getParcelable(EXTRA_FILE);
        }

        mTrackTitle = (TextView) rootView.findViewById(R.id.now_playing_text_track_title);
        mTrackAlbum = (TextView) rootView.findViewById(R.id.now_playing_text_track_album);
        mTrackArtist = (TextView) rootView.findViewById(R.id.now_playing_text_track_artist);
        mTrackAlbumArtist = (TextView) rootView.findViewById(R.id.now_playing_text_album_artist);

        mTrackNo = (TextView) rootView.findViewById(R.id.now_playing_text_track_no);
        mTrackDisc = (TextView) rootView.findViewById(R.id.now_playing_text_disc_no);
        mTrackDate = (TextView) rootView.findViewById(R.id.now_playing_text_date);

        mTrackTitleMBID = (TextView) rootView.findViewById(R.id.now_playing_text_track_mbid);
        mTrackAlbumMBID = (TextView) rootView.findViewById(R.id.now_playing_text_album_mbid);
        mTrackArtistMBID = (TextView) rootView.findViewById(R.id.now_playing_text_artist_mbid);
        mTrackAlbumArtistMBID = (TextView) rootView.findViewById(R.id.now_playing_text_album_artist_mbid);

        mTrackURI = (TextView) rootView.findViewById(R.id.now_playing_text_track_uri);

        if ( null != mFile) {
            mTrackTitle.setText(mFile.getTrackTitle());
            mTrackAlbum.setText(mFile.getTrackAlbum());
            mTrackArtist.setText(mFile.getTrackArtist());
            mTrackAlbumArtist.setText(mFile.getTrackAlbumArtist());

            // FIXME total track count
            mTrackNo.setText(String.valueOf(mFile.getTrackNumber()));
            // FIXME total disc count
            mTrackDisc.setText(String.valueOf(mFile.getDiscNumber()));
            mTrackDate.setText(mFile.getDate());

            mTrackTitleMBID.setText(mFile.getTrackMBID());
            mTrackAlbumMBID.setText(mFile.getTrackAlbumMBID());
            mTrackArtistMBID.setText(mFile.getTrackArtistMBID());
            mTrackAlbumArtistMBID.setText(mFile.getTrackAlbumArtistMBID());

            mTrackURI.setText(mFile.getPath());
        }

        ((Button)rootView.findViewById(R.id.button_enqueue)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( null != mFile ) {
                    MPDQueryHandler.addSong(mFile.getPath());
                }
                dismiss();
            }
        });

        ((Button)rootView.findViewById(R.id.button_cancel)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        // Return the ready inflated and configured fragment view.
        return rootView;
    }
}
