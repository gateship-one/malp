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

package org.gateshipone.malp.application.utils;


import android.content.Context;

import org.gateshipone.malp.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatHelper {
    private static final String LUCENE_SPECIAL_CHARACTERS_REGEX = "([\\+\\-\\!\\(\\)\\{\\}\\[\\]\\^\\\"\\~\\*\\?\\:\\\\\\/])";

    /**
     * Helper method to uniformly format length strings in M.A.L.P..
     *
     * @param length Length value in milliseconds
     * @return the formatted string, usable in the ui
     */
    public static String formatTracktimeFromMS(long length) {

        String retVal;

        int seconds = (int) (length / 1000);

        int hours = seconds / 3600;

        int minutes = (seconds - (hours * 3600)) / 60;

        seconds = seconds - (hours * 3600) - (minutes * 60);

        if (hours == 0) {
            retVal = String.format(Locale.getDefault(), "%02d" + ":" + "%02d", minutes, seconds);
        } else {
            retVal = String.format(Locale.getDefault(), "%02d" + ":" + "%02d" + ":" + "%02d", hours, minutes, seconds);
        }

        return retVal;
    }

    /**
     * Helper method to uniformly format length strings in M.A.L.P..
     *
     * @param length Length value in milliseconds
     * @return the formatted string, usable in the ui
     */
    public static String formatTracktimeFromS(long length) {

        String retVal;

        int seconds = (int) (length);

        int hours = seconds / 3600;

        int minutes = (seconds - (hours * 3600)) / 60;

        seconds = seconds - (hours * 3600) - (minutes * 60);

        if (hours == 0) {
            retVal = String.format(Locale.getDefault(), "%02d" + ":" + "%02d", minutes, seconds);
        } else {
            retVal = String.format(Locale.getDefault(), "%02d" + ":" + "%02d" + ":" + "%02d", hours, minutes, seconds);
        }

        return retVal;
    }

    /**
     * Helper method to uniformly format length strings in M.A.L.P..
     *
     * @param length Length value in milliseconds
     * @return the formatted string, usable in the ui
     */
    public static String formatTracktimeFromSWithDays(long length, Context context) {

        String retVal;

        int seconds = (int) (length);

        int days = seconds / 86400;
        seconds -= days * 86400;

        int hours = seconds / 3600;
        seconds -= hours * 3600;

        int minutes = seconds / 60;
        seconds -= minutes * 60;

        if (days == 0 && hours == 0) {
            retVal = String.format(Locale.getDefault(), "%02d" + ":" + "%02d", minutes, seconds);
        } else if (days == 0 && hours != 0)  {
            retVal = String.format(Locale.getDefault(), "%02d" + ":" + "%02d" + ":" + "%02d", hours, minutes, seconds);
        } else {
            retVal = String.format(Locale.getDefault(), "%02d" + " " + context.getResources().getString(R.string.duration_days)+ " %02d" + ":" + "%02d" + ":" + "%02d", days, hours, minutes, seconds);
        }

        return retVal;
    }

    /**
     * Helper method to format the mediastore track number to a track number string
     *
     * @param trackNumber the tracknumber from the mediastore
     * @return the formatted string, usable in the ui
     */
    public static String formatTrackNumber(int trackNumber) {

        String trackNumberString = String.valueOf(trackNumber);

        // mediastore combines track and cd number in one string so cut off the first two literals
        if (trackNumberString.length() >= 4) {
            trackNumberString = trackNumberString.substring(2);
        }

        return trackNumberString;
    }

    /**
     * Helper method to format a timestamp to a uniformly format date string in M.A.L.P..
     *
     * @param timestamp The timestamp in milliseconds
     * @return the formatted string, usable in the ui
     */
    public static String formatTimeStampToString(long timestamp) {
        Date date = new Date(timestamp);
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM, Locale.getDefault()).format(date);
    }


    /**
     * Returns the last part of the complete filename URl
     * @param url URL to separate
     * @return Filename (last part behind a /)
     */
    public static final String getFilenameFromPath(String url) {
        String[] splitPath = url.split("/");
        if ( splitPath.length > 0 ) {
            return splitPath[splitPath.length - 1];
        }
        return url;
    }

    /**
     * Escapes Apache lucene special characters (e.g. used by MusicBrainz) for URLs.
     * See:
     * https://lucene.apache.org/core/4_3_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html
     * @param input Input string
     * @return Escaped string
     */
    public static String escapeSpecialCharsLucene(String input) {
        String retVal = input.replaceAll("(\\&\\&)", "\\\\&\\\\&");
        retVal = retVal.replaceAll("(\\|\\|)", "\\\\|\\\\|");

        retVal = retVal.replaceAll(LUCENE_SPECIAL_CHARACTERS_REGEX,"\\\\$1");
        return retVal;
    }
}
