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

package org.gateshipone.malp.application.views;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;


import org.gateshipone.malp.R;


public class VolumeStepPreferenceDialog extends DialogFragment implements SeekBar.OnSeekBarChangeListener {
    private static final int WARNING_THRESHOLD = 10;

    private SeekBar mSeekBar;

    private TextView mVolumeLabel;
    private TextView mWarningLabel;

    private int mVolumeStepSize;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.volume_step_preference_dialog, container, false);

        mSeekBar = rootView.findViewById(R.id.volume_seekbar);
        mVolumeLabel = rootView.findViewById(R.id.volume_text);
        mWarningLabel = rootView.findViewById(R.id.volume_warning_text);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        mVolumeStepSize = sharedPreferences.getInt(getString(R.string.pref_volume_steps_key), getResources().getInteger(R.integer.pref_volume_steps_default));

        mSeekBar.setProgress(mVolumeStepSize);
        mSeekBar.setOnSeekBarChangeListener(this);

        rootView.findViewById(R.id.button_ok).setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(getString(R.string.pref_volume_steps_key), mVolumeStepSize == 0 ? 1 : mVolumeStepSize);
            editor.apply();
            dismiss();
        });

        rootView.findViewById(R.id.button_cancel).setOnClickListener(v -> dismiss());

        updateLabels();

        return rootView;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mVolumeStepSize = progress;
        updateLabels();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void updateLabels() {
        if (mVolumeStepSize > WARNING_THRESHOLD) {
            mWarningLabel.setVisibility(View.VISIBLE);
        } else {
            mWarningLabel.setVisibility(View.INVISIBLE);
        }
        mVolumeLabel.setText(getString(R.string.volume_step_size_dialog_title, mVolumeStepSize));
    }
}
