/*
 * Copyright (C) 2016 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

public abstract class GenericSectionAdapter<T extends MPDGenericItem> extends ScrollSpeedAdapter implements SectionIndexer {
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

    private boolean mSectionsEnabled;
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

        mSectionsEnabled = true;
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

        if (mFilterString.isEmpty()) {
            // create sectionlist for fastscrolling
            if (mSectionsEnabled) {
                createSections();
            }

            notifyDataSetChanged();
        } else {
            // Refilter the new data
            if (mFilterTask != null) {
                mFilterTask.cancel(true);
            }
            mFilterTask = new FilterTask();
            mFilterTask.execute(mFilterString);
        }
    }

    /**
     * Looks up the position(index) of a given section(index)
     *
     * @param sectionIndex Section to get the ListView/GridView position for
     * @return The item position of this section start.
     */
    @Override
    public int getPositionForSection(int sectionIndex) {
        if (mSectionsEnabled) {
            return mSectionPositions.get(sectionIndex);
        } else {
            return 0;
        }
    }

    /**
     * Reverse lookup of a section for a given position
     *
     * @param pos Position to get the section for
     * @return Section (index) for the items position
     */
    @Override
    public int getSectionForPosition(int pos) {
        if (mSectionsEnabled) {
            String sectionTitle = ((MPDGenericItem) getItem(pos)).getSectionTitle();

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
        return 0;
    }

    /**
     * @return A list of all available sections
     */
    @Override
    public Object[] getSections() {
        if (mSectionsEnabled) {
            return mSectionList.toArray();
        }
        return null;
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

            if (mSectionsEnabled) {
                createSections();
            }
            notifyDataSetChanged();
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
                if (mSectionsEnabled) {
                    createSections();
                }
                notifyDataSetChanged();
            }
        }

    }

    /**
     * Allows to enable/disable the support for sections of this adapter.
     * In case of enabling it creates the sections.
     * In case of disabling it will clear the data.
     * @param enabled
     */
    public void enableSections(boolean enabled) {
        mSectionsEnabled = enabled;
        if (mSectionsEnabled) {
            createSections();
        } else {
            mSectionList.clear();
            mSectionPositions.clear();
            mPositionSectionMap.clear();
        }
        notifyDataSetChanged();
    }
}
