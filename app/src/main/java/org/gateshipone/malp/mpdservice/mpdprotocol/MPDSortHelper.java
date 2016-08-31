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

package org.gateshipone.malp.mpdservice.mpdprotocol;

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFile;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;

import java.util.ArrayList;
import java.util.List;

public class MPDSortHelper {

    public static void sortFileListNumeric(List<MPDFileEntry> inList) {
        ArrayList<MPDFile> resList = new ArrayList<>();

        if ( inList.size() == 0) {
            return;
        }

        MPDFile insElement = (MPDFile)inList.remove(0);
        resList.add(0, insElement);
        while ( inList.size() != 0 ) {
            insElement = (MPDFile)inList.remove(0);

            for( int i = 0; i < resList.size(); i++ ) {
                // Check if the element in result list is "bigger" then add the element here.
                if ( resList.get(i).indexCompare(insElement) == 1 ) {
                    resList.add(i, insElement);
                    insElement = null;
                    break;
                }
            }
            // If the element to add was the biggest add at the end
            if ( null != insElement ) {
                // Add at the end
                resList.add(insElement);
            }
        }
        inList.addAll(resList);
    }
}
