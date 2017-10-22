/*
 *  Copyright (C) 2017 Team Gateship-One
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.graphics.Palette;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.adapters.FileAdapter;
import org.gateshipone.malp.application.adapters.SwatchAdapter;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDPlaylist;

import java.util.List;

/**
 * Created by hendrik on 22.10.17.
 */

public class SwatchDialog extends DialogFragment {
    SwatchAdapter mAdapter;

    List<Palette.Swatch> mSwatches;

    public static SwatchDialog createDialog(List<Palette.Swatch> swatches) {
        SwatchDialog dialog = new SwatchDialog();
        dialog.setSwatches(swatches);
        return dialog;
    }


    private void setSwatches(List<Palette.Swatch> swatches) {
        mSwatches = swatches;
    }

    /**
     * Create the dialog to choose to override an existing bookmark or to create a new bookmark.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        mAdapter = new SwatchAdapter(getContext(), mSwatches);

        builder.setAdapter(mAdapter, null);

        builder.setTitle("Swatches");
        // set divider
        AlertDialog dlg = builder.create();


        dlg.getListView().setDivider(new ColorDrawable(ThemeUtils.getThemeColor(getContext(),R.attr.malp_color_background_selected)));
        dlg.getListView().setDividerHeight(getResources().getDimensionPixelSize(R.dimen.list_divider_size));

        return dlg;
    }

}
