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

package org.gateshipone.malp.mpdservice.mpdprotocol;

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;

import java.util.ArrayList;
import java.util.List;

public class MPDSortHelper {

    /**
     * Sorts a list of {@link MPDFileEntry} objects in the right order of their index (if
     * the objects are from type {@link MPDTrack}. All other elements are located at the end of the list.
     * @param inList List of objects to sort.
     */
    public static void sortFileListNumeric(List<MPDFileEntry> inList) {
        ArrayList<MPDFileEntry> resList = new ArrayList<>();

        if ( inList.size() == 0) {
            return;
        }

        MPDFileEntry tmpElement = inList.remove(0);
        MPDTrack trackElement;

        resList.add(0, tmpElement);
        while ( inList.size() != 0 ) {
            tmpElement = inList.remove(0);
            if ( tmpElement instanceof MPDTrack) {
                trackElement = (MPDTrack)tmpElement;

                for( int i = 0; i < resList.size(); i++ ) {
                    MPDFileEntry compareItem = resList.get(i);
                    // Check if the element in result list is "bigger" then add the element here.
                    if ( (compareItem instanceof MPDTrack) && ((MPDTrack)compareItem).indexCompare(trackElement) == 1 ) {
                        resList.add(i, tmpElement);
                        tmpElement = null;
                        break;
                    } else if ( !(compareItem instanceof MPDTrack) ) {
                        resList.add(i,tmpElement);
                        tmpElement = null;
                        break;
                    }
                }
            }


            // If the element to add was the biggest add at the end
            if ( null != tmpElement ) {
                // Add at the end
                resList.add(tmpElement);
            }
        }
        inList.addAll(resList);
    }
}
