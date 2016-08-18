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

package andrompd.org.andrompd.application.adapters;


import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.SectionIndexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDGenericItem;

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

    protected boolean mFiltered;
    protected List<T> mFilteredModelData;

    /**
     * Variable to store the current scroll speed. Used for image view optimizations
     */
    protected int mScrollSpeed;


    public GenericSectionAdapter() {
        super();

        mSectionList = new ArrayList<>();
        mSectionPositions = new ArrayList<>();
        mPositionSectionMap = new HashMap<>();

        mModelData = new ArrayList<>();
    }

    /**
     * Swaps the model of this adapter. This sets the dataset on which the
     * adapter creates the GridItems. This should generally be safe to jll.
     * Clears old section data and model data and recreates sectionScrolling
     * data.
     *
     * @param data
     *            Actual model data
     */
    public void swapModel(List<T> data) {
        if (data == null) {
            mModelData.clear();
        } else {
            mModelData = data;
        }
        // create sectionlist for fastscrolling

        createSections();

        notifyDataSetChanged();
    }

    /**
     * Looks up the position(index) of a given section(index)
     * @param sectionIndex Section to get the ListView/GridView position for
     * @return The item position of this section start.
     */
    @Override
    public int getPositionForSection(int sectionIndex) {
        return mSectionPositions.get(sectionIndex);
    }

    /**
     * Reverse lookup of a section for a given position
     * @param pos Position to get the section for
     * @return Section (index) for the items position
     */
    @Override
    public int getSectionForPosition(int pos) {

        String sectionTitle = mModelData.get(pos).getSectionTitle();

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
     *
     * @return A list of all available sections
     */
    @Override
    public Object[] getSections() {
        return mSectionList.toArray();
    }

    /**
     *
     * @return The length of the modeldata of this adapter.
     */
    @Override
    public int getCount() {

        if ( mFiltered && mFilteredModelData != null) {
            return mFilteredModelData.size();
        } else {
            return mModelData.size();
        }
    }

    /**
     * Simple getter for the model data.
     * @param position Index of the item to get. No check for boundaries here.
     * @return The item at index position.
     */
    @Override
    public Object getItem(int position) {
        if ( mFiltered && mFilteredModelData != null) {
            return mFilteredModelData.get(position);
        } else {
            return mModelData.get(position);
        }
    }

    /**
     * Simple position->id mapping here.
     * @param position Position to get the id from
     * @return The id (position)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Sets the scrollspeed of the parent GridView. For smoother scrolling
     *
     * @param speed
     */
    public void setScrollSpeed(int speed) {
        mScrollSpeed = speed;
    }

    private void createSections() {
        mSectionList.clear();
        mSectionPositions.clear();
        mPositionSectionMap.clear();

        List<T> sectionList;

        if ( mFiltered && mFilteredModelData != null) {
            sectionList = mFilteredModelData;
        } else {
            sectionList = mModelData;
        }

        if (sectionList.size() > 0) {
            MPDGenericItem currentModel = sectionList.get(0);

            char lastSection;
            if ( currentModel.getSectionTitle().length() > 0 ) {
                lastSection = currentModel.getSectionTitle().toUpperCase().charAt(0);
            } else {
                lastSection = ' ';
            }

            mSectionList.add(String.valueOf(lastSection));
            mSectionPositions.add(0);
            mPositionSectionMap.put(lastSection, mSectionList.size() - 1);

            for (int i = 1; i < getCount(); i++) {

                currentModel = sectionList.get(i);

                char currentSection;
                if ( currentModel.getSectionTitle().length() > 0 ) {
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

    public void filterNames(String filterString) {
        Log.v(TAG,"Filter with string: " + filterString);
        mFiltered = true;

        mFilteredModelData = new ArrayList<>();

        for(T elem : mModelData) {
            if ( elem.getSectionTitle().toLowerCase().contains(filterString.toLowerCase()) ) {
                mFilteredModelData.add(elem);
            }
        }
        createSections();
    }

    public void removeFilter() {
        mFiltered = false;
        mFilteredModelData = null;

        createSections();
    }
}
