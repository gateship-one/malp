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

package org.gateshipone.malp.mpdservice.mpdprotocol;

import android.support.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Class to handle reads and writes to the socket connected to MPD.
 * This class can be used to read Strings and binary data from MPD.
 */
public class MPDSocketInterface {

    /**
     * Buffered input stream to improve the performance
     */
    private BufferedInputStream mInputStream;

    /**
     * Object to write to the socket
     */
    private PrintWriter mWriter;


    /**
     * Creates a new socket interface
     * @param inputStream Input stream from the socket to use
     * @param outputStream Output stream from the socket to use
     */
    public MPDSocketInterface(InputStream inputStream, OutputStream outputStream) {
        mInputStream = new BufferedInputStream(inputStream);

        mWriter = new PrintWriter(outputStream);
    }


    /**
     * Reads a line from the buffered input
     * @return The read string without the newline
     * @throws IOException Exception during read
     */
    public String readLine() throws IOException {
        ByteArrayOutputStream readBuffer = new ByteArrayOutputStream();

        int inputVal = mInputStream.read();
        while (inputVal != '\n' && inputVal != -1) {
            readBuffer.write(inputVal);
            inputVal = mInputStream.read();
        }

        // Return the string data from MPD as UTF-8 (default charset on android) strings
        return readBuffer.toString("UTF-8");
    }

    /**
     * @return True if data is ready to be read, false otherwise
     * @throws IOException Exception during read
     */
    public boolean readReady() throws IOException {
        return mInputStream.available() > 0;
    }

    /**
     * Reads binary data from the socket
     * @param size size to read from the socket in bytes
     * @return byte array if data is correctly read, null otherwise.
     * @throws IOException Exception during read
     */
    @Nullable public byte[] readBinary(int size) throws IOException {
        byte data[] = new byte[size];
        mInputStream.read(data, 0, size);

        // Read last newline from MPD (s. https://www.musicpd.org/doc/protocol/database.html - command
        // albumart)
        return mInputStream.read() == '\n' ? data : null;
    }

    /**
     * Writes a line to the socket.
     * @param line String to write to the socket. No newline required.
     */
    public void writeLine(String line) {
        mWriter.println(line);
        mWriter.flush();
    }
}
