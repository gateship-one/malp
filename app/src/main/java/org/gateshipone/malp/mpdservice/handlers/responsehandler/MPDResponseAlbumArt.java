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

package org.gateshipone.malp.mpdservice.handlers.responsehandler;

import android.os.Bundle;
import android.os.Message;

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;


public abstract class MPDResponseAlbumArt extends MPDResponseHandler {
    private static final String BUNDLE_EXTRA_IMAGE_DATA = "imageData";
    private static final String BUNDLE_EXTRA_FILE = "file";

    /**
     * Handle function that calls the abstract method implemented by the user
     * of this response handler
     * @param msg Message object containing a list of MPDAlbum items.
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        Bundle data = msg.getData();

        byte[] artworkData = data.getByteArray(BUNDLE_EXTRA_IMAGE_DATA);
        String url = data.getString(BUNDLE_EXTRA_FILE);

        handleAlbumArt(artworkData, url);
    }

    /**
     * Sends an album artwork as a byte array and the corresponding
     * {@link MPDAlbum} to the receiving handler thread
     * @param artworkData Byte data of the image that was requested from the server
     * @param url of the file for which artwork was requested
     */
    public void sendAlbumArtwork(byte[] artworkData, String url) {
        Message message = obtainMessage();

        Bundle data = new Bundle();
        data.putByteArray(BUNDLE_EXTRA_IMAGE_DATA, artworkData);
        data.putString(BUNDLE_EXTRA_FILE, url);

        message.setData(data);
        sendMessage(message);
    }


    abstract void handleAlbumArt(byte[] artworkData, String url);
}
