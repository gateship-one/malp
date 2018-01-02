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

public class MPDException extends Exception {
    private String mError;

    public MPDException(String error) {
        mError = error;
    }

    public String getError() {
        return mError;
    }

    public static class MPDConnectionException extends MPDException {

        public MPDConnectionException(String error) {
            super(error);
        }
    }

    public static class MPDServerException extends MPDException {
        private static final String TAG = MPDServerException.class.getSimpleName();
        private int mErrorCode;
        private int mCommandOffset;
        private String mErrorMessage;
        private String mCommand;

        public MPDServerException(String error) {
            super(error);

            // Parse the error message (s. https://www.musicpd.org/doc/protocol/response_syntax.html#failure_response_syntax)
            String substring;
            // Start with the [ErrorCode@Offset]
            substring = error.substring(error.indexOf('[') + 1,error.lastIndexOf('@'));
            try {
                mErrorCode = Integer.valueOf(substring);
            } catch (NumberFormatException e) {
                mErrorCode = -4711;
            }

            substring = error.substring(error.indexOf('@') + 1,error.lastIndexOf(']'));
            try {
                mCommandOffset = Integer.valueOf(substring);
            } catch (NumberFormatException e) {
                mCommandOffset = -1;
            }

            // Get the command from {command}
            substring = error.substring(error.indexOf('{') + 1,error.lastIndexOf('}'));
            mCommand = substring;

            // Get the message from } on.
            substring = error.substring(error.lastIndexOf('}') + 2,error.length());
            mErrorMessage = substring;
        }

        public int getErrorCode() {
            return mErrorCode;
        }

        public int getCommandOffset() {
            return mCommandOffset;
        }

        public String getCommand() {
            return mCommand;
        }

        public String getServerMessage() {
            return mErrorMessage;
        }
    }
}
