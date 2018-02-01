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
import android.util.Log;

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
    private static final String TAG = MPDSocketInterface.class.getSimpleName();
    /**
     * Buffered input stream to improve the performance
     */
    private InputStream mInputStream;

    /**
     * Object to write to the socket
     */
    private PrintWriter mWriter;

    private static final int READ_BUFFER_SIZE = 512 * 1024; // 512kB

    private byte[] mReadBuffer;

    private int mReadBufferWritePos;
    private int mReadBufferReadPos;


    /**
     * Creates a new socket interface
     * @param inputStream Input stream from the socket to use
     * @param outputStream Output stream from the socket to use
     */
    public MPDSocketInterface(InputStream inputStream, OutputStream outputStream) {
        mInputStream = inputStream;

        mWriter = new PrintWriter(outputStream);
        mReadBuffer = new byte[READ_BUFFER_SIZE];

        mReadBufferReadPos = 0;
        mReadBufferWritePos = 0;
    }


    /**
     * Reads as much as possible data from the socket into its buffer.
     * Both pointers are reset, ensure to only call this on an empty buffer or data will
     * be lost!
     * @throws IOException
     */
    private void fillReadBuffer() throws IOException {

        int readBytes = mInputStream.read(mReadBuffer, 0, READ_BUFFER_SIZE);

       // Log.v(TAG,"Buffer fill read " + readBytes + " bytes");
        mReadBufferWritePos = readBytes;
        mReadBufferReadPos = 0;
    }

    private int dataReady() {
        return mReadBufferWritePos - mReadBufferReadPos;
    }

    /**
     * Reads a line from the buffered input
     * @return The read string without the newline
     * @throws IOException Exception during read
     */
    public String readLine() throws IOException {
        if (dataReady() == 0) {
            fillReadBuffer();
        }

        ByteArrayOutputStream readBuffer = new ByteArrayOutputStream();

        int localReadPos = mReadBufferReadPos;
        // Read until newline
        while (mReadBuffer[localReadPos] != '\n') {
            localReadPos++;

            if (localReadPos == mReadBufferWritePos) {
                // End of filled buffer found, copy whats read so far
                readBuffer.write(mReadBuffer, mReadBufferReadPos, mReadBufferWritePos-mReadBufferReadPos);
                mReadBufferReadPos = mReadBufferWritePos;
                fillReadBuffer();
                localReadPos = 0;
            }

            // Found newline
            if (mReadBuffer[localReadPos] == '\n') {
                readBuffer.write(mReadBuffer, mReadBufferReadPos, localReadPos-mReadBufferReadPos);
                mReadBufferReadPos = (localReadPos + 1);
                break;
            }
        }

        // Return the string data from MPD as UTF-8 (default charset on android) strings
        return readBuffer.toString("UTF-8");
    }

    /**
     * @return True if data is ready to be read, false otherwise
     * @throws IOException Exception during read
     */
    public boolean readReady() throws IOException {
        return dataReady() > 0 || mInputStream.available() > 0;
    }

    /**
     * Reads binary data from the socket
     * @param size size to read from the socket in bytes
     * @return byte array if data is correctly read
     * @throws IOException Exception during read
     */
     public byte[] readBinary(int size) throws IOException {
        byte data[] = new byte[size];

        int dataRead = 0;

        int dataToRead = 0;
        int readyData = 0;
        while (dataRead < size) {
            readyData = dataReady();

            // Check how much data is necessary to read (do not read more data than requested!)
            dataToRead = readyData > (size - dataRead) ? (size-dataRead) : readyData;

            // Read data that is ready or requested
            System.arraycopy(mReadBuffer, mReadBufferReadPos, data, dataRead, dataToRead);
            dataRead += dataToRead;
            mReadBufferReadPos += dataToRead;

            // Check if the data buffer is depleted
            if(dataReady() == 0 && dataRead != size) {
                fillReadBuffer();
            }
        }

        // Skip one byte to catch last newline
        mReadBufferReadPos++;

        // Read last newline from MPD (s. https://www.musicpd.org/doc/protocol/database.html - command
        // albumart)
        return data;
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
