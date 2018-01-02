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


import android.widget.AbsListView;
import android.widget.GridView;

import org.gateshipone.malp.application.adapters.ScrollSpeedAdapter;
import org.gateshipone.malp.application.listviewitems.AbsImageListViewItem;

public class ScrollSpeedListener implements AbsListView.OnScrollListener {
    private static final String TAG = ScrollSpeedListener.class.getSimpleName();
    private long mLastTime = 0;
    private int mLastFirstVisibleItem = 0;

    /**
     * Items per second scrolling over the screen
     */
    private int mScrollSpeed = 0;

    private final ScrollSpeedAdapter mAdapter;
    private final AbsListView mListView;

    public ScrollSpeedListener(ScrollSpeedAdapter adapter, AbsListView listView) {
        super();
        mListView = listView;
        mAdapter = adapter;
    }

    /**
     * Called when a scroll is started/ended and resets the values.
     * If scrolling stops this will start coverimage tasks
     * @param view View that has a scrolling state change
     * @param scrollState New scrolling state of the view
     */
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            mScrollSpeed = 0;
            mAdapter.setScrollSpeed(0);
            for (int i = 0; i <= mListView.getLastVisiblePosition() - mListView.getFirstVisiblePosition(); i++) {
                AbsImageListViewItem listItem = (AbsImageListViewItem) mListView.getChildAt(i);
                listItem.startCoverImageTask();
            }
        } else if ( scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL ) {
            // Reset time values
            mLastTime = System.currentTimeMillis();
        }
    }

    /**
     * Called when the associated Listview/GridView is scrolled by the user.
     * This method evaluates if the view is scrolled slow enough to start loading images.
     *
     * @param view View that is being scrolled.
     * @param firstVisibleItem Index of the first visible item
     * @param visibleItemCount Count of visible items
     * @param totalItemCount Total item count
     */
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // New row started if this is true.
        if (firstVisibleItem != mLastFirstVisibleItem) {
            long currentTime = System.currentTimeMillis();
            if (currentTime == mLastTime) {
                return;
            }
            // Calculate the duration of scroll per line
            long timeScrollPerRow = currentTime - mLastTime;


            if ( view instanceof GridView ) {
                GridView gw = (GridView)view;
                mScrollSpeed = (int) (1000 / timeScrollPerRow) * gw.getNumColumns();
            } else {
                mScrollSpeed = (int) (1000 / timeScrollPerRow);
            }

            // Calculate how many items per second of loading images is possible
            int possibleItems = (int)(1000/mAdapter.getAverageImageLoadTime());


            // Set the scrollspeed in the adapter
            mAdapter.setScrollSpeed(mScrollSpeed);

            // Save values for next comparsion
            mLastFirstVisibleItem = firstVisibleItem;
            mLastTime = currentTime;
            // Start the items image loader task only if scroll speed is slow enough:
            // The devices is able to render the images needed for the scroll speed
            if (mScrollSpeed < possibleItems) {
                for (int i = 0; i < visibleItemCount; i++) {
                    AbsImageListViewItem listItem = (AbsImageListViewItem) mListView.getChildAt(i);
                    listItem.startCoverImageTask();
                }
            }
        }

    }
}
