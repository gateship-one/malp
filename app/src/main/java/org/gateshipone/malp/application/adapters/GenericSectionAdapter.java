/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
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

package org.gateshipone.malp.application.adapters;


import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDGenericItem;

public abstract class GenericSectionAdapter<T extends MPDGenericItem> extends BaseAdapter implements SectionIndexer, ScrollSpeedAdapter {
    private static final String TAG = "GenericSectionAdapter";
    /**
     * Variables used for sectioning (fast scroll).
     */
    private final ArrayList<String> mSectionList;
    private final ArrayList<Integer> mSectionPositions;
    private final HashMap<Character, Integer> mPositionSectionMap;

    /**
     * Abstract list with model data used for this adapter.
     */
    protected List<T> mModelData;

    protected final List<T> mFilteredModelData;

    private String mFilterString;

    /**
     * Variable to store the current scroll speed. Used for image view optimizations
     */
    protected int mScrollSpeed;

    /**
     * Determines how the new time value affects the average (0.0(new value has no effect) - 1.0(average is only the new value, no smoothing)
     */
    private static final float mSmoothingFactor = 0.3f;

    /**
     * Smoothed average(exponential smoothing) value
     */
    private long mAvgImageTime;


    /**
     * Task used to do the filtering of the list asynchronously
     */
    private FilterTask mFilterTask;


    public GenericSectionAdapter() {
        super();

        mSectionList = new ArrayList<>();
        mSectionPositions = new ArrayList<>();
        mPositionSectionMap = new HashMap<>();

        mModelData = new ArrayList<>();

        mFilteredModelData = new ArrayList<>();
        mFilterString = "";
    }

    /**
     * Swaps the model of this adapter. This sets the dataset on which the
     * adapter creates the GridItems. This should generally be safe to jll.
     * Clears old section data and model data and recreates sectionScrolling
     * data.
     *
     * @param data Actual model data
     */
    public void swapModel(List<T> data) {
        if (data == null) {
            mModelData.clear();
        } else {
            mModelData.clear();
            mModelData.addAll(data);
        }
        synchronized (mFilteredModelData) {
            mFilteredModelData.clear();
        }
        setScrollSpeed(0);
        // create sectionlist for fastscrolling
        createSections();

        notifyDataSetChanged();
    }

    /**
     * Looks up the position(index) of a given section(index)
     *
     * @param sectionIndex Section to get the ListView/GridView position for
     * @return The item position of this section start.
     */
    @Override
    public int getPositionForSection(int sectionIndex) {
        return mSectionPositions.get(sectionIndex);
    }

    /**
     * Reverse lookup of a section for a given position
     *
     * @param pos Position to get the section for
     * @return Section (index) for the items position
     */
    @Override
    public int getSectionForPosition(int pos) {

        String sectionTitle = ((MPDGenericItem)getItem(pos)).getSectionTitle();

        char itemSection;
        if (sectionTitle.length() > 0) {
            itemSection = sectionTitle.toUpperCase().charAt(0);
        } else {
            itemSection = ' ';
        }

        if (mPositionSectionMap.containsKey(itemSection)) {
            int sectionIndex = mPositionSectionMap.get(itemSection);
            return sectionIndex;
        }
        return 0;
    }

    /**
     * @return A list of all available sections
     */
    @Override
    public Object[] getSections() {
        return mSectionList.toArray();
    }

    /**
     * @return The length of the modeldata of this adapter.
     */
    @Override
    public int getCount() {
        synchronized (mFilteredModelData) {
            if (!mFilteredModelData.isEmpty() || !mFilterString.isEmpty()) {
                return mFilteredModelData.size();
            } else {
                return mModelData.size();
            }
        }
    }

    /**
     * Simple getter for the model data.
     *
     * @param position Index of the item to get. No check for boundaries here.
     * @return The item at index position.
     */
    @Override
    public Object getItem(int position) {
        synchronized (mFilteredModelData) {
            if (!mFilteredModelData.isEmpty() || !mFilterString.isEmpty()) {
                return mFilteredModelData.get(position);
            } else {
                return mModelData.get(position);
            }
        }
    }

    /**
     * Simple position->id mapping here.
     *
     * @param position Position to get the id from
     * @return The id (position)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Sets the scrollspeed in items per second.
     *
     * @param speed
     */
    public void setScrollSpeed(int speed) {
        mScrollSpeed = speed;
    }

    /**
     * Returns the smoothed average loading time of images.
     * This value is used by the scrollspeed listener to determine if
     * the scrolling is slow enough to render images (artist, album images)
     * @return Average time to load an image in ms
     */
    public long getAverageImageLoadTime() {
        return mAvgImageTime == 0 ? 1: mAvgImageTime;
    }

    /**
     * This method adds new loading times to the smoothed average.
     * Should only be called from the async cover loader.
     * @param time Time in ms to load a image
     */
    public void addImageLoadTime(long time) {
        // Implement exponential smoothing here
        if ( mAvgImageTime == 0 ) {
            mAvgImageTime = time;
        } else {
            mAvgImageTime = (long) (((1 - mSmoothingFactor) * mAvgImageTime) + (mSmoothingFactor * time));
        }
    }

    private void createSections() {
        mSectionList.clear();
        mSectionPositions.clear();
        mPositionSectionMap.clear();

        if (getCount() > 0) {
            MPDGenericItem currentModel = (MPDGenericItem) getItem(0);

            char lastSection;
            if (currentModel.getSectionTitle().length() > 0) {
                lastSection = currentModel.getSectionTitle().toUpperCase().charAt(0);
            } else {
                lastSection = ' ';
            }

            mSectionList.add(String.valueOf(lastSection));
            mSectionPositions.add(0);
            mPositionSectionMap.put(lastSection, mSectionList.size() - 1);

            for (int i = 1; i < getCount(); i++) {

                currentModel = (MPDGenericItem) getItem(i);

                char currentSection;
                if (currentModel.getSectionTitle().length() > 0) {
                    currentSection = currentModel.getSectionTitle().toUpperCase().charAt(0);
                } else {
                    currentSection = ' ';
                }

                if (lastSection != currentSection) {
                    mSectionList.add("" + currentSection);

                    lastSection = currentSection;
                    mSectionPositions.add(i);
                    mPositionSectionMap.put(currentSection, mSectionList.size() - 1);
                }

            }
        }
        notifyDataSetChanged();
    }

    public void applyFilter(String filterString) {
        if (!filterString.equals(mFilterString)) {
            mFilterString = filterString;
            if (mFilterTask != null) {
                mFilterTask.cancel(true);
            }
            mFilterTask = new FilterTask();
            mFilterTask.execute(filterString);
        }

    }

    public void removeFilter() {
        if (!mFilterString.isEmpty()) {
            synchronized (mFilteredModelData) {
                mFilteredModelData.clear();
            }

            mFilterString = "";

            createSections();

        }
    }

    private class FilterTask extends AsyncTask<String, Object, Pair<List<T>, String>> {

        @Override
        protected Pair<List<T>, String> doInBackground(String... lists) {
            List<T> resultList = new ArrayList<>();

            String filterString = lists[0];

            for (T elem : mModelData) {
                // Check if task was cancelled from the outside.
                if (isCancelled()) {
                    resultList.clear();
                    return new Pair<>(resultList, filterString);
                }
                if (elem.getSectionTitle().toLowerCase().contains(filterString.toLowerCase())) {
                    resultList.add(elem);
                }
            }

            return new Pair<List<T>, String>(resultList, filterString);
        }

        protected void onPostExecute(Pair<List<T>, String> result) {
            if (!isCancelled() && mFilterString.equals(result.second)) {

                synchronized (mFilteredModelData) {
                    mFilteredModelData.clear();

                    mFilteredModelData.addAll(result.first);
                }
                setScrollSpeed(0);
                createSections();
            }
        }

    }
}
