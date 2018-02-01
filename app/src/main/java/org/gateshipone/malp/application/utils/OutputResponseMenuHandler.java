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

package org.gateshipone.malp.application.utils;


import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.PopupMenu;

import org.gateshipone.malp.R;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseOutputList;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDOutput;

import java.lang.ref.WeakReference;
import java.util.List;

public class OutputResponseMenuHandler extends MPDResponseOutputList {

    private WeakReference<Context> mContext;

    private WeakReference<View> mView;

    public OutputResponseMenuHandler(Context context, View view) {
        mContext = new WeakReference<>(context);
        mView = new WeakReference<>(view);
    }

    @Override
    public void handleOutputs(final List<MPDOutput> outputList) {
        // we need at least 2 output plugins configured
        if (outputList != null && outputList.size() > 1) {
            PopupMenu popup = new PopupMenu(mContext.get(), mView.get());
            Menu menu = popup.getMenu();
            SubMenu menuSwitch =  menu.addSubMenu(R.string.action_switch_to_output);
            SubMenu menuToggle = menu.addSubMenu(R.string.action_toggle_outputs);

            int menuId = 0;
            for (final MPDOutput output : outputList) {
                MenuItem subMenuItem = menuToggle.add(0, menuId, 0, output.getOutputName())
                        .setCheckable(true)
                        .setChecked(output.getOutputState());

                subMenuItem.setOnMenuItemClickListener(item -> {
                    MPDOutput out = outputList.get(item.getItemId());

                    if (out.getOutputState()) {
                        MPDCommandHandler.disableOutput(out.getID());
                    } else {
                        MPDCommandHandler.enableOutput(out.getID());
                    }
                    out.setOutputState(!out.getOutputState());

                    item.setChecked(out.getOutputState());
                    // Keep the popup menu open
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                    item.setActionView(new View(mContext.get()));
                    item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                        @Override
                        public boolean onMenuItemActionExpand(MenuItem item) {
                            return false;
                        }

                        @Override
                        public boolean onMenuItemActionCollapse(MenuItem item) {
                            return false;
                        }
                    });
                    return false;
                });

                subMenuItem = menuSwitch.add(0, menuId, 0, output.getOutputName());
                subMenuItem.setOnMenuItemClickListener(item -> {
                    MPDOutput selectedOut = outputList.get(item.getItemId());

                    // first enable the selected output so we have always an active ones
                    MPDCommandHandler.enableOutput(selectedOut.getID());
                    selectedOut.setOutputState(true);

                    for(MPDOutput current: outputList) {
                        if (current != selectedOut) {
                            MPDCommandHandler.disableOutput(current.getID());
                            current.setOutputState(false);
                        }
                    }
                    return false;
                });
                menuId++;
            }
            popup.show();
        } else {
            // Only one output, show toggle menu
            PopupMenu popup = new PopupMenu(mContext.get(), mView.get());

            Menu menu = popup.getMenu();

            for (final MPDOutput output : outputList) {
                MenuItem subMenuItem = menu.add(0, 0, 0, output.getOutputName())
                        .setCheckable(true)
                        .setChecked(output.getOutputState());

                subMenuItem.setOnMenuItemClickListener(item -> {
                    MPDOutput out = outputList.get(item.getItemId());

                    if (out.getOutputState() == true) {
                        MPDCommandHandler.disableOutput(out.getID());
                    } else {
                        MPDCommandHandler.enableOutput(out.getID());
                    }
                    out.setOutputState(!out.getOutputState());

                    item.setChecked(out.getOutputState());
                    // Keep the popup menu open
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
                    item.setActionView(new View(mContext.get()));
                    item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                        @Override
                        public boolean onMenuItemActionExpand(MenuItem item) {
                            return false;
                        }

                        @Override
                        public boolean onMenuItemActionCollapse(MenuItem item) {
                            return false;
                        }
                    });
                    return false;
                });

            }
            popup.show();
        }
    }
}
